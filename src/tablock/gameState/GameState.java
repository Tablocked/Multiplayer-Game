package tablock.gameState;

import javafx.scene.canvas.GraphicsContext;
import tablock.network.Client;

public abstract class GameState
{
    Client client;

    abstract void renderNextFrame(GraphicsContext gc);
}
