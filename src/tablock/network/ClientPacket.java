package tablock.network;

public enum ClientPacket
{
    TICK
    {
        @Override
        void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server)
        {
            clientIdentifier.timeDuringLastPacketReceived = System.currentTimeMillis();

            if(decodedData.length > 0)
            {
                clientIdentifier.player.x = (double) decodedData[0];
                clientIdentifier.player.y = (double) decodedData[1];
                clientIdentifier.player.rotationAngle = (double) decodedData[2];
            }
        }
    },

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

                dataTypes[i * 2] = DataType.INTEGER.encode(hostedLevel.identifier);
                dataTypes[(i * 2) + 1] = DataType.STRING.encode(hostedLevel.levelName);
            }

            server.send(ServerPacket.LOBBY_LIST, clientIdentifier, dataTypes);
        }
    },

    HOST
    {
        @Override
        void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server)
        {
            server.hostedLevels.add(new HostedLevel(server.nextHostIdentifier, (byte[]) decodedData[0], (String) decodedData[1], clientIdentifier));

            server.nextHostIdentifier++;
        }
    },

    JOIN_HOST
    {
        @Override
        void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server)
        {
            for(HostedLevel hostedLevel : server.hostedLevels)
                if(hostedLevel.identifier == (int) decodedData[0])
                {
                    server.send(ServerPacket.JOIN_HOST, clientIdentifier, DataType.BYTE_ARRAY.encode(hostedLevel.level), DataType.STRING.encode(hostedLevel.levelName));

                    clientIdentifier.player = new Player(0, 0, 0);

                    hostedLevel.addClient(clientIdentifier);
                }
        }
    },

    LEAVE_HOST
    {
        @Override
        void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server)
        {
            if(clientIdentifier.clientsInHostedLevel != null)
            {
                clientIdentifier.clientsInHostedLevel.remove(clientIdentifier);

                clientIdentifier.clientsInHostedLevel = null;
            }
        }
    };

    abstract void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server);
}