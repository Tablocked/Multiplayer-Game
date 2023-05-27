package tablock.network;

import java.net.InetAddress;
import java.util.List;

public class ClientIdentifier
{
    long timeDuringLastPacketReceived = System.currentTimeMillis();
    List<ClientIdentifier> clientsInHostedLevel;
    final Player player = new Player();
    final byte identifier;
    final InetAddress inetAddress;
    final int port;

    public ClientIdentifier(byte identifier, InetAddress inetAddress, int port)
    {
        this.identifier = identifier;
        this.inetAddress = inetAddress;
        this.port = port;
    }
}