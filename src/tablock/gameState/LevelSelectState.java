package tablock.gameState;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import tablock.core.FilePointer;
import tablock.core.Input;
import tablock.core.TextFieldHandler;
import tablock.level.Level;
import tablock.network.Client;
import tablock.network.ClientPacket;
import tablock.network.DataType;
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

public class LevelSelectState extends GameState
{
    private ButtonStrip optionButtonStrip;
    private ButtonStrip confirmButtonStrip;
    private TextButton hostButton;
    private boolean renamingInProgress;
    private TextFieldHandler textFieldHandler;
    private final List<File> levelFiles = new ArrayList<>();

    private final PagedList<File> pagedList = new PagedList<>(levelFiles, "Select Level", CLIENT)
    {
        @Override
        public void createButtons()
        {
            updateLevelFiles();

            optionButtonStrip = null;
            confirmButtonStrip = null;
            hostButton = null;

            newButton.setFrozen(false);

            super.createButtons();
        }

        @Override
        protected void onItemButtonActivation(File levelFile, TextButton levelButton, int yPosition)
        {
            FilePointer levelPointer = new FilePointer(levelFile);

            hostButton = new TextButton("Host", 50, () ->
            {
                try
                {
                    CLIENT.send(ClientPacket.HOST, DataType.BYTE_ARRAY.encode(Files.readAllBytes(levelPointer.getFile().toPath())), DataType.STRING.encode(levelPointer.getFile().getName()));
                    CLIENT.switchGameState(new PlayState(deserializeLevel(levelPointer.getFile())));
                }
                catch(IOException exception)
                {
                    exception.printStackTrace();
                }
            });

            TextButton editButton = new TextButton("Edit", 50, () -> CLIENT.switchGameState(new CreateState(deserializeLevel(levelPointer.getFile()), levelPointer.getFile().toPath())));
            TextButton renameButton = new TextButton("Rename", 50, null);
            TextButton deleteButton = new TextButton("Delete", 50, () -> onDeleteButtonActivation(yPosition, levelPointer.getFile()));

            renameButton.setActivationHandler(() -> onRenameButtonActivation(levelButton, levelPointer));

            optionButtonStrip = new ButtonStrip(ButtonStrip.Orientation.HORIZONTAL, 1384, yPosition, 10, hostButton, editButton, renameButton, deleteButton);

            optionButtonStrip.setIndex(3);
            optionButtonStrip.preventActivationForOneFrame();

            pagedList.getItemButtonStrip().setFrozen(true);
        }

        @Override
        protected String getItemButtonName(File levelFile, int index)
        {
            return levelFile.getName();
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

    public LevelSelectState()
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
            optionButtonStrip.setIndex(3);
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

        renamingInProgress = true;
        
        textFieldHandler = new TextFieldHandler(levelButton.getText(), levelButton.getX() - 760, levelButton.getY())
        {
            @Override
            public void onConfirmation(String text)
            {
                String newPath = levelPointer.getFile().toPath().getParent().toString() + "/" + levelButton.getText();

                if(!levelPointer.changeFilePath(newPath))
                    levelButton.setText(text);

                stopRenaming();
            }

            @Override
            public void onKeyTyped(String text, boolean cancelling)
            {
                super.onKeyTyped(text, cancelling);

                levelButton.setText(text);

                if(cancelling)
                    stopRenaming();
            }
        };

        Input.setTextFieldHandler(textFieldHandler);
    }

    private void deselectNewButton()
    {
        ButtonStrip itemButtonStrip = pagedList.getItemButtonStrip();

        if(itemButtonStrip.getIndex() == itemButtonStrip.getMaximumIndex())
            itemButtonStrip.setIndex(itemButtonStrip.getMaximumIndex() - 1);
    }

    private void stopRenaming()
    {
        optionButtonStrip.setFrozen(false);
        optionButtonStrip.setHidden(false);
        
        textFieldHandler = null;
    }

    @Override
    public void renderNextFrame(GraphicsContext gc)
    {
        gc.setFill(Color.BLACK);
        gc.fillRect(150, 30, 1620, 950);
        gc.setFont(Font.font("Arial", 80));
        gc.setFill(Color.WHITE);

        String pageText = "Page " + pagedList.getPage() + " of " + pagedList.getMaxPage();

        Client.fillText(pageText, 960, 990, gc);
        Client.fillText("Select Level", 960, 160, gc);

        if(hostButton != null)
            hostButton.setDisabled(!CLIENT.isConnected());

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
            if(Input.BACK.wasJustActivated() && confirmButtonStrip == null && !renamingInProgress)
            {
                optionButtonStrip = null;
                hostButton = null;

                pagedList.getItemButtonStrip().setFrozen(false);
            }
            else
                Objects.requireNonNullElseGet(confirmButtonStrip, () -> optionButtonStrip).render(gc);

            String text = confirmButtonStrip != null || renamingInProgress ? "Cancel" : "Back";

            pagedList.getInputIndicator().add(text, Input.BACK);
        }

        renamingInProgress = Input.isTextFieldActive();

        if(textFieldHandler != null)
            textFieldHandler.renderTypingCursor(false, gc);

        newButton.detectIfHoveredAndRender(gc);

        pagedList.getInputIndicator().render(gc);
    }
}