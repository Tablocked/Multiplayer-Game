package tablock.gameState;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
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
    private double scale = 1;
    private int gridSize = 128;
    private double complexPlatformAlertTime = 0;
    private double timeDuringPreviousFrame = 0;
    private Point2D mousePositionDuringDragStart;
    private Point2D mousePositionDuringSelectionStart;
    private Point2D worldInterfacePosition = new Point2D(0, 0);
    private boolean mouseNeverMovedDuringSelection = true;
    private boolean interfaceOpen = false;
    private boolean platformMode = false;
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
                if(objectSelector.getComplexPlatforms().size() == 0)
                {
                    saveLevel(levelPath);
                    switchToLevelSelectScreen();
                }
                else
                {
                    paused = false;

                    activateComplexPlatformAlert();
                }
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

    private void activateComplexPlatformAlert()
    {
        timeDuringPreviousFrame = System.currentTimeMillis();
        complexPlatformAlertTime = 1000;

        Point2D platformCenter = objectSelector.getComplexPlatforms().get(0).calculateScreenCenter();

        if(isScreenPointOffScreen(platformCenter))
            offset = offset.subtract(platformCenter).add(960, 540);

        objectSelector.selectTheFirstComplexPlatform();
    }

    private void switchToPlayScreen(double startX, double startY)
    {
        if(objectSelector.getComplexPlatforms().size() == 0)
            CLIENT.switchGameState(new PlayState(this, startX, startY));
        else
            activateComplexPlatformAlert();

        Input.setForceMouseVisible(false);
    }

    private Point2D getScreenPoint(Point2D worldPoint)
    {
        return worldPoint.multiply(scale).add(offset);
    }

    private Point2D getWorldPoint(Point2D screenPoint)
    {
        return screenPoint.subtract(offset).multiply(1 / scale);
    }

    private boolean isScreenPointOffScreen(Point2D screenPoint)
    {
        return !(screenPoint.getX() > 0) || !(screenPoint.getX() < 1920) || !(screenPoint.getY() > 0) || !(screenPoint.getY() < 1080);
    }

    private void drawGrid(double cellSize, GraphicsContext gc)
    {
        Point2D localPoint = getWorldPoint(new Point2D(0, 0)).multiply(1D / cellSize);
        Point2D snappedPoint = getScreenPoint(new Point2D(Math.ceil(localPoint.getX()) * cellSize, Math.ceil(localPoint.getY()) * cellSize));
        double increment = cellSize * scale;

        for(double x = snappedPoint.getX(); x < 1920; x += increment)
            gc.strokeLine(x, 0, x, 1080);

        for(double y = snappedPoint.getY(); y < 1080; y += increment)
            gc.strokeLine(0, y, 1920, y);
    }

    @Override
    public void renderNextFrame(GraphicsContext gc)
    {
        Point2D screenMouse = Input.getMousePosition();
        Point2D worldMouse = screenMouse.subtract(offset).multiply(1 / scale);

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

            if(Input.MOUSE_RIGHT.wasJustActivated() && objectSelector.areNoObjectsBeingClicked())
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

            if(Input.UP_ARROW.wasJustActivated() && gridSize < 512)
                gridSize *= 2;

            if(Input.DOWN_ARROW.wasJustActivated() && gridSize != 1)
                gridSize /= 2;
        }

        if(!Input.MOUSE_MIDDLE.isActive() || paused)
            mousePositionDuringDragStart = null;

        if(!paused && (!platformMode || Input.isShiftPressed()) && Input.MOUSE_LEFT.wasJustActivated() && objectSelector.areNoObjectsHovered() && currentInterface.areNoButtonsSelected())
            mousePositionDuringSelectionStart = worldMouse;

        if(mousePositionDuringSelectionStart != null && mousePositionDuringSelectionStart.distance(worldMouse) != 0)
            mouseNeverMovedDuringSelection = false;

        boolean objectsAreSelectable = !paused && placedPlatformVertices.size() == 0 && mousePositionDuringDragStart == null && mousePositionDuringSelectionStart == null && (currentInterface.areNoButtonsSelected() || !objectSelector.areNoObjectsBeingClicked());
        Point2D snappedWorldMouse = new Point2D(Math.round(worldMouse.getX() / gridSize) * gridSize, Math.round(worldMouse.getY() / gridSize) * gridSize);

        objectSelector.calculateHoveredObjects(objectsAreSelectable, true, worldMouse, snappedWorldMouse, scale);

        if(!Input.MOUSE_LEFT.isActive() && mouseNeverMovedDuringSelection && mousePositionDuringSelectionStart != null)
            objectSelector.deselectAllObjects();

        objectSelector.tick(objectsAreSelectable, mousePositionDuringSelectionStart == null, worldMouse, snappedWorldMouse, scale, gridSize);

        if(interfaceOpen)
        {
            currentInterface.setFrozen(paused || placedPlatformVertices.size() != 0 || mousePositionDuringSelectionStart != null || !objectSelector.areNoObjectsBeingClicked() || objectSelector.isTransformationInProgress());
            currentInterface.calculateSelectedButtons();
        }

        objectSelector.render(currentInterface.areNoButtonsSelected(), offset, scale, gc);

        if(complexPlatformAlertTime != 0)
        {
            if(!paused)
                complexPlatformAlertTime -= System.currentTimeMillis() - timeDuringPreviousFrame;

            timeDuringPreviousFrame = System.currentTimeMillis();

            double opacity = complexPlatformAlertTime / 1000;

            if(opacity < 0)
                complexPlatformAlertTime = 0;
            else
                for(Platform platform : objectSelector.getComplexPlatforms())
                    platform.renderComplexPolygonAlert(opacity, gc);
        }

        double cellSize = Math.max(64 * Math.pow(2, Math.floor(Math.log(1 / scale) / Math.log(2))), gridSize);

        gc.setStroke(Color.GRAY);
        gc.setLineWidth(1);

        drawGrid(cellSize, gc);

        gc.setLineWidth(3);

        drawGrid(cellSize * 5, gc);

        if(!paused)
        {
            if(platformMode && objectSelector.areNoObjectsHovered() && objectSelector.areNoObjectsBeingClicked() && (!Input.isShiftPressed() || placedPlatformVertices.size() != 0))
            {
                Point2D worldFirstVertex = placedPlatformVertices.size() == 0 ? null : placedPlatformVertices.get(0);
                Point2D snappedScreenMouse = getScreenPoint(snappedWorldMouse);
                boolean firstVertexBeingHovered = worldFirstVertex != null && (getScreenPoint(worldFirstVertex).distance(screenMouse) <= 20 || snappedWorldMouse.equals(worldFirstVertex));

                mousePositionDuringSelectionStart = null;

                if(Input.MOUSE_LEFT.wasJustActivated() && currentInterface.areNoButtonsSelected() && !Input.MOUSE_MIDDLE.isActive())
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
                        placedPlatformVertices.add(snappedWorldMouse);

                gc.beginPath();

                for(Point2D worldVertex : placedPlatformVertices)
                {
                    Point2D screenVertex = getScreenPoint(worldVertex);

                    gc.lineTo(screenVertex.getX(), screenVertex.getY());
                }

                if(worldFirstVertex != null)
                {
                    Point2D screenFirstVertex = getScreenPoint(worldFirstVertex);

                    if(firstVertexBeingHovered)
                        gc.lineTo(screenFirstVertex.getX(), screenFirstVertex.getY());
                    else
                        gc.lineTo(snappedScreenMouse.getX(), snappedScreenMouse.getY());
                }

                gc.setStroke(Color.GOLD);
                gc.setLineWidth(5);
                gc.setLineDashes(10);
                gc.stroke();
                gc.closePath();

                if(!firstVertexBeingHovered && currentInterface.areNoButtonsSelected() && !Input.MOUSE_MIDDLE.isActive())
                    Vertex.renderAddVertex(snappedScreenMouse.getX(), snappedScreenMouse.getY(), gc);

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

        Point2D screenStartPoint = getScreenPoint(new Point2D(0, 0));

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

            if(objectSelector.getComplexPlatforms().size() != 0)
                gc.drawImage(Client.WARNING_TEXTURE, 1269, 570);

            unsavedLevelMessage.render(gc);
        }
        else
        {
            gc.setFont(Font.font("Arial", 30));

            String gridSizeText = "Grid Size: " + gridSize;
            Bounds textShape = Client.computeTextShape(gridSizeText, gc);

            gc.setFill(Color.rgb(50, 50, 50));
            gc.fillRoundRect(949 - (textShape.getWidth() / 2), 940, textShape.getWidth() + 20, 40, 20, 20);
            gc.setFill(Color.WHITE);

            Client.fillText(gridSizeText, 960, 988, textShape, gc);

            gc.drawImage(Client.DOWN_ARROW_TEXTURE, 902 - (textShape.getWidth() / 2), 940);
            gc.drawImage(Client.UP_ARROW_TEXTURE, 976 + (textShape.getWidth() / 2), 940);

            if(objectSelector.areAnyObjectsSelected() && !Input.MOUSE_MIDDLE.isActive() && placedPlatformVertices.size() == 0 && mousePositionDuringSelectionStart == null && currentInterface.areNoButtonsSelected())
            {
                if(objectSelector.canSelectedObjectsBeDeleted() && objectsAreSelectable)
                    inputIndicator.add("Delete", Client.KEYBOARD_DELETE_TEXTURE);

                if(objectSelector.areNoObjectsBeingClicked())
                {
                    inputIndicator.add(objectSelector.isScaleInProgress() ? "Confirm Scale" : "Scale", Client.KEYBOARD_S_TEXTURE);
                    inputIndicator.add(objectSelector.isRotationInProgress() ? "Confirm Rotation" : "Rotate", Client.KEYBOARD_R_TEXTURE);
                }
            }

            if(currentInterface.areNoButtonsSelected())
                inputIndicator.add("Pan View", Client.MOUSE_MIDDLE_TEXTURE);

            if(objectSelector.areNoObjectsBeingClicked())
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

    public void deselectInterfaceButtons()
    {
        currentInterface.deselectAllButtons();
    }

    public Level getLevel()
    {
        return level;
    }
}