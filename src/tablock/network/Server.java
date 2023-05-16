package tablock.network;

import javafx.animation.AnimationTimer;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Server extends Network
{
    int nextHostIdentifier = 0;
    final List<ClientIdentifier> clients = new ArrayList<>();
    final List<Integer> hostIdentifiers = new ArrayList<>();
    final List<byte[]> hostedLevels = new ArrayList<>();
    final List<String> hostedLevelNames = new ArrayList<>();

    public static void main(String[] args)
    {
        launch(args);
    }

    public Server() throws SocketException
    {
        super(new DatagramSocket(PORT));
    }

    private void respondToPacket(ClientIdentifier clientIdentifier, byte[] data, int dataLength)
    {
        System.out.println("RECEIVED: " + Arrays.toString(data));

        ClientPacket.values()[data[1]].respondToClientPacket(decodePacket(data, dataLength), clientIdentifier, this);
    }

    @Override
    void respondToPacket(DatagramPacket receivedPacket, byte[] data, int dataLength)
    {
        for(ClientIdentifier clientIdentifier : clients)
            if(clientIdentifier.inetAddress.equals(receivedPacket.getAddress()) && clientIdentifier.port == receivedPacket.getPort())
            {
                respondToPacket(clientIdentifier, data, dataLength);

                return;
            }

        ClientIdentifier newClient = new ClientIdentifier(receivedPacket);

        clients.add(newClient);

        respondToPacket(newClient, data, dataLength);
    }

    @Override
    public void start(Stage stage)
    {
        super.start(stage);

        Canvas canvas = new Canvas(960, 540);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFont(Font.font("Arial", 20));

        AnimationTimer animationTimer = new AnimationTimer()
        {
            @Override
            public void handle(long l)
            {
                int yPosition = 50;

                gc.clearRect(0, 0, 960, 540);
                gc.fillText("Clients (" + clients.size() + ")", 10, 20);

                yPosition += 30;

                gc.fillText("Hosted Levels (" + hostIdentifiers.size() + ")", 10, yPosition);

                for(int i = 0; i < hostIdentifiers.size(); i++)
                {
                    yPosition += 30;

                    gc.fillText((i + 1) + ") Name: " + hostedLevelNames.get(i) + " | Size: " + hostedLevels.get(i).length + " bytes | Host Identifier: " + hostIdentifiers.get(i), 10, yPosition);
                }
            }
        };

        animationTimer.start();

        stage.setScene(new Scene(new Group(canvas)));
        stage.setTitle("Server");
        stage.setWidth(960);
        stage.setHeight(540);
        stage.show();
    }

    void send(ServerPacket serverPacket, ClientIdentifier clientIdentifier, byte[]... dataTypes)
    {
        send(serverPacket.ordinal(), clientIdentifier.inetAddress, clientIdentifier.port, dataTypes);
    }
}