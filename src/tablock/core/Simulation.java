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
	public final Player player = new Player();
	private Vector2 jumpVector = new Vector2(0,0);
	private Vector2 previousJumpVector = new Vector2(0,0);
	private Vector2 movementVector = new Vector2(0, 0);
	private double localDoubleJumpAngle;
	private boolean canDoubleJump;
	private boolean onGround;
	private boolean touchingGround;
	private double timeDuringJumpStart = Double.MAX_VALUE;
	private final Body playerBody = new Body();
	
	public Simulation(double startX, double startY)
	{
		BodyFixture bodyFixture = new BodyFixture(Geometry.createRectangle(50, 50));

		bodyFixture.setFriction(0);

		playerBody.addFixture(bodyFixture);
		playerBody.translate(startX, startY);
		playerBody.setMass(MassType.NORMAL);

		addBody(playerBody);

		addContactListener(new ContactListenerAdapter<>()
		{
			@Override
			public void collision(ContactCollisionData<Body> collision)
			{
				Body body1 = collision.getBody1();
				Body body2 = collision.getBody2();
				Fixture objectFixture = body1 == playerBody ? collision.getFixture2() : collision.getFixture1();
				Vector2[] playerVertices = computeBodyVertices(playerBody);
				Vector2[] objectVertices = computeFixtureVertices(body1 == playerBody ? body2 : body1, objectFixture);

				if(body1 == playerBody || body2 == playerBody)
				{
					canDoubleJump = false;
					touchingGround = true;

					for(int i = 0; i < playerVertices.length; i++)
					{
						Vector2 playerVertex1 = playerVertices[i];
						Vector2 playerVertex2 = playerVertices[(i + 1) % playerVertices.length];

						for(int j = 0; j < objectVertices.length; j++)
						{
							Vector2 platformVertex1 = objectVertices[j];
							Vector2 platformVertex2 = objectVertices[(j + 1) % objectVertices.length];
							Vector2 projection1 = VectorMath.projectPointOntoLine(platformVertex1, platformVertex2, playerVertex1);
							Vector2 projection2 = VectorMath.projectPointOntoLine(platformVertex1, platformVertex2, playerVertex2);

							if(projection1.distance(playerVertex1) < 0.5 && projection2.distance(playerVertex2) < 0.5)
							{
								Vector2 vector = projection2.copy().subtract(projection1);

								movementVector.add(vector);

								if(VectorMath.isProjectionOnLineSegment(projection1, platformVertex1, platformVertex2) && VectorMath.isProjectionOnLineSegment(projection2, platformVertex1, platformVertex2))
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

		addStepListener(new StepListenerAdapter<>()
		{
			@Override
			public void begin(TimeStep step, PhysicsWorld<Body, ?> world)
			{
				jumpVector = new Vector2(0, 0);
				movementVector = new Vector2(0, 0);
				onGround = false;
				touchingGround = false;
			}

			@Override
			public void end(TimeStep step, PhysicsWorld<Body, ?> world)
			{
				jumpVector.normalize();
				movementVector.normalize();

				Vector2 playerCenter = playerBody.getWorldCenter();
				Vector2 localJumpVector = playerBody.getLocalVector(jumpVector);
				boolean straightJumpVector = (localJumpVector.x > -0.05 && localJumpVector.x < 0.05) || (localJumpVector.y > -0.05 && localJumpVector.y < 0.05);

				player.x = playerCenter.x;
				player.y = playerCenter.y;
				player.rotationAngle -= playerBody.getTransform().getRotation().toVector().getAngleBetween(playerBody.getPreviousTransform().getRotation().toVector());

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

					if(!touchingGround)
					{
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
				}
				else
				{
					Vector2 linearVelocity = playerBody.getLinearVelocity().copy();

					if(Input.LEFT.isActive())
						playerBody.applyForce(movementVector.copy().multiply(Input.LEFT.getValue() * -50000));

					if(Input.RIGHT.isActive())
						playerBody.applyForce(movementVector.copy().multiply(Input.RIGHT.getValue() * 50000));

					playerBody.applyForce(linearVelocity.multiply(-1000));

					if(!jumpVector.equals(0, 0) && !straightJumpVector)
						playerBody.setLinearVelocity(0, 0);
				}

				if(!onGround || Math.abs(jumpVector.getAngleBetween(previousJumpVector)) > 0.01)
				{
					timeDuringJumpStart = Double.MAX_VALUE;
					player.jumpProgress = -128;
				}

				if(onGround)
				{
					double localJumpVectorAngle = localJumpVector.getDirection();

					if(Input.JUMP.wasJustActivated())
						timeDuringJumpStart = System.nanoTime();

					if(timeDuringJumpStart != Double.MAX_VALUE)
					{
						double floatingPointDirection = localJumpVectorAngle / (Math.PI / 2);

						player.animationDirection = (byte) (straightJumpVector ? Math.round(floatingPointDirection) : Math.floor(floatingPointDirection));
						player.jumpProgress = (byte) Math.min(VectorMath.computeLinearEquation(0, -128, straightJumpVector ? 5 * 1e8 : 1e9, 127, System.nanoTime() - timeDuringJumpStart), 127);
					}

					if((!Input.JUMP.isActive() || player.jumpProgress == 127) && player.jumpProgress > -128)
					{
						double jumpForce = (800 * (((int) player.jumpProgress) + 128)) + 10000;

						playerBody.applyImpulse(jumpVector.copy().multiply(straightJumpVector ? jumpForce : jumpForce * 2));

						canDoubleJump = true;
						player.animationType = (byte) (straightJumpVector ? PlayerAnimationType.STRAIGHT_DOUBLE_JUMP : PlayerAnimationType.DIAGONAL_DOUBLE_JUMP).ordinal();
						localDoubleJumpAngle = localJumpVectorAngle;
					}
				}
				else if(canDoubleJump && Input.JUMP.wasJustActivated())
				{
					playerBody.clearForce();
					playerBody.setLinearVelocity(0, 0);
					playerBody.applyImpulse(new Vector2(localDoubleJumpAngle + player.rotationAngle).multiply(player.animationType == PlayerAnimationType.DIAGONAL_DOUBLE_JUMP.ordinal() ? 400000 : 200000));

					canDoubleJump = false;
				}

				previousJumpVector = jumpVector.copy();

				if(!canDoubleJump)
					if(timeDuringJumpStart != Double.MAX_VALUE)
						player.animationType = (byte) (straightJumpVector ? PlayerAnimationType.STRAIGHT_JUMP : PlayerAnimationType.DIAGONAL_JUMP).ordinal();
					else
						player.animationType = (byte) PlayerAnimationType.NONE.ordinal();
			}
		});
	}

	private Vector2[] computeFixtureVertices(Body body, Fixture fixture)
	{
		Polygon polygon = (Polygon) fixture.getShape();
		int sides = polygon.getVertices().length;
		Vector2[] vertices = new Vector2[sides];

		for(int index = 0; index < sides; index++)
			vertices[index] = body.getWorldPoint(polygon.getVertices()[index]);

		return vertices;
	}

	private Vector2[] computeBodyVertices(Body body)
	{
		return computeFixtureVertices(body, body.getFixture(0));
	}

	public void resetPlayer()
	{
		player.x = 0;
		player.y = 0;
		player.rotationAngle = 0;
		player.jumpProgress = 0;
		player.animationType = 0;
		player.animationDirection = 0;
		player.reset = true;

		playerBody.translateToOrigin();
		playerBody.setLinearVelocity(0, 0);
		playerBody.setAngularVelocity(0);
		playerBody.getTransform().setRotation(0);
		playerBody.setAtRest(false);

		canDoubleJump = false;
	}

	public enum PlayerAnimationType
	{
		NONE,
		STRAIGHT_JUMP,
		DIAGONAL_JUMP,
		STRAIGHT_DOUBLE_JUMP,
		DIAGONAL_DOUBLE_JUMP
	}
}