package tablock.userInterface;

import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import tablock.core.Input;
import tablock.core.Texture;
import tablock.gameState.TitleState;
import tablock.network.Client;

import java.util.List;

public abstract class PagedList<T>
{
    private ButtonStrip itemButtonStrip;
    private int page = 1;
    private int maxPage;
    private TextButton newButton;
    private final List<T> list;
    private final String headerText;
    private final InputIndicator inputIndicator = new InputIndicator();
    private final TextButton backButton;
    private final ImageButton leftArrowButton = new ImageButton(870, 880, Texture.LEFT_ARROW_BUTTON.get(), () -> {page--; createButtons();});
    private final ImageButton rightArrowButton = new ImageButton(1050, 900, Texture.RIGHT_ARROW_BUTTON.get(), () -> {page++; createButtons();});

    public PagedList(List<T> list, String headerText, Client client)
    {
        this.list = list;
        this.headerText = headerText;

        backButton = new TextButton(575, 800, "Back", 80, Color.WHITE, true, () -> client.switchGameState(new TitleState()));

        backButton.setActionButton(Input.BACK);
        backButton.setSelectedColor(Color.rgb(0, 80, 0));
        backButton.setDeselectedColor(Color.rgb(80, 0 , 0));
        backButton.setWidth(750);
        leftArrowButton.setActionButton(Input.PREVIOUS_PAGE);
        rightArrowButton.setActionButton(Input.NEXT_PAGE);
    }

    protected abstract void onItemButtonActivation(T item, TextButton levelButton, int yPosition);
    protected abstract String getItemButtonName(T item, int index);

    public void createButtons()
    {
        maxPage = (int) Math.ceil(list.size() / 5.0);
        maxPage = maxPage == 0 ? 1 : maxPage;
        page = Math.max(page, 1);
        page = Math.min(page, maxPage);

        int itemsOnPage = list.size() - (page * 5) < 0 ? (list.size() % 5) : 5;
        int buttonCount = newButton == null ? itemsOnPage : itemsOnPage + 1;

        Button[] itemButtons = new Button[buttonCount];

        if(newButton != null)
            itemButtons[itemsOnPage] = newButton;

        for(int i = 0; i < itemsOnPage; i++)
        {
            int index = ((page - 1) * 5) + i;
            int yPosition = 200 + (i * 120);
            T item = list.get(index);

            TextButton itemButton = new TextButton(960, yPosition, null, 80, Color.WHITE, false, null);

            itemButton.setActivationHandler(() -> onItemButtonActivation(item, itemButton, yPosition));
            itemButton.setWidth(1520);
            itemButton.setSelectedColor(Color.rgb(0, 80, 0));
            itemButton.setDeselectedColor(Color.rgb(80, 0 , 0));
            itemButton.setText(getItemButtonName(item, index));

            itemButtons[i] = itemButton;
        }

        int index = 0;
        int buttonBeingClickedIndex = -1;

        if(itemButtonStrip != null)
        {
            index = itemButtonStrip.getIndex() == itemButtonStrip.getMaximumIndex() ? itemsOnPage : itemButtonStrip.getIndex();

            for(int i = 0; i < itemButtonStrip.buttons.length; i++)
                if(itemButtonStrip.buttons[i].beingClicked)
                    buttonBeingClickedIndex = i;
        }

        itemButtonStrip = new ButtonStrip(ButtonStrip.Orientation.VERTICAL, itemButtons);

        itemButtonStrip.setIndex(index > itemButtonStrip.getMaximumIndex() ? itemsOnPage - 1 : index);

        if(Input.MOUSE_LEFT.isActive() && buttonBeingClickedIndex != -1)
            itemButtonStrip.forceButtonToBeClicked(buttonBeingClickedIndex);
    }

    public void renderArrowButtons(GraphicsContext gc)
    {
        if(Input.isUsingMouseControls())
        {
            backButton.setWidth(newButton == null ? 1520 : 750);
            backButton.setPosition(newButton == null ? 960 : 575, 800);
            backButton.detectIfHoveredAndRender(gc);
            leftArrowButton.detectIfHoveredAndRender(gc);
            rightArrowButton.detectIfHoveredAndRender(gc);
        }

        backButton.checkForActionButtonActivation();
        leftArrowButton.checkForActionButtonActivation();
        rightArrowButton.checkForActionButtonActivation();

        inputIndicator.add("Back to Main Menu", Input.BACK);
        inputIndicator.add("Previous Page", Input.PREVIOUS_PAGE);
        inputIndicator.add("Next Page", Input.NEXT_PAGE);
    }

    public void renderBackgroundAndItemButtonStrip(GraphicsContext gc)
    {
        gc.setFill(Color.BLACK);
        gc.fillRect(150, 30, 1620, 950);
        gc.setFont(Font.font("Arial", 80));
        gc.setFill(Color.WHITE);

        String pageText = "Page " + page + " of " + maxPage;
        Bounds pageTextShape = Client.computeTextShape(pageText, gc);

        Client.fillText(pageText, 960, 990, gc);
        Client.fillText(headerText, 960, 160, gc);

        leftArrowButton.setPosition(880 - (pageTextShape.getWidth() / 2), 915);
        rightArrowButton.setPosition(1040 + (pageTextShape.getWidth() / 2), 915);

        itemButtonStrip.render(gc);
    }

    public ButtonStrip getItemButtonStrip()
    {
        return itemButtonStrip;
    }

    public int getPage()
    {
        return page;
    }

    public void setPage(int page)
    {
        this.page = page;
    }

    public int getMaxPage()
    {
        return maxPage;
    }

    public InputIndicator getInputIndicator()
    {
        return inputIndicator;
    }

    public void setNewButton(TextButton newButton)
    {
        this.newButton = newButton;
    }
}