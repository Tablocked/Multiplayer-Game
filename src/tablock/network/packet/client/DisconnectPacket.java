package tablock.network.packet.client;

import tablock.network.Player;
import tablock.network.Server;
import tablock.network.packet.Packet.ClientPacket;

import java.io.Serial;
import java.net.DatagramPacket;
import java.util.List;

public class DisconnectPacket implements ClientPacket
{
	@Serial
    private static final long serialVersionUID = -7794826997280274641L;
	
	@Override
	public void respondToClientPacket(Server server, DatagramPacket receivedPacket, List<Player> players)
	{
		players.removeIf((player) -> player.getIpAddress().equals(receivedPacket.getAddress()) && player.getPort() == receivedPacket.getPort());
	}
}