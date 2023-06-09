package tablock.gameState;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.*;
import org.dyn4j.geometry.decompose.SweepLine;
import tablock.core.Input;
import tablock.core.Simulation;
import tablock.core.TargetedPlayer;
import tablock.core.VectorMath;
import tablock.level.Level;
import tablock.level.Platform;
import tablock.network.Client;
import tablock.network.ClientPacket;
import tablock.userInterface.AttentionMessage;
import tablock.userInterface.ButtonStrip;
import tablock.userInterface.TextButton;

import java.util.List;

public class PlayState extends GameState
{
    private long frameTime = System.nanoTime();
    private boolean paused;
    private AttentionMessage disconnectionMessage;
    private final Simulation simulation;
    private final CreateState createState;
    private final Level level;
    private final Vector2[] idlePlayerVertices = {new Vector2(-25, -25), new Vector2(25, -25), new Vector2(25, 25), new Vector2(-25, 25)};
    private final Vector2[] straightDoubleJumpVertices = {new Vector2(25, -25), new Vector2(-25, -25), new Vector2(-12.5, 0), new Vector2(-25, 25), new Vector2(25, 25)};
    private final Vector2[] diagonalDoubleJumpVertices = {new Vector2(-25, 25), new Vector2(25, 25), new Vector2(25, -25), new Vector2(-6.25, -25), new Vector2(-12.5, -12.5), new Vector2(-25, -6.25)};

    private final ButtonStrip pauseButtons = new ButtonStrip
    (
        ButtonStrip.Orientation.VERTICAL,

        new TextButton(960, 440, "Resume", 100, () -> paused = false),
        new TextButton(960, 640, "Disconnect", 100, this::leaveHostAndQuitToTitleScreen)
    );

    public PlayState(Level level)
    {
        simulation = new Simulation(0, 0);
        createState = null;
        this.level = level;

        CLIENT.player = simulation.player;

        addObjectsToSimulation();
    }

    public PlayState(CreateState createState, double startX, double startY)
    {
        simulation = new Simulation(startX, -startY);
        this.createState = createState;
        this.level = createState.getLevel();

        addObjectsToSimulation();
    }

    private void addObjectsToSimulation()
    {
        SweepLine sweepLine = new SweepLine();

        for(Platform object : level.getObjects())
        {
            Body body = new Body();
            Vector2[] vertices = object.convertToVectorArray();

            if(vertices.length > 3)
            {
                List<Triangle> triangles = sweepLine.triangulate(vertices);

                for(Triangle triangle : triangles)
                    body.addFixture(triangle);
            }
            else
            {
                if(Geometry.getWinding(vertices) < 0)
                    Geometry.reverseWinding(vertices);

                body.addFixture(new Polygon(vertices));
            }

            body.setMass(MassType.INFINITE);

            simulation.addBody(body);
        }
    }

    private void leaveHostAndQuitToTitleScreen()
    {
        CLIENT.player = null;

        CLIENT.send(ClientPacket.LEAVE_HOST);
        CLIENT.switchGameState(new TitleState());
    }

    private Vector2[] mapVerticesOntoPlayer(double x, double y, double rotationAngle, byte animationDirection, Vector2[] vertices)
    {
        Vector2[] mappedVertices = new Vector2[vertices.length];
        double angle = rotationAngle + (animationDirection * (Math.PI / 2));

        for(int i = 0; i < vertices.length; i++)
            mappedVertices[i] = vertices[i].copy().rotate(angle).add(x, y);

        return mappedVertices;
    }

    private void drawPlayer(double x, double y, double rotationAngle, double jumpProgress, byte animationDirection, byte animationType, double offsetX, double offsetY, GraphicsContext gc)
    {
        Vector2[] playerVertices = null;

        switch(Simulation.PlayerAnimationType.values()[animationType])
        {
            case NONE -> playerVertices = mapVerticesOntoPlayer(x, y, rotationAngle, animationDirection, idlePlayerVertices);

            case STRAIGHT_JUMP ->
            {
                double length = VectorMath.computeLinearEquation(-128, 25, 127, -10, jumpProgress);

                playerVertices = mapVerticesOntoPlayer(x, y, rotationAngle, animationDirection, new Vector2[]{new Vector2(-25, 25), new Vector2(-25, -25), new Vector2(length, -25), new Vector2(length, 25)});
            }

            case DIAGONAL_JUMP ->
            {
                double length = VectorMath.computeLinearEquation(-128, 25, 127, 0, jumpProgress);

                playerVertices = mapVerticesOntoPlayer(x, y, rotationAngle, animationDirection, new Vector2[]{new Vector2(-25, -25), new Vector2(length, -25), new Vector2(length, length), new Vector2(-25, length)});
            }

            case STRAIGHT_DOUBLE_JUMP -> playerVertices = mapVerticesOntoPlayer(x, y, rotationAngle, animationDirection, straightDoubleJumpVertices);
            case DIAGONAL_DOUBLE_JUMP -> playerVertices = mapVerticesOntoPlayer(x, y, rotationAngle, animationDirection, diagonalDoubleJumpVertices);
        }

        if(playerVertices != null)
        {
            gc.beginPath();

            for(Vector2 vertex : playerVertices)
                gc.lineTo(vertex.x + offsetX, -vertex.y + offsetY);

            gc.fill();
            gc.closePath();
        }
    }

    @Override
    public void renderNextFrame(GraphicsContext gc)
    {
        double elapsedTime = (System.nanoTime() - frameTime) / 1e9;

        frameTime = System.nanoTime();

        if(disconnectionMessage == null)
            if(Input.PAUSE.wasJustActivated())
            {
                if(createState == null)
                {
                    paused = !paused;

                    pauseButtons.setIndex(0);
                }
                else
                {
                    Input.setForceMouseHidden(false);

                    createState.deselectInterfaceButtons();

                    CLIENT.switchGameState(createState);

                    renderLevel(-simulation.player.x + 960, simulation.player.y + 540, gc);

                    return;
                }
            }
            else if(Input.BACK.wasJustActivated() && paused)
                paused = false;

        if(paused || disconnectionMessage != null)
            Input.setForceMouseHidden(false);
        else
        {
            Input.setForceMouseHidden(true);

            if(Input.RESET.wasJustActivated())
                simulation.resetPlayer();

            simulation.update(elapsedTime * 10, Integer.MAX_VALUE);
        }

        double offsetX = -simulation.player.x + 960;
        double offsetY = simulation.player.y + 540;

        for(TargetedPlayer targetedPlayer : CLIENT.playersInHostedLevel.values())
        {
            targetedPlayer.pursueTargetValues();

            double opacity = Math.min((0.004 * Math.sqrt(Math.pow(targetedPlayer.x.get() - simulation.player.x, 2) + Math.pow(targetedPlayer.y.get() - simulation.player.y, 2))) + 0.2, 1);

            gc.setFill(Color.rgb(255, 0, 0, opacity));

            drawPlayer(targetedPlayer.x.get(), targetedPlayer.y.get(), targetedPlayer.rotationAngle.get(), targetedPlayer.jumpProgress.get(), targetedPlayer.getAnimationDirection(), targetedPlayer.getAnimationType(), offsetX, offsetY, gc);

            gc.setFont(Font.font("Arial", 20));

            Client.fillText(targetedPlayer.getName(), targetedPlayer.x.get() + offsetX, -targetedPlayer.y.get() + offsetY - 40, gc);
        }

        gc.setFill(Color.RED);

        drawPlayer(simulation.player.x, simulation.player.y, simulation.player.rotationAngle, simulation.player.jumpProgress, simulation.player.animationDirection, simulation.player.animationType, offsetX, offsetY, gc);

        renderLevel(offsetX, offsetY, gc);

        gc.setFont(Font.font("Arial", 20));

        for(TargetedPlayer targetedPlayer : CLIENT.playersInHostedLevel.values())
        {
            double opacity = Math.min((0.004 * Math.sqrt(Math.pow(targetedPlayer.x.get() - simulation.player.x, 2) + Math.pow(targetedPlayer.y.get() - simulation.player.y, 2))) + 0.2, 1);

            gc.setFill(Color.rgb(255, 0, 0, opacity));

            Client.fillText(targetedPlayer.getName(), targetedPlayer.x.get() + offsetX, -targetedPlayer.y.get() + offsetY - 40, gc);
        }

        if(paused)
        {
            gc.setFill(Color.rgb(255, 255, 255, 0.5));
            gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

            pauseButtons.render(gc);
        }

        if(createState == null && !CLIENT.isConnected() && disconnectionMessage == null)
        {
            disconnectionMessage = new AttentionMessage("Server connection was lost!", this::leaveHostAndQuitToTitleScreen, false);

            disconnectionMessage.activate();

            pauseButtons.setFrozen(true);
            pauseButtons.unhighlightAllButtons();
        }

        if(disconnectionMessage != null)
            disconnectionMessage.render(gc);
    }

    private void renderLevel(double offsetX, double offsetY, GraphicsContext gc)
    {
        level.render(new Point2D(offsetX, offsetY), 1, gc);
    }
}