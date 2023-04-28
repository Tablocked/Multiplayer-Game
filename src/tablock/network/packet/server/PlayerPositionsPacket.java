package tablock.network.packet.server;

import tablock.network.Client;
import tablock.network.Player;
import tablock.network.packet.Packet;
import tablock.network.packet.client.MovePacket;

import java.io.Serial;
import java.util.List;

public class PlayerPositionsPacket implements Packet.ServerPacket
{
	@Serial
	private static final long serialVersionUID = -3395086319927835914L;
	private final List<Player> players;
	public PlayerPositionsPacket(List<Player> players)
	{
		this.players = players;
	}
	
	@Override
	public void respondToServerPacket(Client client, List<Player> onlinePlayers, Player player)
	{
		players.removeIf((onlinePlayer) -> onlinePlayer.matches(player));
		
		onlinePlayers.clear();
		onlinePlayers.addAll(players);
		
		client.sendPacket(new MovePacket(player));
	}
}