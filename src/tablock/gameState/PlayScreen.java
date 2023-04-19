package tablock.gameState;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.dyn4j.geometry.Vector2;
import tablock.core.*;
import tablock.gameState.Renderer.GameState;
import tablock.network.Client;
import tablock.userInterface.ButtonStrip;
import tablock.userInterface.TextButton;

import java.util.List;

public class PlayScreen extends GameState
{
    private long frameTime = System.nanoTime();
    private boolean paused = false;
    private final Simulation simulation;
    private final CreateScreen createScreen;

    private final ButtonStrip buttonStrip = new ButtonStrip
    (
        ButtonStrip.Orientation.VERTICAL,

        new TextButton(960, 340, "Resume", 100, () -> paused = false),
        new TextButton(960, 540, "Quit To Main Menu", 100, () -> switchGameState(new TitleScreen())),
        new TextButton(960, 740, "Quit To Desktop", 100, () -> System.exit(0))
    );

    public PlayScreen(Level level)
    {
        simulation = new Simulation(new PlayerBody(0, 600));
        createScreen = null;

        level.addPlatformsToSimulation(simulation);
    }

    public PlayScreen(CreateScreen createScreen, List<Platform> platforms, double startX, double startY)
    {
        simulation = new Simulation(new PlayerBody(startX, startY));
        this.createScreen = createScreen;

        for(Platform platform : platforms)
            simulation.addBody(platform);
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

                switchGameState(createScreen);

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

        for(SimulationBody body : simulation.getBodies())
        {
            Vector2[] vertices = body instanceof Platform ? body.getVertices() : simulation.getPlayerVertices();
            Vector2[] doubleJumpVertices = simulation.getDoubleJumpVertices();
            boolean shouldDrawDoubleJumpEffect = doubleJumpVertices != null && body instanceof PlayerBody;
            double cameraX = -simulation.getPlayerCenter().x + 935;
            double cameraY = simulation.getPlayerCenter().y + 515;

            gc.beginPath();

            if(shouldDrawDoubleJumpEffect)
            {
                for(int i = 0; i < doubleJumpVertices.length; i++)
                {
                    Vector2 vertex = doubleJumpVertices[i];
                    double x = vertex.x + cameraX;
                    double y = -vertex.y + cameraY;

                    if(i == doubleJumpVertices.length - 2)
                    {
                        Vector2 endVertex = doubleJumpVertices[doubleJumpVertices.length - 1];

                        gc.quadraticCurveTo(x, y, endVertex.x + cameraX, -endVertex.y + cameraY);
                    }
                    else
                    {
                        gc.lineTo(x, y);
                    }
                }
            }
            else
            {
                for(Vector2 vertex : vertices)
                    gc.lineTo(vertex.x + cameraX, -vertex.y + cameraY);
            }

            gc.setFill(body instanceof Platform ? Color.BLACK : Color.RED);
            gc.fill();
            gc.closePath();
        }

        if(paused)
        {
            gc.setFill(Color.rgb(255, 255, 255, 0.5));
            gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

            buttonStrip.render(gc);
        }
    }
}