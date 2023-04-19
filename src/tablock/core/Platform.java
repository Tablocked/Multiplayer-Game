package tablock.core;

import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

public class Platform extends SimulationBody
{
    public Platform(Vertex[] vertices)
    {
        Vector2[] vectors = new Vector2[vertices.length];

        for(int i = 0; i < vectors.length; i++)
            vectors[i] = new Vector2(vertices[i].x(), vertices[i].y());

        addFixture(Geometry.createPolygon(vectors));
        setMass(MassType.INFINITE);
    }
}