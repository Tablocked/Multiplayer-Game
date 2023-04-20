package tablock.gameState;

import javafx.scene.canvas.GraphicsContext;

public interface GameState
{
    void renderNextFrame(GraphicsContext gc);
}
