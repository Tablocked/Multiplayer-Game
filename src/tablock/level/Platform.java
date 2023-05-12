package tablock.level;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;

import java.io.Serial;

public class Platform extends Selectable
{
    @Serial
    private static final long serialVersionUID = 7944900121369452854L;
    private boolean simplePolygon = true;

    public Platform(double[] worldXValues, double[] worldYValues)
    {
        super(worldXValues,worldYValues);
    }

    @Override
    public Shape getShape()
    {
        Polygon polygon = new Polygon();

        for(int i = 0; i < vertexCount; i++)
            polygon.getPoints().addAll(screenXValues[i], screenYValues[i]);

        return polygon;
    }

    @Override
    public void renderObject(GraphicsContext gc)
    {
        gc.setFill(simplePolygon ? Color.BLACK : Color.DARKRED);

        gc.fillPolygon(screenXValues, screenYValues, vertexCount);
    }

    @Override
    public void renderOutline(boolean highlighted, boolean selected, GraphicsContext gc)
    {
        gc.setLineWidth(10);

        if(highlighted && selected)
            gc.setLineDashes(20);

        gc.setStroke(highlighted && !selected ? Color.RED.desaturate().desaturate() : Color.LIGHTGREEN);
        gc.strokePolygon(screenXValues, screenYValues, vertexCount);
        gc.setLineDashes(0);
    }

    public void renderComplexPolygonAlert(double opacity, GraphicsContext gc)
    {
        gc.setFill(Color.rgb(255, 255, 0, opacity));
        gc.fillPolygon(screenXValues, screenYValues, vertexCount);
    }

    public boolean calculateSimplePolygon()
    {
        simplePolygon = true;

        for(int i = 0; i < vertexCount; i++)
        {
            double x1 = worldXValues[i];
            double y1 = worldYValues[i];
            double x2 = worldXValues[(i + 1) % vertexCount];
            double y2 = worldYValues[(i + 1) % vertexCount];

            for(int j = 0; j < vertexCount && simplePolygon; j++)
            {
                double x3 = worldXValues[j];
                double y3 = worldYValues[j];
                double x4 = worldXValues[(j + 1) % vertexCount];
                double y4 = worldYValues[(j + 1) % vertexCount];
                double a = ((x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4));
                double t = (((x1 - x3) * (y3 - y4)) - ((y1 - y3) * (x3 - x4))) / a;
                double u = (((x1 - x3) * (y1 - y2)) - ((y1 - y3) * (x1 - x2))) / a;

                if(t < 1 && t > 0 && u < 1 && u > 0)
                    simplePolygon = false;
            }
        }

        return simplePolygon;
    }

    public Point2D getScreenCenter()
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
}