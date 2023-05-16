package tablock.network;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class ClientIdentifier
{
    final InetAddress inetAddress;
    final int port;

    public ClientIdentifier(DatagramPacket connectPacket)
    {
        this.inetAddress = connectPacket.getAddress();
        this.port = connectPacket.getPort();
    }
}