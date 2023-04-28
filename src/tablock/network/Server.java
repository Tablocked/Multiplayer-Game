package tablock.network;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import tablock.network.packet.Packet;
import tablock.network.packet.server.PlayerPositionsPacket;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.List;

public class Server extends Network
{
	private final List<Player> players = new ArrayList<>();
	
	public Server()
	{
		super(true);

		Timeline timeline = new Timeline(new KeyFrame(Duration.millis(10), (event) -> tick()));

		timeline.setCycleCount(Timeline.INDEFINITE);
		timeline.play();
	}

	private void tick()
	{
		List<Player> copiedPlayers = new ArrayList<>(this.players);

		copiedPlayers.forEach((player) -> sendPacket(new PlayerPositionsPacket(copiedPlayers), player.getIpAddress(), player.getPort()));
	}
	
	@Override
	protected void respondToPacket(byte[] packet, DatagramPacket receivedPacket)
	{
		Packet.ClientPacket clientPacket = (Packet.ClientPacket) deserializePacket(packet);
		
		clientPacket.respondToClientPacket(this, receivedPacket, players);
	}
}