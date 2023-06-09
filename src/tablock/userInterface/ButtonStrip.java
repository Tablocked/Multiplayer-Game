package tablock.userInterface;

import javafx.scene.canvas.GraphicsContext;
import tablock.core.Input;

public class ButtonStrip
{
    private final Orientation orientation;
    private int index;
    private boolean frozen = false;
    final Button[] buttons;

    public ButtonStrip(Orientation orientation, Button... buttons)
    {
        this.orientation = orientation;
        this.buttons = buttons;
    }

    public ButtonStrip(Orientation orientation, double x, double y, double spacing, Button... buttons)
    {
        this(orientation, buttons);

        double totalLength = 0;
        double offset = 0;

        for(Button button : buttons)
            totalLength += (orientation == Orientation.VERTICAL ? button.height : button.width) + spacing;

        for(Button button : buttons)
        {
            double xPosition = orientation == Orientation.HORIZONTAL ? x - (totalLength / 2) + offset + (button.width / 2) : x;
            double yPosition = orientation == Orientation.VERTICAL ? y - (totalLength / 2) + offset + (button.height / 2) : y;

            offset += (orientation == Orientation.VERTICAL ? button.height : button.width) + spacing;

            button.setPosition(xPosition, yPosition);
        }
    }

    private void updateIndex(Orientation orientation, Input incrementInput, Input decrementInput)
    {
        if(this.orientation == orientation && !frozen)
        {
            int newIndex = index;

            if(incrementInput.wasJustActivated())
                newIndex++;

            if(decrementInput.wasJustActivated())
                newIndex--;

            newIndex = Math.max(newIndex, 0);
            newIndex = Math.min(newIndex, buttons.length - 1);

            if(buttons.length > 0 && !buttons[newIndex].disabled)
                index = newIndex;
        }
    }

    public void render(GraphicsContext gc)
    {
        updateIndex(Orientation.VERTICAL, Input.DOWN, Input.UP);
        updateIndex(Orientation.HORIZONTAL, Input.RIGHT, Input.LEFT);

        for(int i = 0; i < buttons.length; i++)
        {
            Button button = buttons[i];

            if(!frozen)
                button.setHovered(index == i && !Input.isUsingMouseControls());

            button.detectIfHoveredAndRender(gc);
            button.checkForActionButtonActivation();
        }
    }

    public void preventActivationForOneFrame()
    {
        for(Button button: buttons)
            button.preventActivationForOneFrame();
    }

    public int getMaximumIndex()
    {
        return buttons.length - 1;
    }

    public void setIndex(int index)
    {
        this.index = index;
    }

    public int getIndex()
    {
        return index;
    }

    public void setFrozen(boolean frozen)
    {
        this.frozen = frozen;

        for(Button button : buttons)
            button.setFrozen(frozen);
    }

    public void setHidden(boolean hidden)
    {
        for(Button button : buttons)
            button.setHidden(hidden);
    }

    public void forceButtonToBeClicked(int buttonBeingClickedIndex)
    {
        buttons[buttonBeingClickedIndex].beingClicked = true;
    }

    public void unhighlightAllButtons()
    {
        for(Button button : buttons)
            button.setHovered(false);
    }

    public enum Orientation
    {
        VERTICAL,
        HORIZONTAL
    }
}