package tablock.gameState;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import tablock.network.Client;
import tablock.network.ClientPacket;
import tablock.network.DataType;
import tablock.userInterface.PagedList;
import tablock.userInterface.TextButton;

import java.util.ArrayList;

public class JoinState extends GameState
{
    private long timeDuringLastHostListRequest = 0;
    private ArrayList<String> previousHostedLevelNames;

    private final PagedList<Byte> hostList = new PagedList<>(CLIENT.hostIdentifiers, "Select Host", CLIENT)
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
        hostList.createButtons();

        CLIENT.hostedLevelNames.clear();
        CLIENT.hostIdentifiers.clear();
    }

    @Override
    public void renderNextFrame(GraphicsContext gc)
    {
        double timeElapsedSinceLastHostListRequest = System.currentTimeMillis() - timeDuringLastHostListRequest;

        if(CLIENT.isConnected())
        {
            if(timeElapsedSinceLastHostListRequest > 1000)
            {
                timeDuringLastHostListRequest = System.currentTimeMillis();

                CLIENT.send(ClientPacket.HOST_LIST);
            }

            if(!CLIENT.hostedLevelNames.equals(previousHostedLevelNames))
                hostList.createButtons();

            previousHostedLevelNames = new ArrayList<>(CLIENT.hostedLevelNames);
        }

        hostList.renderBackgroundAndItemButtonStrip(gc);
        hostList.renderArrowButtons(gc);
        hostList.getInputIndicator().render(gc);

        if(!CLIENT.isConnected())
        {
            gc.setFont(Font.font("Arial", 50));
            gc.setFill(Color.RED);

            Client.fillText("Multiplayer features are disabled!", 960, 200, gc);
        }
    }
}