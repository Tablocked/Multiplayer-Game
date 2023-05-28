package tablock.network;

import java.util.ArrayList;
import java.util.List;

public class HostedLevel extends Identifier
{
    final byte[] level;
    final String levelName;
    final List<ClientIdentifier> joinedClients = new ArrayList<>();

    public HostedLevel(byte[] levelBytes, String levelName, ClientIdentifier clientIdentifier)
    {
        this.level = levelBytes;
        this.levelName = levelName;

        addClient(clientIdentifier);
    }

    void addClient(ClientIdentifier clientIdentifier)
    {
        joinedClients.add(clientIdentifier);

        clientIdentifier.clientsInHostedLevel = joinedClients;
    }
}