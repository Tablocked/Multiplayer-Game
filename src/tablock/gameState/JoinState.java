package tablock.gameState;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import tablock.network.Client;
import tablock.network.ClientPacket;
import tablock.network.DataType;
import tablock.userInterface.PagedList;
import tablock.userInterface.TextButton;

public class JoinState extends GameState
{
    private long timeDuringLastHostListRequest = 0;

    private final PagedList<Byte> pagedList = new PagedList<>(CLIENT.hostIdentifiers, "Select Host", CLIENT)
    {
        @Override
        protected void onItemButtonActivation(Byte hostIdentifier, TextButton levelButton, int yPosition)
        {
            CLIENT.send(ClientPacket.JOIN_HOST, DataType.BYTE.encode(hostIdentifier));
        }

        @Override
        protected String getItemButtonName(Byte hostIdentifier, int index)
        {
            return CLIENT.hostedLevelNames.get(index);
        }
    };

    public JoinState()
    {
        CLIENT.hostedLevelNames.clear();
        CLIENT.hostIdentifiers.clear();
    }

    @Override
    public void renderNextFrame(GraphicsContext gc)
    {
        if(System.currentTimeMillis() - timeDuringLastHostListRequest > 1000)
        {
            if(CLIENT.isConnected())
            {
                timeDuringLastHostListRequest = System.currentTimeMillis();

                CLIENT.send(ClientPacket.HOST_LIST);
            }

            pagedList.createButtons();
        }

        pagedList.renderBackgroundAndItemButtonStrip(gc);
        pagedList.renderArrowButtons(gc);
        pagedList.getInputIndicator().render(gc);

        if(!CLIENT.isConnected())
        {
            gc.setFont(Font.font("Arial", 50));
            gc.setFill(Color.RED);

            Client.fillText("Multiplayer features are disabled!", 960, 200, gc);
        }
    }
}