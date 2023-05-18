package tablock.network;

import java.net.InetAddress;
import java.util.List;

public class ClientIdentifier
{
    long timeDuringLastPacketReceived = System.currentTimeMillis();
    Player player = new Player(0, 0, 0);
    List<ClientIdentifier> clientsInHostedLevel;
    final InetAddress inetAddress;
    final int port;

    public ClientIdentifier(InetAddress inetAddress, int port)
    {
        this.inetAddress = inetAddress;
        this.port = port;
    }
}