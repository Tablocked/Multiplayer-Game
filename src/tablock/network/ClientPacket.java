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

    NAME_CHANGE
    {
        @Override
        void respondToClientPacket(Object[] decodedData, ClientIdentifier clientIdentifier, Server server)
        {
            clientIdentifier.name = (String) decodedData[0];
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
                    byte[][] playerNames = new byte[hostedLevel.joinedClients.size() * 2][];
                    byte[] encodedIdentifier = DataType.BYTE.encode(clientIdentifier.identifier);
                    byte[][] encodedPlayer = new byte[8][];

                    encodedPlayer[0] = encodedIdentifier;

                    clientIdentifier.player = new Player();

                    System.arraycopy(clientIdentifier.player.encode(), 0, encodedPlayer, 1, 7);

                    for(ClientIdentifier joinedClient : hostedLevel.joinedClients)
                    {
                        server.send(ServerPacket.TICK, joinedClient, encodedPlayer);
                        server.send(ServerPacket.PLAYER_NAMES, joinedClient, encodedIdentifier, DataType.STRING.encode(clientIdentifier.name));
                    }

                    for(int i = 0; i < playerNames.length; i += 2)
                    {
                        ClientIdentifier joinedClient = hostedLevel.joinedClients.get(i / 2);

                        playerNames[i] = DataType.BYTE.encode(joinedClient.identifier);
                        playerNames[i + 1] = DataType.STRING.encode(joinedClient.name);
                    }

                    server.send(ServerPacket.JOIN_HOST, clientIdentifier, DataType.BYTE_ARRAY.encode(hostedLevel.level));

                    hostedLevel.addClient(clientIdentifier);

                    server.send(ServerPacket.TICK, clientIdentifier, server.encodeClientsInHostedLevel(clientIdentifier));
                    server.send(ServerPacket.PLAYER_NAMES, clientIdentifier, playerNames);

                    break;
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