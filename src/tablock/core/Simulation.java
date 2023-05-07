package tablock.core;

import org.dyn4j.collision.Fixture;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.TimeStep;
import org.dyn4j.geometry.Polygon;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.ContactCollisionData;
import org.dyn4j.world.PhysicsWorld;
import org.dyn4j.world.World;
import org.dyn4j.world.listener.ContactListenerAdapter;
import org.dyn4j.world.listener.StepListenerAdapter;

public class Simulation extends World<Body>
{
	private Vector2 jumpVector = new Vector2(0,0);
	private Vector2 movementVector = new Vector2(0, 0);
	private double previousJumpAngle = -1;
	private double previousPlayerAngle = -1;
	private boolean canDoubleJump = false;
	private boolean onGround = false;
	private boolean isHoldingSpace = false;
	private double jumpStart = Double.MAX_VALUE;
	private final Body player;
	
	public Simulation(Body player)
	{
		this.player = player;

		addBody(player);

		addStepListener(new StepListenerAdapter<>()
		{
			@Override
			public void begin(TimeStep step, PhysicsWorld<Body, ?> world)
			{
				jumpVector = new Vector2(0, 0);
				movementVector = new Vector2(0, 0);
				onGround = false;
			}

			@Override
			public void end(TimeStep step, PhysicsWorld<Body, ?> world)
			{
				jumpVector.normalize();
				movementVector.normalize();

				if(movementVector.x == 0 && movementVector.y == 0)
				{
					double angularVelocity = Simulation.this.player.getAngularVelocity();

					if(Input.LEFT.isActive() && angularVelocity < 1)
					{
						Simulation.this.player.applyTorque(Input.LEFT.getValue() * 200000);
					}

					if(Input.RIGHT.isActive() && angularVelocity > -1)
					{
						Simulation.this.player.applyTorque(Input.RIGHT.getValue() * -200000);
					}

					if(angularVelocity > 0)
					{
						Simulation.this.player.applyTorque(-50000);

						if(angularVelocity < 0)
						{
							Simulation.this.player.setAngularVelocity(0);
						}
					}

					if(angularVelocity < 0)
					{
						Simulation.this.player.applyTorque(50000);

						if(angularVelocity > 0)
						{
							Simulation.this.player.setAngularVelocity(0);
						}
					}
				}
				else
				{
					if(Input.LEFT.isActive())
					{
						Simulation.this.player.applyForce(movementVector.copy().multiply(Input.LEFT.getValue() * 20000));
					}

					if(Input.RIGHT.isActive())
					{
						Simulation.this.player.applyForce(movementVector.copy().multiply(Input.RIGHT.getValue() * -20000));
					}
				}

				if(onGround)
				{
					double jumpTime = System.nanoTime() - jumpStart;

					if((!Input.JUMP.isActive() || jumpTime > 1e9) && jumpTime > 25e7)
					{
						Simulation.this.player.applyForce(jumpVector.copy().multiply((0.0107 * jumpTime) + 2333333));

						canDoubleJump = true;
						previousJumpAngle = jumpVector.getDirection();
						previousPlayerAngle = Simulation.this.player.getTransform().getRotationAngle();
						jumpStart = Double.MAX_VALUE;
					}

					if(Input.JUMP.isActive())
					{
						if(!isHoldingSpace)
							jumpStart = System.nanoTime();
					}
					else
					{
						jumpStart = Double.MAX_VALUE;
					}
				}
				else if(canDoubleJump && !isHoldingSpace && Input.JUMP.isActive())
				{
					double playerAngleDifference = previousPlayerAngle - Simulation.this.player.getTransform().getRotationAngle();

					Simulation.this.player.clearForce();
					Simulation.this.player.setLinearVelocity(0, 0);
					Simulation.this.player.applyForce(new Vector2(previousJumpAngle - playerAngleDifference).multiply(10000000));

					canDoubleJump = false;
				}

				if(!onGround)
				{
					jumpStart = Double.MAX_VALUE;
				}

				isHoldingSpace = Input.JUMP.isActive();
			}
		});

		addContactListener(new ContactListenerAdapter<>()
		{
			@Override
			public void collision(ContactCollisionData<Body> collision)
			{
				Body body1 = collision.getBody1();
				Body body2 = collision.getBody2();
				Fixture fixture1 = collision.getFixture1();
				Fixture fixture2 = collision.getFixture2();
				Fixture objectFixture = body1 == player ? fixture2 : fixture1;
				Vector2[] playerVertices = getBodyVertices(player);
				Vector2[] objectVertices = getFixtureVertices(body1 == player ? body2 : body1, objectFixture);

				if(body1 == player || body2 == player)
				{
					canDoubleJump = false;

					for(int i = 0; i < playerVertices.length; i++)
					{
						Vector2 playerVertex1 = playerVertices[i];
						Vector2 playerVertex2 = playerVertices[(i + 1) % playerVertices.length];

						for(int j = 0; j < objectVertices.length; j++)
						{
							Vector2 platformVertex1 = objectVertices[j];
							Vector2 platformVertex2 = objectVertices[(j + 1) % objectVertices.length];
							Vector2 projection1 = VectorUtilities.projectPointOntoLine(platformVertex1, platformVertex2, playerVertex1);
							Vector2 projection2 = VectorUtilities.projectPointOntoLine(platformVertex1, platformVertex2, playerVertex2);

							if(projection1.distance(playerVertex1) < 0.1 && projection2.distance(playerVertex2) < 0.1)
							{
								Vector2 normal = ((Polygon) objectFixture.getShape()).getNormals()[j];

								movementVector.add(normal.copy().rotate(Math.PI / 2));

								if(VectorUtilities.isProjectionOnLineSegment(projection1, platformVertex1, platformVertex2) && VectorUtilities.isProjectionOnLineSegment(projection2, platformVertex1, platformVertex2))
								{
									jumpVector.add(normal);
									onGround = true;
								}
							}
						}
					}
				}
			}
		});
	}

	private Vector2[] getFixtureVertices(Body body, Fixture fixture)
	{
		Polygon polygon = (Polygon) fixture.getShape();
		int sides = polygon.getVertices().length;
		Vector2[] vertices = new Vector2[sides];

		for(int index = 0; index < sides; index++)
		{
			vertices[index] = body.getWorldPoint(polygon.getVertices()[index]);
		}

		return vertices;
	}

	private Vector2[] getBodyVertices(Body body)
	{
		return getFixtureVertices(body, body.getFixture(0));
	}

	public Vector2 getPlayerCenter()
	{
		return player.getWorldCenter();
	}

	private Vector2[] transformVertexTemplate(Vector2[] vertexTemplate, double offsetAngle)
	{
		double playerAngleDifference = previousPlayerAngle - player.getTransform().getRotationAngle();
		double doubleJumpAngle = previousJumpAngle - playerAngleDifference;
		Vector2 rotationOffset = new Vector2(doubleJumpAngle + offsetAngle);

		for(int i = 0; i < vertexTemplate.length; i++)
		{
			vertexTemplate[i].rotate(player.getLocalVector(rotationOffset).getDirection());
			vertexTemplate[i] = player.getWorldPoint(vertexTemplate[i]);
		}

		return vertexTemplate;
	}

	public Vector2[] getPlayerVertices()
	{
		Vector2[] playerVertices = getBodyVertices(player);

		if(canDoubleJump)
		{
			double playerAngleDifference = previousPlayerAngle - player.getTransform().getRotationAngle();
			Vector2 localJumpVector = player.getLocalVector(new Vector2(previousJumpAngle - playerAngleDifference));

			if(Math.round(localJumpVector.x) != 0 && Math.round(localJumpVector.y) != 0)
			{
				Vector2[] vertexTemplate = new Vector2[]{new Vector2(-25, -25), new Vector2(25, -25), new Vector2(25, 25), new Vector2(-6.25, 25), new Vector2(-12.5, 12.5), new Vector2(-25, 6.25)};

				playerVertices = transformVertexTemplate(vertexTemplate, Math.PI / 4);
			}
			else
			{
				Vector2[] vertexTemplate = new Vector2[]{new Vector2(-25, -25), new Vector2(25, -25), new Vector2(25, 25), new Vector2(0, 12.5), new Vector2(-25, 25)};

				playerVertices = transformVertexTemplate(vertexTemplate, Math.PI / 2);
			}
		}
		else if(jumpStart != Double.MAX_VALUE)
		{
			Vector2 localJumpVector = player.getLocalVector(jumpVector);
			Vector2[] components = {localJumpVector.getXComponent(), localJumpVector.getYComponent()};

			for(Vector2 component : components)
			{
				int direction = (int) Math.round((component.getDirection() + (Math.PI * 2)) / (Math.PI / 2));
				int index1 = (direction + 1) % playerVertices.length;
				int index2 = (direction + 2) % playerVertices.length;
				double jumpTime = System.nanoTime() - jumpStart;
				Vector2 shrinkVector = component.copy().rotate(player.getTransform().getRotationAngle()).multiply(4e-8 * jumpTime);

				playerVertices[index1].subtract(shrinkVector);
				playerVertices[index2].subtract(shrinkVector);
			}
		}

		return playerVertices;
	}
}