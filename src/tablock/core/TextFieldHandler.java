package tablock.core;

import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import tablock.network.Client;

public abstract class TextFieldHandler
{
    String text;
    private long timeReference;
    final String originalText;
    final double x;
    final double y;

    public TextFieldHandler(String originalText, double x, double y)
    {
        text = originalText;

        this.originalText = originalText;
        this.x = x;
        this.y = y;

        timeReference = System.currentTimeMillis();
    }

    public abstract void onConfirmation(String text);

    public void onKeyTyped(String text, boolean cancelling)
    {
        timeReference = System.currentTimeMillis();
    }

    public void renderTypingCursor(boolean center, GraphicsContext gc)
    {
        if((System.currentTimeMillis() - timeReference) % 1000 < 500)
        {
            Bounds textShape = Client.computeTextShape(text, Font.font("Arial", 80));

            gc.setFill(Color.WHITE);
            gc.fillRect(x + (center ? textShape.getWidth() / 2 : textShape.getWidth() + 15) + 5, y - 40, 5, 80);
        }
    }

    public void cancel()
    {
        onKeyTyped(originalText, true);
    }
}