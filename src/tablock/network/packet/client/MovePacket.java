package tablock.network.packet.client;

import tablock.network.Player;
import tablock.network.Server;
import tablock.network.packet.Packet.ClientPacket;

import java.io.Serial;
import java.net.DatagramPacket;
import java.util.List;

public class MovePacket implements ClientPacket
{
	@Serial
	private static final long serialVersionUID = 1789172009713692239L;
	private final Player player;
	
	public MovePacket(Player player)
	{
		this.player = player;
	}
	
	@Override
	public void respondToClientPacket(Server server, DatagramPacket receivedPacket, List<Player> players)
	{
		players.removeIf(player -> player.matches(this.player));
		players.add(player);
	}
}