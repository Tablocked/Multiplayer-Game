package tablock.network;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class Server extends Network
{
    final IdentifierList<ClientIdentifier> clients = new IdentifierList<>();
    final IdentifierList<HostedLevel> hostedLevels = new IdentifierList<>();
    private final LoopCounter tickCounter = new LoopCounter();

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
        ClientPacket.values()[data[1]].respondToClientPacket(decodePacket(data, dataLength), clientIdentifier, this);
    }

    @Override
    void respondToPacket(DatagramPacket receivedPacket, byte[] data, int dataLength)
    {
        for(ClientIdentifier clientIdentifier : clients.list)
            if(clientIdentifier.inetAddress.equals(receivedPacket.getAddress()) && clientIdentifier.port == receivedPacket.getPort())
            {
                respondToPacket(clientIdentifier, data, dataLength);

                return;
            }

        clients.add(new ClientIdentifier(receivedPacket.getAddress(), receivedPacket.getPort()));
    }

    @Override
    public void start(Stage stage)
    {
        super.start(stage);

        Canvas canvas = new Canvas(960, 540);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFont(Font.font("Arial", 20));

        Timeline tickLoop = new Timeline(new KeyFrame(Duration.millis(16.67), (actionEvent) ->
        {
            List<ClientIdentifier> copyOfClients = new ArrayList<>(clients.list);

            for(ClientIdentifier clientIdentifier : copyOfClients)
                if(System.currentTimeMillis() - clientIdentifier.timeDuringLastPacketReceived > 10000)
                {
                    if(clientIdentifier.clientsInHostedLevel != null)
                        clientIdentifier.clientsInHostedLevel.remove(clientIdentifier);

                    clients.remove(clientIdentifier);
                }
                else
                {
                    byte[][] dataTypes = new byte[0][];

                    if(clientIdentifier.clientsInHostedLevel != null)
                    {
                        ArrayList<ClientIdentifier> clientsInHostedLevel = new ArrayList<>(clientIdentifier.clientsInHostedLevel);

                        clientsInHostedLevel.remove(clientIdentifier);

                        dataTypes = new byte[clientsInHostedLevel.size() * 7][];

                        for(int i = 0; i < clientsInHostedLevel.size(); i++)
                        {
                            ClientIdentifier clientInHostedLevel = clientsInHostedLevel.get(i);
                            int index = i * 7;

                            dataTypes[index] = DataType.BYTE.encode(clientInHostedLevel.identifier);

                            System.arraycopy(clientInHostedLevel.player.encode(), 0, dataTypes, index + 1, 6);
                        }
                    }

                    send(ServerPacket.TICK, clientIdentifier, dataTypes);
                }

            List<HostedLevel> copyOfHostedLevels = new ArrayList<>(hostedLevels.list);

            for(HostedLevel hostedLevel : copyOfHostedLevels)
                if(hostedLevel.joinedClients.size() == 0)
                    hostedLevels.remove(hostedLevel);

            tickCounter.increment();
        }));

        tickLoop.setCycleCount(Timeline.INDEFINITE);
        tickLoop.play();

        AnimationTimer renderLoop = new AnimationTimer()
        {
            @Override
            public void handle(long l)
            {
                int y = 20;

                gc.clearRect(0, 0, 960, 540);
                gc.fillText(tickCounter.computeLoopsPerSecond() + " TPS", 10, y);
                gc.fillText(getBytesSent() / 1024 + " KB Sent", 10, y += 30);
                gc.fillText(getBytesReceived() / 1024 + " KB Received", 10, y += 30);
                gc.fillText("Clients (" + clients.list.size() + ")", 10, y += 60);
                gc.fillText("Hosted Levels (" + hostedLevels.list.size() + ")", 10, y += 30);

                for(int i = 0; i < hostedLevels.list.size(); i++)
                {
                    HostedLevel hostedLevel = hostedLevels.list.get(i);

                    gc.fillText((i + 1) + ") Name: " + hostedLevel.levelName + " | Size: " + hostedLevel.level.length + " bytes | Joined Clients: " + hostedLevel.joinedClients.size() + " | Host Identifier: " + hostedLevel.identifier, 10, y += 30);
                }
            }
        };

        renderLoop.start();

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