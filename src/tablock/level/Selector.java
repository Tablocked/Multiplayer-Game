package tablock.level;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import tablock.core.Input;

import java.util.ArrayList;
import java.util.List;

public class Selector<T extends Selectable>
{
    T objectBeingClicked;
    Point2D snappedWorldMouseDuringDragStart;
    boolean objectWasJustSelected;
    boolean objectWasNeverMoved = true;
    final List<T> objects;
    final List<T> hoveredObjects = new ArrayList<>();
    final List<T> selectedObjects = new ArrayList<>();

    public Selector(List<T> objects)
    {
        this.objects = objects;
    }

    public Selector()
    {
        this(new ArrayList<>());
    }

    public void calculateHoveredObjects(boolean objectsAreSelectable, Point2D worldMouse, Point2D snappedWorldMouse, double scale)
    {
        hoveredObjects.clear();

        if(objectsAreSelectable && objectBeingClicked == null)
            for(T object : objects)
                if(object.getShape(scale).contains(worldMouse))
                {
                    if(Input.MOUSE_LEFT.wasJustActivated())
                    {
                        objectBeingClicked = object;
                        snappedWorldMouseDuringDragStart = snappedWorldMouse;
                    }

                    hoveredObjects.add(object);
                }
    }

    public void calculateAndDragSelectedObjects(boolean objectsAreSelectable, Point2D worldMouse, Point2D snappedWorldMouse, double scale, int gridSize)
    {
        if(objectBeingClicked != null && snappedWorldMouseDuringDragStart != null)
        {
            if(!selectedObjects.contains(objectBeingClicked))
                objectWasJustSelected = true;

            if(snappedWorldMouseDuringDragStart.distance(snappedWorldMouse) != 0)
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
                    if(!snappedWorldMouse.equals(snappedWorldMouseDuringDragStart))
                    {
                        Point2D translation = snappedWorldMouse.subtract(snappedWorldMouseDuringDragStart);

                        for(Selectable selectedObject : selectedObjects)
                            selectedObject.translate(translation, gridSize);

                        snappedWorldMouseDuringDragStart = snappedWorldMouse;

                        hoveredObjects.add(objectBeingClicked);
                    }
                }
                else if(!objectWasJustSelected && objectWasNeverMoved)
                    selectedObjects.remove(objectBeingClicked);
            }
        }

        if(!Input.MOUSE_LEFT.isActive() || !objectsAreSelectable)
        {
            if(objectBeingClicked != null)
            {
                objectBeingClicked = null;

                calculateHoveredObjects(objectsAreSelectable, worldMouse, snappedWorldMouse, scale);
            }

            objectWasJustSelected = false;
            objectWasNeverMoved = true;
            snappedWorldMouseDuringDragStart = null;
        }
    }

    public void renderObjectOutlines(boolean doNotDrawHoveredObjects, GraphicsContext gc)
    {
        T lastHoveredObject;

        if(objectBeingClicked == null)
            lastHoveredObject = hoveredObjects.size() == 0 || doNotDrawHoveredObjects ? null : hoveredObjects.get(hoveredObjects.size() - 1);
        else
            lastHoveredObject = objectBeingClicked;

        if(lastHoveredObject != null && !selectedObjects.contains(lastHoveredObject))
            lastHoveredObject.renderOutline(true, false, gc);

        for(Selectable selectedObject : selectedObjects)
            selectedObject.renderOutline(selectedObject == lastHoveredObject, true, gc);
    }

    public void selectAllObjectsIntersectingRectangle(Rectangle rectangle, double scale)
    {
        for(T object : objects)
        {
            Path path = (Path) Shape.intersect(object.getShape(scale), rectangle);

            if(path.getElements().size() != 0 && !selectedObjects.contains(object))
                selectedObjects.add(object);
        }
    }
}