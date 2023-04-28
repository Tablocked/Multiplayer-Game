package tablock.level;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import tablock.core.Input;

import java.io.Serial;
import java.io.Serializable;

public class Platform implements Selectable, Serializable
{
    @Serial
    private static final long serialVersionUID = 7944900121369452854L;
    private final int vertexCount = 4;
    private final double[] worldXValues;

    private final double[] worldYValues;
    private final double[] screenXValues;
    private final double[] screenYValues;

    public Platform(Point2D point1, Point2D point2)
    {
        worldXValues = new double[]{point1.getX(), point1.getX(), point2.getX(), point2.getX()};
        worldYValues = new double[]{point1.getY(), point2.getY(), point2.getY(), point1.getY()};
        screenXValues = new double[vertexCount];
        screenYValues = new double[vertexCount];
    }

    public void transformScreenValues(Point2D offset, double scale)
    {
        for(int i = 0; i < vertexCount; i++)
        {
            screenXValues[i] = (worldXValues[i] * scale) + offset.getX();
            screenYValues[i] = (worldYValues[i] * scale) + offset.getY();
        }
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
    public void translate(Point2D point2D)
    {
        for(int i = 0; i < vertexCount; i++)
        {
            worldXValues[i] += point2D.getX();
            worldYValues[i] += point2D.getY();
        }
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
        if(highlighted || selected)
        {
            gc.setLineWidth(10);

            if(highlighted && selected)
                gc.setLineDashes(20);

            gc.setStroke(highlighted && !selected ? Color.RED.desaturate().desaturate() : Color.LIGHTGREEN);
            gc.strokePolygon(screenXValues, screenYValues, screenXValues.length);

            gc.setLineDashes(0);
        }
    }

    public double[] getWorldXValues()
    {
        return worldXValues;
    }

    public double[] getWorldYValues()
    {
        return worldYValues;
    }

    public double[] getScreenXValues()
    {
        return screenXValues;
    }

    public double[] getScreenYValues()
    {
        return screenYValues;
    }
}