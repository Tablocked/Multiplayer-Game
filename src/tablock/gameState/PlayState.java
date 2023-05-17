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
import tablock.userInterface.ButtonStrip;
import tablock.userInterface.TextButton;

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
        new TextButton(960, 640, "Quit To Main Menu", 100, () -> CLIENT.switchGameState(new TitleState()))
    );

    public PlayState(Level level)
    {
        simulation = new Simulation(createPlayer(0, 0));
        createState = null;
        this.level = level;

        addObjectsToSimulation();
    }

    public PlayState(CreateState createState, double startX, double startY)
    {
        simulation = new Simulation(createPlayer(startX, -startY));
        this.createState = createState;
        this.level = createState.getLevel();

        addObjectsToSimulation();
    }

    private Body createPlayer(double x, double y)
    {
        Body player = new Body();

        player.addFixture(Geometry.createRectangle(50, 50));
        player.translate(x, y);
        player.setMass(MassType.NORMAL);

        return player;
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
        }

        double offsetX = -simulation.getPlayerCenter().x + 960;
        double offsetY = simulation.getPlayerCenter().y + 540;

        gc.beginPath();

        for(Vector2 vertex : simulation.getPlayerVertices())
            gc.lineTo(vertex.x + offsetX, -vertex.y + offsetY);

        gc.setFill(Color.RED);
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