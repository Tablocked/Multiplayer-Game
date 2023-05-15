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

public class PlayScreen extends GameState
{
    private long frameTime = System.nanoTime();
    private boolean paused = false;
    private final Simulation simulation;
    private final CreateScreen createScreen;
    private final Level level;

    private final ButtonStrip buttonStrip = new ButtonStrip
    (
        ButtonStrip.Orientation.VERTICAL,

        new TextButton(960, 340, "Resume", 100, () -> paused = false),
        new TextButton(960, 540, "Quit To Main Menu", 100, () -> Renderer.setCurrentState(new TitleScreen())),
        new TextButton(960, 740, "Quit To Desktop", 100, () -> System.exit(0))
    );

    public PlayScreen(Level level)
    {
        simulation = new Simulation(createPlayer(0, 0));
        createScreen = null;
        this.level = level;

        addObjectsToSimulation();
    }

    public PlayScreen(CreateScreen createScreen, double startX, double startY)
    {
        simulation = new Simulation(createPlayer(startX, -startY));
        this.createScreen = createScreen;
        this.level = createScreen.getLevel();

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
            if(createScreen == null)
            {
                paused = !paused;

                buttonStrip.setIndex(0);
            }
            else
            {
                simulation.removeAllBodies();

                Input.setForceMouseHidden(false);

                Renderer.setCurrentState(createScreen);

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

        double offsetX = -simulation.getPlayerCenter().x + 935;
        double offsetY = simulation.getPlayerCenter().y + 515;

        gc.beginPath();

        for(Vector2 vertex : simulation.getPlayerVertices())
            gc.lineTo(vertex.x + offsetX, -vertex.y + offsetY);

        gc.setFill(Color.RED);
        gc.fill();
        gc.closePath();

        level.render(new Point2D(offsetX, offsetY), 1, gc);

//        for(Body body : simulation.getBodies())
//        {
//            for(Fixture fixture : body.getFixtures())
//            {
//                gc.beginPath();
//
//                for(Vector2 vertex : ((Polygon) fixture.getShape()).getVertices())
//                    gc.lineTo(vertex.x + offsetX, -vertex.y + offsetY);
//
//                Vector2 vertex = ((Polygon) fixture.getShape()).getVertices()[0];
//
//                gc.lineTo(vertex.x + offsetX, -vertex.y + offsetY);
//
//                gc.setFill(Color.DARKRED);
//                gc.setStroke(Color.BLACK);
//                gc.setLineWidth(1);
//                gc.fill();
//                gc.stroke();
//                gc.closePath();
//            }
//        }

        if(paused)
        {
            gc.setFill(Color.rgb(255, 255, 255, 0.5));
            gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

            buttonStrip.render(gc);
        }
    }
}