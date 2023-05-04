package tablock.level;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import tablock.core.Input;
import tablock.core.Main;

import java.io.Serial;

public class Vertex extends Selectable
{
    @Serial
    private static final long serialVersionUID = -7071404509826592462L;
    private static final Image VERTEX_TEXTURE = Main.getTexture("vertex");
    protected int index;

    public Vertex(double worldX, double worldY, int index)
    {
        super(new double[]{worldX}, new double[]{worldY});

        this.index = index;
    }

    @Override
    public boolean isHoveredByMouse()
    {
        return new Circle(screenXValues[0], screenYValues[0], 12.5).contains(Input.getMousePosition());
    }

    @Override
    public void renderObject(GraphicsContext gc)
    {
        gc.setFill(Color.BLACK);
        gc.fillOval(screenXValues[0] - 15, screenYValues[0] - 15, 30, 30);
        gc.drawImage(VERTEX_TEXTURE, screenXValues[0] - 12.5, screenYValues[0] - 12.5);
    }

    @Override
    public void renderOutline(boolean highlighted, boolean selected, GraphicsContext gc)
    {
        gc.setLineWidth(5);

        if(highlighted && selected)
            gc.setLineDashes(8);

        gc.setStroke(highlighted && !selected ? Color.RED.desaturate().desaturate() : Color.LIGHTGREEN);
        gc.strokeOval(screenXValues[0] - 15, screenYValues[0] - 15, 30, 30);
        gc.setLineDashes(0);
    }
}