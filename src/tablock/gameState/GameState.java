package tablock.gameState;

import javafx.scene.canvas.GraphicsContext;
import tablock.network.Client;

public abstract class GameState
{
    static Client CLIENT;

    public static void initialize(Client client)
    {
        CLIENT = client;
    }

    public abstract void renderNextFrame(GraphicsContext gc);
}
