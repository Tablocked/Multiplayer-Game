package tablock.network;

import java.util.List;

public record HostedLevel(int identifier, byte[] level, String levelName, List<ClientIdentifier> joinedClients) {}