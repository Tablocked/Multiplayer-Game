package tablock.gameState;

import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import tablock.core.FilePointer;
import tablock.core.Input;
import tablock.level.Level;
import tablock.network.Client;
import tablock.userInterface.ButtonStrip;
import tablock.userInterface.PagedList;
import tablock.userInterface.TextButton;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LevelSelectScreen extends GameState
{
    private ButtonStrip optionButtonStrip;
    private ButtonStrip confirmButtonStrip;
    private TextButton levelButtonDuringLevelRename;
    private String levelNameDuringRenameStart;
    private final List<File> levelFiles = new ArrayList<>();

    private final PagedList<File> pagedList = new PagedList<>(levelFiles, "Select Level")
    {
        @Override
        public void createButtons()
        {
            updateLevelFiles();

            optionButtonStrip = null;
            confirmButtonStrip = null;

            newButton.setFrozen(false);

            super.createButtons();
        }

        @Override
        protected void onItemButtonActivation(File levelFile, TextButton levelButton, int yPosition)
        {
            FilePointer levelPointer = new FilePointer(levelFile);
            TextButton playButton = new TextButton("Host", 50, () -> Renderer.setCurrentState(new PlayScreen(deserializeLevel(levelPointer.getFile()))));
            TextButton editButton = new TextButton("Edit", 50, () -> Renderer.setCurrentState(new CreateScreen(deserializeLevel(levelPointer.getFile()), levelPointer.getFile().toPath())));
            TextButton renameButton = new TextButton("Rename", 50, null);
            TextButton deleteButton = new TextButton("Delete", 50, () -> onDeleteButtonActivation(yPosition, levelPointer.getFile()));

            renameButton.setActivationHandler(() -> onRenameButtonActivation(levelButton, levelPointer));

            optionButtonStrip = new ButtonStrip(ButtonStrip.Orientation.HORIZONTAL, 1384, yPosition, 10, playButton, editButton, renameButton, deleteButton);

            optionButtonStrip.preventActivationForOneFrame();

            pagedList.getItemButtonStrip().setFrozen(true);
        }

        @Override
        protected String getItemButtonName(File item)
        {
            return item.getName();
        }
    };

    private final TextButton newButton = new TextButton(960, 800, "New", 80, Color.WHITE, true, () ->
    {
        try
        {
            byte[] serializedLevel = Client.serializeObject(new Level());
            String levelDirectory = Client.getSavedData("levels").getPath() + "/";
            String levelName = "Unnamed 0";
            Path levelPath = Path.of(levelDirectory + levelName);

            for(int i = 1; Files.exists(levelPath); i++)
            {
                levelName = "Unnamed " + i;
                levelPath = Path.of(levelDirectory + levelName);
            }

            assert serializedLevel != null;

            Files.write(levelPath, serializedLevel);

            updateLevelFiles();

            for(int i = 0; i < levelFiles.size(); i++)
                if(levelFiles.get(i).equals(levelPath.toFile()))
                    pagedList.setPage((i / 5) + 1);

            pagedList.createButtons();

            deselectNewButton();
        }
        catch(IOException exception)
        {
            throw new RuntimeException(exception);
        }
    });

    public LevelSelectScreen()
    {
        newButton.setSelectedColor(Color.rgb(0, 80, 0));
        newButton.setDeselectedColor(Color.rgb(80, 0 , 0));

        pagedList.setNewButton(newButton);
        pagedList.createButtons();
    }

    private void updateLevelFiles()
    {
        levelFiles.clear();
        levelFiles.addAll(List.of(Objects.requireNonNull(Client.getSavedData("levels").listFiles())));
    }

    private Level deserializeLevel(File level)
    {
        try
        {
            byte[] bytes = Files.readAllBytes(level.toPath());
            Level deserializedLevel = (Level) Client.deserializeObject(bytes);

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

            pagedList.createButtons();

            deselectNewButton();
        });

        TextButton cancelButton = new TextButton("Cancel", 50, () ->
        {
            optionButtonStrip.setIndex(2);
            confirmButtonStrip = null;
        });

        cancelButton.setActionButton(Input.BACK);

        confirmButtonStrip = new ButtonStrip(ButtonStrip.Orientation.HORIZONTAL, 1512, yPosition, 10, confirmButton, cancelButton);

        confirmButtonStrip.setIndex(1);
    }

    private void onRenameButtonActivation(TextButton levelButton, FilePointer levelPointer)
    {
        optionButtonStrip.setFrozen(true);
        optionButtonStrip.setHidden(true);

        levelButtonDuringLevelRename = levelButton;
        levelNameDuringRenameStart = levelButton.getText();

        Input.setOnKeyTypedHandler(keyEvent -> onKeyTyped(keyEvent, levelPointer));
    }

    private void onKeyTyped(KeyEvent keyEvent, FilePointer levelPointer)
    {
        String text = levelButtonDuringLevelRename.getText();
        String character = keyEvent.getCharacter();
        int asciiCode = character.charAt(0);

        if(character.equals(".") || character.equals("/") || character.equals("\\"))
            return;
        else if(character.equals("\b"))
        {
            if(text.length() == 0)
                text = "";
            else
                text = text.substring(0, text.length() - 1);
        }
        else if(character.equals("\r"))
        {
            String newPath = levelPointer.getFile().toPath().getParent().toString() + "/" + levelButtonDuringLevelRename.getText();

            if(!levelPointer.changeFilePath(newPath))
                levelButtonDuringLevelRename.setText(levelNameDuringRenameStart);

            stopRenaming();

            return;
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

        levelButtonDuringLevelRename.setText(text);
    }

    private void deselectNewButton()
    {
        ButtonStrip itemButtonStrip = pagedList.getItemButtonStrip();

        if(itemButtonStrip.getIndex() == itemButtonStrip.getMaximumIndex())
            itemButtonStrip.setIndex(itemButtonStrip.getMaximumIndex() - 1);
    }

    private void stopRenaming()
    {
        Input.setOnKeyTypedHandler(null);

        optionButtonStrip.setFrozen(false);
        optionButtonStrip.setHidden(false);

        levelButtonDuringLevelRename = null;
        levelNameDuringRenameStart = null;
    }

    @Override
    public void renderNextFrame(GraphicsContext gc)
    {
        gc.setFill(Color.BLACK);
        gc.fillRect(150, 30, 1620, 950);
        gc.setFont(Font.font("Arial", 80));
        gc.setFill(Color.WHITE);

        String pageText = "Page " + pagedList.getPage() + " of " + pagedList.getMaxPage();

        Renderer.fillText(960, 990, pageText, gc);
        Renderer.fillText(960, 160, "Select Level", gc);

        newButton.setWidth(Input.isUsingMouseControls() ? 750 : 1520);
        newButton.setPosition(Input.isUsingMouseControls() ? 1345 : 960, 800);
        newButton.setHidden(true);

        pagedList.renderBackgroundAndItemButtonStrip(gc);

        if(optionButtonStrip == null && confirmButtonStrip == null)
        {
            pagedList.renderArrowButtons(gc);

            newButton.setHidden(false);
        }
        else
        {
            if(Input.BACK.wasJustActivated() && confirmButtonStrip == null)
            {
                if(levelButtonDuringLevelRename != null)
                {
                    levelButtonDuringLevelRename.setText(levelNameDuringRenameStart);

                    stopRenaming();
                }
                else
                {
                    optionButtonStrip = null;

                    pagedList.getItemButtonStrip().setFrozen(false);
                }
            }
            else
                Objects.requireNonNullElseGet(confirmButtonStrip, () -> optionButtonStrip).render(gc);

            String text = confirmButtonStrip != null || levelButtonDuringLevelRename != null ? "Cancel" : "Back";

            pagedList.getInputIndicator().add(text, Input.BACK);
        }

        newButton.calculateSelectedAndRender(gc);

        pagedList.getInputIndicator().render(gc);
    }
}