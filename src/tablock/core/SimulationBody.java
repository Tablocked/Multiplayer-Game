package tablock.core;

import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Polygon;
import org.dyn4j.geometry.Vector2;

public class SimulationBody extends Body
{
    public Vector2[] getVertices()
    {
        Polygon polygon = (Polygon) getFixture(0).getShape();
        int sides = polygon.getVertices().length;
        Vector2[] vertices = new Vector2[sides];

        for(int index = 0; index < sides; index++)
        {
            vertices[index] = getWorldPoint(polygon.getVertices()[index]);
        }

        return vertices;
    }
}