package tablock.network;

import java.util.ArrayList;
import java.util.List;

public enum ClientPacket
{
    CLIENT_NAME
    {
        @Override
        void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server)
        {
            server.send(ServerPacket.CLIENT_NAME, clientIdentifier, DataType.INTEGER.encode(server.clients.size()));
        }
    },

    DISCONNECT
    {
        @Override
        void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server)
        {
            server.clients.remove(clientIdentifier);
        }
    },

    LOBBY_LIST
    {
        @Override
        void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server)
        {
            byte[][] dataTypes = new byte[server.hostedLevels.size() * 2][];

            for(int i = 0; i < dataTypes.length / 2; i++)
            {
                HostedLevel hostedLevel = server.hostedLevels.get(i);

                dataTypes[i * 2] = DataType.INTEGER.encode(hostedLevel.identifier());
                dataTypes[(i * 2) + 1] = DataType.STRING.encode(hostedLevel.levelName());
            }

            server.send(ServerPacket.LOBBY_LIST, clientIdentifier, dataTypes);
        }
    },

    HOST
    {
        @Override
        void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server)
        {
            server.hostedLevels.add(new HostedLevel(server.nextHostIdentifier, (byte[]) decodedData[0], (String) decodedData[1], new ArrayList<>(List.of(clientIdentifier))));

            server.nextHostIdentifier++;
        }
    },

    JOIN
    {
        @Override
        void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server)
        {
            for(HostedLevel hostedLevel : server.hostedLevels)
                if(hostedLevel.identifier() == (int) decodedData[0])
                {
                    server.send(ServerPacket.JOIN, clientIdentifier, DataType.BYTE_ARRAY.encode(hostedLevel.level()), DataType.STRING.encode(hostedLevel.levelName()));

                    hostedLevel.joinedClients().add(clientIdentifier);
                }
        }
    };

    abstract void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server);
}