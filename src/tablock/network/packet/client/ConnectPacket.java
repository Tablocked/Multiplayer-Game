package tablock.network.packet.client;

import tablock.core.Player;
import tablock.network.Server;
import tablock.network.packet.Packet.ClientPacket;
import tablock.network.packet.server.NamePacket;

import java.io.Serial;
import java.net.DatagramPacket;
import java.util.List;

public class ConnectPacket implements ClientPacket
{
	@Serial
    private static final long serialVersionUID = 5659279448121148474L;
	
	@Override
	public void respondToClientPacket(Server server, DatagramPacket receivedPacket, List<Player> players)
	{
		players.add(new Player(receivedPacket.getAddress(), receivedPacket.getPort()));
		
		server.sendPacket(new NamePacket(receivedPacket.getAddress(), receivedPacket.getPort()), receivedPacket);
	}
}