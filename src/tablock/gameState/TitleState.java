package tablock.gameState;

import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import tablock.network.Client;
import tablock.network.ClientPacket;
import tablock.userInterface.ButtonStrip;
import tablock.userInterface.TextButton;

public class TitleState extends GameState
{
    private final ButtonStrip buttonStrip = new ButtonStrip
    (
        ButtonStrip.Orientation.VERTICAL,

        new TextButton(960, 340, "Create", 100, () -> CLIENT.switchGameState(new LevelSelectState())),
        new TextButton(960, 540, "Join", 100, () -> CLIENT.switchGameState(new JoinState())),

        new TextButton(960, 740, "Quit", 100, () ->
        {
            CLIENT.send(ClientPacket.DISCONNECT);

            System.exit(0);
        })
    );

    @Override
    public void renderNextFrame(GraphicsContext gc)
    {
        String connectionStatus = CLIENT.isConnected() ? "Connected to server | Multiplayer features enabled!" : "Unable to connect to server | Multiplayer features disabled";

        gc.setFont(Font.font("Arial", 30));

        Bounds textShape = Client.computeTextShape(connectionStatus, gc);

        gc.setFill(CLIENT.isConnected() ? Color.GREEN : Color.RED);
        gc.fillText(connectionStatus, 1900 - textShape.getWidth(), 35);

        buttonStrip.render(gc);
    }
}