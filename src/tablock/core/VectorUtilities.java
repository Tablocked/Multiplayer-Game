package tablock.core;

import org.dyn4j.geometry.Vector2;

public class VectorUtilities
{
    public static Vector2 projectPointOntoLine(Vector2 startPoint, Vector2 endPoint, Vector2 point)
    {
        Vector2 projectionLine = endPoint.copy().subtract(startPoint);
        Vector2 projectionPoint = point.copy().subtract(startPoint);

        return projectionPoint.project(projectionLine).add(startPoint);
    }

    public static boolean isProjectionOnLineSegment(Vector2 projection, Vector2 startPoint, Vector2 endPoint)
    {
        double minX = Math.min(startPoint.x, endPoint.x) - 0.5;
        double maxX = Math.max(startPoint.x, endPoint.x) + 0.5;
        double minY = Math.min(startPoint.y, endPoint.y) - 0.5;
        double maxY = Math.max(startPoint.y, endPoint.y) + 0.5;

        return projection.x >= minX && projection.x <= maxX && projection.y >= minY && projection.y <= maxY;
    }
}