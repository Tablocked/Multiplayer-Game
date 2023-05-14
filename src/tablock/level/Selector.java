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
    Point2D previousMousePositionDuringDrag;
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

    public void calculateHoveredObjects(boolean objectsAreSelectable, Point2D worldMouse, double scale)
    {
        hoveredObjects.clear();

        if(objectsAreSelectable)
            for(T object : objects)
                if(object.getShape(scale).contains(worldMouse))
                {
                    if(Input.MOUSE_LEFT.wasJustActivated())
                    {
                        objectBeingClicked = object;
                        previousMousePositionDuringDrag = worldMouse;
                    }

                    hoveredObjects.add(object);
                }
    }

    public void calculateAndDragSelectedObjects(boolean objectsAreSelectable, boolean forceObjectToBeDeselected, Point2D worldMouse)
    {
        if(objectBeingClicked != null && previousMousePositionDuringDrag != null)
        {
            if(!selectedObjects.contains(objectBeingClicked))
                objectWasJustSelected = true;

            if(previousMousePositionDuringDrag.distance(worldMouse) != 0)
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
                    Point2D translation = worldMouse.subtract(previousMousePositionDuringDrag);

                    for(Selectable selectedObject : selectedObjects)
                        selectedObject.translate(translation);

                    previousMousePositionDuringDrag = worldMouse;

                    hoveredObjects.add(objectBeingClicked);
                }
                else if(!objectWasJustSelected && objectWasNeverMoved && forceObjectToBeDeselected)
                    selectedObjects.remove(objectBeingClicked);
            }
        }

        if(!Input.MOUSE_LEFT.isActive() || !objectsAreSelectable)
        {
            objectWasJustSelected = false;
            objectWasNeverMoved = true;
            objectBeingClicked = null;
            previousMousePositionDuringDrag = null;
        }
    }

    public void renderObjectOutlines(GraphicsContext gc)
    {
        T lastHoveredObject = hoveredObjects.size() == 0 ? null : hoveredObjects.get(hoveredObjects.size() - 1);

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