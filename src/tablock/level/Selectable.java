package tablock.level;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.Shape;

import java.io.Serial;
import java.io.Serializable;

public abstract class Selectable implements Serializable
{
    @Serial
    private static final long serialVersionUID = 2724148026362484980L;
    protected int vertexCount;
    protected double[] worldXValues;
    protected double[] worldYValues;
    protected double[] screenXValues;
    protected double[] screenYValues;

    public Selectable(double[] worldXValues, double[] worldYValues)
    {
        vertexCount = worldXValues.length;

        this.worldXValues = worldXValues;
        this.worldYValues = worldYValues;

        screenXValues = new double[vertexCount];
        screenYValues = new double[vertexCount];
    }

    private void updateScreenValue(int index, Point2D offset, double scale)
    {
        screenXValues[index] = (worldXValues[index] * scale) + offset.getX();
        screenYValues[index] = (worldYValues[index] * scale) + offset.getY();
    }

    public abstract Shape getShape();
    public abstract void renderObject(GraphicsContext gc);
    public abstract void renderOutline(boolean highlighted, boolean selected, GraphicsContext gc);

    public void updateScreenValues(Point2D offset, double scale)
    {
        for(int i = 0; i < vertexCount; i++)
            updateScreenValue(i, offset, scale);
    }

    public void translate(Point2D point2D, Point2D offset, double scale)
    {
        for(int i = 0; i < vertexCount; i++)
        {
            worldXValues[i] += point2D.getX();
            worldYValues[i] += point2D.getY();

            updateScreenValue(i, offset, scale);
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
}
