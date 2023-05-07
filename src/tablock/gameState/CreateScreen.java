package tablock.gameState;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import tablock.core.Input;
import tablock.core.Main;
import tablock.level.Level;
import tablock.level.Platform;
import tablock.level.Selector;
import tablock.userInterface.ButtonStrip;
import tablock.userInterface.CircularButtonStrip;
import tablock.userInterface.ImageButton;
import tablock.userInterface.TextButton;

public class CreateScreen implements GameState
{
    private boolean paused = false;
    private Point2D offset = new Point2D(960, 540);
    private Point2D mousePositionDuringDragStart;
    private Point2D offsetDuringDragStart;
    private Point2D objectPlacementStart;
    private double scale = 1;
    private boolean interfaceOpen = false;
    private Point2D worldInterfacePosition = new Point2D(0, 0);
    private boolean platformMode = false;
    private double complexPolygonAlertTime = 0;
    private double timeDuringPreviousFrame = 0;
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

        boolean anObjectBeingClicked = objectSelector.isAnObjectBeingClicked();
        boolean objectsAreSelectable = !paused && objectPlacementStart == null && (currentInterface.areNoButtonsSelected() || anObjectBeingClicked);

        objectSelector.calculateHoveredObjects(objectsAreSelectable, offset, scale, worldMouse);
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

        if(paused)
            objectPlacementStart = null;
        else if(platformMode && currentInterface.areNoButtonsSelected() && !anObjectBeingClicked && objectSelector.areNoObjectsHovered())
        {
            if(Input.MOUSE_LEFT.wasJustActivated())
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
                Point2D point1 = objectPlacementStart.multiply(-1);
                Point2D point2 = worldMouse.multiply(-1);
                Platform platform = new Platform(point1, point2);

                if(point1.getX() != point2.getX() && point1.getY() != point2.getY())
                    objectSelector.addObject(platform);

                objectPlacementStart = null;
            }
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

            currentInterface.setFrozen(paused || objectPlacementStart != null || anObjectBeingClicked);
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