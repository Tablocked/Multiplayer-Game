package tablock.gameState;

import javafx.scene.canvas.GraphicsContext;
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
        buttonStrip.render(gc);
    }
}