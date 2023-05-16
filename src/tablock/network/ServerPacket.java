package tablock.network;

import tablock.gameState.PlayScreen;
import tablock.gameState.Renderer;
import tablock.level.Level;

public enum ServerPacket
{
    CLIENT_NAME
    {
        @Override
        void respondToServerPacket(Object[] decodedData, Client client)
        {
            client.name = "Player " + decodedData[0];
        }
    },

    LOBBY_LIST
    {
        @Override
        void respondToServerPacket(Object[] decodedData, Client client)
        {
            client.hostIdentifiers.clear();
            client.hostedLevelNames.clear();

            for(int i = 0; i < decodedData.length; i += 2)
            {
                client.hostIdentifiers.add((int) decodedData[i]);
                client.hostedLevelNames.add((String) decodedData[i + 1]);
            }
        }
    },

    JOIN
    {
        @Override
        void respondToServerPacket(Object[] decodedData, Client client)
        {
            Renderer.setCurrentState(new PlayScreen((Level) Client.deserializeObject((byte[]) decodedData[0])));
        }
    };

    abstract void respondToServerPacket(Object[] decodedData, Client client);
}