package tablock.network;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;

public abstract class Network extends Application
{
    static final int PORT = 3925;
    private static final int MAX_PACKET_LENGTH = 1024;
    final DatagramSocket datagramSocket;
    private int nextLargePacketIdentifier = 1;
    private final HashMap<Byte, byte[]> incompleteLargePackets = new HashMap<>();
    private boolean running = true;

    public Network(DatagramSocket datagramSocket)
    {
        this.datagramSocket = datagramSocket;
    }

    abstract void respondToPacket(DatagramPacket receivedPacket, byte[] data, int dataLength);

    Object[] decodePacket(byte[] data, int dataLength)
    {
        Object[] decodedData = new Object[MAX_PACKET_LENGTH];
        int index = 2;
        int decodedDataCount = 0;

        while(index < dataLength)
        {
            int dataTypeLength = (data[index] & 255) << 8 | (data[index + 1] & 255);
            byte[] dataType = new byte[dataTypeLength];

            System.out.println(dataTypeLength);

            index += 2;

            System.arraycopy(data, index, dataType, 0, dataTypeLength);

            index += dataTypeLength;

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
            byte[] data = new byte[MAX_PACKET_LENGTH];
            int index = 2;
            boolean largePacket = false;

            data[1] = (byte) packetOrdinal;

            for(byte[] dataType : dataTypes)
            {
                int spaceRemaining = MAX_PACKET_LENGTH - index;

                System.arraycopy(dataType, 0, data, index, Math.min(dataType.length, spaceRemaining));

                if(index + dataType.length > MAX_PACKET_LENGTH)
                {
                    largePacket = true;
                    data[0] = (byte) nextLargePacketIdentifier;

                    datagramSocket.send(new DatagramPacket(data, MAX_PACKET_LENGTH, inetAddress, port));

                    data = new byte[MAX_PACKET_LENGTH];

                    System.arraycopy(dataType, spaceRemaining, data, 1, dataType.length - spaceRemaining);

                    data[0] = (byte) nextLargePacketIdentifier;

                    index = (dataType.length - spaceRemaining) + 1;
                }
                else
                    index += dataType.length;
            }

            if(largePacket)
                nextLargePacketIdentifier++;

            byte[] encodedData = new byte[index];

            System.arraycopy(data, 0, encodedData, 0, index);

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
                    DatagramPacket receivedPacket = new DatagramPacket(new byte[MAX_PACKET_LENGTH], MAX_PACKET_LENGTH);

                    datagramSocket.receive(receivedPacket);

                    byte largePacketIdentifier = receivedPacket.getData()[0];

                    if(largePacketIdentifier != 0)
                    {
                        System.out.println("LARGE PACKET! -> " + largePacketIdentifier);

                        incompleteLargePackets.merge(largePacketIdentifier, receivedPacket.getData(), (incompleteData, additionalData) ->
                        {
                            int additionalDataLength = (receivedPacket.getLength() - 1);
                            byte[] mergedData = new byte[incompleteData.length + additionalDataLength];

                            System.out.println("LENGTH: " + receivedPacket.getLength());

                            System.arraycopy(incompleteData, 0, mergedData, 0, incompleteData.length);
                            System.arraycopy(additionalData, 1, mergedData, incompleteData.length, additionalDataLength);

                            return mergedData;
                        });

                        if(receivedPacket.getLength() < MAX_PACKET_LENGTH)
                        {
                            byte[] data = incompleteLargePackets.get(largePacketIdentifier);

                            incompleteLargePackets.remove(largePacketIdentifier);

                            System.out.println("COMPLETED LARGE PACKET! -> " + Arrays.toString(data) + "\n length: " + data.length);

                            respondToPacket(receivedPacket, data, data.length);
                        }
                    }
                    else
                        respondToPacket(receivedPacket, receivedPacket.getData(), receivedPacket.getLength());
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