package tablock.userInterface;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import tablock.core.Input;
import tablock.gameState.Renderer;

public class InputIndicatorStrip
{
    private final InputIndicator[] inputIndicators;
    private final double xOffset;

    public InputIndicatorStrip(InputIndicator... inputIndicators)
    {
        double totalWidth = 0;

        this.inputIndicators = inputIndicators;

        for(InputIndicator inputIndicator : inputIndicators)
        {
            double textWidth = Renderer.getTextShape(inputIndicator.text(), Font.font("Arial", 50)).getWidth();
            double imageWidth = inputIndicator.input().getImage().getWidth();

            totalWidth += textWidth + imageWidth;
        }

        xOffset = (1920 - totalWidth) / (inputIndicators.length + 1);
    }

    public void render(GraphicsContext gc)
    {
        double xPosition = xOffset;

        gc.setFont(Font.font("Arial", 50));
        gc.setFill(Color.BLACK);

        for(InputIndicator inputIndicator : inputIndicators)
        {
            Input input = inputIndicator.input();
            String text = inputIndicator.text();
            Image image = input.getImage();
            double textWidth = Renderer.getTextShape(text, gc).getWidth();

            gc.drawImage(image, xPosition, 1000);
            gc.fillText(text, xPosition + image.getWidth(), 1050);

            xPosition += textWidth + image.getWidth() + xOffset;
        }
    }
}