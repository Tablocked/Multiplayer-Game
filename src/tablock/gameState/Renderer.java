package tablock.gameState;

import javafx.animation.AnimationTimer;
import javafx.geometry.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import tablock.core.Input;

public class Renderer
{
    private GameState currentState;
    private final GraphicsContext gc;

    public Renderer(GameState startingState, GraphicsContext gc)
    {
        this.currentState = startingState;
        this.gc = gc;

        currentState.renderer = this;
    }

    public void start()
    {
        Canvas canvas = gc.getCanvas();
        double scaleFactor = Screen.getPrimary().getBounds().getWidth() / 1920;

        gc.scale(scaleFactor, scaleFactor);

        AnimationTimer gameLoop = new AnimationTimer()
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

        gameLoop.start();
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

    public static abstract class GameState
    {
        protected Renderer renderer;

        protected void switchGameState(GameState nextState)
        {
            nextState.renderer = renderer;

            renderer.currentState = nextState;
        }

        public abstract void renderNextFrame(GraphicsContext gc);
    }
}