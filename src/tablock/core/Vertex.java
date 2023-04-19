package tablock.core;

import java.io.Serial;
import java.io.Serializable;

public record Vertex(double x, double y) implements Serializable
{
    @Serial
    private static final long serialVersionUID = 934901L;
}