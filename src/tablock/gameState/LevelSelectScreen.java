package tablock.gameState;

import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import tablock.core.Input;
import tablock.core.Level;
import tablock.core.Main;
import tablock.userInterface.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LevelSelectScreen implements GameState
{
    private ButtonStrip levelButtonStrip;
    private ButtonStrip optionButtonStrip;
    private ButtonStrip confirmButtonStrip;
    private int page = 1;
    private int maxPage;
    private boolean renamingInProgress = false;
    private final InputIndicatorStrip levelInputIndicatorStrip;
    private final InputIndicatorStrip optionInputIndicatorStrip;
    private final InputIndicatorStrip deleteInputIndicatorStrip;
    private final Button backButton = new TextButton(575, 800, "Back", 80, Color.WHITE, true, () -> Renderer.setCurrentState(new TitleScreen()));
    private final Button leftArrowButton = new ImageButton(870, 880, Main.getTexture("leftArrowButton"), () -> {page--; createButtons();});
    private final Button rightArrowButton = new ImageButton(1050, 900, Main.getTexture("rightArrowButton"), () -> {page++; createButtons();});

    private final Button newButton = new TextButton(960, 800, "New", 80, Color.WHITE, true, () ->
    {
        try
        {
            byte[] serializedLevel = Main.serializeObject(new Level());
            String levelDirectory = Main.getSavedData("levels").getPath() + "/";
            String levelName = "Unnamed 0";
            Path levelPath = Path.of(levelDirectory + levelName);

            for(int i = 1; Files.exists(levelPath); i++)
            {
                levelName = "Unnamed " + i;
                levelPath = Path.of(levelDirectory + levelName);
            }

            assert serializedLevel != null;

            Files.write(levelPath, serializedLevel);

            File[] levelFiles = getLevels();

            for(int i = 0; i < levelFiles.length; i++)
                if(levelFiles[i].equals(levelPath.toFile()))
                    page = (i / 5) + 1;

            createButtons();
            deselectNewButton();
        }
        catch(IOException exception)
        {
            throw new RuntimeException(exception);
        }
    });

    public LevelSelectScreen()
    {
        levelInputIndicatorStrip = new InputIndicatorStrip
        (
            new InputIndicator(Input.UI_BACK, "Back to Main Menu"),
            new InputIndicator(Input.UI_PAGE_LEFT, "Previous Page"),
            new InputIndicator(Input.UI_PAGE_RIGHT, "Next Page")
        );

        optionInputIndicatorStrip = new InputIndicatorStrip(new InputIndicator(Input.UI_BACK, "Back"));
        deleteInputIndicatorStrip = new InputIndicatorStrip(new InputIndicator(Input.UI_BACK, "Cancel"));

        backButton.setActionButton(Input.UI_BACK);
        backButton.setSelectedColor(Color.rgb(0, 80, 0));
        backButton.setDeselectedColor(Color.rgb(80, 0 , 0));
        backButton.setWidth(750);
        leftArrowButton.setActionButton(Input.UI_PAGE_LEFT);
        rightArrowButton.setActionButton(Input.UI_PAGE_RIGHT);
        newButton.setSelectedColor(Color.rgb(0, 80, 0));
        newButton.setDeselectedColor(Color.rgb(80, 0 , 0));

        createButtons();
    }

    private void createButtons()
    {
        List<File> levels = new ArrayList<>(Arrays.asList(getLevels()));

        maxPage = (int) Math.ceil(levels.size() / 5.0);
        maxPage = maxPage == 0 ? 1 : maxPage;
        page = Math.max(page, 1);
        page = Math.min(page, maxPage);

        optionButtonStrip = null;
        confirmButtonStrip = null;

        int levelsOnPage = levels.size() - (page * 5) < 0 ? (levels.size() % 5) : 5;

        Button[] levelButtons = new Button[levelsOnPage + 1];

        newButton.setFrozen(false);

        levelButtons[levelsOnPage] = newButton;

        for(int i = 0; i < levelsOnPage; i++)
        {
            File level = levels.get(((page - 1) * 5) + i);
            int yPosition = 200 + (i * 120);

            TextButton renameButton = new TextButton("Rename", 50, null);

            TextButton levelButton = new TextButton(960, yPosition, level.getName(), 80, Color.WHITE, false, () -> onLevelButtonActivation(level, renameButton, yPosition));

            renameButton.setActivationHandler(() -> onRenameButtonActivation(levelButton, level));
            levelButton.setWidth(1520);
            levelButton.setSelectedColor(Color.rgb(0, 80, 0));
            levelButton.setDeselectedColor(Color.rgb(80, 0 , 0));

            levelButtons[i] = levelButton;
        }

        int index = 0;

        if(levelButtonStrip != null)
            index = levelButtonStrip.getIndex() == levelButtonStrip.getMaximumIndex() ? levelsOnPage : levelButtonStrip.getIndex();

        levelButtonStrip = new ButtonStrip(ButtonStrip.Orientation.VERTICAL, levelButtons);

        levelButtonStrip.setIndex(index > levelButtonStrip.getMaximumIndex() ? levelsOnPage - 1 : index);
    }

    private File[] getLevels()
    {
        return Main.getSavedData("levels").listFiles();
    }

    private void onLevelButtonActivation(File level, Button renameButton, int yPosition)
    {
        Button playButton = new TextButton("Play", 50, () -> Renderer.setCurrentState(new PlayScreen(deserializeLevel(level))));
        Button editButton = new TextButton("Edit", 50, () -> Renderer.setCurrentState(new CreateScreen(deserializeLevel(level))));
        Button deleteButton = new TextButton("Delete", 50, () -> onDeleteButtonActivation(yPosition, level));

        optionButtonStrip = new ButtonStrip(ButtonStrip.Orientation.HORIZONTAL, 1384, yPosition, 10, playButton, editButton, renameButton, deleteButton);

        optionButtonStrip.preventActivationForOneFrame();
        levelButtonStrip.setFrozen(true);
    }

    private Level deserializeLevel(File level)
    {
        try
        {
            byte[] bytes = Files.readAllBytes(level.toPath());
            Level deserializedLevel = (Level) Main.deserializeObject(bytes);

            assert deserializedLevel != null;

            return deserializedLevel;
        }
        catch(IOException exception)
        {
            throw new RuntimeException(exception);
        }
    }

    private void onDeleteButtonActivation(int yPosition, File level)
    {
        TextButton confirmButton = new TextButton("Confirm", 50, () ->
        {
            try
            {
                Files.delete(level.toPath());
            }
            catch(IOException exception)
            {
                throw new RuntimeException(exception);
            }

            createButtons();
            deselectNewButton();
        });

        TextButton cancelButton = new TextButton("Cancel", 50, () ->
        {
            optionButtonStrip.setIndex(2);
            confirmButtonStrip = null;
        });

        cancelButton.setActionButton(Input.UI_BACK);

        confirmButtonStrip = new ButtonStrip(ButtonStrip.Orientation.HORIZONTAL, 1512, yPosition, 10, confirmButton, cancelButton);

        confirmButtonStrip.setIndex(1);
    }

    private void onRenameButtonActivation(TextButton levelButton, File level)
    {
        optionButtonStrip.setFrozen(true);
        optionButtonStrip.setHidden(true);

        renamingInProgress = true;

        Input.setOnKeyTypedHandler(keyEvent -> onKeyTyped(levelButton, keyEvent, level));
    }

    private void onKeyTyped(TextButton levelButton, KeyEvent keyEvent, File level)
    {
        String text = levelButton.getText();
        String character = keyEvent.getCharacter();
        int asciiCode = character.charAt(0);

        if(character.equals("\b"))
        {
            if(text.length() == 0)
                text = "";
            else
                text = text.substring(0, text.length() - 1);
        }
        else if(character.equals("\r"))
        {
            String newPath = level.toPath().getParent().toString() + "/" + levelButton.getText();

            level.renameTo(new File(newPath));

            stopRenaming();
        }
        else if(asciiCode >= 32 && asciiCode <= 126)
            text += character;

        Font font = Font.font("Arial", 80);
        Bounds textShape = Renderer.getTextShape(text, font);

        while(textShape.getWidth() > 800)
        {
            text = text.substring(0, text.length() - 1);
            textShape = Renderer.getTextShape(text, font);
        }

        levelButton.setText(text);
    }

    private void deselectNewButton()
    {
        if(levelButtonStrip.getIndex() == levelButtonStrip.getMaximumIndex())
            levelButtonStrip.setIndex(levelButtonStrip.getMaximumIndex() - 1);
    }

    private void stopRenaming()
    {
        Input.setOnKeyTypedHandler(null);

        createButtons();

        renamingInProgress = false;
    }

    @Override
    public void renderNextFrame(GraphicsContext gc)
    {
        gc.setFill(Color.BLACK);
        gc.fillRect(150, 30, 1620, 950);
        gc.setFont(Font.font("Arial", 80));
        gc.setFill(Color.WHITE);

        String pageText = "Page " + page + " of " + maxPage;
        Bounds pageTextShape = Renderer.getTextShape(pageText, gc);

        Renderer.fillText(960, 990, pageText, gc);
        Renderer.fillText(960, 160, "Select Level", gc);

        leftArrowButton.setPosition(880 - (pageTextShape.getWidth() / 2), 915);
        rightArrowButton.setPosition(1040 + (pageTextShape.getWidth() / 2), 915);
        newButton.setWidth(Input.isUsingMouseControls() ? 750 : 1520);
        newButton.setPosition(Input.isUsingMouseControls() ? 1345 : 960, 800);
        newButton.setHidden(true);

        levelButtonStrip.render(gc);

        if(optionButtonStrip == null && confirmButtonStrip == null)
        {
            if(Input.isUsingMouseControls())
            {
                backButton.render(gc);
                leftArrowButton.render(gc);
                rightArrowButton.render(gc);
            }

            backButton.checkForActionButtonActivation();
            leftArrowButton.checkForActionButtonActivation();
            rightArrowButton.checkForActionButtonActivation();
            newButton.setHidden(false);

            levelInputIndicatorStrip.render(gc);
        }
        else
        {
            if(Input.UI_BACK.wasJustActivated() && confirmButtonStrip == null)
            {
                if(renamingInProgress)
                    stopRenaming();
                else
                {
                    optionButtonStrip = null;

                    levelButtonStrip.setFrozen(false);
                }
            }
            else
            {
                if(confirmButtonStrip == null)
                {
                    optionButtonStrip.render(gc);
                    optionInputIndicatorStrip.render(gc);
                }
                else
                {
                    confirmButtonStrip.render(gc);
                    deleteInputIndicatorStrip.render(gc);
                }
            }
        }

        newButton.render(gc);
    }
}