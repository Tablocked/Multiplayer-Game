package tablock.network;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public abstract class Network extends Application
{
    static final int PORT = 3925;
    final DatagramSocket datagramSocket;
    private boolean running = true;

    public Network(DatagramSocket datagramSocket)
    {
        this.datagramSocket = datagramSocket;
    }

    abstract void respondToPacket(DatagramPacket receivedPacket);

    Object[] decodePacket(DatagramPacket receivedPacket)
    {
        byte[] data = receivedPacket.getData();
        Object[] decodedData = new Object[350];
        int index = 1;
        int decodedDataCount = 0;

        while(index < receivedPacket.getLength())
        {
            byte dataTypeLength = data[index];
            byte[] dataType = new byte[dataTypeLength];

            System.arraycopy(data, index + 1, dataType, 0, dataTypeLength);

            index += dataTypeLength + 1;

            decodedData[decodedDataCount] = DataType.values()[data[index]].decode(dataType);

            index++;
            decodedDataCount++;
        }

        Object[] decodedPacket = new Object[decodedDataCount];

        System.arraycopy(decodedData, 0, decodedPacket, 0, decodedDataCount);

        return decodedPacket;
    }

    void send(int packetOrdinal, InetAddress inetAddress, int port, byte[][] dataTypes)
    {
        try
        {
            byte[] bytes = new byte[1024];
            int index = 1;

            bytes[0] = (byte) packetOrdinal;

            for(byte[] dataType : dataTypes)
            {
                System.arraycopy(dataType, 0, bytes, index, dataType.length);

                index += dataType.length;
            }

            byte[] encodedData = new byte[index];

            System.arraycopy(bytes, 0, encodedData, 0, index);

            datagramSocket.send(new DatagramPacket(encodedData, encodedData.length, inetAddress, port));
        }
        catch(IOException exception)
        {
            exception.printStackTrace();
        }
    }

    @Override
    public void start(Stage stage)
    {
        stage.setOnCloseRequest((windowEvent) ->
        {
            datagramSocket.close();

            running = false;
        });

        Thread thread = new Thread(() ->
        {
            while(running)
            {
                try
                {
                    DatagramPacket receivedPacket = new DatagramPacket(new byte[1024], 1024);

                    datagramSocket.receive(receivedPacket);

                    respondToPacket(receivedPacket);
                }
                catch(IOException exception)
                {
                    exception.printStackTrace();
                }
            }
        });

        thread.start();
    }
}