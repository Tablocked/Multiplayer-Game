package tablock.gameState;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import org.dyn4j.geometry.Vector2;
import tablock.core.*;
import tablock.gameState.Renderer.GameState;
import tablock.userInterface.ButtonStrip;
import tablock.userInterface.CircularButtonStrip;
import tablock.userInterface.ImageButton;
import tablock.userInterface.TextButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CreateScreen extends GameState
{
    private boolean paused = false;
    private Point2D offset = new Point2D(960, 1080);
    private Point2D mousePositionDuringDragStart;
    private Point2D offsetDuringDragStart;
    private Point2D objectPlacementStart;
    private Platform objectBeingClicked;
    private Platform selectedObject;
    private double scale = 1;
    private boolean interfaceOpen = false;
    private Point2D worldInterfacePosition = new Point2D(0, 0);
    private ArrayList<Platform> platforms;
    private boolean platformMode = false;
    private final ImageButton playFromStartButton = new ImageButton(Main.getTexture("playFromStartButton"), () -> switchGameState(new PlayScreen(this, platforms, 0, 600)), "Play from start");
    private final ImageButton playFromHereButton = new ImageButton(Main.getTexture("playFromHereButton"), () -> switchGameState(new PlayScreen(this, platforms, -worldInterfacePosition.getX(), worldInterfacePosition.getY())), "Play from here");
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
        new TextButton(960, 540, "Quit To Main Menu", 100, () -> switchGameState(new TitleScreen())),
        new TextButton(960, 740, "Quit To Desktop", 100, () -> System.exit(0))
    );

    private CircularButtonStrip currentInterface = mainInterfaceButtons;

    public CreateScreen(Level level)
    {
        this.platforms = new ArrayList<>(List.of(level.getPlatforms()));

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

    @Override
    public void renderNextFrame(GraphicsContext gc)
    {
        Point2D worldMouse = getWorldMouse();
        Point2D screenMouse = Input.getMousePosition();
        double[] hoveredObjectXValues = null;
        double[] hoveredObjectYValues = null;
        double[] selectedObjectXValues = null;
        double[] selectedObjectYValues = null;

        if(Input.PAUSE.wasJustActivated())
        {
            paused = !paused;

            buttonStrip.setIndex(0);
        }
        else if(Input.UI_BACK.wasJustActivated() && paused)
            paused = false;

        gc.setLineWidth(10);

        for(Platform platform : platforms)
        {
            Vector2[] vertices = platform.getVertices();
            double[] xValues = new double[vertices.length];
            double[] yValues = new double[vertices.length];
            Polygon polygon = new Polygon();

            for(int i = 0; i < vertices.length; i++)
            {
                Point2D screenPoint = getScreenPoint(new Point2D(-vertices[i].x, vertices[i].y));

                xValues[i] = screenPoint.getX();
                yValues[i] = screenPoint.getY();

                polygon.getPoints().addAll(xValues[i], yValues[i]);
            }

            boolean beingHoveredByMouse = polygon.contains(screenMouse);

            if(!paused && currentInterface.areNoButtonsSelected() && objectPlacementStart == null)
                if(beingHoveredByMouse)
                {
                    if(Input.MOUSE_LEFT.wasJustActivated())
                        objectBeingClicked = platform;

                    if(platform.equals(objectBeingClicked) && !Input.MOUSE_LEFT.isActive())
                    {
                        objectBeingClicked = null;

                        selectedObject = selectedObject == platform ? null : platform;
                    }

                    if(!platform.equals(selectedObject))
                    {
                        hoveredObjectXValues = xValues;
                        hoveredObjectYValues = yValues;
                    }
                }
                else if(objectBeingClicked == platform)
                    objectBeingClicked = null;

            if(platform.equals(selectedObject))
            {
                selectedObjectXValues = xValues;
                selectedObjectYValues = yValues;
            }

            gc.setFill(Color.BLACK);
            gc.fillPolygon(xValues, yValues, vertices.length);
        }

        if(hoveredObjectXValues != null)
        {
            gc.setStroke(Color.RED.desaturate().desaturate());
            gc.strokePolygon(hoveredObjectXValues, hoveredObjectYValues, hoveredObjectXValues.length);
        }

        if(selectedObjectXValues != null)
        {
            gc.setStroke(Color.LIGHTGREEN);
            gc.strokePolygon(selectedObjectXValues, selectedObjectYValues, selectedObjectXValues.length);
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

                    gc.setFill(Color.rgb(0, 0, 0, 0.5));
                    gc.fillPolygon(xValues, yValues, 4);
                }
                else if(objectPlacementStart != null && objectPlacementStart.distance(worldMouse) != 0)
                {
                    Point2D midpoint = objectPlacementStart.add(worldMouse).multiply(0.5);
                    Point2D point1 = midpoint.subtract(objectPlacementStart);
                    Point2D point2 = midpoint.subtract(worldMouse);
                    Vertex[] vertices = {new Vertex(point1.getX(), point1.getY()), new Vertex(point1.getX(), point2.getY()), new Vertex(point2.getX(), point2.getY()), new Vertex(point2.getX(), point1.getY())};
                    Platform platform;
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
                        try
                        {
                            platform = new Platform(vertices);
                        }
                        catch(IllegalArgumentException exception)
                        {
                            Collections.reverse(Arrays.asList(vertices));

                            platform = new Platform(vertices);
                        }

                        Point2D platformPosition = worldMouse.subtract(worldMouse.subtract(midpoint));

                        platform.translate(-platformPosition.getX(), platformPosition.getY());

                        platforms.add(platform);
                    }

                    objectPlacementStart = null;
                }
            }

            if(Input.MOUSE_MIDDLE.wasJustActivated())
            {
                mousePositionDuringDragStart = screenMouse;
                offsetDuringDragStart = offset;
            }

            if(Input.MOUSE_MIDDLE.isActive())
                offset = offsetDuringDragStart.subtract(mousePositionDuringDragStart.subtract(screenMouse));

            if(Input.MOUSE_RIGHT.wasJustActivated())
            {
                if(objectPlacementStart != null)
                    objectPlacementStart = null;
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

        if(interfaceOpen)
        {
            Point2D interfacePosition = getScreenPoint(worldInterfacePosition);
            double hologramOffset = 25 * scale;
            double hologramLength = 50 * scale;

            gc.setFill(Color.rgb(255, 0, 0, 0.5));

            if(playFromStartButton.isSelected())
                gc.fillRect(offset.getX() - hologramOffset, offset.getY() + (-600 * scale) - hologramOffset, hologramLength, hologramLength);
            else if(playFromHereButton.isSelected())
                gc.fillRect(interfacePosition.getX() - hologramOffset, interfacePosition.getY() - hologramOffset, hologramLength, hologramLength);

            platformButton.setForceHighlighted(platformMode);

            currentInterface.setFrozen(paused || objectPlacementStart != null);
            currentInterface.render(interfacePosition.getX(), interfacePosition.getY(), gc);
        }

        if(paused)
        {
            gc.setFill(Color.rgb(255, 255, 255, 0.5));
            gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

            buttonStrip.render(gc);
        }
    }
}