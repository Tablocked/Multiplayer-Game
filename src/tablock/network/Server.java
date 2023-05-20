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
    int nextHostIdentifier = 0;
    final List<ClientIdentifier> clients = new ArrayList<>();
    final List<HostedLevel> hostedLevels = new ArrayList<>();
    private long previousTime = System.nanoTime();
    private int ticksPerSecond;
    private int tickCount = 0;

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
        for(ClientIdentifier clientIdentifier : clients)
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
            List<ClientIdentifier> copyOfClients = new ArrayList<>(clients);

            hostedLevels.removeIf(hostedLevel -> hostedLevel.joinedClients.size() == 0);

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
                        ArrayList<ClientIdentifier> copyOfClientsInHostedLevel = new ArrayList<>(clientIdentifier.clientsInHostedLevel);

                        copyOfClientsInHostedLevel.remove(clientIdentifier);

                        dataTypes = new byte[copyOfClientsInHostedLevel.size() * 3][];

                        for(int i = 0; i < copyOfClientsInHostedLevel.size(); i++)
                        {
                            Player player = copyOfClientsInHostedLevel.get(i).player;
                            int index = i * 3;

                            dataTypes[index] = DataType.DOUBLE.encode(player.x);
                            dataTypes[index + 1] = DataType.DOUBLE.encode(player.y);
                            dataTypes[index + 2] = DataType.DOUBLE.encode(player.rotationAngle);
                        }
                    }

                    send(ServerPacket.TICK, clientIdentifier, dataTypes);
                }

            tickCount++;
        }));

        tickLoop.setCycleCount(Timeline.INDEFINITE);
        tickLoop.play();

        AnimationTimer renderLoop = new AnimationTimer()
        {
            @Override
            public void handle(long l)
            {
                int yPosition = 20;

                if(System.nanoTime() - previousTime > 1e9)
                {
                    previousTime = System.nanoTime();
                    ticksPerSecond = tickCount;
                    tickCount = 0;
                }

                gc.clearRect(0, 0, 960, 540);
                gc.fillText(ticksPerSecond + " TPS", 10, yPosition);
                gc.fillText(getBytesSent() / 1024 + " KB Sent", 10, yPosition += 30);
                gc.fillText(getBytesReceived() / 1024 + " KB Received", 10, yPosition += 30);
                gc.fillText("Clients (" + clients.size() + ")", 10, yPosition += 60);
                gc.fillText("Hosted Levels (" + hostedLevels.size() + ")", 10, yPosition += 30);

                for(int i = 0; i < hostedLevels.size(); i++)
                {
                    HostedLevel hostedLevel = hostedLevels.get(i);

                    gc.fillText((i + 1) + ") Name: " + hostedLevel.levelName + " | Size: " + hostedLevel.level.length + " bytes | Joined Clients: " + hostedLevel.joinedClients.size() + " | Host Identifier: " + hostedLevel.identifier, 10, yPosition += 30);
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