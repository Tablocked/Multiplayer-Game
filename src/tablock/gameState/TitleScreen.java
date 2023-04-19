package tablock.gameState;

import javafx.scene.canvas.GraphicsContext;
import tablock.gameState.Renderer.GameState;
import tablock.userInterface.ButtonStrip;
import tablock.userInterface.TextButton;

public class TitleScreen extends GameState
{
    private final ButtonStrip buttonStrip = new ButtonStrip
    (
        ButtonStrip.Orientation.VERTICAL,

        new TextButton(960, 440, "Create", 100, () -> switchGameState(new LevelSelectScreen())),
        new TextButton(960, 640, "Quit", 100, () -> System.exit(0))
    );

    @Override
    public void renderNextFrame(GraphicsContext gc)
    {
        buttonStrip.render(gc);
    }
}