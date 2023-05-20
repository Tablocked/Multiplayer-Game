package tablock.gameState;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import tablock.core.Input;
import tablock.level.Level;
import tablock.level.ObjectSelector;
import tablock.level.Platform;
import tablock.level.Vertex;
import tablock.network.Client;
import tablock.userInterface.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CreateState extends GameState
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
    private CircularButtonStrip currentInterface;
    private final List<Point2D> placedPlatformVertices = new ArrayList<>();
    private final ObjectSelector objectSelector;
    private final ImageButton playFromStartButton = new ImageButton(Client.PLAY_FROM_START_BUTTON_TEXTURE, () -> switchToPlayScreen(0, 0), "Play from start");
    private final ImageButton playFromHereButton = new ImageButton(Client.PLAY_FROM_HERE_BUTTON_TEXTURE, () -> switchToPlayScreen(worldInterfacePosition.getX(), worldInterfacePosition.getY()), "Play from here");
    private final ImageButton platformButton = new ImageButton(Client.PLATFORM_BUTTON_TEXTURE, () -> platformMode = !platformMode, "Platform");
    private final InputIndicator inputIndicator = new InputIndicator();
    private final CircularButtonStrip objectInterface = new CircularButtonStrip(platformButton);
    private final CircularButtonStrip mainInterface;
    private final ButtonStrip pauseButtons;
    private final AttentionMessage unsavedLevelMessage = new AttentionMessage("All unsaved changes will be lost!", this::switchToLevelSelectScreen, true);
    private final Level level;

    public CreateState(Level level, Path levelPath)
    {
        this.level = level;

        objectSelector = new ObjectSelector(level.getObjects());

        mainInterface = new CircularButtonStrip
        (
            playFromStartButton,
            playFromHereButton,
            new ImageButton(Client.OBJECTS_BUTTON_TEXTURE, () -> currentInterface = objectInterface, "Objects"),
            new ImageButton(Client.SAVE_BUTTON_TEXTURE, () -> saveLevel(levelPath),"Save")
        );

        pauseButtons = new ButtonStrip
        (
            ButtonStrip.Orientation.VERTICAL,

            new TextButton(960, 340, "Resume", 100, () -> paused = false),

            new TextButton(960, 540, "Save and Exit", 100, () ->
            {
                saveLevel(levelPath);
                switchToLevelSelectScreen();
            }),

            new TextButton(960, 740, "Exit Without Saving", 100, unsavedLevelMessage::activate)
        );

        unsavedLevelMessage.initializeCancelButton(pauseButtons::preventActivationForOneFrame);

        currentInterface = mainInterface;

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

        for(Platform platform : level.getObjects())
            if(!platform.isSimplePolygon())
                objectSelector.getComplexPlatforms().add(platform);
    }

    private void switchToLevelSelectScreen()
    {
        Input.setForceMouseVisible(false);

        CLIENT.switchGameState(new LevelSelectState());
    }

    private void saveLevel(Path levelPath)
    {
        try
        {
            Files.write(levelPath, Objects.requireNonNull(Client.serializeObject(level)));
        }
        catch(IOException exception)
        {
            throw new RuntimeException(exception);
        }
    }

    private void switchToPlayScreen(double startX, double startY)
    {
        if(objectSelector.getComplexPlatforms().size() == 0)
            CLIENT.switchGameState(new PlayState(this, startX, startY));
        else
        {
            timeDuringPreviousFrame = System.currentTimeMillis();
            complexPolygonAlertTime = 1000;

            Point2D platformCenter = objectSelector.getComplexPlatforms().get(0).calculateScreenCenter();

            if(isScreenPointOffScreen(platformCenter))
                offset = offset.subtract(platformCenter).add(960, 540);
        }

        Input.setForceMouseVisible(false);
    }

    private Point2D getScreenPoint(Point2D worldPoint)
    {
        return worldPoint.multiply(scale).add(offset);
    }

    private boolean isScreenPointOffScreen(Point2D screenPoint)
    {
        return !(screenPoint.getX() > 0) || !(screenPoint.getX() < 1920) || !(screenPoint.getY() > 0) || !(screenPoint.getY() < 1080);
    }

    @Override
    public void renderNextFrame(GraphicsContext gc)
    {
        Point2D screenMouse = Input.getMousePosition();
        Point2D worldMouse = screenMouse.subtract(offset).multiply(1 / scale);
        Point2D screenStartPoint = getScreenPoint(new Point2D(0, 0));

        if(!unsavedLevelMessage.isActive())
            if(Input.PAUSE.wasJustActivated())
            {
                paused = !paused;

                pauseButtons.setIndex(0);
            }
            else if(Input.BACK.wasJustActivated() && paused)
                paused = false;

        Input.setForceMouseVisible(!paused);

        if(!paused)
        {
            if(Input.MOUSE_MIDDLE.wasJustActivated() && currentInterface.areNoButtonsSelected())
                mousePositionDuringDragStart = screenMouse;

            if(Input.MOUSE_MIDDLE.isActive() && mousePositionDuringDragStart != null)
            {
                offset = offset.add(screenMouse.subtract(mousePositionDuringDragStart));

                mousePositionDuringDragStart = screenMouse;
            }

            if(Input.MOUSE_RIGHT.wasJustActivated() && !objectSelector.areAnyObjectsBeingClicked())
            {
                if(placedPlatformVertices.size() != 0)
                    placedPlatformVertices.clear();
                else if(objectSelector.isTransformationInProgress())
                    objectSelector.transformSelectedObjects(null);
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

        boolean objectsAreSelectable = !paused && placedPlatformVertices.size() == 0 && mousePositionDuringDragStart == null && mousePositionDuringSelectionStart == null && (currentInterface.areNoButtonsSelected() || objectSelector.areAnyObjectsBeingClicked());

        objectSelector.calculateHoveredObjects(objectsAreSelectable, worldMouse, scale);

        if(!Input.MOUSE_LEFT.isActive() && mouseNeverMovedDuringSelection && mousePositionDuringSelectionStart != null)
            objectSelector.deselectAllObjects();

        objectSelector.tick(objectsAreSelectable, mousePositionDuringSelectionStart == null, worldMouse, scale);

        if(interfaceOpen)
        {
            currentInterface.setFrozen(paused || placedPlatformVertices.size() != 0 || mousePositionDuringSelectionStart != null || objectSelector.areAnyObjectsBeingClicked() || objectSelector.isTransformationInProgress());
            currentInterface.calculateSelectedButtons();
        }

        objectSelector.render(currentInterface.areNoButtonsSelected(), offset, scale, gc);

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
            if(platformMode && objectSelector.areNoObjectsHovered() && (!Input.isShiftPressed() || placedPlatformVertices.size() != 0))
            {
                Point2D worldFirstVertex = placedPlatformVertices.size() != 0 ? placedPlatformVertices.get(0) : null;
                boolean firstVertexBeingHovered = placedPlatformVertices.size() != 0 && getScreenPoint(worldFirstVertex).distance(screenMouse) <= 20;

                mousePositionDuringSelectionStart = null;

                if(Input.MOUSE_LEFT.wasJustActivated() && currentInterface.areNoButtonsSelected() && !objectSelector.areAnyObjectsBeingClicked() && !Input.MOUSE_MIDDLE.isActive())
                    if(firstVertexBeingHovered)
                    {
                        if(placedPlatformVertices.size() > 2)
                        {
                            double[] worldXValues = new double[placedPlatformVertices.size()];
                            double[] worldYValues = new double[placedPlatformVertices.size()];

                            for(int i = 0; i < placedPlatformVertices.size(); i++)
                            {
                                Point2D worldVertex = placedPlatformVertices.get(i);

                                worldXValues[i] = worldVertex.getX();
                                worldYValues[i] = worldVertex.getY();
                            }

                            Platform platform = new Platform(worldXValues, worldYValues);

                            platform.updateScreenValues(offset, scale);

                            placedPlatformVertices.clear();

                            objectSelector.addObject(platform);
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
                gc.setLineWidth(5);
                gc.setLineDashes(10);
                gc.stroke();
                gc.closePath();

                if(!firstVertexBeingHovered && objectSelector.areNoObjectsHovered() && currentInterface.areNoButtonsSelected() && !Input.MOUSE_MIDDLE.isActive())
                    Vertex.renderAddVertex(screenMouse.getX(), screenMouse.getY(), gc);

                for(Point2D worldVertex : placedPlatformVertices)
                {
                    Point2D screenVertex = getScreenPoint(worldVertex);

                    gc.setFill(Color.GOLD);

                    if(worldVertex == worldFirstVertex)
                    {
                        gc.fillOval(screenVertex.getX() - 20, screenVertex.getY() - 20, 40, 40);
                        gc.drawImage(Client.CHECKMARK_TEXTURE, screenVertex.getX() - 15, screenVertex.getY() - 15);

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
            else if(placedPlatformVertices.size() == 0)
            {
                if(mousePositionDuringSelectionStart != null)
                {
                    Point2D startPoint = mousePositionDuringSelectionStart;
                    Point2D dimensions = worldMouse.subtract(startPoint);
                    double x = dimensions.getX() > 0 ? startPoint.getX() : startPoint.getX() + dimensions.getX();
                    double y = dimensions.getY() > 0 ? startPoint.getY() : startPoint.getY() + dimensions.getY();
                    double width = Math.abs(dimensions.getX());
                    double height = Math.abs(dimensions.getY());

                    if(startPoint.distance(worldMouse) != 0)
                        if(Input.MOUSE_LEFT.isActive())
                        {
                            gc.setStroke(Color.GOLD);
                            gc.setLineWidth(5);
                            gc.setLineDashes(10);
                            gc.strokeRect((x * scale) + offset.getX(), (y * scale) + offset.getY(), width * scale, height * scale);
                            gc.setLineDashes(0);
                        }
                        else
                            objectSelector.selectAllObjectsIntersectingRectangle(new Rectangle(x, y, width, height), scale);
                }
            }

            if(objectSelector.isTransformationInProgress())
                objectSelector.renderTransformationIndicator(offset, scale, gc);
        }
        else
            placedPlatformVertices.clear();

        if(!Input.MOUSE_LEFT.isActive() || paused)
        {
            mousePositionDuringSelectionStart = null;
            mouseNeverMovedDuringSelection = true;
        }

        gc.drawImage(Client.START_POINT_TEXTURE, screenStartPoint.getX() - 50, screenStartPoint.getY() - 50);

        for(Platform platform : objectSelector.getComplexPlatforms())
        {
            Point2D center = platform.calculateScreenCenter();

            gc.drawImage(Client.WARNING_TEXTURE, center.getX() - 25, center.getY() - 25);
        }

        if(interfaceOpen)
        {
            Point2D screenInterfacePosition = getScreenPoint(worldInterfacePosition);
            double hologramOffset = 25 * scale;
            double hologramLength = 50 * scale;

            gc.setFill(Color.rgb(255, 0, 0, 0.5));

            if(objectSelector.getComplexPlatforms().size() == 0 && (playFromStartButton.isHovered() || playFromHereButton.isHovered()))
            {
                Point2D hologramPosition = (playFromStartButton.isHovered() ? screenStartPoint : screenInterfacePosition).subtract(hologramOffset, hologramOffset);

                gc.fillRect(hologramPosition.getX(), hologramPosition.getY(), hologramLength, hologramLength);
            }

            platformButton.setForceHighlighted(platformMode);

            currentInterface.render(screenInterfacePosition.getX(), screenInterfacePosition.getY(), gc);

            if(objectSelector.getComplexPlatforms().size() != 0 && currentInterface == mainInterface)
            {
                gc.drawImage(Client.WARNING_TEXTURE, playFromStartButton.getX(), playFromStartButton.getY());
                gc.drawImage(Client.WARNING_TEXTURE, playFromHereButton.getX(), playFromHereButton.getY());
            }
        }
        else
            currentInterface.deselectAllButtons();

        if(paused)
        {
            gc.setFill(Color.rgb(255, 255, 255, 0.5));
            gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

            pauseButtons.setFrozen(unsavedLevelMessage.isActive());

            if(unsavedLevelMessage.isActive())
                pauseButtons.unhighlightAllButtons();

            pauseButtons.render(gc);
            unsavedLevelMessage.render(gc);
        }
        else
        {
            if(objectSelector.areAnyObjectsSelected() && !Input.MOUSE_MIDDLE.isActive() && placedPlatformVertices.size() == 0 && mousePositionDuringSelectionStart == null && currentInterface.areNoButtonsSelected())
            {
                if(objectSelector.canSelectedObjectsBeDeleted() && objectsAreSelectable)
                    inputIndicator.add("Delete", Client.KEYBOARD_DELETE_TEXTURE);

                if(!objectSelector.areAnyObjectsBeingClicked())
                {
                    inputIndicator.add(objectSelector.isScaleInProgress() ? "Confirm Scale" : "Scale", Client.KEYBOARD_S_TEXTURE);
                    inputIndicator.add(objectSelector.isRotationInProgress() ? "Confirm Rotation" : "Rotate", Client.KEYBOARD_R_TEXTURE);
                }
            }

            if(currentInterface.areNoButtonsSelected())
                inputIndicator.add("Pan View", Client.MOUSE_MIDDLE_TEXTURE);

            if(!objectSelector.areAnyObjectsBeingClicked())
            {
                String mouseRightText;

                if(placedPlatformVertices.size() != 0)
                    mouseRightText = "Cancel";
                else if(objectSelector.isTransformationInProgress())
                    mouseRightText = objectSelector.isScaleInProgress() ? "Cancel Scale" : "Cancel Rotation";
                else if(interfaceOpen && !isScreenPointOffScreen(getScreenPoint(worldInterfacePosition)))
                {
                    if(currentInterface == mainInterface)
                        mouseRightText = "Close Interface";
                    else
                        mouseRightText = "Back";
                }
                else
                    mouseRightText = "Open Interface";

                inputIndicator.add(mouseRightText, Client.MOUSE_RIGHT_TEXTURE);
            }

            if(!Input.MOUSE_MIDDLE.isActive() && currentInterface.areNoButtonsSelected() && placedPlatformVertices.size() == 0 && !objectSelector.isTransformationInProgress())
                inputIndicator.add( "Select Multiple", Client.KEYBOARD_SHIFT_AND_MOUSE_LEFT_TEXTURE);

            inputIndicator.render(gc);
        }
    }

    public Level getLevel()
    {
        return level;
    }
}