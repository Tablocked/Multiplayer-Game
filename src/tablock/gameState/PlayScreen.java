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
import tablock.network.Client;
import tablock.userInterface.ButtonStrip;
import tablock.userInterface.TextButton;

import java.util.List;

public class PlayScreen implements GameState
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
        simulation = new Simulation(createPlayer(0, 600));
        createScreen = null;
        this.level = level;

        addObjectsToSimulation();
    }

    public PlayScreen(CreateScreen createScreen, double startX, double startY)
    {
        simulation = new Simulation(createPlayer(startX, startY));
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
            double[] xValues = object.getWorldXValues();
            double[] yValues = object.getWorldYValues();
            Vector2[] vertices = new Vector2[xValues.length];

            for(int i = 0; i < xValues.length; i++)
                vertices[i] = new Vector2(xValues[i], -yValues[i]);

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

//    public PlayScreen()
//    {
//        //Server server = new Server();
//
//        //server.start();
//
//        Client client = new Client("localhost");
//
////        stage.setOnCloseRequest((event) ->
////        {
////            client.closeSocket();
////            server.closeSocket();
////        });
//
//        init(client);
//    }

//    public PlayScreen(Stage stage, String addressName)
//    {
//        super(stage);
//
//        Client client = new Client(addressName);
//
//        stage.setOnCloseRequest((event) -> client.closeSocket());
//
//        init(client);
//    }

    private void init(Client client)
    {
//        simulation.addBody(new Platform(299, -500, new Vector2(0, -300), new Vector2(702, -300), new Vector2(702, 0), new Vector2(0, 0)));
//        simulation.addBody(new Platform(1000, -300, new Vector2(0, -500), new Vector2(400, -300), new Vector2(400, -200), new Vector2(0, 0)));
//        simulation.addBody(new Platform(50, -300, new Vector2(100, -500), new Vector2(250, -500), new Vector2(250, 0), new Vector2(0, 0)));

        //client.start();
        //client.sendPacket(new ConnectPacket());
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

                Input.setMouseHidden(false);

                Renderer.setCurrentState(createScreen);

                return;
            }
        }
        else if(Input.UI_BACK.wasJustActivated() && paused)
            paused = false;

        if(paused)
            Input.setMouseHidden(false);
        else
        {
            Input.setMouseHidden(true);

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