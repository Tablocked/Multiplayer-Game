package tablock.network;

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

    LOBBY_LIST
    {
        @Override
        void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server)
        {
            byte[][] dataTypes = new byte[server.hostIdentifiers.size() * 2][];

            for(int i = 0; i < dataTypes.length / 2; i++)
            {
                dataTypes[i * 2] = DataType.INTEGER.encode(server.hostIdentifiers.get(i));
                dataTypes[(i * 2) + 1] = DataType.STRING.encode(server.hostedLevelNames.get(i));
            }

            server.send(ServerPacket.LOBBY_LIST, clientIdentifier, dataTypes);
        }
    },

    HOST
    {
        @Override
        void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server)
        {
            server.hostIdentifiers.add(server.nextHostIdentifier);
            server.hostedLevels.add((byte[]) decodedData[0]);
            server.hostedLevelNames.add((String) decodedData[1]);

            server.nextHostIdentifier++;
        }
    },

    JOIN
    {
        @Override
        void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server)
        {
            for(int i = 0; i < server.hostIdentifiers.size(); i++)
                if(server.hostIdentifiers.get(i) == (int) decodedData[0])
                    server.send(ServerPacket.JOIN, clientIdentifier, DataType.BYTE_ARRAY.encode(server.hostedLevels.get(i)), DataType.STRING.encode(server.hostedLevelNames.get(i)));
        }
    };

    abstract void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server);
}