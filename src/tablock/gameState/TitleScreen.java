package tablock.gameState;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import tablock.userInterface.ButtonStrip;
import tablock.userInterface.TextButton;

public class TitleScreen extends GameState
{
    private final ButtonStrip buttonStrip = new ButtonStrip
    (
        ButtonStrip.Orientation.VERTICAL,

        new TextButton(960, 340, "Host and Create", 100, () -> Renderer.setCurrentState(new LevelSelectScreen())),
        new TextButton(960, 540, "Join", 100, () -> Renderer.setCurrentState(new JoinScreen())),
        new TextButton(960, 740, "Quit", 100, () -> System.exit(0))
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