package tablock.network.packet.server;

import tablock.core.Player;
import tablock.network.Client;
import tablock.network.packet.Packet.ServerPacket;
import tablock.network.packet.client.MovePacket;

import java.io.Serial;
import java.util.List;

public class PlayerPositionsPacket implements ServerPacket
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