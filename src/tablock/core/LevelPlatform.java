package tablock.core;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class LevelPlatform
{
    private final int vertexCount;
    private final double[] worldXValues;

    private final double[] worldYValues;
    private final double[] screenXValues;
    private final double[] screenYValues;

    public LevelPlatform(Vertex... vertices)
    {
        vertexCount = vertices.length;
        worldXValues = new double[vertexCount];
        worldYValues = new double[vertexCount];
        screenXValues = new double[vertexCount];
        screenYValues = new double[vertexCount];

        for(int i = 0; i < vertexCount; i++)
        {
            worldXValues[i] = vertices[i].x();
            worldYValues[i] = vertices[i].y();
        }
    }

    public void translate(Point2D point2D)
    {
        for(int i = 0; i < vertexCount; i++)
        {
            worldXValues[i] += point2D.getX();
            worldYValues[i] += point2D.getY();
        }
    }

    public void transformScreenValues(Point2D offset, double scale)
    {
        for(int i = 0; i < vertexCount; i++)
        {
            screenXValues[i] = (worldXValues[i] * scale) + offset.getX();
            screenYValues[i] = (worldYValues[i] * scale) + offset.getY();
        }
    }

    public void render(GraphicsContext gc)
    {
        gc.setFill(Color.BLACK);
        gc.fillPolygon(screenXValues, screenYValues, vertexCount);
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