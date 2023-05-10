package tablock.level;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import org.dyn4j.geometry.Vector2;
import tablock.core.Input;
import tablock.core.VectorUtilities;

import java.util.ArrayList;
import java.util.List;

public class Selector<T extends Selectable>
{
    private T objectBeingClicked;
    private Point2D previousMousePosition;
    private boolean objectWasJustSelected;
    private boolean objectWasNeverMoved = true;
    private boolean verticesWereNeverSelected = true;
    private Vector2 addVertexIndicatorPosition;
    private final List<T> objects;
    private final List<T> hoveredObjects = new ArrayList<>();
    private final List<T> selectedObjects = new ArrayList<>();
    private final List<Platform> complexPlatforms = new ArrayList<>();
    private final Selector<Vertex> vertexSelector;

    public Selector()
    {
        objects = new ArrayList<>();
        vertexSelector = null;
    }

    public Selector(List<T> objects, boolean movableVertices)
    {
        this.objects = objects;

        vertexSelector = movableVertices ? new Selector<>() : null;
    }

    private void resetVertexSelector()
    {
        if(vertexSelector != null)
        {
            vertexSelector.objectBeingClicked = null;

            vertexSelector.objects.clear();
            vertexSelector.hoveredObjects.clear();
            vertexSelector.selectedObjects.clear();
        }
    }

    private void initializeVertexSelector(Point2D offset, double scale)
    {
        if(vertexSelector != null && selectedObjects.size() == 1)
        {
            Selectable onlySelectedObject = selectedObjects.get(0);

            for(int i = 0; i < onlySelectedObject.vertexCount; i++)
            {
                Vertex vertex = new Vertex(onlySelectedObject.worldXValues[i], onlySelectedObject.worldYValues[i]);

                vertex.updateScreenValues(offset, scale);

                vertexSelector.objects.add(vertex);
            }
        }
    }

    public void addObject(T object, Point2D offset, double scale)
    {
        objects.add(object);
        selectedObjects.add(object);

        initializeVertexSelector(offset, scale);
    }

    public void calculateHoveredObjects(boolean objectsAreSelectable, boolean mouseNeverMovedDuringSelection, double scale, Point2D worldMouse, Point2D offset)
    {
        if(vertexSelector != null)
            vertexSelector.calculateHoveredObjects(objectsAreSelectable, mouseNeverMovedDuringSelection, scale, worldMouse, offset);

        hoveredObjects.clear();

        for(T object : objects)
        {
            object.updateScreenValues(offset, scale);

            if(objectsAreSelectable && object.getShape().contains(Input.getMousePosition()) && (vertexSelector == null || (vertexSelector.areNoObjectsHovered() && vertexSelector.objectBeingClicked == null)))
            {
                if(Input.MOUSE_LEFT.wasJustActivated())
                {
                    objectBeingClicked = object;
                    previousMousePosition = worldMouse;
                }

                hoveredObjects.add(object);
            }
        }

        if((Input.MOUSE_LEFT.wasJustActivated() && vertexSelector != null && objectBeingClicked == null && vertexSelector.objectBeingClicked == null && objectsAreSelectable && addVertexIndicatorPosition == null) || mouseNeverMovedDuringSelection)
        {
            selectedObjects.clear();

            resetVertexSelector();
        }
    }

    public void calculateAndDragSelectedObjects(boolean objectsAreSelectable, Point2D offset, double scale, Point2D worldMouse)
    {
        if(objectBeingClicked != null && previousMousePosition != null)
        {
            if(!selectedObjects.contains(objectBeingClicked))
                objectWasJustSelected = true;

            if(previousMousePosition.distance(worldMouse) != 0)
                objectWasNeverMoved = false;

            if(vertexSelector != null && vertexSelector.selectedObjects.size() != 0)
                verticesWereNeverSelected = false;

            if(!Input.isShiftPressed())
            {
                if(selectedObjects.size() > 1)
                    objectWasJustSelected = true;

                selectedObjects.clear();
                selectedObjects.add(objectBeingClicked);

                if(hoveredObjects.size() != 1)
                    hoveredObjects.remove(objectBeingClicked);
            }
            else if(!selectedObjects.contains(objectBeingClicked))
                selectedObjects.add(objectBeingClicked);

            if(!(Input.MOUSE_LEFT.isActive() && Input.isShiftPressed()) && objectWasNeverMoved)
            {
                int startingIndex = objects.indexOf(objectBeingClicked);
                List<T> objectsCopy = new ArrayList<>(objects);

                objects.clear();

                for(int i = 0; i < objectsCopy.size(); i++)
                    objects.add(objectsCopy.get((startingIndex + i) % objectsCopy.size()));
            }

            if(objectsAreSelectable)
            {
                if(Input.MOUSE_LEFT.isActive())
                {
                    Point2D translation = previousMousePosition.subtract(worldMouse);

                    for(Selectable selectedObject : selectedObjects)
                        selectedObject.translate(translation, offset, scale);

                    previousMousePosition = worldMouse;

                    hoveredObjects.add(objectBeingClicked);
                }
                else if(!objectWasJustSelected && objectWasNeverMoved && verticesWereNeverSelected && addVertexIndicatorPosition == null)
                    selectedObjects.remove(objectBeingClicked);
            }

            resetVertexSelector();
            initializeVertexSelector(offset, scale);
        }

        addVertexIndicatorPosition = null;

        if(vertexSelector != null && selectedObjects.size() == 1)
        {
            Platform platform = (Platform) selectedObjects.get(0);

            if(vertexSelector.hoveredObjects.size() == 0 && !objectWasJustSelected && objectsAreSelectable)
                for(int i = 0; i < platform.vertexCount; i++)
                {
                    int endPointIndex = (i + 1) % platform.vertexCount;
                    Vector2 startPoint = new Vector2(platform.screenXValues[i], platform.screenYValues[i]);
                    Vector2 endPoint = new Vector2(platform.screenXValues[endPointIndex], platform.screenYValues[endPointIndex]);
                    Vector2 screenMouse = new Vector2(Input.getMousePosition().getX(), Input.getMousePosition().getY());
                    Vector2 screenMouseProjection = VectorUtilities.projectPointOntoLine(startPoint, endPoint, screenMouse);

                    if(VectorUtilities.isProjectionOnLineSegment(screenMouseProjection, startPoint, endPoint) && screenMouseProjection.distance(screenMouse) < 20)
                    {
                        Point2D worldMouseProjection = offset.subtract(screenMouseProjection.x, screenMouseProjection.y).multiply(-1 / scale);

                        if(Input.MOUSE_LEFT.wasJustActivated())
                        {
                            vertexSelector.objects.add(endPointIndex, new Vertex(worldMouseProjection.getX(), worldMouseProjection.getY(), screenMouseProjection.x, screenMouseProjection.y));

                            platform.vertexCount += 1;

                            platform.worldXValues = new double[platform.vertexCount];
                            platform.worldYValues = new double[platform.vertexCount];
                            platform.screenXValues = new double[platform.vertexCount];
                            platform.screenYValues = new double[platform.vertexCount];
                        }

                        addVertexIndicatorPosition = screenMouseProjection;

                        break;
                    }
                }

            vertexSelector.calculateAndDragSelectedObjects(objectsAreSelectable, offset, scale, worldMouse);

            for(int i = 0; i < vertexSelector.objects.size(); i++)
            {
                Vertex vertex = vertexSelector.objects.get(i);

                platform.worldXValues[i] = vertex.worldXValues[0];
                platform.worldYValues[i] = vertex.worldYValues[0];
                platform.screenXValues[i] = vertex.screenXValues[0];
                platform.screenYValues[i] = vertex.screenYValues[0];
            }

            if(platform.calculateSimplePolygon())
                complexPlatforms.remove(platform);
            else if(!complexPlatforms.contains(platform))
                    complexPlatforms.add(platform);
        }

        if(!Input.MOUSE_LEFT.isActive() || !objectsAreSelectable)
        {
            objectWasJustSelected = false;
            objectWasNeverMoved = true;
            verticesWereNeverSelected = true;
            objectBeingClicked = null;
            previousMousePosition = null;
        }

        if(Input.DELETE.wasJustActivated() && vertexSelector != null)
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
                Selectable object = selectedObjects.get(0);

                vertexSelector.objects.removeAll(vertexSelector.selectedObjects);
                vertexSelector.hoveredObjects.removeAll(vertexSelector.selectedObjects);
                vertexSelector.selectedObjects.clear();

                vertexSelector.objectBeingClicked = null;

                int newVertexCount = vertexSelector.objects.size();

                object.vertexCount = newVertexCount;
                object.worldXValues = new double[newVertexCount];
                object.worldYValues = new double[newVertexCount];
                object.screenXValues = new double[newVertexCount];
                object.screenYValues = new double[newVertexCount];

                for(int i = 0; i < newVertexCount; i++)
                {
                    Vertex vertex = vertexSelector.objects.get(i);

                    object.worldXValues[i] = vertex.worldXValues[0];
                    object.worldYValues[i] = vertex.worldYValues[0];
                    object.screenXValues[i] = vertex.screenXValues[0];
                    object.screenYValues[i] = vertex.screenYValues[0];
                }
            }
        }
    }

    public void render(GraphicsContext gc)
    {
        T lastHoveredObject = hoveredObjects.size() == 0 ? null : hoveredObjects.get(hoveredObjects.size() - 1);

        for(Selectable object : objects)
            object.renderObject(gc);

        if(lastHoveredObject != null && !selectedObjects.contains(lastHoveredObject))
            lastHoveredObject.renderOutline(true, false, gc);

        for(Selectable selectedObject : selectedObjects)
            selectedObject.renderOutline(selectedObject == lastHoveredObject, true, gc);

        if(vertexSelector != null && selectedObjects.size() == 1)
        {
            if(addVertexIndicatorPosition != null)
                Vertex.renderAddVertex(addVertexIndicatorPosition.x, addVertexIndicatorPosition.y, gc);

            vertexSelector.render(gc);
        }
    }

    public void selectAllObjectsIntersectingRectangle(double x, double y, double width, double height, Point2D offset, double scale)
    {
        for(T object : objects)
            if(object.getShape().intersects(x, y, width, height) && !selectedObjects.contains(object))
                selectedObjects.add(object);

        if(vertexSelector != null && selectedObjects.size() == 1)
        {
            if(vertexSelector.objects.size() == 0)
                initializeVertexSelector(offset, scale);
            else
                vertexSelector.selectAllObjectsIntersectingRectangle(x, y, width, height, offset, scale);
        }
        else
            resetVertexSelector();
    }

    public boolean isAnObjectBeingClicked()
    {
        return objectBeingClicked != null || (vertexSelector == null || vertexSelector.objectBeingClicked != null);
    }

    public boolean areNoObjectsHovered()
    {
        return hoveredObjects.size() == 0 && addVertexIndicatorPosition == null && (vertexSelector == null || vertexSelector.areNoObjectsHovered());
    }

    public List<Platform> getComplexPlatforms()
    {
        return complexPlatforms;
    }
}
