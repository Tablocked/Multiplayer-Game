package tablock.gameState;

import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import tablock.core.Input;
import tablock.core.TextFieldHandler;
import tablock.network.Client;
import tablock.network.ClientPacket;
import tablock.network.DataType;
import tablock.userInterface.ButtonStrip;
import tablock.userInterface.TextButton;

public class TitleState extends GameState
{
    private boolean renamingInProgress;
    private TextFieldHandler textFieldHandler;

    private final ButtonStrip buttonStrip = new ButtonStrip
    (
        ButtonStrip.Orientation.VERTICAL,

        new TextButton(960, 340, "Create", 100, () -> CLIENT.switchGameState(new LevelSelectState())),
        new TextButton(960, 540, "Join", 100, () -> CLIENT.switchGameState(new JoinState())),

        new TextButton(960, 740, "Quit", 100, () ->
        {
            CLIENT.send(ClientPacket.DISCONNECT);

            System.exit(0);
        })
    );

    private void stopRenaming()
    {
        renamingInProgress = false;
        textFieldHandler = null;
    }

    @Override
    public void renderNextFrame(GraphicsContext gc)
    {
        Rectangle textField = new Rectangle(550, 100, 820, 100);
        String connectionStatus = CLIENT.isConnected() ? "Connected to server | Multiplayer features enabled!" : "Unable to connect to server | Multiplayer features disabled";

        gc.setFont(Font.font("Arial", 30));

        Bounds connectionStatusShape = Client.computeTextShape(connectionStatus, gc);

        gc.setFill(CLIENT.isConnected() ? Color.GREEN : Color.RED);
        gc.fillText(connectionStatus, 1900 - connectionStatusShape.getWidth(), 35);

        gc.setFont(Font.font("Arial", 80));

        gc.setFill(Color.DARKRED);
        gc.fillRect(textField.getX(), textField.getY(), textField.getWidth(), textField.getHeight());
        gc.setFill(Color.WHITE);

        Client.fillText(CLIENT.name, 960, 224, gc);

        if(Input.MOUSE_LEFT.wasJustActivated())
        {
            boolean textFieldContainsMouse = textField.contains(Input.getMousePosition());

            if(renamingInProgress && !textFieldContainsMouse)
                buttonStrip.preventActivationForOneFrame();

            renamingInProgress = textFieldContainsMouse;

            if(renamingInProgress)
            {
                textFieldHandler = new TextFieldHandler(CLIENT.name, 960, 150)
                {
                    @Override
                    public void onConfirmation(String text)
                    {
                        CLIENT.send(ClientPacket.NAME_CHANGE, DataType.STRING.encode(text));

                        stopRenaming();
                    }

                    @Override
                    public void onKeyTyped(String text, boolean cancelling)
                    {
                        super.onKeyTyped(text, cancelling);

                        CLIENT.name = text;

                        if(cancelling)
                            stopRenaming();
                    }
                };

                Input.setTextFieldHandler(textFieldHandler);
            }
            else if(textFieldHandler != null)
            {
                textFieldHandler.cancel();

                textFieldHandler = null;
            }
        }

        if(textFieldHandler != null)
            textFieldHandler.renderTypingCursor(true, gc);

        buttonStrip.setFrozen(renamingInProgress);
        buttonStrip.render(gc);
    }
}