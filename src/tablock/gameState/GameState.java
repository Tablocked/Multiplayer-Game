package tablock.gameState;

import javafx.scene.canvas.GraphicsContext;
import tablock.network.Client;

public abstract class GameState
{
    static Client CLIENT;

    abstract void renderNextFrame(GraphicsContext gc);
}
