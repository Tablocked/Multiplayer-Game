package tablock.core;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import tablock.gameState.Renderer;
import tablock.gameState.TitleScreen;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main extends Application
{
	public static final Image PLAY_FROM_START_BUTTON_TEXTURE = getTexture("playFromStartButton");
	public static final Image PLAY_FROM_HERE_BUTTON_TEXTURE = getTexture("playFromHereButton");
	public static final Image OBJECTS_BUTTON_TEXTURE = getTexture("objectsButton");
	public static final Image SAVE_BUTTON_TEXTURE = getTexture("saveButton");
	public static final Image PLATFORM_BUTTON_TEXTURE = getTexture("platformButton");
	public static final Image WARNING_TEXTURE = getTexture("warning");
	public static final Image START_POINT_TEXTURE = getTexture("startPoint");
	public static final Image VERTEX_TEXTURE = getTexture("vertex");
	public static final Image ADD_VERTEX_TEXTURE = getTexture("addVertex");
	public static final Image CHECKMARK_TEXTURE = getTexture("checkmark");
	public static final Image MOUSE_RIGHT_TEXTURE = getTexture("mouseRight");
	public static final Image MOUSE_MIDDLE_TEXTURE = getTexture("mouseMiddle");
	public static final Image KEYBOARD_DELETE_TEXTURE = getTexture("keyboardDelete");
	public static final Image KEYBOARD_S_TEXTURE = getTexture("keyboardS");
	public static final Image KEYBOARD_R_TEXTURE = getTexture("keyboardR");
	public static final Image KEYBOARD_SHIFT_AND_MOUSE_LEFT_TEXTURE = getTexture("keyboardShiftAndMouseLeft");
	private static final String SAVE_DIRECTORY = System.getenv("APPDATA") + "/MultiplayerGame/";

	public static void main(String[] args)
	{
		launch(args);
	}

	@Override
	public void start(Stage stage)
	{
		Canvas canvas = new Canvas(1920, 1080);
		Renderer renderer = new Renderer(new TitleScreen(), canvas.getGraphicsContext2D());
		Scene scene = new Scene(new Group(canvas));

		createFolder("");
		createFolder("levels");

		renderer.start();

		Input.initialize(scene);

		stage.setScene(scene);
		stage.setFullScreenExitHint("");
		stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
		stage.setFullScreen(true);
		stage.show();
	}

	private static void createFolder(String path)
	{
		Path fullPath = Path.of(SAVE_DIRECTORY + path);

		try
		{
			if(!Files.exists(fullPath))
				Files.createDirectory(fullPath);
		}
		catch(IOException exception)
		{
			exception.printStackTrace();
		}
	}

	public static Image getTexture(String textureName)
	{
		InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("textures/" + textureName + ".png");

		assert inputStream != null;

		return new Image(inputStream);
	}

	public static File getSavedData(String savedDataPath)
	{
		return new File(SAVE_DIRECTORY + savedDataPath);
	}

	public static byte[] serializeObject(Object object)
	{
		try
		{
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);

			objectOutputStream.writeObject(object);

			return byteArrayOutputStream.toByteArray();
		}
		catch(IOException exception)
		{
			exception.printStackTrace();
		}

		return null;
	}

	public static Object deserializeObject(byte[] object)
	{
		try
		{
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(object);
			ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

			return objectInputStream.readObject();
		}
		catch(IOException | ClassNotFoundException exception)
		{
			exception.printStackTrace();
		}

		return null;
	}
}