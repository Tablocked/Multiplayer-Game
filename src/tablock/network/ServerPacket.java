package tablock.network;

public enum ServerPacket
{
    PLAYER_NAME
    {
        @Override
        void respondToServerPacket(Object[] decodedData, Client client)
        {
            client.name = "Player " + decodedData[0];
        }
    };

    abstract void respondToServerPacket(Object[] decodedData, Client client);
}