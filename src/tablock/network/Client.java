package tablock.network;

import tablock.core.Player;
import tablock.network.packet.Packet;
import tablock.network.packet.client.DisconnectPacket;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Client extends Network
{
	private InetAddress ipAddress;
	private final List<Player> onlinePlayers = new ArrayList<>();
	private final Player player = new Player(null, -1);
	
	public Client(String addressName)
	{
		super(false);

		try
		{
			this.ipAddress = InetAddress.getByName(addressName);
		}
		catch(UnknownHostException exception)
		{
			exception.printStackTrace();
		}
	}
	
	@Override
	public void respondToPacket(byte[] packet, DatagramPacket receivedPacket)
	{
		Packet.ServerPacket serverPacket = (Packet.ServerPacket) deserializePacket(packet);
		
		serverPacket.respondToServerPacket(this, onlinePlayers, player);
	}

	@Override
	public void closeSocket()
	{
		sendPacket(new DisconnectPacket());

		super.closeSocket();
	}

	public void sendPacket(Packet packet)
	{
		super.sendPacket(packet, this.ipAddress, super.port);
	}

	public List<Player> getOnlinePlayers()
	{
		return new ArrayList<>(onlinePlayers);
	}

	
	public Player getPlayer()
	{
		return player;
	}
}