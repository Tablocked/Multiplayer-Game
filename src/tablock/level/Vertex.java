package tablock.level;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import tablock.core.Main;

import java.io.Serial;

public class Vertex extends Selectable
{
    @Serial
    private static final long serialVersionUID = -7071404509826592462L;
    int index;
    private final Platform platform;

    public Vertex(Platform platform, int index)
    {
        this.platform = platform;
        this.index = index;
    }

    public static void renderAddVertex(double x, double y, GraphicsContext gc)
    {
        gc.drawImage(Main.ADD_VERTEX_TEXTURE, x - 12.5, y - 12.5);
        gc.setFill(Color.rgb(255, 200, 0, 0.5));
        gc.fillOval(x - 15, y - 15, 30, 30);
    }

    @Override
    public Shape getShape(double scale)
    {
        return new Circle(platform.worldXValues[index], platform.worldYValues[index], 12.5 * (1 / scale));
    }

    @Override
    public void renderObject(GraphicsContext gc)
    {
        gc.setFill(Color.rgb(255, 200, 0));
        gc.fillOval(platform.screenXValues[index] - 15, platform.screenYValues[index] - 15, 30, 30);
        gc.drawImage(Main.VERTEX_TEXTURE, platform.screenXValues[index] - 12.5, platform.screenYValues[index] - 12.5);
    }

    @Override
    public void translate(Point2D translation)
    {
        platform.worldXValues[index] += translation.getX();
        platform.worldYValues[index] += translation.getY();
    }

    @Override
    public void renderOutline(boolean highlighted, boolean selected, GraphicsContext gc)
    {
        super.renderOutline(highlighted, selected, gc);

        gc.strokeOval(platform.screenXValues[index] - 15, platform.screenYValues[index] - 15, 30, 30);
        gc.setLineDashes(0);
    }
}