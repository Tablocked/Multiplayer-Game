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
import tablock.userInterface.ButtonStrip;
import tablock.userInterface.TextButton;

import java.util.ArrayList;
import java.util.List;

public class PlayState extends GameState
{
    private long frameTime = System.nanoTime();
    private boolean paused = false;
    private final Simulation simulation;
    private final CreateState createState;
    private final Level level;

    private final ButtonStrip buttonStrip = new ButtonStrip
    (
        ButtonStrip.Orientation.VERTICAL,

        new TextButton(960, 440, "Resume", 100, () -> paused = false),
        new TextButton(960, 640, "Quit To Main Menu", 100, () ->
        {
            CLIENT.player = null;

            CLIENT.playersInHostedLevel.clear();

            CLIENT.send(ClientPacket.LEAVE_HOST);
            CLIENT.switchGameState(new TitleState());
        })
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

    @Override
    public void renderNextFrame(GraphicsContext gc)
    {
        double elapsedTime = (System.nanoTime() - frameTime) / 1e9;

        frameTime = System.nanoTime();

        if(Input.PAUSE.wasJustActivated())
        {
            if(createState == null)
            {
                paused = !paused;

                buttonStrip.setIndex(0);
            }
            else
            {
                simulation.removeAllBodies();

                Input.setForceMouseHidden(false);

                CLIENT.switchGameState(createState);

                return;
            }
        }
        else if(Input.BACK.wasJustActivated() && paused)
            paused = false;

        if(paused)
            Input.setForceMouseHidden(false);
        else
        {
            Input.setForceMouseHidden(true);

            simulation.update(elapsedTime * 10, Integer.MAX_VALUE);

            Vector2 playerCenter = simulation.getPlayerCenter();

            if(CLIENT.player != null)
            {
                CLIENT.player.x = playerCenter.x;
                CLIENT.player.y = -playerCenter.y;
                CLIENT.player.rotationAngle = -simulation.getPlayerRotationAngle();
            }
        }

        double offsetX = -simulation.getPlayerCenter().x + 960;
        double offsetY = simulation.getPlayerCenter().y + 540;
        List<Player> playersInHostedLevel = new ArrayList<>(CLIENT.playersInHostedLevel);

        gc.setFill(Color.RED);

        for(Player player : playersInHostedLevel)
        {
            double[] xValues = {-25, 25, 25, -25};
            double[] yValues = {-25, -25, 25, 25};

            for(int i = 0; i < 4; i++)
            {
                double x = xValues[i];
                double y = yValues[i];

                xValues[i] = ((x * Math.cos(player.rotationAngle)) - (y * Math.sin(player.rotationAngle))) + player.x + offsetX;
                yValues[i] = ((x * Math.sin(player.rotationAngle)) + (y * Math.cos(player.rotationAngle))) + player.y + offsetY;
            }

            gc.fillPolygon(xValues, yValues, 4);
        }

        gc.beginPath();

        for(Vector2 vertex : simulation.getPlayerVertices())
            gc.lineTo(vertex.x + offsetX, -vertex.y + offsetY);

        gc.fill();
        gc.closePath();

        level.render(new Point2D(offsetX, offsetY), 1, gc);

        if(paused)
        {
            gc.setFill(Color.rgb(255, 255, 255, 0.5));
            gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

            buttonStrip.render(gc);
        }
    }
}