package tablock.userInterface;

import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import tablock.core.Input;
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
    private final ImageButton leftArrowButton = new ImageButton(870, 880, Client.getTexture("leftArrowButton"), () -> {page--; createButtons();});
    private final ImageButton rightArrowButton = new ImageButton(1050, 900, Client.getTexture("rightArrowButton"), () -> {page++; createButtons();});

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

        int levelsOnPage = list.size() - (page * 5) < 0 ? (list.size() % 5) : 5;
        int buttonCount = newButton == null ? levelsOnPage : levelsOnPage + 1;

        Button[] levelButtons = new Button[buttonCount];

        if(newButton != null)
            levelButtons[levelsOnPage] = newButton;

        for(int i = 0; i < levelsOnPage; i++)
        {
            int index = ((page - 1) * 5) + i;
            int yPosition = 200 + (i * 120);
            T item = list.get(index);

            TextButton levelButton = new TextButton(960, yPosition, null, 80, Color.WHITE, false, null);

            levelButton.setActivationHandler(() -> onItemButtonActivation(item, levelButton, yPosition));
            levelButton.setWidth(1520);
            levelButton.setSelectedColor(Color.rgb(0, 80, 0));
            levelButton.setDeselectedColor(Color.rgb(80, 0 , 0));
            levelButton.setText(getItemButtonName(item, index));

            levelButtons[i] = levelButton;
        }

        int index = 0;
        int buttonBeingClickedIndex = -1;

        if(itemButtonStrip != null)
        {
            index = itemButtonStrip.getIndex() == itemButtonStrip.getMaximumIndex() ? levelsOnPage : itemButtonStrip.getIndex();

            for(int i = 0; i < itemButtonStrip.buttons.length; i++)
                if(itemButtonStrip.buttons[i].beingClicked)
                    buttonBeingClickedIndex = i;
        }

        itemButtonStrip = new ButtonStrip(ButtonStrip.Orientation.VERTICAL, levelButtons);

        itemButtonStrip.setIndex(index > itemButtonStrip.getMaximumIndex() ? levelsOnPage - 1 : index);

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