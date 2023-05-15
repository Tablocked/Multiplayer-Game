package tablock.network;

import javafx.stage.Stage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Server extends Network
{
    int clientCount = 0;

    public static void main(String[] args)
    {
        launch(args);
    }

    public Server() throws SocketException
    {
        super(new DatagramSocket(PORT));
    }

    @Override
    void respondToPacket(DatagramPacket receivedPacket)
    {
        ClientPacket.values()[receivedPacket.getData()[0]].respondToClientPacket(decodePacket(receivedPacket), receivedPacket, this);
    }

    @Override
    public void start(Stage stage)
    {
        super.start(stage);

        stage.setTitle("Server");
        stage.setWidth(960);
        stage.setHeight(540);
        stage.show();
    }

    void send(ServerPacket serverPacket, DatagramPacket receivedPacket, byte[]... dataTypes)
    {
        send(serverPacket.ordinal(), receivedPacket.getAddress(), receivedPacket.getPort(), dataTypes);
    }
}