package tablock.core;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Level implements Serializable
{
    @Serial
    private static final long serialVersionUID = 7221953647432304606L;
    private final List<Platform> objects = new ArrayList<>();

    public void render(Point2D offset, double scale, GraphicsContext gc)
    {
        gc.setFill(Color.BLACK);

        for(Platform object : objects)
        {
            object.transformScreenValues(offset, scale);

            gc.fillPolygon(object.getScreenXValues(), object.getScreenYValues(), object.getScreenXValues().length);
        }
    }

    public List<Platform> getObjects()
    {
        return objects;
    }
}