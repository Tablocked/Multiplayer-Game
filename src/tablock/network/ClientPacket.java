package tablock.network;

import java.net.DatagramPacket;

public enum ClientPacket
{
    CONNECT
    {
        @Override
        void respondToClientPacket(Object[] decodedData, DatagramPacket receivedPacket, Server server)
        {
            server.clientCount++;

            server.send(ServerPacket.PLAYER_NAME, receivedPacket, DataType.INTEGER.encode(server.clientCount));
        }
    };

    abstract void respondToClientPacket(Object[] decodedData, DatagramPacket receivedPacket, Server server);
}