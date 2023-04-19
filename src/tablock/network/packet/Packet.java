package tablock.network.packet;

import tablock.core.Player;
import tablock.network.Client;
import tablock.network.Server;

import java.io.Serializable;
import java.net.DatagramPacket;
import java.util.List;

public interface Packet extends Serializable
{
	interface ServerPacket extends Packet
	{
		void respondToServerPacket(Client client, List<Player> onlinePlayers, Player player);
	}
	
	interface ClientPacket extends Packet
	{
		void respondToClientPacket(Server server, DatagramPacket receivedPacket, List<Player> players);
	}
}