package tablock.core;

import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Screen;

import java.util.ArrayList;
import java.util.List;

public enum Input
{
    MOUSE_LEFT,
    MOUSE_RIGHT,
    MOUSE_MIDDLE,
    DELETE,
    UP,
    LEFT,
    DOWN,
    RIGHT,
    JUMP,
    S,
    R,
    PAUSE,
    UI_SELECT,
    BACK("nintendoB", "playstationCircle", "keyboardEscape", "mouseRight"),
    PREVIOUS_PAGE("nintendoLeftButton", "playstationLeftButton", "keyboardQ"),
    NEXT_PAGE("nintendoRightButton", "playstationRightButton", "keyboardE");
    private static final ControllerManager controllers = new ControllerManager();
    private static Point2D mousePosition = new Point2D(-1, -1);
    private static Point2D scaledMousePosition;
    private static boolean usingMouseControls = true;
    private static boolean forceMouseHidden = false;
    private static boolean forceMouseVisible = false;
    private static boolean shiftPressed = false;
    private static final double scaleFactor = Screen.getPrimary().getBounds().getWidth() / 1920;
    private static final List<KeyCode> keysPressed = new ArrayList<>();
    private static final List<MouseButton> mouseButtonsPressed = new ArrayList<>();
    private static Scene scene;
    private double value;
    private boolean activePreviousFrame = false;
    private Image nintendoImage;
    private Image playStationImage;
    private Image keyboardImage;
    private Image mouseImage;

    Input() {}

    Input(String nintendoImageName, String playStationImageName, String keyboardImageName)
    {
        this.nintendoImage = Main.getTexture(nintendoImageName);
        this.playStationImage = Main.getTexture(playStationImageName);
        this.keyboardImage = Main.getTexture(keyboardImageName);

        mouseImage = keyboardImage;
    }

    Input(String nintendoImageName, String playStationImageName, String keyboardImageName, String mouseImageName)
    {
        this(nintendoImageName, playStationImageName, keyboardImageName);

        this.mouseImage = Main.getTexture(mouseImageName);
    }

    public static void initialize(Scene scene)
    {
        Input.scene = scene;

        scene.setOnMousePressed(mouseEvent ->
        {
            if(!mouseButtonsPressed.contains(mouseEvent.getButton()))
                mouseButtonsPressed.add(mouseEvent.getButton());

            usingMouseControls = true;
        });

        scene.setOnMouseReleased(mouseEvent ->
        {
            mouseButtonsPressed.remove(mouseEvent.getButton());

            usingMouseControls = true;
        });

        scene.setOnMouseMoved(mouseEvent ->
        {
            mousePosition = new Point2D(mouseEvent.getX(), mouseEvent.getY());
            usingMouseControls = true;
        });

        scene.setOnMouseDragged(scene.getOnMouseMoved());

        scene.setOnKeyPressed(keyEvent ->
        {
            if(!keysPressed.contains(keyEvent.getCode()))
                keysPressed.add(keyEvent.getCode());

            shiftPressed = keyEvent.isShiftDown();
        });

        scene.setOnKeyReleased(keyEvent ->
        {
            keysPressed.remove(keyEvent.getCode());

            shiftPressed = keyEvent.isShiftDown();
        });

        controllers.initSDLGamepad();
    }

    public static void beginPoll()
    {
        ControllerState controller = controllers.getState(0);

        recordDigitalValue(MOUSE_LEFT, mouseButtonsPressed.contains(MouseButton.PRIMARY));
        recordDigitalValue(MOUSE_RIGHT, mouseButtonsPressed.contains(MouseButton.SECONDARY));
        recordDigitalValue(MOUSE_MIDDLE, mouseButtonsPressed.contains(MouseButton.MIDDLE));
        recordDigitalValue(DELETE, keysPressed.contains(KeyCode.DELETE));

        recordDigitalOrAnalogValue(UP, keysPressed.contains(KeyCode.W) || keysPressed.contains(KeyCode.UP) || controller.dpadUp, controller.leftStickY, true);
        recordDigitalOrAnalogValue(LEFT, keysPressed.contains(KeyCode.A) || keysPressed.contains(KeyCode.LEFT) || controller.dpadLeft, controller.leftStickX, false);
        recordDigitalOrAnalogValue(DOWN, keysPressed.contains(KeyCode.S) || keysPressed.contains(KeyCode.DOWN) || controller.dpadDown, controller.leftStickY, false);
        recordDigitalOrAnalogValue(RIGHT, keysPressed.contains(KeyCode.D) || keysPressed.contains(KeyCode.RIGHT) || controller.dpadRight, controller.leftStickX, true);

        recordDigitalValue(JUMP, keysPressed.contains(KeyCode.SPACE) || isActionButtonPressed());
        recordDigitalValue(PAUSE, keysPressed.contains(KeyCode.ESCAPE) || controller.start);
        recordDigitalValue(S, keysPressed.contains(KeyCode.S));
        recordDigitalValue(R, keysPressed.contains(KeyCode.R));

        recordDigitalValue(UI_SELECT, keysPressed.contains(KeyCode.SPACE) || isActionButtonPressed());
        recordDigitalValue(BACK, keysPressed.contains(KeyCode.ESCAPE) || MOUSE_RIGHT.isActive() || isBackButtonPressed());
        recordDigitalValue(PREVIOUS_PAGE, keysPressed.contains(KeyCode.Q) || controller.lb);
        recordDigitalValue(NEXT_PAGE, keysPressed.contains(KeyCode.E) || controller.rb);

        if(forceMouseVisible)
            usingMouseControls = true;
        else if(UP.isActive() || LEFT.isActive() || DOWN.isActive() || RIGHT.isActive() || forceMouseHidden)
            usingMouseControls = false;

        scene.setCursor(usingMouseControls ? Cursor.DEFAULT : Cursor.NONE);

        scaledMousePosition = mousePosition.multiply(1 / scaleFactor);
    }

    private static void recordDigitalValue(Input input, boolean digitalValue)
    {
        input.value = digitalValue ? 1 : 0;
    }

    private static void recordDigitalOrAnalogValue(Input input, boolean digitalValue, double analogValue, boolean positive)
    {
        input.value = digitalValue ? 1 : 0;

        if(!input.isActive())
        {
            if(positive)
                input.value = analogValue > 0.5 ? analogValue : 0;
            else
                input.value = analogValue < -0.5 ? -analogValue : 0;
        }
    }

    private static boolean isActionButtonPressed()
    {
        ControllerState controller = controllers.getState(0);
        String controllerType = controller.controllerType;

        if(controllerType.contains("Nintendo"))
            return controller.b;
        else
            return controller.a;
    }

    private static boolean isBackButtonPressed()
    {
        ControllerState controller = controllers.getState(0);
        String controllerType = controller.controllerType;

        if(controllerType.contains("Nintendo"))
            return controller.a;
        else
            return controller.b;
    }

    public static void endPoll()
    {
        for(Input input : values())
            input.activePreviousFrame = input.isActive();
    }

    public static Point2D getMousePosition()
    {
        return scaledMousePosition;
    }

    public static boolean isUsingMouseControls()
    {
        return usingMouseControls;
    }

    public static boolean isShiftPressed()
    {
        return shiftPressed;
    }

    public static void setOnKeyTypedHandler(EventHandler<KeyEvent> onKeyTypedHandler)
    {
        scene.setOnKeyTyped(onKeyTypedHandler);
    }

    public static void setOnScrollHandler(EventHandler<ScrollEvent> onScrollHandler)
    {
        scene.setOnScroll(onScrollHandler);
    }

    public static void setForceMouseHidden(boolean forceMouseHidden)
    {
        Input.forceMouseHidden = forceMouseHidden;
    }

    public static void setForceMouseVisible(boolean forceMouseVisible)
    {
        Input.forceMouseVisible = forceMouseVisible;
    }

    public boolean isActive()
    {
        return value != 0;
    }

    public boolean wasJustActivated()
    {
        return isActive() && !activePreviousFrame;
    }

    public double getValue()
    {
        return value;
    }

    public Image getImage()
    {
        ControllerState controller = controllers.getState(0);
        String controllerType = controller.controllerType;

        if(controller.isConnected)
            return controllerType.contains("Nintendo") ? nintendoImage : playStationImage;
        else if(usingMouseControls)
            return mouseImage;
        else
            return keyboardImage;
    }
}