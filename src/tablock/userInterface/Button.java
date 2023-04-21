package tablock.userInterface;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import tablock.core.Input;
import tablock.gameState.Renderer;

public abstract class Button
{
    protected double x;
    protected double y;
    protected double width;
    protected double height;
    protected String hoverText;
    private ActivationHandler activationHandler;
    private Input input;
    private boolean beingClicked = false;
    private boolean selected = false;
    private boolean forceHighlighted = false;
    private boolean circular = false;
    private boolean canBeDragged = false;
    private Point2D mousePositionDuringDragStart;
    private Point2D positionDuringDragStart;
    private boolean hoverTextLeftSided = false;
    private Color selectedColor = Color.GREEN;
    private Color deselectedColor = Color.RED;
    private boolean frozen = false;
    private boolean preventActivation = false;
    protected boolean hidden = false;

    protected Button(double x, double y, ActivationHandler activationHandler)
    {
        this.x = x;
        this.y = y;
        this.activationHandler = activationHandler;
    }

    public void checkForActivation()
    {
        Rectangle2D rectangularShape = new Rectangle2D(x - (width / 2), y - (height / 2), width, height);
        double diagonal = Math.sqrt(2 * Math.pow(width, 2));
        Circle circularShape = new Circle(x, y, diagonal / 2);
        Point2D mousePosition = Input.getMousePosition();

        if(!frozen && Input.isUsingMouseControls())
        {
            if((rectangularShape.contains(mousePosition) && !circular) || (circularShape.contains(mousePosition) && circular))
            {
                selected = true;

                if(canBeDragged && Input.MOUSE_LEFT.wasJustActivated())
                {
                    mousePositionDuringDragStart = mousePosition;
                    positionDuringDragStart = new Point2D(x, y);
                }
                else
                {
                    if(Input.MOUSE_LEFT.wasJustActivated())
                        beingClicked = true;

                    if(beingClicked && !Input.MOUSE_LEFT.isActive())
                    {
                        beingClicked = false;

                        activationHandler.onActivation();
                    }
                }
            }
            else
            {
                selected = beingClicked = false;
            }

            if(canBeDragged)
            {
                if(Input.MOUSE_LEFT.isActive() && mousePositionDuringDragStart != null)
                {
                    Point2D newPosition = positionDuringDragStart.subtract(mousePositionDuringDragStart.subtract(mousePosition));

                    x = newPosition.getX();
                    y = newPosition.getY();
                }
                else if(!Input.MOUSE_LEFT.isActive())
                    mousePositionDuringDragStart = null;
            }
        }

        if(selected && Input.UI_SELECT.wasJustActivated() && !frozen && !preventActivation)
            activationHandler.onActivation();

        preventActivation = false;
    }

    public void render(GraphicsContext gc)
    {
        if(hidden)
            return;

        Rectangle2D rectangularShape = new Rectangle2D(x - (width / 2), y - (height / 2), width, height);
        double diagonal = Math.sqrt(2 * Math.pow(width, 2));
        double offset = diagonal / 2;

        checkForActivation();

        if(selected && hoverText != null)
        {
            gc.setFont(Font.font("Arial", 50));

            Bounds textShape = Renderer.getTextShape(hoverText, gc);
            double rectangleWidth = textShape.getWidth() + offset + 20;
            double xOffset = hoverTextLeftSided ? rectangleWidth : 0;

            gc.setFill(Color.GREEN.darker().darker());
            gc.fillRect(x - xOffset, y - offset, rectangleWidth, diagonal);
            gc.setFill(Color.WHITE);
            gc.fillText(hoverText, x + offset - xOffset + (hoverTextLeftSided ? -20 : 10), y + (offset / 2));
        }

        gc.setFill(selected || forceHighlighted || mousePositionDuringDragStart != null ? selectedColor : deselectedColor);

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
            gc.fillRect(rectangularShape.getMinX(), rectangularShape.getMinY(), width, height);
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

    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }

    public boolean isSelected()
    {
        return selected;
    }

    public void setForceHighlighted(boolean forceHighlighted)
    {
        this.forceHighlighted = forceHighlighted;
    }

    public void setCircular(boolean circular)
    {
        this.circular = circular;
    }

    public void setCanBeDragged(boolean canBeDragged)
    {
        this.canBeDragged = canBeDragged;
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

    public interface ActivationHandler
    {
        void onActivation();
    }
}