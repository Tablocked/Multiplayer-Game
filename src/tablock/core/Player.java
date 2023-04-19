package tablock.core;

import java.io.Serial;
import java.io.Serializable;
import java.net.InetAddress;

public class Player implements Serializable
{
	@Serial
	private static final long serialVersionUID = 5780245481599583647L;
	private InetAddress ipAddress;
	private int port;
	private int x;
	private int y;
	
	public Player(InetAddress ipAddress, int port)
	{
		this.ipAddress = ipAddress;
		this.port = port;
		this.x = 700;
		this.y = 100;
	}
	
	public boolean matches(Player player)
	{
		return ipAddress.equals(player.ipAddress) && port == player.port;
	}
	
	public InetAddress getIpAddress()
	{
		return ipAddress;
	}
	
	public void setIpAddress(InetAddress ipAddress)
	{
		this.ipAddress = ipAddress;
	}
	
	public int getPort()
	{
		return port;
	}
	
	public void setPort(int port)
	{
		this.port = port;
	}
	
	public int getX()
	{
		return x;
	}

	public void setX(int x)
	{
		this.x = x;
	}

	public int getY()
	{
		return y;
	}
	
	public void setY(int y)
	{
		this.y = y;
	}
}