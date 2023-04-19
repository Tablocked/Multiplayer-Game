package tablock.userInterface;

import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import tablock.gameState.Renderer;

public class TextButton extends Button
{
    private final Text text;
    private Color textColor = Color.BLACK;
    private boolean centerText;

    public TextButton(double x, double y, String text, int fontSize, ActivationHandler activationHandler)
    {
        super(x, y, activationHandler);

        this.text = new Text(text);
        this.text.setFont(Font.font("Arial", fontSize));

        Bounds textBounds = this.text.getBoundsInParent();

        width = textBounds.getWidth() + (fontSize * 0.5);
        height = textBounds.getHeight() + (fontSize * 0.15);
    }

    public TextButton(double x, double y, String text, int fontSize, Color textColor, boolean centerText, ActivationHandler activationHandler)
    {
        this(x, y, text, fontSize, activationHandler);

        this.textColor = textColor;
        this.centerText = centerText;
    }

    public TextButton(String text, int fontSize, ActivationHandler activationHandler)
    {
        this(0, 0, text, fontSize, activationHandler);
    }

    @Override
    public void render(GraphicsContext gc)
    {
        if(hidden)
            return;

        double fontSize = text.getFont().getSize();
        double xOffset = width / 2;
        double yOffset = height / 2;
        double textX = x + (fontSize * 0.2);
        double textY = y + fontSize;

        super.render(gc);

        gc.setFill(textColor);
        gc.setFont(text.getFont());

        if(centerText)
            Renderer.fillText(textX, textY, text.getText(), gc);
        else
        {
            textX -= xOffset;
            textY -= yOffset;

            gc.fillText(text.getText(), textX, textY);
        }
    }

    public void setText(String text)
    {
        this.text.setText(text);
    }

    public String getText()
    {
        return text.getText();
    }
}