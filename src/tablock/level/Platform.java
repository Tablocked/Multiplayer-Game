package tablock.level;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import tablock.core.Input;

import java.io.Serial;

public class Platform extends Selectable
{
    @Serial
    private static final long serialVersionUID = 7944900121369452854L;

    public Platform(Point2D point1, Point2D point2)
    {
        super(new double[]{point1.getX(), point1.getX(), point2.getX(), point2.getX()}, new double[]{point1.getY(), point2.getY(), point2.getY(), point1.getY()});
    }

    @Override
    public boolean isHoveredByMouse()
    {
        Polygon polygon = new Polygon();

        for(int i = 0; i < vertexCount; i++)
            polygon.getPoints().addAll(screenXValues[i], screenYValues[i]);

        return polygon.contains(Input.getMousePosition());
    }

    @Override
    public void renderObject(GraphicsContext gc)
    {
        gc.setFill(Color.BLACK);
        gc.fillPolygon(screenXValues, screenYValues, screenXValues.length);
    }

    @Override
    public void renderOutline(boolean highlighted, boolean selected, GraphicsContext gc)
    {
        gc.setLineWidth(10);

        if(highlighted && selected)
            gc.setLineDashes(20);

        gc.setStroke(highlighted && !selected ? Color.RED.desaturate().desaturate() : Color.LIGHTGREEN);
        gc.strokePolygon(screenXValues, screenYValues, screenXValues.length);

        gc.setLineDashes(0);
    }
}