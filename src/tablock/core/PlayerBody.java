package tablock.core;

import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;

public class PlayerBody extends SimulationBody
{
    public PlayerBody(double x, double y)
    {
        addFixture(Geometry.createRectangle(50, 50));
        translate(x, y);
        setMass(MassType.NORMAL);
    }
}