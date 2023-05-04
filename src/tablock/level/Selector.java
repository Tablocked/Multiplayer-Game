package tablock.level;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import org.dyn4j.geometry.Polygon;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.geometry.decompose.SweepLine;
import tablock.core.Input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Selector<T extends Selectable>
{
    private T objectBeingClicked;
    private Point2D mousePositionDuringDragStart;
    private boolean objectWasJustSelected;
    private boolean objectWasNeverMoved = true;
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

    public void addObject(T object)
    {
        objects.add(object);
    }

    public void calculateHoveredObjects(boolean objectsAreSelectable, Point2D offset, double scale, Point2D worldMouse)
    {
        if(vertexSelector != null)
            vertexSelector.calculateHoveredObjects(objectsAreSelectable, offset, scale, worldMouse);

        hoveredObjects.clear();

        for(T object : objects)
        {
            object.updateScreenValues(offset, scale);

            if(objectsAreSelectable && object.isHoveredByMouse() && (vertexSelector == null || (vertexSelector.areNoObjectsHovered() && vertexSelector.objectBeingClicked == null)))
            {
                if(Input.MOUSE_LEFT.wasJustActivated())
                {
                    objectBeingClicked = object;
                    mousePositionDuringDragStart = worldMouse;
                }

                hoveredObjects.add(object);
            }
        }

        if(Input.MOUSE_LEFT.wasJustActivated() && objectBeingClicked == null && (vertexSelector == null || vertexSelector.objectBeingClicked == null) && objectsAreSelectable)
        {
            selectedObjects.clear();

            if(vertexSelector != null)
                vertexSelector.objects.clear();
        }
    }

    public void tick(boolean objectsAreSelectable, Point2D offset, double scale, Point2D worldMouse)
    {
        if(objectBeingClicked != null && mousePositionDuringDragStart != null)
        {
            if(!selectedObjects.contains(objectBeingClicked))
                objectWasJustSelected = true;

            if(mousePositionDuringDragStart.distance(worldMouse) != 0)
                objectWasNeverMoved = false;

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
                    Point2D translation = mousePositionDuringDragStart.subtract(worldMouse);

                    for(Selectable selectedObject : selectedObjects)
                        selectedObject.translate(translation, offset, scale);

                    mousePositionDuringDragStart = worldMouse;

                    hoveredObjects.add(objectBeingClicked);
                }
                else if(!objectWasJustSelected && objectWasNeverMoved)
                    selectedObjects.remove(objectBeingClicked);
            }

            resetVertexSelector();

            if(vertexSelector != null && selectedObjects.size() == 1)
            {
                Selectable lastSelectedObject = selectedObjects.get(0);

                for(int i = 0; i < lastSelectedObject.vertexCount; i++)
                {
                    Vertex vertex = new Vertex(lastSelectedObject.worldXValues[i], lastSelectedObject.worldYValues[i], i);

                    vertex.updateScreenValues(offset, scale);

                    vertexSelector.objects.add(vertex);
                }
            }
        }

        if(vertexSelector != null && selectedObjects.size() == 1)
        {
            List<Vector2> vertices = new ArrayList<>();
            SweepLine sweepLine = new SweepLine();
            Platform platform = (Platform) selectedObjects.get(0);
            boolean successfulDecomposition;
            boolean successfulPolygon;

            vertexSelector.tick(objectsAreSelectable, offset, scale, worldMouse);

            for(Vertex vertex : vertexSelector.objects)
            {
                platform.worldXValues[vertex.index] = vertex.worldXValues[0];
                platform.worldYValues[vertex.index] = vertex.worldYValues[0];
                platform.screenXValues[vertex.index] = vertex.screenXValues[0];
                platform.screenYValues[vertex.index] = vertex.screenYValues[0];

                vertices.add(new Vector2(vertex.worldXValues[0], vertex.worldYValues[0]));
            }

            try
            {
                sweepLine.decompose(vertices);

                successfulDecomposition = true;
            }
            catch(IllegalArgumentException exception)
            {
                successfulDecomposition = false;
            }

            try
            {
                new Polygon(vertices.toArray(new Vector2[0]));

                successfulPolygon = true;
            }
            catch(IllegalArgumentException exception1)
            {
                try
                {
                    Collections.reverse(vertices);

                    new Polygon(vertices.toArray(new Vector2[0]));

                    successfulPolygon = true;
                }
                catch(IllegalArgumentException exception2)
                {
                    successfulPolygon = false;
                }
            }

            if(successfulDecomposition || successfulPolygon)
            {
                platform.setSimplePolygon(true);

                complexPlatforms.remove(platform);
            }
            else
            {
                platform.setSimplePolygon(false);

                if(!complexPlatforms.contains(platform))
                    complexPlatforms.add(platform);
            }
        }

        if(!Input.MOUSE_LEFT.isActive() || !objectsAreSelectable)
        {
            objectWasJustSelected = false;
            objectWasNeverMoved = true;
            objectBeingClicked = null;
            mousePositionDuringDragStart = null;
        }

        if(Input.DELETE.wasJustActivated() && vertexSelector != null)
        {
            if(vertexSelector.selectedObjects.size() == 0)
            {
                objects.removeAll(selectedObjects);
                hoveredObjects.removeAll(selectedObjects);
                complexPlatforms.removeIf(selectedObjects::contains);
                selectedObjects.clear();

                resetVertexSelector();
            }
            else if(selectedObjects.size() == 1 && vertexSelector.objects.size() > 3)
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

                    vertex.index = i;

                    object.worldXValues[i] = vertex.worldXValues[0];
                    object.worldYValues[i] = vertex.worldYValues[0];
                    object.screenXValues[i] = vertex.screenXValues[0];
                    object.screenYValues[i] = vertex.screenYValues[0];
                }
            }
        }
    }

    public void render(boolean objectsAreSelectable, Point2D offset, double scale, Point2D worldMouse, GraphicsContext gc)
    {
        T lastHoveredObject = hoveredObjects.size() == 0 ? null : hoveredObjects.get(hoveredObjects.size() - 1);

        for(Selectable object : objects)
            object.renderObject(gc);

        if(lastHoveredObject != null && !selectedObjects.contains(lastHoveredObject))
            lastHoveredObject.renderOutline(true, false, gc);

        for(Selectable selectedObject : selectedObjects)
            selectedObject.renderOutline(selectedObject == lastHoveredObject, true, gc);

        if(vertexSelector != null)
            vertexSelector.render(objectsAreSelectable, offset, scale, worldMouse, gc);
    }

    public boolean isAnObjectBeingClicked()
    {
        return objectBeingClicked != null || (vertexSelector == null || vertexSelector.objectBeingClicked != null);
    }

    public boolean areNoObjectsHovered()
    {
        return hoveredObjects.size() == 0 && (vertexSelector == null || vertexSelector.areNoObjectsHovered());
    }

    public List<Platform> getComplexPlatforms()
    {
        return complexPlatforms;
    }
}
