package tablock.gameState;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.*;
import org.dyn4j.geometry.decompose.SweepLine;
import tablock.core.Input;
import tablock.core.Simulation;
import tablock.level.Level;
import tablock.level.Platform;
import tablock.network.ClientPacket;
import tablock.network.Player;
import tablock.userInterface.AttentionMessage;
import tablock.userInterface.ButtonStrip;
import tablock.userInterface.TextButton;

import java.util.ArrayList;
import java.util.List;

public class PlayState extends GameState
{
    private long frameTime = System.nanoTime();
    private boolean paused;
    private AttentionMessage disconnectionMessage;
    private final Simulation simulation;
    private final CreateState createState;
    private final Level level;

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
                List<Triangle> fixtures = sweepLine.triangulate(vertices);

                for(Triangle fixture : fixtures)
                    body.addFixture(fixture);
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

        CLIENT.playersInHostedLevel.clear();

        CLIENT.send(ClientPacket.LEAVE_HOST);
        CLIENT.switchGameState(new TitleState());
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

                    renderLevel(-simulation.computePlayerCenter().x + 960, simulation.computePlayerCenter().y + 540, gc);

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

        Vector2 playerCenter = simulation.computePlayerCenter();
        double offsetX = -playerCenter.x + 960;
        double offsetY = playerCenter.y + 540;
        List<Player> playersInHostedLevel = new ArrayList<>(CLIENT.playersInHostedLevel);

        playerCenter.y = -playerCenter.y;

        if(CLIENT.player != null)
        {
            CLIENT.player.x = playerCenter.x;
            CLIENT.player.y = playerCenter.y;
            CLIENT.player.rotationAngle = -simulation.computePlayerRotationAngle();
        }

        for(Player onlinePlayer : playersInHostedLevel)
        {
            double[] xValues = {-25, 25, 25, -25};
            double[] yValues = {-25, -25, 25, 25};
            double opacity = Math.min((0.004 * Math.sqrt(Math.pow(onlinePlayer.x - playerCenter.x, 2) + Math.pow(onlinePlayer.y - playerCenter.y, 2))) + 0.2, 1);

            for(int i = 0; i < 4; i++)
            {
                double x = xValues[i];
                double y = yValues[i];

                xValues[i] = ((x * Math.cos(onlinePlayer.rotationAngle)) - (y * Math.sin(onlinePlayer.rotationAngle))) + onlinePlayer.x + offsetX;
                yValues[i] = ((x * Math.sin(onlinePlayer.rotationAngle)) + (y * Math.cos(onlinePlayer.rotationAngle))) + onlinePlayer.y + offsetY;
            }

            gc.setFill(Color.rgb(255, 0, 0, opacity));
            gc.fillPolygon(xValues, yValues, 4);
        }

        gc.setFill(Color.RED);
        gc.beginPath();

        for(Vector2 vertex : simulation.getPlayerVertices())
            gc.lineTo(vertex.x + offsetX, -vertex.y + offsetY);

        gc.fill();
        gc.closePath();

        renderLevel(offsetX, offsetY, gc);

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