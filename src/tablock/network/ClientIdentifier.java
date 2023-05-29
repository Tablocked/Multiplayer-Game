package tablock.network;

import tablock.core.Player;

import java.net.InetAddress;
import java.util.List;

public class ClientIdentifier extends Identifier
{
    long timeDuringLastPacketReceived = System.currentTimeMillis();
    List<ClientIdentifier> clientsInHostedLevel;
    Player player = new Player();
    String name = "Player";
    final InetAddress inetAddress;
    final int port;

    public ClientIdentifier(InetAddress inetAddress, int port)
    {
        this.inetAddress = inetAddress;
        this.port = port;
    }
}