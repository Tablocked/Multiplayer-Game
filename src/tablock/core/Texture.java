package tablock.core;

import javafx.scene.image.Image;
import tablock.network.Client;

import java.io.InputStream;

public enum Texture
{
    PLAY_FROM_START_BUTTON,
    PLAY_FROM_HERE_BUTTON,
    OBJECTS_BUTTON,
    SAVE_BUTTON,
    PLATFORM_BUTTON,
    WARNING,
    START_POINT,
    VERTEX,
    ADD_VERTEX,
    CHECKMARK,
    MOUSE_RIGHT,
    MOUSE_MIDDLE,
    KEYBOARD_DELETE,
    KEYBOARD_S,
    KEYBOARD_R,
    KEYBOARD_SHIFT_AND_MOUSE_LEFT,
    KEYBOARD_UP_ARROW,
    KEYBOARD_DOWN_ARROW,
    NINTENDO_B,
    NINTENDO_LEFT_BUTTON,
    NINTENDO_RIGHT_BUTTON,
    PLAYSTATION_CIRCLE,
    PLAYSTATION_LEFT_BUTTON,
    PLAYSTATION_RIGHT_BUTTON,
    KEYBOARD_ESCAPE,
    KEYBOARD_Q,
    KEYBOARD_E,
    LEFT_ARROW_BUTTON,
    RIGHT_ARROW_BUTTON;

    private final Image texture;

    Texture()
    {
        String[] words = name().toLowerCase().split("_");
        StringBuilder textureName = new StringBuilder().append(words[0]);

        for(int i = 1; i < words.length; i++)
            textureName.append(words[i].substring(0, 1).toUpperCase()).append(words[i].substring(1));

        InputStream inputStream = Client.class.getClassLoader().getResourceAsStream("textures/" + textureName + ".png");

        assert inputStream != null;

        this.texture = new Image(inputStream);
    }

    public Image get()
    {
        return texture;
    }
}