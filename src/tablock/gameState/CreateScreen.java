package tablock.gameState;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import org.dyn4j.geometry.Vector2;
import tablock.core.*;
import tablock.userInterface.ButtonStrip;
import tablock.userInterface.CircularButtonStrip;
import tablock.userInterface.ImageButton;
import tablock.userInterface.TextButton;

import java.util.ArrayList;
import java.util.List;

public class CreateScreen implements GameState
{
    private boolean paused = false;
    private Point2D offset = new Point2D(960, 1080);
    private Point2D mousePositionDuringDragStart;
    private Point2D offsetDuringDragStart;
    private Point2D objectPlacementStart;
    private LevelPlatform objectBeingClicked;
    private double scale = 1;
    private boolean interfaceOpen = false;
    private Point2D worldInterfacePosition = new Point2D(0, 0);
    private boolean platformMode = false;
    private List<LevelPlatform> platforms = new ArrayList<>();
    private Point2D mousePositionDuringObjectDragStart;
    private boolean objectWasJustSelected = false;
    private boolean objectWasNeverMoved = true;
    private final List<LevelPlatform> selectedObjects = new ArrayList<>();
    private final ImageButton playFromStartButton = new ImageButton(Main.getTexture("playFromStartButton"), () -> Renderer.setCurrentState(new PlayScreen(this, null, 0, 600)), "Play from start");
    private final ImageButton playFromHereButton = new ImageButton(Main.getTexture("playFromHereButton"), () -> Renderer.setCurrentState(new PlayScreen(this, null, -worldInterfacePosition.getX(), worldInterfacePosition.getY())), "Play from here");
    private final ImageButton platformButton = new ImageButton(Main.getTexture("platformButton"), () -> platformMode = !platformMode, "Platform");
    private final CircularButtonStrip objectButtons = new CircularButtonStrip(platformButton);

    private final CircularButtonStrip mainInterfaceButtons = new CircularButtonStrip
    (
        playFromStartButton,
        playFromHereButton,
        new ImageButton(Main.getTexture("objectsButton"), () -> currentInterface = objectButtons, "Objects")
    );

    private final ButtonStrip buttonStrip = new ButtonStrip
    (
        ButtonStrip.Orientation.VERTICAL,

        new TextButton(960, 340, "Resume", 100, () -> paused = false),
        new TextButton(960, 540, "Quit To Main Menu", 100, () -> Renderer.setCurrentState(new TitleScreen())),
        new TextButton(960, 740, "Quit To Desktop", 100, () -> System.exit(0))
    );

    private CircularButtonStrip currentInterface = mainInterfaceButtons;

    public CreateScreen(Level level)
    {
        //this.platforms = new ArrayList<>(List.of(level.getPlatforms()));

        Input.setOnScrollHandler(scrollEvent ->
        {
            if(!paused)
            {
                double scaleFactor = scrollEvent.getDeltaY() > 0 ? 1.05 : 0.95;
                double xOffset = scrollEvent.getX() - offset.getX();
                double yOffset = scrollEvent.getY() - offset.getY();

                scale *= scaleFactor;
                offset = offset.add(xOffset - (xOffset * scaleFactor), yOffset - (yOffset * scaleFactor));
            }
        });
    }

    private Point2D getWorldPoint(Point2D screenPoint)
    {
        return offset.subtract(screenPoint).multiply(1 / scale);
    }

    private Point2D getWorldMouse()
    {
        return getWorldPoint(Input.getMousePosition());
    }

    private Point2D getScreenPoint(Point2D worldPoint)
    {
        return worldPoint.multiply(-scale).add(offset);
    }

    private double[] getPlatformXValues(Platform platform)
    {
        Vector2[] vertices = platform.getVertices();
        double[] xValues = new double[vertices.length];

        for(int i = 0; i < vertices.length; i++)
            xValues[i] = (vertices[i].x * scale) + offset.getX();

        return xValues;
    }

    private double[] getPlatformYValues(Platform platform)
    {
        Vector2[] vertices = platform.getVertices();
        double[] yValues = new double[vertices.length];

        for(int i = 0; i < vertices.length; i++)
            yValues[i] = (vertices[i].y * -scale) + offset.getY();

        return yValues;
    }

    @Override
    public void renderNextFrame(GraphicsContext gc)
    {
        Point2D worldMouse = getWorldMouse();
        Point2D screenMouse = Input.getMousePosition();
        boolean platformsAreClickable = !paused && currentInterface.areNoButtonsSelected() && objectPlacementStart == null;
        List<LevelPlatform> hoveredObjects = new ArrayList<>();

        if(Input.PAUSE.wasJustActivated())
        {
            paused = !paused;

            buttonStrip.setIndex(0);
        }
        else if(Input.UI_BACK.wasJustActivated() && paused)
            paused = false;

        if(!paused)
        {
            if(Input.MOUSE_MIDDLE.wasJustActivated())
            {
                mousePositionDuringDragStart = screenMouse;
                offsetDuringDragStart = offset;
            }

            if(Input.MOUSE_MIDDLE.isActive() && mousePositionDuringDragStart != null)
                offset = offsetDuringDragStart.subtract(mousePositionDuringDragStart.subtract(screenMouse));

            if(Input.MOUSE_RIGHT.wasJustActivated())
            {
                if(objectPlacementStart != null)
                    objectPlacementStart = null;
                else if(!new Rectangle(0, 0, 1920, 1080).contains(getScreenPoint(worldInterfacePosition)))
                {
                    interfaceOpen = true;
                    worldInterfacePosition = worldMouse;
                    currentInterface = mainInterfaceButtons;
                }
                else if(currentInterface == mainInterfaceButtons)
                {
                    interfaceOpen = !interfaceOpen;
                    worldInterfacePosition = worldMouse;
                }
                else
                {
                    mainInterfaceButtons.deselectAllButtons();

                    currentInterface = mainInterfaceButtons;
                }
            }
        }

        for(LevelPlatform platform : platforms)
        {
            platform.transformScreenValues(offset, scale);

            double[] xValues = platform.getScreenXValues();
            double[] yValues = platform.getScreenYValues();
            Polygon polygon = new Polygon();

            for(int i = 0; i < xValues.length; i++)
                polygon.getPoints().addAll(xValues[i], yValues[i]);

            if(platformsAreClickable && polygon.contains(screenMouse))
            {
                if(Input.MOUSE_LEFT.wasJustActivated())
                {
                    objectBeingClicked = platform;
                    mousePositionDuringObjectDragStart = worldMouse;
                }

                hoveredObjects.add(platform);
            }
        }

        if(objectBeingClicked != null && mousePositionDuringObjectDragStart != null)
        {
            if(!selectedObjects.contains(objectBeingClicked))
                objectWasJustSelected = true;

            if(mousePositionDuringObjectDragStart.distance(worldMouse) != 0)
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
                List<LevelPlatform> newPlatforms = new ArrayList<>();
                int startingIndex = platforms.indexOf(objectBeingClicked);

                for(int i = 0; i < platforms.size(); i++)
                    newPlatforms.add(platforms.get((startingIndex + i) % platforms.size()));

                platforms = newPlatforms;
            }

            if(platformsAreClickable)
            {
                if(Input.MOUSE_LEFT.isActive())
                {
                    Point2D translation = mousePositionDuringObjectDragStart.subtract(worldMouse);

                    for(LevelPlatform selectedObject : selectedObjects)
                        selectedObject.translate(translation);

                    mousePositionDuringObjectDragStart = worldMouse;
                }
                else
                {
                    if(!objectWasJustSelected && objectWasNeverMoved)
                        selectedObjects.remove(objectBeingClicked);

                    objectWasJustSelected = false;
                    objectWasNeverMoved = true;
                    objectBeingClicked = null;
                    mousePositionDuringObjectDragStart = null;
                }
            }
            else
                mousePositionDuringObjectDragStart = null;
        }

        for(LevelPlatform platform : platforms)
            platform.render(gc);

        LevelPlatform lastHoveredObject = hoveredObjects.size() == 0 ? null : hoveredObjects.get(hoveredObjects.size() - 1);

        gc.setLineWidth(10);

        if(lastHoveredObject != null && !selectedObjects.contains(lastHoveredObject))
        {
            gc.setStroke(Color.RED.desaturate().desaturate());
            gc.strokePolygon(lastHoveredObject.getScreenXValues(), lastHoveredObject.getScreenYValues(), lastHoveredObject.getScreenXValues().length);
        }

        gc.setStroke(Color.LIGHTGREEN);

        for(LevelPlatform selectedObject : selectedObjects)
        {
            if(lastHoveredObject == selectedObject)
                gc.setLineDashes(20);

            gc.strokePolygon(selectedObject.getScreenXValues(), selectedObject.getScreenYValues(), selectedObject.getScreenXValues().length);

            gc.setLineDashes(0);
        }

        if(paused)
            objectPlacementStart = null;
        else
        {
            if(platformMode && currentInterface.areNoButtonsSelected())
            {
                if(Input.MOUSE_LEFT.wasJustActivated() && objectBeingClicked == null)
                    objectPlacementStart = worldMouse;

                if(Input.MOUSE_LEFT.isActive() && objectPlacementStart != null)
                {
                    Point2D point1 = getScreenPoint(objectPlacementStart);
                    double[] xValues = {point1.getX(), point1.getX(), screenMouse.getX(), screenMouse.getX()};
                    double[] yValues = {point1.getY(), screenMouse.getY(), screenMouse.getY(), point1.getY()};

                    gc.setStroke(Color.GOLD);
                    gc.setLineWidth(10);
                    gc.setLineDashes(20);
                    gc.strokePolygon(xValues, yValues, 4);
                    gc.setLineDashes(0);
                }
                else if(objectPlacementStart != null && objectPlacementStart.distance(worldMouse) != 0)
                {
                    //Point2D midpoint = objectPlacementStart.add(worldMouse).multiply(0.5);
                    Point2D point1 = objectPlacementStart.multiply(-1);
                    Point2D point2 = worldMouse.multiply(-1);
                    Vertex[] vertices = {new Vertex(point1.getX(), point1.getY()), new Vertex(point1.getX(), point2.getY()), new Vertex(point2.getX(), point2.getY()), new Vertex(point2.getX(), point1.getY())};
                    LevelPlatform platform = new LevelPlatform(vertices);
                    boolean coincidentVertices = false;

                    for(int i = 0; i < vertices.length; i++)
                        for(int j = 0; j < vertices.length; j++)
                            if(i != j && vertices[i].equals(vertices[j]))
                            {
                                coincidentVertices = true;

                                break;
                            }

                    if(!coincidentVertices)
                    {
//                        try
//                        {
//                            platform = new Platform(vertices);
//                        }
//                        catch(IllegalArgumentException exception)
//                        {
//                            Collections.reverse(Arrays.asList(vertices));
//
//                            platform = new Platform(vertices);
//                        }

                        //Point2D platformPosition = worldMouse.subtract(worldMouse.subtract(midpoint));

                        //platform.translate(-platformPosition.getX(), platformPosition.getY());

                        platforms.add(platform);
                    }

                    objectPlacementStart = null;
                }
            }
        }

        if(interfaceOpen)
        {
            Point2D screenInterfacePosition = getScreenPoint(worldInterfacePosition);
            double hologramOffset = 25 * scale;
            double hologramLength = 50 * scale;

            gc.setFill(Color.rgb(255, 0, 0, 0.5));

            if(playFromStartButton.isSelected())
                gc.fillRect(offset.getX() - hologramOffset, offset.getY() + (-600 * scale) - hologramOffset, hologramLength, hologramLength);
            else if(playFromHereButton.isSelected())
                gc.fillRect(screenInterfacePosition.getX() - hologramOffset, screenInterfacePosition.getY() - hologramOffset, hologramLength, hologramLength);

            platformButton.setForceHighlighted(platformMode);

            currentInterface.setFrozen(paused || objectPlacementStart != null);
            currentInterface.render(screenInterfacePosition.getX(), screenInterfacePosition.getY(), gc);
        }
        else
            currentInterface.deselectAllButtons();

        if(paused)
        {
            gc.setFill(Color.rgb(255, 255, 255, 0.5));
            gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

            buttonStrip.render(gc);
        }
    }
}