package tablock.userInterface;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class CircularButtonStrip
{
    private final Button[] buttons;

    public CircularButtonStrip(Button... buttons)
    {
        this.buttons = buttons;

        for(int i = 0; i < buttons.length; i++)
        {
            Button button = buttons[i];

            button.setCircular(true);
            button.setHoverTextLeftSided(i < (buttons.length / 2) + 1);
        }
    }

    public void setFrozen(boolean frozen)
    {
        for(Button button : buttons)
            button.setFrozen(frozen);
    }

    public void deselectAllButtons()
    {
        for(Button button : buttons)
            button.setHovered(false);
    }

    public boolean areNoButtonsSelected()
    {
        for(Button button : buttons)
            if(button.isHovered())
                return false;

        return true;
    }

    public void calculateSelectedButtons()
    {
        for(Button button : buttons)
            button.calculateSelected();
    }

    public void render(double x, double y, GraphicsContext gc)
    {
        double radius = 80;

        gc.setLineWidth(20);
        gc.setStroke(Color.DARKRED);
        gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);

        for(int i = 0; i < buttons.length; i++)
        {
            double angle = ((2 * Math.PI) / buttons.length) * i;
            double buttonX = -(radius * Math.sin(angle));
            double buttonY = radius * Math.cos(angle);
            Button button = buttons[i];

            button.setPosition(buttonX + x, buttonY + y);
            button.render(gc);
        }
    }
}