package tablock.network;

import tablock.core.Main;
import tablock.network.packet.Packet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public abstract class Network extends Thread
{
	protected DatagramSocket socket;
	protected int port;
	private boolean isRunning;
	
	public Network(boolean isListeningToPort)
	{
		try
		{
			this.port = 9999;
			this.socket = isListeningToPort ? new DatagramSocket(this.port) : new DatagramSocket();
			this.isRunning = true;
		}
		catch(SocketException exception)
		{
			exception.printStackTrace();
		}
	}
	
	protected abstract void respondToPacket(byte[] packet, DatagramPacket receivedPacket);
	
	@Override
	public void run()
	{
		while(this.isRunning)
		{
			byte[] packet = new byte[1024];
			DatagramPacket receivedPacket = new DatagramPacket(packet, packet.length);

			try
			{
				socket.receive(receivedPacket);

				respondToPacket(packet, receivedPacket);
			}
			catch(IOException ignored) {}
		}
	}
	
	protected Packet deserializePacket(byte[] packet)
	{
		return (Packet) Main.deserializeObject(packet);
	}
	
	public void sendPacket(Packet packet, InetAddress ipAddress, int port)
	{
		try
		{
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			
			objectOutputStream.writeObject(packet);
			
			byte[] byteArray = byteArrayOutputStream.toByteArray();
			
			DatagramPacket datagramPacket = new DatagramPacket(byteArray, byteArray.length, ipAddress, port);
			
			socket.send(datagramPacket);
		}
		catch(IOException exception)
		{
			exception.printStackTrace();
		}
	}
	
	public void sendPacket(Packet packet, DatagramPacket receivedPacket)
	{
		sendPacket(packet, receivedPacket.getAddress(), receivedPacket.getPort());
	}
	
	public void closeSocket()
	{
		isRunning = false;

		socket.close();
	}
}