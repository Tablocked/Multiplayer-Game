package tablock.level;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import tablock.core.Input;

import java.util.ArrayList;
import java.util.List;

public class Selector<T extends Selectable>
{
    private T objectBeingClicked;
    private Point2D mousePositionDuringDragStart;
    private boolean objectWasJustSelected;
    private boolean objectWasNeverMoved = true;
    private List<T> objects;
    private List<Selectable> hoveredObjects;
    private final List<Selectable> selectedObjects = new ArrayList<>();

    public Selector(List<T> objects)
    {
        this.objects = objects;
    }

    public void addObject(T object)
    {
        objects.add(object);
    }

    public void render(boolean objectsAreSelectable, Point2D offset, double scale, Point2D worldMouse, GraphicsContext gc)
    {
        hoveredObjects = new ArrayList<>();

        for(T object : objects)
        {
            if(object instanceof Platform)
                ((Platform) object).transformScreenValues(offset, scale);

            if(objectsAreSelectable && object.isHoveredByMouse())
            {
                if(Input.MOUSE_LEFT.wasJustActivated())
                {
                    objectBeingClicked = object;
                    mousePositionDuringDragStart = worldMouse;
                }

                hoveredObjects.add(object);
            }
        }

        if(objectBeingClicked != null && mousePositionDuringDragStart != null)
        {
            if(!selectedObjects.contains(objectBeingClicked))
                objectWasJustSelected = true;

            if(mousePositionDuringDragStart.distance(worldMouse) != 0)
                objectWasNeverMoved = false;

            if(!Input.isShiftPressed())
            {
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
                        selectedObject.translate(translation);

                    mousePositionDuringDragStart = worldMouse;

                    hoveredObjects.add(objectBeingClicked);
                }
                else
                {
                    if(!objectWasJustSelected && objectWasNeverMoved)
                        selectedObjects.remove(objectBeingClicked);

                    objectWasJustSelected = false;
                    objectWasNeverMoved = true;
                    objectBeingClicked = null;
                    mousePositionDuringDragStart = null;
                }
            }
            else
                mousePositionDuringDragStart = null;
        }

        Selectable lastHoveredObject = hoveredObjects.size() == 0 ? null : hoveredObjects.get(hoveredObjects.size() - 1);

        for(Selectable object : objects)
        {
            if(object instanceof Platform)
                ((Platform) object).transformScreenValues(offset, scale);

            object.renderObject(gc);
        }

        for(Selectable object : objects)
            object.renderOutline(object == lastHoveredObject, selectedObjects.contains(object), gc);
    }

    public boolean areNoObjectsHovered()
    {
        return hoveredObjects.size() == 0;
    }
}
