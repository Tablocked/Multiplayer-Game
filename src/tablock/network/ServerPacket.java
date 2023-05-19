package tablock.network;

import tablock.gameState.PlayState;
import tablock.level.Level;

public enum ServerPacket
{
    TICK
    {
        @Override
        void respondToServerPacket(Object[] decodedData, Client client)
        {
            byte[][] dataTypes = new byte[0][];

            if(client.player != null)
                dataTypes = new byte[][]{DataType.DOUBLE.encode(client.player.x), DataType.DOUBLE.encode(client.player.y), DataType.DOUBLE.encode(client.player.rotationAngle)};

            client.send(ClientPacket.TICK, dataTypes);
            client.playersInHostedLevel.clear();

            if(decodedData.length > 0)
                for(int i = 0; i < decodedData.length / 3; i += 3)
                    client.playersInHostedLevel.add(new Player((double) decodedData[i], (double) decodedData[i + 1], (double) decodedData[i + 2]));
        }
    },

    HOST_LIST
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

    JOIN_HOST
    {
        @Override
        void respondToServerPacket(Object[] decodedData, Client client)
        {
            client.player = new Player(0, 0, 0);

            client.switchGameState(new PlayState((Level) Client.deserializeObject((byte[]) decodedData[0])));
        }
    };

    abstract void respondToServerPacket(Object[] decodedData, Client client);
}