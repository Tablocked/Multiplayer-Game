package tablock.gameState;

import javafx.scene.canvas.GraphicsContext;
import tablock.userInterface.PagedList;
import tablock.userInterface.TextButton;

import java.util.ArrayList;

public class JoinScreen extends GameState
{
    private final PagedList<String> pagedList = new PagedList<>(new ArrayList<>(), "Select Lobby")
    {
        @Override
        protected void onItemButtonActivation(String item, TextButton levelButton, int yPosition)
        {

        }

        @Override
        protected String getItemButtonName(String item)
        {
            return "...";
        }
    };

    public JoinScreen()
    {
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