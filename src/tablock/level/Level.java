package tablock.level;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;

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
        for(Platform object : objects)
        {
            object.updateScreenValues(offset, scale);
            object.renderObject(gc);
        }
    }

    public List<Platform> getObjects()
    {
        return objects;
    }
}