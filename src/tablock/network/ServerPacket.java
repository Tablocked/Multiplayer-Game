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
                    TargetedPlayer targetedPlayer = client.playersInHostedLevel.get(identifier);
                    Object[] playerData = Arrays.copyOfRange(decodedData, i + 1, i + 8);

                    if(targetedPlayer == null)
                        client.playersInHostedLevel.put(identifier, new TargetedPlayer(playerData));
                    else
                        targetedPlayer.update(playerData);

                    identifiers.add(identifier);
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
                TargetedPlayer targetedPlayer = client.playersInHostedLevel.get((byte) decodedData[i]);

                if(targetedPlayer != null)
                    targetedPlayer.name = (String) decodedData[i + 1];
            }
        }
    };

    abstract void respondToServerPacket(Object[] decodedData, Client client);
}