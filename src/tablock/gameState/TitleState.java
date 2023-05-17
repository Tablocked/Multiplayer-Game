package tablock.gameState;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import tablock.network.ClientPacket;
import tablock.userInterface.ButtonStrip;
import tablock.userInterface.TextButton;

public class TitleState extends GameState
{
    private final ButtonStrip buttonStrip = new ButtonStrip
    (
        ButtonStrip.Orientation.VERTICAL,

        new TextButton(960, 340, "Host and Create", 100, () -> CLIENT.switchGameState(new LevelSelectState())),
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
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", 50));
        gc.fillText(CLIENT.getName(), 960, 100);

        buttonStrip.render(gc);
    }
}