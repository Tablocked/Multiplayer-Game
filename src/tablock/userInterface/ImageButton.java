package tablock.userInterface;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class ImageButton extends Button
{
    private Image image;

    public ImageButton(double x, double y, Image image, ActivationHandler activationHandler)
    {
        super(x, y, activationHandler);

        this.image = image;

        width = image.getWidth();
        height = image.getHeight();
    }

    public ImageButton(Image image, ActivationHandler activationHandler, String hoverText)
    {
        this(0, 0, image, activationHandler);

        this.setHoverText(hoverText);
    }

    @Override
    public void render(GraphicsContext gc)
    {
        if(hidden)
            return;

        double xOffset = width / 2;
        double yOffset = height / 2;

        super.render(gc);

        gc.drawImage(image, x - xOffset, y - yOffset);
    }
}