package tablock.network;

import java.net.InetAddress;
import java.util.List;

public class ClientIdentifier
{
    long timeDuringLastPacketReceived = System.currentTimeMillis();
    List<ClientIdentifier> clientsInHostedLevel;
    final Player player = new Player(0, 0, 0);
    final InetAddress inetAddress;
    final int port;

    public ClientIdentifier(InetAddress inetAddress, int port)
    {
        this.inetAddress = inetAddress;
        this.port = port;
    }
}