package tablock.network;

import javafx.animation.AnimationTimer;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCombination;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import tablock.core.Input;
import tablock.core.Player;
import tablock.core.TargetedPlayer;
import tablock.gameState.GameState;
import tablock.gameState.TitleState;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Client extends Network
{
	public Player player = null;
	public String name = "Player";
	public final List<Byte> hostIdentifiers = new ArrayList<>();
	public final List<String> hostedLevelNames = new ArrayList<>();
	public final HashMap<Byte, TargetedPlayer> playersInHostedLevel = new HashMap<>();
	private GameState gameState = new TitleState();
	private long timeDuringLastPacketReceived = 0;
	private long timeDuringLastConnectPacketSent = 0;
	private boolean displayInfo;
	private final LoopCounter frameCounter = new LoopCounter();
	private final InetAddress inetAddress;

	public static void main(String[] args)
	{
		launch(args);
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

	public static Bounds computeTextShape(String text, Font font)
	{
		Text textObject = new Text(text);

		textObject.setFont(font);

		return textObject.getBoundsInParent();
	}

	public static Bounds computeTextShape(String text, GraphicsContext gc)
	{
		return computeTextShape(text, gc.getFont());
	}

	public static void fillText(String text, double x, double y, Bounds textShape, GraphicsContext gc)
	{
		gc.fillText(text, x - (textShape.getWidth() / 2), y - (textShape.getHeight() / 2));
	}

	public static void fillText(String text, double x, double y, GraphicsContext gc)
	{
		fillText(text, x, y, computeTextShape(text, gc), gc);
	}

	public Client() throws SocketException, UnknownHostException
	{
		super(new DatagramSocket());

		inetAddress = InetAddress.getByName("tablocked.us.to");
	}

	@Override
	void respondToPacket(DatagramPacket receivedPacket, byte[] data, int dataLength)
	{
		timeDuringLastPacketReceived = System.currentTimeMillis();

		ServerPacket.values()[data[1]].respondToServerPacket(decodePacket(data, dataLength), this);
	}

	@Override
	public void start(Stage stage)
	{
		super.start(stage);

		Canvas canvas = new Canvas(1920, 1080);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		Scene scene = new Scene(new Group(canvas));
		double scaleFactor = Screen.getPrimary().getBounds().getWidth() / 1920;

		Input.initialize(scene);
		GameState.initialize(this);

		gc.scale(scaleFactor, scaleFactor);

		AnimationTimer renderLoop = new AnimationTimer()
		{
			@Override
			public void handle(long l)
			{
				int framesPerSecond = frameCounter.computeLoopsPerSecond();

				if(System.currentTimeMillis() - timeDuringLastPacketReceived > 10000 && System.currentTimeMillis() - timeDuringLastConnectPacketSent > 1000)
				{
					timeDuringLastConnectPacketSent = System.currentTimeMillis();

					send(ClientPacket.CONNECT);
				}

				gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

				Input.beginPoll();

				gameState.renderNextFrame(gc);

				if(Input.DISPLAY_INFO.wasJustActivated())
					displayInfo = !displayInfo;

				if(displayInfo)
				{
					int y = 15;

					gc.setFill(Color.BLACK);
					gc.setStroke(Color.YELLOW);
					gc.setLineWidth(2);
					gc.setFont(Font.font("Arial", 30));

					fillAndStrokeText(framesPerSecond + " FPS", y += 30, gc);
					fillAndStrokeText( getBytesSent() / 1024 + " KB sent", y += 30, gc);
					fillAndStrokeText( getBytesReceived() / 1024 + " KB received", y += 30, gc);
					fillAndStrokeText( "Memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576 + " MB out of " + Runtime.getRuntime().maxMemory() / 1048576 + " MB", y += 30, gc);
				}

				Input.endPoll();

				frameCounter.increment();
			}
		};

		renderLoop.start();

		stage.setScene(scene);
		stage.setFullScreenExitHint("");
		stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
		stage.setFullScreen(true);
		stage.show();
	}

	public void switchGameState(GameState nextGameState)
	{
		gameState = nextGameState;
	}

	public void send(ClientPacket clientPacket, byte[]... dataTypes)
	{
		send(clientPacket.ordinal(), inetAddress, PORT, dataTypes);
	}

	public boolean isConnected()
	{
		return System.currentTimeMillis() - timeDuringLastPacketReceived <= 10000;
	}

	private void fillAndStrokeText(String text, double y, GraphicsContext gc)
	{
		gc.strokeText(text, 20, y);
		gc.fillText(text, 20, y);
	}
}