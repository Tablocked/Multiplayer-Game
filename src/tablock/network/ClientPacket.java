package tablock.network;

import tablock.core.Player;

public enum ClientPacket
{
    TICK
    {
        @Override
        void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server)
        {
            clientIdentifier.timeDuringLastPacketReceived = System.currentTimeMillis();

            if(decodedData.length > 0)
                clientIdentifier.player.update(decodedData);
        }
    },

    CONNECT
    {
        @Override
        void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server) {}
    },

    DISCONNECT
    {
        @Override
        void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server)
        {
            server.clients.remove(clientIdentifier);
        }
    },

    HOST_LIST
    {
        @Override
        void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server)
        {
            byte[][] dataTypes = new byte[server.hostedLevels.list.size() * 2][];

            for(int i = 0; i < dataTypes.length / 2; i++)
            {
                HostedLevel hostedLevel = server.hostedLevels.list.get(i);

                dataTypes[i * 2] = DataType.BYTE.encode(hostedLevel.identifier);
                dataTypes[(i * 2) + 1] = DataType.STRING.encode(hostedLevel.levelName);
            }

            server.send(ServerPacket.HOST_LIST, clientIdentifier, dataTypes);
        }
    },

    HOST
    {
        @Override
        void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server)
        {
            server.hostedLevels.add(new HostedLevel((byte[]) decodedData[0], (String) decodedData[1], clientIdentifier));
        }
    },

    JOIN_HOST
    {
        @Override
        void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server)
        {
            for(HostedLevel hostedLevel : server.hostedLevels.list)
                if(hostedLevel.identifier == (byte) decodedData[0])
                {
                    server.send(ServerPacket.JOIN_HOST, clientIdentifier, DataType.BYTE_ARRAY.encode(hostedLevel.level), DataType.STRING.encode(hostedLevel.levelName));

                    clientIdentifier.player = new Player();

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