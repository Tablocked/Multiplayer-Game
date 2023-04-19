package tablock.network.packet.server;

import tablock.core.Player;
import tablock.network.Client;
import tablock.network.packet.Packet.ServerPacket;

import java.io.Serial;
import java.net.InetAddress;
import java.util.List;

public class NamePacket implements ServerPacket
{
	@Serial
	private static final long serialVersionUID = -4295288205191646156L;
	private final InetAddress ipAddress;
	private final int port;
	
	public NamePacket(InetAddress ipAddress, int port)
	{
		this.ipAddress = ipAddress;
		this.port = port;
	}
	
	@Override
	public void respondToServerPacket(Client client, List<Player> onlinePlayers, Player player)
	{
		player.setIpAddress(ipAddress);
		player.setPort(port);
	}
}