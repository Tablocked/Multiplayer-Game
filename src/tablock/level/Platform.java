package tablock.level;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import org.dyn4j.geometry.Vector2;

import java.io.Serial;

public class Platform extends Selectable
{
    @Serial
    private static final long serialVersionUID = 7944900121369452854L;
    int vertexCount;
    double[] worldXValues;
    double[] worldYValues;
    double[] screenXValues;
    double[] screenYValues;
    private boolean simplePolygon = true;

    public Platform(double[] worldXValues, double[] worldYValues)
    {
        vertexCount = worldXValues.length;

        this.worldXValues = worldXValues;
        this.worldYValues = worldYValues;

        screenXValues = new double[vertexCount];
        screenYValues = new double[vertexCount];
    }

    @Override
    public Shape getShape(double scale)
    {
        Polygon polygon = new Polygon();

        for(int i = 0; i < vertexCount; i++)
            polygon.getPoints().addAll(worldXValues[i], worldYValues[i]);

        return polygon;
    }

    @Override
    public void renderObject(GraphicsContext gc)
    {
        gc.setFill(simplePolygon ? Color.BLACK : Color.DARKRED);

        gc.fillPolygon(screenXValues, screenYValues, vertexCount);
    }

    @Override
    public void translate(Point2D translation, int gridSize)
    {
        for(int i = 0; i < vertexCount; i++)
        {
            worldXValues[i] += translation.getX();
            worldYValues[i] += translation.getY();
        }
    }

    @Override
    public void renderOutline(boolean highlighted, boolean selected, GraphicsContext gc)
    {
        super.renderOutline(highlighted, selected, gc);

        gc.strokePolygon(screenXValues, screenYValues, vertexCount);
        gc.setLineDashes(0);
    }

    public void updateScreenValues(Point2D offset, double scale)
    {
        for(int i = 0; i < vertexCount; i++)
        {
            screenXValues[i] = (worldXValues[i] * scale) + offset.getX();
            screenYValues[i] = (worldYValues[i] * scale) + offset.getY();
        }
    }

    public void renderComplexPolygonAlert(double opacity, GraphicsContext gc)
    {
        gc.setFill(Color.rgb(255, 255, 0, opacity));
        gc.fillPolygon(screenXValues, screenYValues, vertexCount);
    }

    public boolean calculateSimplePolygon()
    {
        simplePolygon = true;

        vertexLoop:
        for(int i = 0; i < vertexCount; i++)
        {
            double x1 = worldXValues[i];
            double y1 = worldYValues[i];
            double x2 = worldXValues[(i + 1) % vertexCount];
            double y2 = worldYValues[(i + 1) % vertexCount];

            for(int j = 0; j < vertexCount; j++)
            {
                double x3 = worldXValues[j];
                double y3 = worldYValues[j];
                double x4 = worldXValues[(j + 1) % vertexCount];
                double y4 = worldYValues[(j + 1) % vertexCount];
                double a = ((x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4));
                double t = (((x1 - x3) * (y3 - y4)) - ((y1 - y3) * (x3 - x4))) / a;
                double u = (((x1 - x3) * (y1 - y2)) - ((y1 - y3) * (x1 - x2))) / a;

                if((t < 1 && t > 0 && u < 1 && u > 0) || (i != j && x1 == x3 && y1 == y3))
                {
                    simplePolygon = false;

                    break vertexLoop;
                }
            }
        }

        return simplePolygon;
    }

    public Point2D calculateScreenCenter()
    {
        double sumX = 0;
        double sumY = 0;

        for(int i = 0; i < screenXValues.length; i++)
        {
            sumX += screenXValues[i];
            sumY += screenYValues[i];
        }

        return new Point2D(sumX / vertexCount, sumY / vertexCount);
    }

    public Vector2[] convertToVectorArray()
    {
        Vector2[] vertices = new Vector2[vertexCount];

        for(int i = 0; i < vertexCount; i++)
            vertices[i] = new Vector2(worldXValues[i], -worldYValues[i]);

        return vertices;
    }

    public boolean isSimplePolygon()
    {
        return simplePolygon;
    }
}