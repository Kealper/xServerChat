package cbp.double0negative.xServer.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;
import java.lang.Character;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.ChatColor;

import cbp.double0negative.xServer.XServer;
import cbp.double0negative.xServer.packets.Packet;
import cbp.double0negative.xServer.packets.PacketTypes;
import cbp.double0negative.xServer.util.LogManager;

import me.odium.simplechatchannels.Loader;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;

public class Client extends Thread
{

	private String ip;
	private int port;
	private InputStream in;
	private OutputStream out;
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
				skt.setKeepAlive(true);
				skt.setTcpNoDelay(true);
				open = true;
				HashMap<String, String> form = new HashMap<String, String>();
				form.put("SERVERNAME", XServer.serverName);
				send(new Packet(PacketTypes.PACKET_CLIENT_CONNECTED, form));
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
					in = skt.getInputStream();
					ByteArrayOutputStream json = new ByteArrayOutputStream();
					while (true) {
						int b = in.read();
						// I probably just suck at Java but the docs say that line above should throw an IOException.
						// It doesn't so test and throw one here instead.
						if (b == -1) {
							throw new IOException();
						}
						if (b == 0) {
							break;
						}
						json.write(b);
					}
					Type typeOfPacket = new TypeToken<Packet>() { }.getType();
					Packet p = new GsonBuilder().create().fromJson(json.toString("UTF-8"), typeOfPacket);
					parse(p);
					errLevel = 0;
				} catch (Exception e)
				{
					LogManager.getInstance().error("Lost connection to host");
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
				Map<String, String> form = (Map<String, String>) p.getArgs();
				if (form.get("CANCELLED").equalsIgnoreCase("false")) {
					String m = XServer.format(p.getFormat(), form, "MESSAGE");
					sendLocalMessage(m);
					Bukkit.getServer().getConsoleSender().sendMessage(m);
				} else {
					if (form.get("CHANNEL").equalsIgnoreCase("false")) {
						if (XServer.notifyCancelledChat) {
							sendLocalMessage(ChatColor.RED + ChatColor.stripColor("[Cancelled] " + form.get("USERNAME") + ": " + form.get("MESSAGE")), "xserver.message.cancelled", true);
						}
					} else {
						sendLocalMessage(XServer.format(p.getFormat(), form, "CHANNEL"), "xserver.message.channel", true);
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
				sendLocalMessage(XServer.format(p.getFormat(), (Map<String, String>) p.getArgs(), s));
			} else if (p.getType() == PacketTypes.PACKET_PLAYER_DEATH)
			{
				sendLocalMessage(XServer.format(p.getFormat(), (Map<String, String>) p.getArgs(), "DEATH"));
			} else if (p.getType() == PacketTypes.PACKET_CLIENT_CONNECTED)
			{
				sendLocalMessage(XServer.format(p.getFormat(), (Map<String, String>) p.getArgs(), "CONNECT"));
			} else if (p.getType() == PacketTypes.PACKET_CLIENT_DC)
			{
				sendLocalMessage(XServer.format(p.getFormat(), (Map<String, String>) p.getArgs(), "DISCONNECT"));
			} else if (p.getType() == PacketTypes.PACKET_PLAYER_ACTION)
			{
				Map<String, String> form = (Map<String, String>) p.getArgs();
				sendLocalMessage(XServer.format(p.getFormat(), form, "ACTION"));
			} else if (p.getType() == PacketTypes.PACKET_PLAYER_BROADCAST)
			{
				Map<String, String> form = (Map<String, String>) p.getArgs();
				sendLocalMessage(XServer.format(p.getFormat(), form, "BROADCAST"), true);
			} else if (p.getType() == PacketTypes.PACKET_PLAYER_SOCIALSPY)
			{
				Map<String, String> form = (Map<String, String>) p.getArgs();
				sendLocalMessage(XServer.format(p.getFormat(), form, "SOCIALSPY"), "essentials.socialspy", true);
			} else if (p.getType() == PacketTypes.PACKET_PLAYER_HELPOP)
			{
				Map<String, String> form = (Map<String, String>) p.getArgs();
				sendLocalMessage(XServer.format(p.getFormat(), form, "HELPOP"), "essentials.helpop.receive", true);
			} else if (p.getType() == PacketTypes.PACKET_PLAYER_OPCHAT)
			{
				Map<String, String> form = (Map<String, String>) p.getArgs();
				sendLocalMessage(XServer.format(p.getFormat(), form, "OPCHAT"), "xserver.opchat.receive", true);
			} else if (p.getType() == PacketTypes.PACKET_SERVER_COMMAND)
			{
				final Map<String, String> form = (Map<String, String>) p.getArgs();
				boolean ignored = false;
				Iterator cmds = XServer.ignoredCommands.iterator();
				while (cmds.hasNext()) {
					if (form.get("MESSAGE").toLowerCase().startsWith((String)cmds.next())) {
						ignored = true;
						break;
					}
				}
				if (!ignored) {
					this.p.getServer().getScheduler().runTask(this.p, new Runnable()
					{
						public void run()
						{
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), form.get("MESSAGE"));
						}
					});
				}
			}

		} catch (Exception e)
		{
			LogManager.getInstance().error("Malformed Packet");
			e.printStackTrace();
		}
	}

	public boolean playerIsInChannel(Player player) {
		if (XServer.sccPluginHook != null) {
			if (((Loader) XServer.sccPluginHook).InChannel.containsKey(player)) {
				return true;
			}
		}
		if (XServer.scPluginHook != null) {
			SimpleClans sc = (SimpleClans) XServer.scPluginHook;
			ClanPlayer cp = sc.getClanManager().getClanPlayer(player);
			if (cp != null) {
				if (cp.getChannel().equals(ClanPlayer.Channel.CLAN) || cp.getChannel().equals(ClanPlayer.Channel.ALLY)) {
					return true;
				}
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
		if (s.equals("")) {
			return;
		}
		for (Player player: p.getServer().getOnlinePlayers()) {
			if (playerIsInChannel(player) && !alwaysSend) {
				continue;
			}
			if (XServer.checkPerm(player, perm)) {
				player.sendMessage(s);
			}
		}
	}

	public void send(Packet p)
	{
		try
		{
			p.setFormat(XServer.formats);
			String json = new GsonBuilder().create().toJson(p) + "\0";
			out = skt.getOutputStream();
			out.write(json.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e)
		{
			LogManager.getInstance().error("Couldn't send packet");
		}
	}

	public void closeConnection()
	{
		HashMap<String, String> form = new HashMap<String, String>();
		form.put("SERVERNAME", XServer.serverName);
		send(new Packet(PacketTypes.PACKET_CLIENT_DC, form));
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
