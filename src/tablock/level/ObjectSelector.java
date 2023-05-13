package tablock.level;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.dyn4j.geometry.Vector2;
import tablock.core.Input;
import tablock.core.VectorUtilities;

import java.util.ArrayList;
import java.util.List;

public class ObjectSelector extends Selector<Platform>
{
    private List<Point2D[]> vertexPositionsDuringTransformationStart;
    private Point2D objectCenterDuringTransformationStart;
    private Point2D mousePositionDuringScaleStart;
    private Point2D mousePositionDuringRotateStart;
    private boolean verticesWereNeverSelected = true;
    private Vector2 addVertexIndicatorPosition;
    private final List<Platform> complexPlatforms = new ArrayList<>();
    private final Selector<Vertex> vertexSelector = new Selector<>();

    public ObjectSelector(List<Platform> objects)
    {
        super(objects);
    }

    private void resetVertexSelector()
    {
        vertexSelector.objectBeingClicked = null;
        vertexSelector.previousMousePositionDuringDrag = null;
        vertexSelector.objectWasJustSelected = false;
        vertexSelector.objectWasNeverMoved = true;

        vertexSelector.objects.clear();
        vertexSelector.hoveredObjects.clear();
        vertexSelector.selectedObjects.clear();
    }

    private void initializeVertexSelector()
    {
        Platform platform = selectedObjects.get(0);

        resetVertexSelector();

        for(int i = 0; i < platform.vertexCount; i++)
            vertexSelector.objects.add(new Vertex(platform, i));
    }

    @Override
    public void calculateHoveredObjects(boolean objectsAreSelectable, Point2D worldMouse, double scale)
    {
        if(vertexPositionsDuringTransformationStart == null)
            vertexSelector.calculateHoveredObjects(objectsAreSelectable, worldMouse, scale);
        else
            vertexSelector.hoveredObjects.clear();

        super.calculateHoveredObjects(objectsAreSelectable && addVertexIndicatorPosition == null && vertexPositionsDuringTransformationStart == null && vertexSelector.hoveredObjects.size() == 0 && vertexSelector.objectBeingClicked == null, worldMouse, scale);
    }

    public void tick(boolean objectsAreSelectable, boolean noSelectionInProgress, Point2D worldMouse, double scale)
    {
        if(objectBeingClicked != null && previousMousePositionDuringDrag != null && vertexSelector.selectedObjects.size() != 0)
            verticesWereNeverSelected = false;

        calculateAndDragSelectedObjects(objectsAreSelectable, verticesWereNeverSelected && addVertexIndicatorPosition == null, worldMouse);

        if(objectBeingClicked != null && selectedObjects.size() == 1)
            initializeVertexSelector();

        addVertexIndicatorPosition = null;

        if(selectedObjects.size() != 0 && objectBeingClicked == null && vertexSelector.objectBeingClicked == null && objectsAreSelectable)
        {
            if(Input.S.wasJustActivated())
            {
                if(mousePositionDuringScaleStart == null)
                {
                    double totalVertexCount = 0;
                    double sumX = 0;
                    double sumY = 0;

                    vertexPositionsDuringTransformationStart = new ArrayList<>();

                    for(Platform platform : selectedObjects)
                    {
                        Point2D[] vertices = new Point2D[platform.vertexCount];

                        for(int i = 0; i < platform.vertexCount; i++)
                        {
                            vertices[i] = new Point2D(platform.worldXValues[i], platform.worldYValues[i]);

                            sumX += platform.worldXValues[i];
                            sumY += platform.worldYValues[i];

                            totalVertexCount++;
                        }

                        vertexPositionsDuringTransformationStart.add(vertices);
                    }

                    mousePositionDuringScaleStart = worldMouse;
                    objectCenterDuringTransformationStart = new Point2D(sumX / totalVertexCount, sumY / totalVertexCount);
                }
                else
                    confirmScale();
            }

            scaleSelectedObjects(worldMouse);

            if(Input.R.wasJustActivated())
            {
                if(mousePositionDuringRotateStart == null)
                {
                    double totalVertexCount = 0;
                    double sumX = 0;
                    double sumY = 0;

                    vertexPositionsDuringTransformationStart = new ArrayList<>();

                    for(Platform platform : selectedObjects)
                    {
                        Point2D[] vertices = new Point2D[platform.vertexCount];

                        for(int i = 0; i < platform.vertexCount; i++)
                        {
                            vertices[i] = new Point2D(platform.worldXValues[i], platform.worldYValues[i]);

                            sumX += platform.worldXValues[i];
                            sumY += platform.worldYValues[i];

                            totalVertexCount++;
                        }

                        vertexPositionsDuringTransformationStart.add(vertices);
                    }

                    mousePositionDuringRotateStart = worldMouse;
                    objectCenterDuringTransformationStart = new Point2D(sumX / totalVertexCount, sumY / totalVertexCount);
                }
                else
                {
                    mousePositionDuringRotateStart = null;
                    objectCenterDuringTransformationStart = null;
                    vertexPositionsDuringTransformationStart = null;
                }
            }

            if(mousePositionDuringRotateStart != null)
            {
                Point2D startMouse = mousePositionDuringRotateStart.subtract(objectCenterDuringTransformationStart);
                Point2D currentMouse = worldMouse.subtract(objectCenterDuringTransformationStart);
                double angleBetweenStartMouseAndCenter = Math.signum(-startMouse.getY()) * Math.toRadians(startMouse.angle(1, 0));
                double sign = (currentMouse.getX() * Math.sin(angleBetweenStartMouseAndCenter)) + (currentMouse.getY() * Math.cos(angleBetweenStartMouseAndCenter)) >= 0 ? 1 : -1;
                double angle = sign * Math.toRadians(startMouse.angle(currentMouse));

                for(int i = 0; i < selectedObjects.size(); i++)
                {
                    Platform platform = selectedObjects.get(i);
                    Point2D[] vertices = vertexPositionsDuringTransformationStart.get(i);

                    for(int j = 0; j < platform.vertexCount; j++)
                    {
                        Point2D localVertex = vertices[j].subtract(objectCenterDuringTransformationStart);
                        double x = (localVertex.getX() * Math.cos(angle)) - (localVertex.getY() * Math.sin(angle));
                        double y = (localVertex.getX() * Math.sin(angle)) + (localVertex.getY() * Math.cos(angle));
                        Point2D vertex = new Point2D(x, y).add(objectCenterDuringTransformationStart);

                        platform.worldXValues[j] = vertex.getX();
                        platform.worldYValues[j] = vertex.getY();
                    }
                }
            }
        }

        if(selectedObjects.size() == 1 && objectsAreSelectable && vertexPositionsDuringTransformationStart == null)
        {
            Platform platform = selectedObjects.get(0);

            if(vertexSelector.objects.size() == 0)
                initializeVertexSelector();

            if(!objectWasJustSelected)
                for(int i = 0; i < platform.vertexCount; i++)
                {
                    int endPointIndex = (i + 1) % platform.vertexCount;
                    Vector2 startPoint = new Vector2(platform.worldXValues[i], platform.worldYValues[i]);
                    Vector2 endPoint = new Vector2(platform.worldXValues[endPointIndex], platform.worldYValues[endPointIndex]);
                    Vector2 worldMouseVector = new Vector2(worldMouse.getX(), worldMouse.getY());
                    Vector2 mouseProjection = VectorUtilities.projectPointOntoLine(startPoint, endPoint, worldMouseVector);

                    if(VectorUtilities.isProjectionOnLineSegment(mouseProjection, startPoint, endPoint) && mouseProjection.distance(worldMouseVector) < 20 * (1 / scale))
                    {
                        if(Input.MOUSE_LEFT.wasJustActivated() && vertexSelector.hoveredObjects.size() == 0)
                        {
                            platform.vertexCount++;

                            double[] newWorldXValues = new double[platform.vertexCount];
                            double[] newWorldYValues = new double[platform.vertexCount];

                            resetVertexSelector();

                            for(int j = 0; j < platform.vertexCount; j++)
                            {
                                int oldIndex = j > endPointIndex ? j - 1 : j;

                                newWorldXValues[j] = j == endPointIndex ? mouseProjection.x : platform.worldXValues[oldIndex];
                                newWorldYValues[j] = j == endPointIndex ? mouseProjection.y : platform.worldYValues[oldIndex];

                                vertexSelector.objects.add(new Vertex(platform, j));
                            }

                            platform.worldXValues = newWorldXValues;
                            platform.worldYValues = newWorldYValues;
                            platform.screenXValues = new double[platform.vertexCount];
                            platform.screenYValues = new double[platform.vertexCount];
                        }

                        addVertexIndicatorPosition = mouseProjection;

                        break;
                    }
                }

            if(addVertexIndicatorPosition == null && vertexSelector.objectBeingClicked == null && vertexSelector.hoveredObjects.size() == 0 && !hoveredObjects.contains(platform) && platform.getShape(scale).contains(worldMouse))
                hoveredObjects.add(selectedObjects.get(0));

            vertexSelector.calculateAndDragSelectedObjects(true, noSelectionInProgress, worldMouse);

            if(platform.calculateSimplePolygon())
                complexPlatforms.remove(platform);
            else if(!complexPlatforms.contains(platform))
                complexPlatforms.add(platform);
        }
        else if(noSelectionInProgress)
            resetVertexSelector();

        if(Input.DELETE.wasJustActivated())
        {
            if(vertexSelector.selectedObjects.size() == 0)
            {
                objects.removeAll(selectedObjects);
                hoveredObjects.removeAll(selectedObjects);
                complexPlatforms.removeAll(selectedObjects);
                selectedObjects.clear();

                resetVertexSelector();
            }
            else if(selectedObjects.size() == 1 && vertexSelector.objects.size() - vertexSelector.selectedObjects.size() > 2)
            {
                int[] indicesMarkedForRemoval = new int[vertexSelector.selectedObjects.size()];
                int indexOffset = 0;
                Platform platform = selectedObjects.get(0);

                for(int i = 0; i < indicesMarkedForRemoval.length; i++)
                    indicesMarkedForRemoval[i] = vertexSelector.selectedObjects.get(i).index;

                resetVertexSelector();

                platform.vertexCount -= indicesMarkedForRemoval.length;

                for(int i = 0; i < platform.vertexCount; i++)
                {
                    for(int indexMarkedForRemoval : indicesMarkedForRemoval)
                        if(i + indexOffset == indexMarkedForRemoval)
                            indexOffset++;

                    platform.worldXValues[i] = platform.worldXValues[i + indexOffset];
                    platform.worldYValues[i] = platform.worldYValues[i + indexOffset];

                    vertexSelector.objects.add(new Vertex(platform, i));
                }

                platform.screenXValues = new double[platform.vertexCount];
                platform.screenYValues = new double[platform.vertexCount];
            }
        }
    }

    public void render(boolean noInterfaceButtonsSelected, Point2D offset, double scale, GraphicsContext gc)
    {
        for(Platform platform : objects)
        {
            platform.updateScreenValues(offset, scale);
            platform.renderObject(gc);
        }

        renderObjectOutlines(gc);

        if(!noInterfaceButtonsSelected)
        {
            resetVertexSelector();

            hoveredObjects.clear();
        }

        if(selectedObjects.size() == 1)
        {
            if(addVertexIndicatorPosition != null && vertexSelector.hoveredObjects.size() == 0)
                Vertex.renderAddVertex((addVertexIndicatorPosition.x * scale) + offset.getX(), (addVertexIndicatorPosition.y * scale) + offset.getY(), gc);

            for(Vertex vertex : vertexSelector.objects)
                vertex.renderObject(gc);

            vertexSelector.renderObjectOutlines(gc);
        }
    }

    public void addObject(Platform object)
    {
        selectedObjects.clear();

        objects.add(object);
        selectedObjects.add(object);

        initializeVertexSelector();
    }

    public void deselectAllObjects()
    {
        selectedObjects.clear();

        resetVertexSelector();
    }

    public void selectAllObjectsIntersectingRectangle(Rectangle rectangle, double scale)
    {
        super.selectAllObjectsIntersectingRectangle(rectangle, scale);

        if(selectedObjects.size() == 1)
        {
            vertexSelector.selectAllObjectsIntersectingRectangle(rectangle, scale);
        }
        else
            resetVertexSelector();
    }

    public void scaleSelectedObjects(Point2D worldMouse)
    {
        if(mousePositionDuringScaleStart != null)
        {
            double scaleFactor = -1;

            if(worldMouse != null)
            {
                double startMouseDistance = mousePositionDuringScaleStart.distance(objectCenterDuringTransformationStart);
                double sign = mousePositionDuringScaleStart.subtract(objectCenterDuringTransformationStart).angle(worldMouse.subtract(objectCenterDuringTransformationStart)) < 90 ? 1 : -1;

                scaleFactor = sign * (((worldMouse.distance(objectCenterDuringTransformationStart) - startMouseDistance) / startMouseDistance) + 1);
            }

            for(int i = 0; i < selectedObjects.size(); i++)
            {
                Platform platform = selectedObjects.get(i);
                Point2D[] vertices = vertexPositionsDuringTransformationStart.get(i);

                for(int j = 0; j < platform.vertexCount; j++)
                {
                    Point2D vertex = worldMouse != null ? vertices[j].subtract(objectCenterDuringTransformationStart).multiply(scaleFactor).add(objectCenterDuringTransformationStart) : vertices[j];

                    platform.worldXValues[j] = vertex.getX();
                    platform.worldYValues[j] = vertex.getY();
                }
            }
        }
    }

    public void confirmScale()
    {
        mousePositionDuringScaleStart = null;
        objectCenterDuringTransformationStart = null;
        vertexPositionsDuringTransformationStart = null;
    }

    public void renderScaleIndicator(Point2D offset, double scale, GraphicsContext gc)
    {
        double centerX = (objectCenterDuringTransformationStart.getX() * scale) + offset.getX();
        double centerY = (objectCenterDuringTransformationStart.getY() * scale) + offset.getY();

        gc.setFill(Color.GOLD);
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(5);
        gc.setLineDashes(10);
        gc.fillOval(centerX - 15, centerY - 15, 30, 30);
        gc.strokeLine(centerX, centerY, Input.getMousePosition().getX(), Input.getMousePosition().getY());
        gc.setLineDashes(0);
    }

    public boolean isAnObjectBeingClicked()
    {
        return objectBeingClicked != null || vertexSelector.objectBeingClicked != null;
    }

    public boolean areNoObjectsHovered()
    {
        return hoveredObjects.size() == 0 && addVertexIndicatorPosition == null && vertexPositionsDuringTransformationStart == null && vertexSelector.hoveredObjects.size() == 0;
    }

    public boolean isScaleInProgress()
    {
        return mousePositionDuringScaleStart != null;
    }

    public List<Platform> getComplexPlatforms()
    {
        return complexPlatforms;
    }
}