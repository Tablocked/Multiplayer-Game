package tablock.gameState;

import javafx.scene.canvas.GraphicsContext;
import tablock.network.ClientPacket;
import tablock.network.DataType;
import tablock.userInterface.PagedList;
import tablock.userInterface.TextButton;

public class JoinState extends GameState
{
    private long lobbyListRequestTime = 0;
    private int previousHostCount = 0;

    private final PagedList<Integer> pagedList = new PagedList<>(CLIENT.hostIdentifiers, "Select Host", CLIENT)
    {
        @Override
        protected void onItemButtonActivation(Integer hostIdentifier, TextButton levelButton, int yPosition)
        {
            CLIENT.send(ClientPacket.JOIN_HOST, DataType.INTEGER.encode(hostIdentifier));
        }

        @Override
        protected String getItemButtonName(Integer hostIdentifier, int index)
        {
            return CLIENT.hostedLevelNames.get(index);
        }
    };

    public JoinState()
    {
        CLIENT.hostedLevelNames.clear();
        CLIENT.hostIdentifiers.clear();

        pagedList.createButtons();
    }

    @Override
    public void renderNextFrame(GraphicsContext gc)
    {
        if(System.currentTimeMillis() - lobbyListRequestTime > 1000)
        {
            lobbyListRequestTime = System.currentTimeMillis();

            CLIENT.send(ClientPacket.LOBBY_LIST);
        }

        if(previousHostCount != CLIENT.hostIdentifiers.size())
        {
            previousHostCount = CLIENT.hostIdentifiers.size();

            pagedList.createButtons();
        }

        pagedList.renderBackgroundAndItemButtonStrip(gc);
        pagedList.renderArrowButtons(gc);
        pagedList.getInputIndicator().render(gc);

        previousHostCount = CLIENT.hostIdentifiers.size();
    }
}