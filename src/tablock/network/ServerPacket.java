package tablock.network;

import tablock.core.TargetedPlayer;
import tablock.gameState.PlayState;
import tablock.level.Level;

import java.util.ArrayList;
import java.util.Arrays;

public enum ServerPacket
{
    TICK
    {
        @Override
        void respondToServerPacket(Object[] decodedData, Client client)
        {
            byte[][] dataTypes = new byte[0][];

            if(client.player != null)
            {
                dataTypes = client.player.encode();

                client.player.reset = false;
            }

            client.send(ClientPacket.TICK, dataTypes);

            if(decodedData.length > 0)
            {
                ArrayList<Byte> identifiers = new ArrayList<>();

                for(int i = 0; i < decodedData.length; i += 8)
                {
                    byte identifier = (byte) decodedData[i];
                    Object[] playerData = Arrays.copyOfRange(decodedData, i + 1, i + 8);

                    identifiers.add(identifier);

                    if(client.playersInHostedLevel.containsKey(identifier))
                        client.playersInHostedLevel.get(identifier).update(playerData);
                    else
                        client.playersInHostedLevel.put(identifier, new TargetedPlayer(playerData));
                }

                client.playersInHostedLevel.keySet().removeIf((identifier) -> !identifiers.contains(identifier));
            }

            if(client.player == null || decodedData.length == 0)
                client.playersInHostedLevel.clear();
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
                client.hostIdentifiers.add((byte) decodedData[i]);
                client.hostedLevelNames.add((String) decodedData[i + 1]);
            }
        }
    },

    JOIN_HOST
    {
        @Override
        void respondToServerPacket(Object[] decodedData, Client client)
        {
            client.switchGameState(new PlayState((Level) Client.deserializeObject((byte[]) decodedData[0])));
        }
    },

    PLAYER_NAMES
    {
        @Override
        void respondToServerPacket(Object[] decodedData, Client client)
        {
            for(int i = 0; i < decodedData.length; i += 2)
            {
                byte identifier = (byte) decodedData[i];

                if(client.playersInHostedLevel.containsKey(identifier))
                    client.playersInHostedLevel.get(identifier).name = (String) decodedData[i + 1];
            }
        }
    };

    abstract void respondToServerPacket(Object[] decodedData, Client client);
}