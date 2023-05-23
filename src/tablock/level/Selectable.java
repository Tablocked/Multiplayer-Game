package tablock.level;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;

import java.io.Serial;
import java.io.Serializable;

public abstract class Selectable implements Serializable
{
    @Serial
    private static final long serialVersionUID = 2724148026362484980L;

    public abstract Shape getShape(double scale);
    public abstract void renderObject(GraphicsContext gc);
    public abstract void translate(Point2D translation, int gridSize);

    public void renderOutline(boolean highlighted, boolean selected, GraphicsContext gc)
    {
        gc.setLineWidth(5);

        if(highlighted && selected)
            gc.setLineDashes(10);

        gc.setStroke(highlighted && !selected ? Color.RED.desaturate().desaturate() : Color.LIGHTGREEN);
    }
}
