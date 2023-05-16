package tablock.gameState;

import javafx.animation.AnimationTimer;
import javafx.geometry.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import tablock.core.Input;
import tablock.network.Client;

public class Renderer
{
    private static GameState currentState;
    private final GraphicsContext gc;

    public Renderer(GameState currentState, Client client, GraphicsContext gc)
    {
        GameState.CLIENT = client;
        Renderer.currentState = currentState;

        this.gc = gc;
    }

    public static void setCurrentState(GameState currentState)
    {
        Renderer.currentState = currentState;
    }

    public void start()
    {
        Canvas canvas = gc.getCanvas();
        double scaleFactor = Screen.getPrimary().getBounds().getWidth() / 1920;

        gc.scale(scaleFactor, scaleFactor);

        AnimationTimer animationTimer = new AnimationTimer()
        {
            @Override
            public void handle(long l)
            {
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

                Input.beginPoll();

                currentState.renderNextFrame(gc);

                Input.endPoll();
            }
        };

        animationTimer.start();
    }

    public static Bounds getTextShape(String text, Font font)
    {
        Text textObject = new Text(text);

        textObject.setFont(font);

        return textObject.getBoundsInParent();
    }

    public static Bounds getTextShape(String text, GraphicsContext gc)
    {
        return getTextShape(text, gc.getFont());
    }

    public static void fillText(double x, double y, String text, GraphicsContext gc)
    {
        Bounds textShape = getTextShape(text, gc);

        gc.fillText(text, x - (textShape.getWidth() / 2), y - (textShape.getHeight() / 2));
    }
}