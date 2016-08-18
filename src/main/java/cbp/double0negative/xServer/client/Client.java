package cbp.double0negative.xServer.client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import cbp.double0negative.xServer.XServer;
import cbp.double0negative.xServer.packets.Packet;
import cbp.double0negative.xServer.packets.PacketTypes;
import cbp.double0negative.xServer.util.LogManager;

import me.odium.simplechatchannels.Loader;

public class Client extends Thread
{

	private String ip;
	private int port;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private Socket skt;
	private boolean open = false;
	private boolean closed = false;
	private int errLevel = 0;
	private long sleep = 2000;
	private Plugin p;

	public Client(Plugin p, String ip, int port)
	{
		this.ip = ip;
		this.port = port;
		this.p = p;
	}

	public void openConnection()
	{
		this.start();
	}

	public void run()
	{
		boolean error = false;
		while (!closed)
		{
			try
			{
				skt = new Socket(ip, port);
				open = true;
				send(new Packet(PacketTypes.PACKET_CLIENT_CONNECTED, XServer.serverName));
				sendLocalMessage(XServer.aColor + "[xServer] Connected to host", "xserver.admin", true);
				LogManager.getInstance().info("Client connected to " + ip + ":" + port);

			} catch (Exception e)
			{
				if (!error)
				{
					LogManager.getInstance().error("Failed to create Socket - Client");
				}
				error = true;
			}
			sleep = 2000;

			while (open && !XServer.dc)
			{
				try
				{
					in = new ObjectInputStream(skt.getInputStream());
					Packet p = (Packet) in.readObject();
					parse(p);
					errLevel = 0;
				} catch (Exception e)
				{
					LogManager.getInstance().error("Could not read packet");
					if (open)
					{
						sendLocalMessage(XServer.eColor + "[xServer] Lost connection to host", "xserver.admin", true);
					}
					open = false;
				}
			}
			try
			{
				sleep(sleep);
				sleep = 10000;
			} catch (Exception e)
			{
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void parse(Packet p)
	{
		try
		{
			if (p.getType() == PacketTypes.PACKET_MESSAGE)
			{
				HashMap<String, String> form = (HashMap<String, String>) p.getArgs();
				if (form.get("CANCELLED") == "false") {
					sendLocalMessage(XServer.format(p.getFormat(), form, "MESSAGE"));
				} else {
					if (XServer.notifyCancelledChat) {
						sendLocalMessage(ChatColor.RED + ChatColor.stripColor("[Cancelled] " + form.get("USERNAME") + ": " + form.get("MESSAGE")), "xserver.message.cancelled", true);
					}
				}
			} else if (p.getType() == PacketTypes.PACKET_STATS_REPLY)
			{
				XServer.msgStats((Object[][]) p.getArgs());
			} else if (p.getType() == PacketTypes.PACKET_CC)
			{
				closeConnection();
			} else if (p.getType() == PacketTypes.PACKET_SERVER_DC)
			{
				open = false;
			} else if (p.getType() == PacketTypes.PACKET_PLAYER_JOIN || p.getType() == PacketTypes.PACKET_PLAYER_LEAVE)
			{
				String s = (p.getType() == PacketTypes.PACKET_PLAYER_JOIN) ? "LOGIN" : "LOGOUT";
				sendLocalMessage(XServer.format(p.getFormat(), (HashMap<String, String>) p.getArgs(), s));
			} else if (p.getType() == PacketTypes.PACKET_PLAYER_DEATH)
			{
				sendLocalMessage(XServer.format(p.getFormat(), (HashMap<String, String>) p.getArgs(), "DEATH"));
			} else if (p.getType() == PacketTypes.PACKET_CLIENT_CONNECTED)
			{
				HashMap<String, String> form = new HashMap<String, String>();
				form.put("SERVERNAME", (String) p.getArgs());
				sendLocalMessage(XServer.format(p.getFormat(), form, "CONNECT"));
			} else if (p.getType() == PacketTypes.PACKET_CLIENT_DC)
			{
				HashMap<String, String> form = new HashMap<String, String>();
				form.put("SERVERNAME", (String) p.getArgs());
				sendLocalMessage(XServer.format(p.getFormat(), form, "DISCONNECT"));
			} else if (p.getType() == PacketTypes.PACKET_PLAYER_ACTION)
			{
				HashMap<String, String> form = (HashMap<String, String>) p.getArgs();
				sendLocalMessage(XServer.format(p.getFormat(), form, "ACTION"));
			} else if (p.getType() == PacketTypes.PACKET_PLAYER_BROADCAST)
			{
				HashMap<String, String> form = (HashMap<String, String>) p.getArgs();
				sendLocalMessage(XServer.format(p.getFormat(), form, "BROADCAST"), true);
			} else if (p.getType() == PacketTypes.PACKET_PLAYER_SOCIALSPY)
			{
				HashMap<String, String> form = (HashMap<String, String>) p.getArgs();
				sendLocalMessage(XServer.format(p.getFormat(), form, "SOCIALSPY"), "essentials.socialspy", true);
			} else if (p.getType() == PacketTypes.PACKET_PLAYER_HELPOP)
			{
				HashMap<String, String> form = (HashMap<String, String>) p.getArgs();
				sendLocalMessage(XServer.format(p.getFormat(), form, "HELPOP"), "essentials.helpop.receive", true);
			} else if (p.getType() == PacketTypes.PACKET_SERVER_COMMAND)
			{
				final HashMap<String, String> form = (HashMap<String, String>) p.getArgs();
				this.p.getServer().getScheduler().runTask(this.p, new Runnable()
				{
					public void run()
					{
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), form.get("MESSAGE"));
					}
				});
			}

		} catch (Exception e)
		{
			LogManager.getInstance().error("Malformed Packet");
			e.printStackTrace();
		}
	}

	public boolean playerIsInSCCChannel(Player player) {
		if (XServer.sccPluginHook != null) {
			if (((Loader)XServer.sccPluginHook).InChannel.containsKey(player)) {
				return true;
			}
		}
		return false;
	}

	public void sendLocalMessage(String s)
	{
		sendLocalMessage(s, "xserver.message.receive", false);
	}

	public void sendLocalMessage(String s, boolean alwaysSend)
	{
		sendLocalMessage(s, "xserver.message.receive", alwaysSend);
	}

	public void sendLocalMessage(String s, String perm)
	{
		sendLocalMessage(s, perm, false);
	}

	public void sendLocalMessage(String s, String perm, boolean alwaysSend)
	{
		for (Player player: p.getServer().getOnlinePlayers()) {
			if (playerIsInSCCChannel(player) && !alwaysSend) {
				continue;
			}
			if (XServer.checkPerm(player, perm)) {
				player.sendMessage(s);
			}
		}
	}

	public void sendMessage(String s, String user)
	{
		HashMap<String, String> f = new HashMap<String, String>();
		f.put("MESSAGE", s);
		f.put("SERVERNAME", XServer.serverName);
		f.put("USERNAME", user);

		send(new Packet(PacketTypes.PACKET_MESSAGE, f));
	}

	public void send(Packet p)
	{
		try
		{
			p.setFormat(XServer.formats);
			out = new ObjectOutputStream(skt.getOutputStream());
			out.writeObject(p);
		} catch (Exception e)
		{
			LogManager.getInstance().error("Couldn't send packet");
		}
	}

	public void closeConnection()
	{

		send(new Packet(PacketTypes.PACKET_CLIENT_DC, XServer.serverName));
		try
		{
			in.close();
			out.close();
		} catch (Exception e)
		{
		}
		open = false;

	}

	public void stopClient()
	{
		closeConnection();
		closed = true;
	}

}
