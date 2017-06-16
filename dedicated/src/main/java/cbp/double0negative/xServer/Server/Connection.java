package cbp.double0negative.xServer.Server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

import cbp.double0negative.xServer.packets.Packet;
import cbp.double0negative.xServer.packets.PacketTypes;
import cbp.double0negative.xServer.util.LogManager;

public class Connection extends Thread
{

	ObjectInputStream in;
	ObjectOutputStream out;
	Socket skt;
	private boolean open = true;
	private String name = "";
	private int sent = 0;
	private int received = 0;

	public Connection(Socket skt2)
	{

		this.skt = skt2;
		open = true;
	}

	public void run()
	{
		while (open)
		{
			try
			{
				in = new ObjectInputStream(skt.getInputStream());
				parse((Packet) in.readObject());
				sent++;
			} catch (Exception e)
			{
				closeConnection();
			}
		}
		LogManager.info(name + " has disconnected.");
		interrupt();
	}

	public void send(Packet p)
	{
		try
		{
			out = new ObjectOutputStream(skt.getOutputStream());
			out.writeObject(p);
			received++;
		} catch (Exception e)
		{
		}
	}

	public void parse(Packet p)
	{

		if (p.getType() == PacketTypes.PACKET_CLIENT_CONNECTED)
		{
			name = (String) p.getArgs();
			LogManager.info(name + " has connected.");
			Server.checkIfDupe(p, this);
		}
		else if (p.getType() == PacketTypes.PACKET_STATS_REQ)
		{
			LogManager.info(name + " requested stats");
			Server.genAndSendStats(this);
		}
		else if (p.getType() == PacketTypes.PACKET_CLIENT_DC)
		{
			Server.closeConnection(this);
		}
		else
		{
			Server.sendPacket(p, this);

			if (p.getType() == PacketTypes.PACKET_MESSAGE) {
				HashMap <String, String> args = (HashMap <String, String>) p.getArgs();
				if (args.get("CANCELLED").equalsIgnoreCase("false")) {
					LogManager.println("[" + name + "] " + args.get("USERNAME") + ": " + args.get("MESSAGE"));
				} else {
					LogManager.println("[" + name + "] \u00A7c" + LogManager.stripFormat(args.get("USERNAME") + ": " + args.get("MESSAGE")) + "\u00A7r");
				}
			}
			else if (p.getType() == PacketTypes.PACKET_PLAYER_ACTION) {
				HashMap <String, String> args = (HashMap <String, String>) p.getArgs();
				LogManager.println("[" + name + "] " + args.get("USERNAME") + ": " + args.get("MESSAGE"));
			}
			else if (p.getType() == PacketTypes.PACKET_PLAYER_BROADCAST) {
				HashMap <String, String> args = (HashMap <String, String>) p.getArgs();
				LogManager.println("[BROADCAST] " + args.get("MESSAGE"));
			}
			else if (p.getType() == PacketTypes.PACKET_PLAYER_HELPOP) {
				HashMap <String, String> args = (HashMap <String, String>) p.getArgs();
				LogManager.println("[HELPOP] " + args.get("USERNAME") + ": " + args.get("MESSAGE"));
			}
			else if (p.getType() == PacketTypes.PACKET_PLAYER_JOIN) {
				HashMap <String, String> args = (HashMap <String, String>) p.getArgs();
				LogManager.info(args.get("USERNAME") + " has joined server " + name);
			}
			else if (p.getType() == PacketTypes.PACKET_PLAYER_LEAVE) {
				HashMap <String, String> args = (HashMap <String, String>) p.getArgs();
				LogManager.info(args.get("USERNAME") + " has left server " + name);
			}
			else if (p.getType() == PacketTypes.PACKET_PLAYER_DEATH) {
				HashMap <String, String> args = (HashMap <String, String>) p.getArgs();
				LogManager.info(args.get("MESSAGE") + " on server " + name);
			}
			else if (p.getType() == PacketTypes.PACKET_SERVER_COMMAND) {
				HashMap <String, String> args = (HashMap <String, String>) p.getArgs();
				LogManager.info(args.get("USERNAME") + " issued global command: /" + args.get("MESSAGE"));
			}
		}

	}

	public void closeConnection()
	{
		open = false;
		try
		{
			send(new Packet(PacketTypes.PACKET_CC, null));
			out.close();
			in.close();
			skt.close();
			sent = 0;
			received = 0;
		}
		catch (Exception e)
		{
		}
	}

	public boolean isOpen()
	{
		return open;
	}

	public String getClientName()
	{
		return name;
	}

	public int getSent()
	{
		return sent;
	}

	public int getRecived()
	{
		return received;
	}
}
