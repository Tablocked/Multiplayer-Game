package tablock.core;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.TimeStep;
import org.dyn4j.geometry.Polygon;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.ContactCollisionData;
import org.dyn4j.world.PhysicsWorld;
import org.dyn4j.world.World;
import org.dyn4j.world.listener.ContactListenerAdapter;
import org.dyn4j.world.listener.StepListenerAdapter;

public class Simulation extends World<SimulationBody>
{
	private Vector2 jumpVector = new Vector2(0,0);
	private Vector2 movementVector = new Vector2(0, 0);
	private double previousJumpAngle = -1;
	private double previousPlayerAngle = -1;
	private boolean canDoubleJump = false;
	private boolean onGround = false;
	private boolean isHoldingSpace = false;
	private double jumpStart = Double.MAX_VALUE;
	private final PlayerBody playerBody;
	
	public Simulation(PlayerBody playerBody)
	{
		this.playerBody = playerBody;

		addBody(playerBody);

		addStepListener(new StepListenerAdapter<>()
		{
			@Override
			public void begin(TimeStep step, PhysicsWorld<SimulationBody, ?> world)
			{
				jumpVector = new Vector2(0, 0);
				movementVector = new Vector2(0, 0);
				onGround = false;
			}

			@Override
			public void end(TimeStep step, PhysicsWorld<SimulationBody, ?> world)
			{
				jumpVector.normalize();
				movementVector.normalize();

				if(movementVector.x == 0 && movementVector.y == 0)
				{
					double angularVelocity = playerBody.getAngularVelocity();

					if(Input.LEFT.isActive() && angularVelocity < 1)
					{
						playerBody.applyTorque(Input.LEFT.getValue() * 200000);
					}

					if(Input.RIGHT.isActive() && angularVelocity > -1)
					{
						playerBody.applyTorque(Input.RIGHT.getValue() * -200000);
					}

					if(angularVelocity > 0)
					{
						playerBody.applyTorque(-50000);

						if(angularVelocity < 0)
						{
							playerBody.setAngularVelocity(0);
						}
					}

					if(angularVelocity < 0)
					{
						playerBody.applyTorque(50000);

						if(angularVelocity > 0)
						{
							playerBody.setAngularVelocity(0);
						}
					}
				}
				else
				{
					if(Input.LEFT.isActive())
					{
						playerBody.applyForce(movementVector.copy().multiply(Input.LEFT.getValue() * 20000));
					}

					if(Input.RIGHT.isActive())
					{
						playerBody.applyForce(movementVector.copy().multiply(Input.RIGHT.getValue() * -20000));
					}
				}

				if(onGround)
				{
					double jumpTime = System.nanoTime() - jumpStart;

					if((!Input.JUMP.isActive() || jumpTime > 1e9) && jumpTime > 25e7)
					{
						playerBody.applyForce(jumpVector.copy().multiply((0.0107 * jumpTime) + 2333333));

						canDoubleJump = true;
						previousJumpAngle = jumpVector.getDirection();
						previousPlayerAngle = playerBody.getTransform().getRotationAngle();
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
					double playerAngleDifference = previousPlayerAngle - playerBody.getTransform().getRotationAngle();

					playerBody.clearForce();
					playerBody.setLinearVelocity(0, 0);
					playerBody.applyForce(new Vector2(previousJumpAngle - playerAngleDifference).multiply(10000000));

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
			public void collision(ContactCollisionData<SimulationBody> collision)
			{
				Body body1 = collision.getBody1();
				Body body2 = collision.getBody2();
				Platform platform = null;

				if(body1 instanceof Platform)
					platform = (Platform) body1;

				if(body2 instanceof Platform)
					platform = (Platform) body2;

				if(body1 instanceof PlayerBody || body2 instanceof PlayerBody && platform != null)
				{
					canDoubleJump = false;

					for(int i = 0; i < playerBody.getVertices().length; i++)
					{
						Vector2 playerVertex1 = playerBody.getVertices()[i];
						Vector2 playerVertex2 = playerBody.getVertices()[(i + 1) % playerBody.getVertices().length];

						for(int j = 0; j < platform.getVertices().length; j++)
						{
							Vector2 platformVertex1 = platform.getVertices()[j];
							Vector2 platformVertex2 = platform.getVertices()[(j + 1) % platform.getVertices().length];
							Vector2 normal = ((Polygon) platform.getFixture(0).getShape()).getNormals()[j];

							if(onLineSegment(platformVertex1, platformVertex2, playerVertex1) && onLineSegment(platformVertex1, platformVertex2, playerVertex2))
							{
								jumpVector.add(normal);
								onGround = true;
							}

							if(onLine(platformVertex1, platformVertex2, playerVertex1) && onLine(platformVertex1, platformVertex2, playerVertex1))
							{
								movementVector.add(normal.copy().rotate(Math.PI / 2));
							}
						}
					}
				}
			}
		});
	}

	private Vector2 projectOnLine(Vector2 startPoint, Vector2 endPoint, Vector2 point)
	{
		Vector2 projectionLine = endPoint.copy().subtract(startPoint);
		Vector2 projectionPoint = point.copy().subtract(startPoint);

		return projectionPoint.project(projectionLine).add(startPoint);
	}
	private boolean onLine(Vector2 startPoint, Vector2 endPoint, Vector2 point)
	{
		return projectOnLine(startPoint, endPoint, point).distance(point) < 0.1;
	}
	private boolean onLineSegment(Vector2 startPoint, Vector2 endPoint, Vector2 point)
	{
		Vector2 projection = projectOnLine(startPoint, endPoint, point);
		double minX = Math.min(startPoint.x, endPoint.x);
		double maxX = Math.max(startPoint.x, endPoint.x);
		double minY = Math.min(startPoint.y, endPoint.y);
		double maxY = Math.max(startPoint.y, endPoint.y);

		return projection.x >= minX && projection.x <= maxX && projection.y >= minY && projection.y <= maxY && projection.distance(point) < 0.1;
	}

	public Vector2 getPlayerCenter()
	{
		return playerBody.getWorldCenter();
	}

	public Vector2[] getPlayerVertices()
	{
		if(jumpStart != Double.MAX_VALUE)
		{
			Vector2[] vertices = playerBody.getVertices();
			Vector2 localJumpVector = playerBody.getLocalVector(jumpVector);
			Vector2[] components = {localJumpVector.getXComponent(), localJumpVector.getYComponent()};

			for(Vector2 component : components)
			{
				int direction = (int) Math.round((component.getDirection() + (Math.PI * 2)) / (Math.PI / 2));
				int index1 = (direction + 1) % vertices.length;
				int index2 = (direction + 2) % vertices.length;
				double jumpTime = System.nanoTime() - jumpStart;
				Vector2 shrinkVector = component.copy().rotate(playerBody.getTransform().getRotationAngle()).multiply(4e-8 * jumpTime);

				vertices[index1].subtract(shrinkVector);
				vertices[index2].subtract(shrinkVector);
			}

			return vertices;
		}

		return playerBody.getVertices();
	}

	public Vector2[] getDoubleJumpVertices()
	{
		if(canDoubleJump)
		{
			double playerAngleDifference = previousPlayerAngle - playerBody.getTransform().getRotationAngle();
			Vector2 localJumpVector = playerBody.getLocalVector(new Vector2(previousJumpAngle - playerAngleDifference));
			Vector2[] doubleJumpVertices;

			if(Math.round(localJumpVector.x) != 0 && Math.round(localJumpVector.y) != 0)
			{
				Vector2[] effectTemplate = new Vector2[]{new Vector2(-25, -25), new Vector2(25, -25), new Vector2(25, 25), new Vector2(0, 25), new Vector2(0, 0), new Vector2(-25, 0)};

				doubleJumpVertices = prepareEffectTemplate(effectTemplate, Math.PI / 4);
			}
			else
			{
				Vector2[] effectTemplate = new Vector2[]{new Vector2(-25, -25), new Vector2(25, -25), new Vector2(25, 25), new Vector2(0, 0), new Vector2(-25, 25)};

				doubleJumpVertices = prepareEffectTemplate(effectTemplate, Math.PI / 2);
			}

			return doubleJumpVertices;
		}

		return null;
	}

	private Vector2[] prepareEffectTemplate(Vector2[] effectTemplate, double offsetAngle)
	{
		double playerAngleDifference = previousPlayerAngle - playerBody.getTransform().getRotationAngle();
		double doubleJumpAngle = previousJumpAngle - playerAngleDifference;
		Vector2 rotationOffset = new Vector2(doubleJumpAngle + offsetAngle);

		for(int i = 0; i < effectTemplate.length; i++)
		{
			effectTemplate[i].rotate(playerBody.getLocalVector(rotationOffset).getDirection());
			effectTemplate[i] = playerBody.getWorldPoint(effectTemplate[i]);
		}

		return effectTemplate;
	}
}