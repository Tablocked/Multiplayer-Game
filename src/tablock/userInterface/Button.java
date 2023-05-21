package tablock.userInterface;

import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import tablock.core.Input;
import tablock.network.Client;

public abstract class Button
{
    double x;
    double y;
    double width;
    double height;
    String hoverText;
    boolean beingClicked = false;
    boolean hidden = false;
    boolean disabled = false;
    private ActivationHandler activationHandler;
    private Input input;
    private boolean hovered = false;
    private boolean forceHighlighted = false;
    private boolean circular = false;
    private boolean hoverTextLeftSided = false;
    private Color selectedColor = Color.GREEN;
    private Color deselectedColor = Color.RED;
    private boolean frozen = false;
    private boolean preventActivation = false;

    protected Button(double x, double y, ActivationHandler activationHandler)
    {
        this.x = x;
        this.y = y;
        this.activationHandler = activationHandler;
    }

    public void detectIfHovered()
    {
        if(hidden)
            return;

        if(disabled)
            hovered = false;
        else
        {
            double diagonal = Math.sqrt(2 * Math.pow(width, 2));
            Rectangle rectangle = new Rectangle(x - (width / 2), y - (height / 2), width, height);
            Shape shape = circular ? new Circle(x, y, diagonal / 2) : rectangle;

            if(!frozen && Input.isUsingMouseControls())
            {
                if(shape.contains(Input.getMousePosition()))
                {
                    hovered = true;

                    if(Input.MOUSE_LEFT.wasJustActivated())
                        beingClicked = true;

                    if(beingClicked && !Input.MOUSE_LEFT.isActive())
                    {
                        beingClicked = false;

                        activationHandler.onActivation();
                    }
                }
                else
                {
                    hovered = beingClicked = false;
                }
            }

            if(hovered && Input.SELECT.wasJustActivated() && !frozen && !preventActivation)
                activationHandler.onActivation();

            preventActivation = false;
        }
    }

    public void render(GraphicsContext gc)
    {
        double diagonal = Math.sqrt(2 * Math.pow(width, 2));
        double offset = diagonal / 2;

        if(hovered && hoverText != null)
        {
            gc.setFont(Font.font("Arial", 50));

            Bounds textShape = Client.computeTextShape(hoverText, gc);
            double rectangleWidth = textShape.getWidth() + offset + 20;
            double xOffset = hoverTextLeftSided ? rectangleWidth : 0;

            gc.setFill(Color.GREEN.darker().darker());
            gc.fillRect(x - xOffset, y - offset, rectangleWidth, diagonal);
            gc.setFill(Color.WHITE);
            gc.fillText(hoverText, x + offset - xOffset + (hoverTextLeftSided ? -20 : 10), y + (offset / 2));
        }

        gc.setFill(hovered || forceHighlighted ? selectedColor : deselectedColor);

        if(circular)
        {
            gc.fillOval(x - offset, y - offset, diagonal, diagonal);

            if(forceHighlighted)
            {
                gc.setStroke(Color.LIGHTGREEN);
                gc.setLineWidth(5);
                gc.strokeOval(x - offset, y - offset, diagonal, diagonal);
            }
        }
        else
        {
            Rectangle rectangle = new Rectangle(x - (width / 2), y - (height / 2), width, height);

            gc.fillRect(rectangle.getX(), rectangle.getY(), width, height);

            if(disabled)
            {
                gc.setFill(Color.rgb(0, 0, 0, 0.5));
                gc.fillRect(rectangle.getX(), rectangle.getY(), width, height);
            }
        }
    }

    public void detectIfHoveredAndRender(GraphicsContext gc)
    {
        detectIfHovered();
        render(gc);
    }

    public void preventActivationForOneFrame()
    {
        preventActivation = true;
    }

    public void checkForActionButtonActivation()
    {
        if(input != null && input.wasJustActivated())
            activationHandler.onActivation();
    }

    public void setWidth(double width)
    {
        this.width = width;
    }

    public void setActivationHandler(ActivationHandler activationHandler)
    {
        this.activationHandler = activationHandler;
    }

    public void setActionButton(Input input)
    {
        this.input = input;
    }

    public void setPosition(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    public double getX()
    {
        return x;
    }

    public double getY()
    {
        return y;
    }

    public void setHovered(boolean hovered)
    {
        this.hovered = hovered;
    }

    public boolean isHovered()
    {
        return hovered;
    }

    public void setForceHighlighted(boolean forceHighlighted)
    {
        this.forceHighlighted = forceHighlighted;
    }

    public void setCircular(boolean circular)
    {
        this.circular = circular;
    }

    public void setHoverTextLeftSided(boolean hoverTextLeftSided)
    {
        this.hoverTextLeftSided = hoverTextLeftSided;
    }

    public void setSelectedColor(Color selectedColor)
    {
        this.selectedColor = selectedColor;
    }

    public void setDeselectedColor(Color deselectedColor)
    {
        this.deselectedColor = deselectedColor;
    }

    public void setFrozen(boolean frozen)
    {
        this.frozen = frozen;
    }

    public void setHidden(boolean hidden)
    {
        this.hidden = hidden;
    }

    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    public interface ActivationHandler
    {
        void onActivation();
    }
}