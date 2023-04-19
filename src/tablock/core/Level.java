package tablock.core;

import java.io.Serial;
import java.io.Serializable;

public class Level implements Serializable
{
    @Serial
    private static final long serialVersionUID = 4334452L;
    private final Vertex[][] platformVertices;

    public Level(Vertex[]... platformVertices)
    {
        this.platformVertices = platformVertices;
    }

    public void addPlatformsToSimulation(Simulation simulation)
    {
        for(Platform platform : getPlatforms())
            simulation.addBody(platform);
    }

    public Platform[] getPlatforms()
    {
        Platform[] platforms = new Platform[platformVertices.length];

        for(int i = 0; i < platforms.length; i++)
            platforms[i] = new Platform(platformVertices[i]);

        return platforms;
    }
}