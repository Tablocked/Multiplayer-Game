package tablock.level;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import tablock.core.Input;
import tablock.core.Main;

import java.io.Serial;

public class MovableVertex extends Selectable
{
    @Serial
    private static final long serialVersionUID = -7071404509826592462L;

    public MovableVertex(double worldX, double worldY)
    {
        super(new double[]{worldX}, new double[]{worldY});
    }

    @Override
    public boolean isHoveredByMouse()
    {
        return new Circle(screenXValues[0], screenYValues[0], 12.5).contains(Input.getMousePosition());
    }

    @Override
    public void renderObject(GraphicsContext gc)
    {
        gc.setFill(Color.BLACK);
        gc.fillOval(screenXValues[0] - 15, screenYValues[0] - 15, 30, 30);
        gc.drawImage(Main.getTexture("vertex"), screenXValues[0] - 12.5, screenYValues[0] - 12.5);
    }

    @Override
    public void renderOutline(boolean highlighted, boolean selected, GraphicsContext gc)
    {
        gc.setLineWidth(5);

        if(highlighted && selected)
            gc.setLineDashes(8);

        gc.setStroke(highlighted && !selected ? Color.RED.desaturate().desaturate() : Color.LIGHTGREEN);
        gc.strokeOval(screenXValues[0] - 15, screenYValues[0] - 15, 30, 30);

        gc.setLineDashes(0);
    }
}