package tablock.network;

import java.net.InetAddress;

public record ClientIdentifier(InetAddress inetAddress, int port) {}