package tablock.gameState;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import tablock.core.Input;
import tablock.core.Main;
import tablock.level.Level;
import tablock.level.Platform;
import tablock.level.Selector;
import tablock.level.Vertex;
import tablock.userInterface.ButtonStrip;
import tablock.userInterface.CircularButtonStrip;
import tablock.userInterface.ImageButton;
import tablock.userInterface.TextButton;

import java.util.ArrayList;
import java.util.List;

public class CreateScreen implements GameState
{
    private boolean paused = false;
    private Point2D offset = new Point2D(960, 540);
    private Point2D mousePositionDuringDragStart;
    private Point2D mousePositionDuringSelectionStart;
    private boolean mouseNeverMovedDuringSelection = true;
    private double scale = 1;
    private boolean interfaceOpen = false;
    private Point2D worldInterfacePosition = new Point2D(0, 0);
    private boolean platformMode = false;
    private double complexPolygonAlertTime = 0;
    private double timeDuringPreviousFrame = 0;
    private final List<Point2D> placedPlatformVertices = new ArrayList<>();
    private final Level level;
    private final Selector<Platform> objectSelector;
    private final ImageButton playFromStartButton = new ImageButton(Main.getTexture("playFromStartButton"), () -> switchToPlayScreen(0, 0), "Play from start");
    private final ImageButton playFromHereButton = new ImageButton(Main.getTexture("playFromHereButton"), () -> switchToPlayScreen(-worldInterfacePosition.getX(), worldInterfacePosition.getY()), "Play from here");
    private final ImageButton platformButton = new ImageButton(Main.getTexture("platformButton"), () -> platformMode = !platformMode, "Platform");
    private final CircularButtonStrip objectInterface = new CircularButtonStrip(platformButton);

    private final CircularButtonStrip mainInterface = new CircularButtonStrip
    (
        playFromStartButton,
        playFromHereButton,
        new ImageButton(Main.getTexture("objectsButton"), () -> currentInterface = objectInterface, "Objects")
    );

    private final ButtonStrip buttonStrip = new ButtonStrip
    (
        ButtonStrip.Orientation.VERTICAL,

        new TextButton(960, 340, "Resume", 100, () -> paused = false),
        new TextButton(960, 540, "Quit To Main Menu", 100, () -> Renderer.setCurrentState(new TitleScreen())),
        new TextButton(960, 740, "Quit To Desktop", 100, () -> System.exit(0))
    );

    private CircularButtonStrip currentInterface = mainInterface;

    public CreateScreen(Level level)
    {
        this.level = level;

        objectSelector = new Selector<>(level.getObjects(), true);

        Input.setOnScrollHandler(scrollEvent ->
        {
            if(!paused)
            {
                double scaleFactor = scrollEvent.getDeltaY() > 0 || scrollEvent.getDeltaX() > 0 ? 1.05 : 0.95;
                double xOffset = scrollEvent.getX() - offset.getX();
                double yOffset = scrollEvent.getY() - offset.getY();

                scale *= scaleFactor;
                offset = offset.add(xOffset - (xOffset * scaleFactor), yOffset - (yOffset * scaleFactor));
            }
        });
    }

    private boolean isScreenPointOffScreen(Point2D screenPoint)
    {
        return !(screenPoint.getX() > 0) || !(screenPoint.getX() < 1920) || !(screenPoint.getY() > 0) || !(screenPoint.getY() < 1080);
    }

    private void switchToPlayScreen(double startX, double startY)
    {
        if(objectSelector.getComplexPlatforms().size() == 0)
            Renderer.setCurrentState(new PlayScreen(this, startX, startY));
        else
        {
            timeDuringPreviousFrame = System.currentTimeMillis();
            complexPolygonAlertTime = 1000;

            Point2D platformCenter = objectSelector.getComplexPlatforms().get(0).getScreenCenter();

            if(isScreenPointOffScreen(platformCenter))
                offset = offset.subtract(platformCenter).add(960, 540);
        }
    }

    private Point2D getScreenPoint(Point2D worldPoint)
    {
        return worldPoint.multiply(-scale).add(offset);
    }

    @Override
    public void renderNextFrame(GraphicsContext gc)
    {
        Point2D worldMouse = offset.subtract(Input.getMousePosition()).multiply(1 / scale);
        Point2D screenMouse = Input.getMousePosition();
        Point2D screenStartPoint = getScreenPoint(new Point2D(0, 0));

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
                mousePositionDuringDragStart = screenMouse;

            if(Input.MOUSE_MIDDLE.isActive() && mousePositionDuringDragStart != null)
            {
                offset = offset.add(screenMouse.subtract(mousePositionDuringDragStart));

                mousePositionDuringDragStart = screenMouse;
            }

            if(Input.MOUSE_RIGHT.wasJustActivated())
            {
                if(placedPlatformVertices.size() != 0)
                    placedPlatformVertices.clear();
                else if(isScreenPointOffScreen(getScreenPoint(worldInterfacePosition)))
                {
                    interfaceOpen = true;
                    worldInterfacePosition = worldMouse;
                    currentInterface = mainInterface;
                }
                else if(currentInterface == mainInterface)
                {
                    interfaceOpen = !interfaceOpen;
                    worldInterfacePosition = worldMouse;
                }
                else
                {
                    mainInterface.deselectAllButtons();

                    currentInterface = mainInterface;
                }
            }
        }

        if(!Input.MOUSE_MIDDLE.isActive() || paused)
            mousePositionDuringDragStart = null;

        if(!paused && (!platformMode || Input.isShiftPressed()) && Input.MOUSE_LEFT.wasJustActivated() && objectSelector.areNoObjectsHovered() && currentInterface.areNoButtonsSelected())
            mousePositionDuringSelectionStart = worldMouse;

        if(mousePositionDuringSelectionStart != null && mousePositionDuringSelectionStart.distance(worldMouse) != 0)
            mouseNeverMovedDuringSelection = false;

        boolean objectsAreSelectable = !paused && placedPlatformVertices.size() == 0 && mousePositionDuringDragStart == null && mousePositionDuringSelectionStart == null && (currentInterface.areNoButtonsSelected() || objectSelector.isAnObjectBeingClicked());
        boolean wereNoObjectsHovered = objectSelector.areNoObjectsHovered();

        objectSelector.calculateHoveredObjects(objectsAreSelectable, !Input.MOUSE_LEFT.isActive() && mouseNeverMovedDuringSelection && mousePositionDuringSelectionStart != null, scale, worldMouse, offset);
        objectSelector.calculateAndDragSelectedObjects(objectsAreSelectable, offset, scale, worldMouse);
        objectSelector.render(gc);

        if(complexPolygonAlertTime != 0)
        {
            if(!paused)
                complexPolygonAlertTime -= System.currentTimeMillis() - timeDuringPreviousFrame;

            timeDuringPreviousFrame = System.currentTimeMillis();

            double opacity = complexPolygonAlertTime / 1000;

            if(opacity < 0)
                complexPolygonAlertTime = 0;
            else
                for(Platform platform : objectSelector.getComplexPlatforms())
                    platform.renderComplexPolygonAlert(opacity, gc);
        }

        if(!paused)
        {
            if(platformMode && !Input.isShiftPressed())
            {
                Point2D worldFirstVertex = placedPlatformVertices.size() != 0 ? placedPlatformVertices.get(0) : null;
                boolean firstVertexBeingHovered = placedPlatformVertices.size() != 0 && getScreenPoint(worldFirstVertex).distance(screenMouse) <= 20;

                mousePositionDuringSelectionStart = null;

                if(Input.MOUSE_LEFT.wasJustActivated() && currentInterface.areNoButtonsSelected() && !objectSelector.isAnObjectBeingClicked() && wereNoObjectsHovered)
                    if(firstVertexBeingHovered)
                    {
                        if(placedPlatformVertices.size() > 2)
                        {
                            double[] worldXValues = new double[placedPlatformVertices.size()];
                            double[] worldYValues = new double[placedPlatformVertices.size()];

                            for(int i = 0; i < placedPlatformVertices.size(); i++)
                            {
                                Point2D worldVertex = placedPlatformVertices.get(i);

                                worldXValues[i] = -worldVertex.getX();
                                worldYValues[i] = -worldVertex.getY();
                            }

                            Platform platform = new Platform(worldXValues, worldYValues);

                            objectSelector.addObject(platform, offset, scale);

                            if(!platform.calculateSimplePolygon())
                                objectSelector.getComplexPlatforms().add(platform);

                            placedPlatformVertices.clear();
                        }
                    }
                    else
                        placedPlatformVertices.add(worldMouse);

                gc.beginPath();

                for(Point2D worldVertex : placedPlatformVertices)
                {
                    Point2D screenVertex = getScreenPoint(worldVertex);

                    gc.lineTo(screenVertex.getX(), screenVertex.getY());
                }

                if(placedPlatformVertices.size() != 0)
                {
                    worldFirstVertex = placedPlatformVertices.get(0);
                    firstVertexBeingHovered = getScreenPoint(worldFirstVertex).distance(screenMouse) <= 20;

                    Point2D screenFirstVertex = getScreenPoint(worldFirstVertex);

                    if(firstVertexBeingHovered)
                        gc.lineTo(screenFirstVertex.getX(), screenFirstVertex.getY());
                    else
                        gc.lineTo(screenMouse.getX(), screenMouse.getY());
                }

                gc.setStroke(Color.GOLD);
                gc.setLineDashes(20);
                gc.setLineWidth(10);
                gc.stroke();
                gc.closePath();

                if(!firstVertexBeingHovered && objectSelector.areNoObjectsHovered() && currentInterface.areNoButtonsSelected())
                    Vertex.renderAddVertex(screenMouse.getX(), screenMouse.getY(), gc);

                for(Point2D worldVertex : placedPlatformVertices)
                {
                    Point2D screenVertex = getScreenPoint(worldVertex);

                    gc.setFill(Color.GOLD);

                    if(worldVertex == worldFirstVertex)
                    {
                        gc.fillOval(screenVertex.getX() - 20, screenVertex.getY() - 20, 40, 40);
                        gc.drawImage(Main.CHECKMARK_TEXTURE, screenVertex.getX() - 15, screenVertex.getY() - 15);

                        if(firstVertexBeingHovered)
                        {
                            gc.setStroke(Color.LIGHTGREEN);
                            gc.setLineDashes(10);
                            gc.setLineWidth(5);
                            gc.strokeOval(screenVertex.getX() - 20, screenVertex.getY() - 20, 40, 40);
                        }
                    }
                    else
                        gc.fillOval(screenVertex.getX() - 15, screenVertex.getY() - 15, 30, 30);
                }

                gc.setLineDashes(0);
            }
            else
            {
                placedPlatformVertices.clear();

                if(mousePositionDuringSelectionStart != null)
                {
                    Point2D startPoint = getScreenPoint(mousePositionDuringSelectionStart);
                    Point2D dimensions = screenMouse.subtract(startPoint);
                    double x = dimensions.getX() > 0 ? startPoint.getX() : startPoint.getX() + dimensions.getX();
                    double y = dimensions.getY() > 0 ? startPoint.getY() : startPoint.getY() + dimensions.getY();
                    double width = Math.abs(dimensions.getX());
                    double height = Math.abs(dimensions.getY());

                    if(startPoint.distance(screenMouse) != 0)
                        if(Input.MOUSE_LEFT.isActive())
                        {
                            gc.setStroke(Color.rgb(0, 255, 0, 0.5));
                            gc.setLineDashes(20);
                            gc.setLineWidth(10);
                            gc.strokeRect(x, y, width, height);
                            gc.setLineDashes(0);
                        }
                        else
                            objectSelector.selectAllObjectsIntersectingRectangle(new Rectangle(x, y, width, height), offset, scale);
                }
            }
        }
        else
            placedPlatformVertices.clear();

        if(!Input.MOUSE_LEFT.isActive() || paused)
        {
            mousePositionDuringSelectionStart = null;
            mouseNeverMovedDuringSelection = true;
        }

        gc.drawImage(Main.START_POINT_TEXTURE, screenStartPoint.getX() - 50, screenStartPoint.getY() - 50);

        for(Platform platform : objectSelector.getComplexPlatforms())
        {
            Point2D center = platform.getScreenCenter();

            gc.drawImage(Main.WARNING_TEXTURE, center.getX() - 25, center.getY() - 25);
        }

        if(interfaceOpen)
        {
            Point2D screenInterfacePosition = getScreenPoint(worldInterfacePosition);
            double hologramOffset = 25 * scale;
            double hologramLength = 50 * scale;

            gc.setFill(Color.rgb(255, 0, 0, 0.5));

            if(objectSelector.getComplexPlatforms().size() == 0 && (playFromStartButton.isSelected() || playFromHereButton.isSelected()))
            {
                Point2D hologramPosition = (playFromStartButton.isSelected() ? screenStartPoint : screenInterfacePosition).subtract(hologramOffset, hologramOffset);

                gc.fillRect(hologramPosition.getX(), hologramPosition.getY(), hologramLength, hologramLength);
            }

            platformButton.setForceHighlighted(platformMode);

            currentInterface.setFrozen(paused || placedPlatformVertices.size() != 0 || objectSelector.isAnObjectBeingClicked());
            currentInterface.render(screenInterfacePosition.getX(), screenInterfacePosition.getY(), gc);

            if(objectSelector.getComplexPlatforms().size() != 0 && currentInterface == mainInterface)
            {
                gc.drawImage(Main.WARNING_TEXTURE, playFromStartButton.getX(), playFromStartButton.getY());
                gc.drawImage(Main.WARNING_TEXTURE, playFromHereButton.getX(), playFromHereButton.getY());
            }
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

    public Level getLevel()
    {
        return level;
    }
}