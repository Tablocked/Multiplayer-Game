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