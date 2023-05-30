package tablock.userInterface;

import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import tablock.core.Texture;
import tablock.network.Client;

public class AttentionMessage
{
    private boolean active = false;
    private final String message;
    private final ButtonStrip buttonStrip;
    private final Bounds messageShape;
    private final Bounds attentionShape;

    public AttentionMessage(String message, Button.ActivationHandler okButtonActivationHandler, boolean includeCancelButton)
    {
        TextButton okButton = new TextButton(includeCancelButton ? 839 : 960, 610, "Ok", 50, okButtonActivationHandler);

        this.message = message;

        if(includeCancelButton)
        {
            buttonStrip = new ButtonStrip(ButtonStrip.Orientation.HORIZONTAL, okButton, new TextButton(1033, 610, "Cancel", 50, null));

            buttonStrip.setIndex(1);
        }
        else
            buttonStrip = new ButtonStrip(ButtonStrip.Orientation.HORIZONTAL, okButton);

        messageShape = Client.computeTextShape(message, Font.font("Arial", 40));
        attentionShape = Client.computeTextShape("ATTENTION", Font.font("Arial", 40));
    }

    public void initializeCancelButton(Button.ActivationHandler cancelButtonActivationHandler)
    {
        buttonStrip.buttons[1].setActivationHandler(() ->
        {
            cancelButtonActivationHandler.onActivation();

            active = false;
        });
    }

    public void render(GraphicsContext gc)
    {
        if(active)
        {
            gc.setFill(Color.DARKRED);
            gc.setStroke(Color.GOLD);
            gc.setLineWidth(5);
            gc.fillRect(940 - (messageShape.getWidth() / 2), 415, messageShape.getWidth() + 40, 250);
            gc.strokeRect(940 - (messageShape.getWidth() / 2), 415, messageShape.getWidth() + 40, 250);
            gc.setFill(Color.GOLD);
            gc.setFont(Font.font("Arial", 40));

            Client.fillText("ATTENTION", 960, 490, attentionShape, gc);

            gc.fillText(message, 960 - (messageShape.getWidth() / 2), 545);
            gc.drawImage(Texture.WARNING.get(), 1010 - attentionShape.getWidth(), 430);
            gc.drawImage(Texture.WARNING.get(), 858 + attentionShape.getWidth(), 430);

            buttonStrip.render(gc);
        }
    }

    public void activate()
    {
        buttonStrip.preventActivationForOneFrame();

        active = true;
    }

    public boolean isActive()
    {
        return active;
    }
}