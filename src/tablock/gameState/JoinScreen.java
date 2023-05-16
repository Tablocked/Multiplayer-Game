package tablock.gameState;

import javafx.scene.canvas.GraphicsContext;
import tablock.network.ClientPacket;
import tablock.network.DataType;
import tablock.userInterface.PagedList;
import tablock.userInterface.TextButton;

public class JoinScreen extends GameState
{
    private final PagedList<Integer> pagedList = new PagedList<>(CLIENT.getHostIdentifiers(), "Select Host")
    {
        @Override
        protected void onItemButtonActivation(Integer hostIdentifier, TextButton levelButton, int yPosition)
        {
            CLIENT.send(ClientPacket.JOIN, DataType.INTEGER.encode(hostIdentifier));
        }

        @Override
        protected String getItemButtonName(Integer hostIdentifier, int index)
        {
            return CLIENT.getHostedLevelNames().get(index);
        }
    };

    public JoinScreen()
    {
        CLIENT.send(ClientPacket.LOBBY_LIST);

        pagedList.createButtons();
    }

    @Override
    public void renderNextFrame(GraphicsContext gc)
    {
        pagedList.renderBackgroundAndItemButtonStrip(gc);
        pagedList.renderArrowButtons(gc);
        pagedList.getInputIndicator().render(gc);
    }
}