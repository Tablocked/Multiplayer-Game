package tablock.core;

import org.dyn4j.collision.Fixture;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.TimeStep;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
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
	private Vector2 previousJumpVector = new Vector2(0,0);
	private Vector2 movementVector = new Vector2(0, 0);
	private double previousJumpAngle = -1;
	private double previousPlayerAngle = -1;
	private boolean canDoubleJump;
	private boolean onGround;
	private double timeDuringJumpStart = Double.MAX_VALUE;
	private byte playerAnimationDirection;
	private byte playerJumpProgress = -128;
	private PlayerAnimationType playerAnimationType = PlayerAnimationType.NONE;
	private final Body playerBody = new Body();
	private final Vector2[] straightDoubleJumpVertices = {new Vector2(25, -25), new Vector2(-25, -25), new Vector2(-12.5, 0), new Vector2(-25, 25), new Vector2(25, 25)};
	private final Vector2[] diagonalDoubleJumpVertices = {new Vector2(-25, 25), new Vector2(25, 25), new Vector2(25, -25), new Vector2(-6.25, -25), new Vector2(-12.5, -12.5), new Vector2(-25, -6.25)};
	
	public Simulation(double startX, double startY)
	{
		BodyFixture bodyFixture = new BodyFixture(Geometry.createRectangle(50, 50));

		bodyFixture.setFriction(0);

		playerBody.addFixture(bodyFixture);
		playerBody.translate(startX, startY);
		playerBody.setMass(MassType.NORMAL);

		addBody(playerBody);

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
					double angularVelocity = playerBody.getAngularVelocity();

					if(Input.LEFT.isActive() && angularVelocity < 1)
					{
						playerBody.applyTorque(Input.LEFT.getValue() * 400000);
					}

					if(Input.RIGHT.isActive() && angularVelocity > -1)
					{
						playerBody.applyTorque(Input.RIGHT.getValue() * -400000);
					}

					if(angularVelocity > 0)
					{
						playerBody.applyTorque(-200000);

						if(playerBody.getAngularVelocity() < 0)
							playerBody.setAngularVelocity(0);
					}

					if(angularVelocity < 0)
					{
						playerBody.applyTorque(200000);

						if(playerBody.getAngularVelocity() > 0)
							playerBody.setAngularVelocity(0);
					}
				}
				else
				{
					Vector2 linearVelocity = playerBody.getLinearVelocity().copy();

					if(Input.LEFT.isActive())
						playerBody.applyForce(movementVector.copy().multiply(Input.LEFT.getValue() * -50000));

					if(Input.RIGHT.isActive())
						playerBody.applyForce(movementVector.copy().multiply(Input.RIGHT.getValue() * 50000));

					playerBody.applyForce(linearVelocity.multiply(-1000));
				}

				Vector2 localJumpVector = playerBody.getLocalVector(jumpVector);
				boolean straightJumpVector = (localJumpVector.x > -0.01 && localJumpVector.x < 0.01) || (localJumpVector.y > -0.01 && localJumpVector.y < 0.01);

				if(!onGround || Math.abs(jumpVector.getAngleBetween(previousJumpVector)) > 0.01)
				{
					timeDuringJumpStart = Double.MAX_VALUE;
					playerJumpProgress = -128;
				}

				if(onGround)
				{
					if(Input.JUMP.wasJustActivated())
						timeDuringJumpStart = System.nanoTime();

					if(timeDuringJumpStart != Double.MAX_VALUE)
					{
						double floatingPointDirection = localJumpVector.getDirection() / (Math.PI / 2);

						playerAnimationDirection = (byte) (straightJumpVector ? Math.round(floatingPointDirection) : Math.floor(floatingPointDirection));
						playerJumpProgress = (byte) Math.min(applyLinearRate(0, -128, straightJumpVector ? 5 * 1e8 : 1e9, 127, System.nanoTime() - timeDuringJumpStart), 127);
					}

					if((!Input.JUMP.isActive() || playerJumpProgress == 127) && playerJumpProgress > -128)
					{
						double jumpForce = (800 * (((int) playerJumpProgress) + 128)) + 10000;

						playerBody.applyImpulse(jumpVector.copy().multiply(straightJumpVector ? jumpForce : jumpForce * 2));

						canDoubleJump = true;
						playerAnimationType = straightJumpVector ? PlayerAnimationType.STRAIGHT_DOUBLE_JUMP : PlayerAnimationType.DIAGONAL_DOUBLE_JUMP;
						previousJumpAngle = jumpVector.getDirection();
						previousPlayerAngle = computePlayerRotationAngle();
					}
				}
				else if(canDoubleJump && Input.JUMP.wasJustActivated())
				{
					double playerAngleDifference = previousPlayerAngle - computePlayerRotationAngle();

					playerBody.clearForce();
					playerBody.setLinearVelocity(0, 0);
					playerBody.applyImpulse(new Vector2(previousJumpAngle - playerAngleDifference).multiply(200000));

					canDoubleJump = false;
				}

				previousJumpVector = jumpVector.copy();

				if(!canDoubleJump)
					if(timeDuringJumpStart != Double.MAX_VALUE)
						playerAnimationType = straightJumpVector ? PlayerAnimationType.STRAIGHT_JUMP : PlayerAnimationType.DIAGONAL_JUMP;
					else
						playerAnimationType = PlayerAnimationType.NONE;
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
				Fixture objectFixture = body1 == playerBody ? fixture2 : fixture1;
				Vector2[] playerVertices = getBodyVertices(playerBody);
				Vector2[] objectVertices = getFixtureVertices(body1 == playerBody ? body2 : body1, objectFixture);

				if(body1 == playerBody || body2 == playerBody)
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

							if(projection1.distance(playerVertex1) < 0.5 && projection2.distance(playerVertex2) < 0.5)
							{
								Vector2 vector = projection2.copy().subtract(projection1);

								movementVector.add(vector);

								if(VectorUtilities.isProjectionOnLineSegment(projection1, platformVertex1, platformVertex2) && VectorUtilities.isProjectionOnLineSegment(projection2, platformVertex1, platformVertex2))
								{
									jumpVector.add(vector.copy().rotate(Math.PI / 2));
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
			vertices[index] = body.getWorldPoint(polygon.getVertices()[index]);

		return vertices;
	}

	private Vector2[] getBodyVertices(Body body)
	{
		return getFixtureVertices(body, body.getFixture(0));
	}

	private double applyLinearRate(double startX, double startY, double endX, double endY, double x)
	{
		double rate = (startY - endY) / (startX - endX);

		return (rate * x) + (startY - (rate * startX));
	}

	private Vector2[] mapVerticesOntoPlayer(Vector2[] vertices)
	{
		Vector2[] mappedVertices = new Vector2[vertices.length];
		Vector2 playerCenter = computePlayerCenter();
		double angle = computePlayerRotationAngle() + (playerAnimationDirection * (Math.PI / 2));

		for(int i = 0; i < vertices.length; i++)
			mappedVertices[i] = vertices[i].copy().rotate(angle).add(playerCenter);

		return mappedVertices;
	}

	public Vector2 computePlayerCenter()
	{
		return playerBody.getWorldCenter();
	}

	public Vector2[] getPlayerVertices()
	{
		switch(playerAnimationType)
		{
			case NONE: return getBodyVertices(playerBody);

			case STRAIGHT_JUMP:
			{
				double length = applyLinearRate(-128, 25, 127, -10, playerJumpProgress);

				return mapVerticesOntoPlayer(new Vector2[]{new Vector2(-25, 25), new Vector2(-25, -25), new Vector2(length, -25), new Vector2(length, 25)});
			}

			case DIAGONAL_JUMP:
			{
				double length = applyLinearRate(-128, 25, 127, 0, playerJumpProgress);

				return mapVerticesOntoPlayer(new Vector2[]{new Vector2(-25, -25), new Vector2(length, -25), new Vector2(length, length), new Vector2(-25, length)});
			}

			case STRAIGHT_DOUBLE_JUMP: return mapVerticesOntoPlayer(straightDoubleJumpVertices);
			case DIAGONAL_DOUBLE_JUMP: return mapVerticesOntoPlayer(diagonalDoubleJumpVertices);
		}

		return new Vector2[0];
	}

	public double computePlayerRotationAngle()
	{
		return playerBody.getTransform().getRotationAngle();
	}

	public void resetPlayer()
	{
		playerBody.translateToOrigin();
		playerBody.setLinearVelocity(0, 0);
		playerBody.setAngularVelocity(0);
		playerBody.getTransform().setRotation(0);
		playerBody.setAtRest(false);

		canDoubleJump = false;
	}

	private enum PlayerAnimationType
	{
		NONE,
		STRAIGHT_JUMP,
		DIAGONAL_JUMP,
		STRAIGHT_DOUBLE_JUMP,
		DIAGONAL_DOUBLE_JUMP
	}
}