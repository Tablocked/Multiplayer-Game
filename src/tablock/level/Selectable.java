package tablock.level;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;

public interface Selectable
{
    boolean isHoveredByMouse();
    void translate(Point2D point2D);
    void renderObject(GraphicsContext gc);
    void renderOutline(boolean highlighted, boolean selected, GraphicsContext gc);
}
