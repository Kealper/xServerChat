package cbp.double0negative.xServer;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import cbp.double0negative.xServer.Server.Server;
import cbp.double0negative.xServer.client.ChatListener;
import cbp.double0negative.xServer.client.Client;
import cbp.double0negative.xServer.packets.Packet;
import cbp.double0negative.xServer.packets.PacketTypes;
import cbp.double0negative.xServer.util.LogManager;

/**
 *
 * @ *
 *
 * Authors:
 *
 * @author Drew [ https://github.com/Double0negative ]
 *
 */
public class XServer extends JavaPlugin
{

	public static String version;
	public static List<String> authors;
	public static ChatColor color = ChatColor.WHITE;
	public static ChatColor seccolor = ChatColor.WHITE;
	public static ChatColor aColor = ChatColor.AQUA;
	public static ChatColor pColor = ChatColor.GOLD;
	public static ChatColor eColor = ChatColor.DARK_RED;
	public static String pre = "[xServer] ";
	public static String xpre = pColor + pre;
	public static String ip;
	public static int port;
	public static String prefix;
	public static String serverName;
	public static boolean isHost = false;
	private Server server;
	private Client client;
	public static boolean netActive = true;
	public static int restartMode = 0;
	public static boolean dc = false;
	public static boolean hostdc = false;
	private static Player stat_req = null;
	private ChatListener cl = new ChatListener();
	public static HashMap<String, String> formats = new HashMap<String, String>();
	public static HashMap<String, String> override = new HashMap<String, String>();
	private static boolean formatoveride = false;
	public static boolean notifyCancelledChat = false;
	public static boolean ignoreCancelledSCC = false;
	private static int permsMode = 0;
	LogManager log = LogManager.getInstance();
	static Permission permission = null;
	private PluginDescriptionFile info;
	public static HashMap<String, Object> forwardedCommands;
	public static Plugin sccPluginHook;
	public static Plugin scPluginHook;
	public static List<String> ignoredCommands;

	public void onEnable()
	{

		netActive = true;
		LogManager log = LogManager.getInstance();
		log.setup(this);
		info = this.getDescription();
		version = info.getVersion();
		authors = info.getAuthors();
		log.info("xServerChat Version " + version + " Initializing");
		log.info("Created by: "+authors.toString());

		getConfig().options().copyDefaults(true);
		saveDefaultConfig();
		ip = getConfig().getString("ip");
		port = getConfig().getInt("port");
		prefix = getConfig().getString("prefix");
		isHost = getConfig().getBoolean("host");
		serverName = getConfig().getString("serverName");
		notifyCancelledChat = getConfig().getBoolean("notifyCancelledChat");
		ignoreCancelledSCC = getConfig().getBoolean("ignoreCancelledSCC");
		ignoredCommands = getConfig().getStringList("ignoredCommands");
		sccPluginHook = getServer().getPluginManager().getPlugin("SimpleChatChannels");
		scPluginHook = getServer().getPluginManager().getPlugin("SimpleClans");

		if (sccPluginHook != null) {
			log.info("SimpleChatChannels found, using SCC API.");
		}

		if (scPluginHook != null) {
			log.info("SimpleClans found, using SimpleClans API.");
		}

		formats.put("MESSAGE", getConfig().getString("formats.Message"));
		formats.put("LOGIN", getConfig().getString("formats.Login"));
		formats.put("LOGOUT", getConfig().getString("formats.Logout"));
		formats.put("DEATH", getConfig().getString("formats.Death"));
		formats.put("CONNECT", getConfig().getString("formats.Connect"));
		formats.put("DISCONNECT", getConfig().getString("formats.Disconnect"));
		formats.put("ACTION", getConfig().getString("formats.Action"));
		formats.put("BROADCAST", getConfig().getString("formats.Broadcast"));
		formats.put("SOCIALSPY", getConfig().getString("formats.SocialSpy"));
		formats.put("HELPOP", getConfig().getString("formats.HelpOp"));
		formats.put("OPCHAT", getConfig().getString("formats.OpChat"));
		formats.put("CHANNEL", getConfig().getString("formats.Channel"));

		formatoveride = getConfig().getBoolean("override.enabled");
		override.put("MESSAGE", getConfig().getString("override.Message"));
		override.put("LOGIN", getConfig().getString("override.Login"));
		override.put("LOGOUT", getConfig().getString("override.Logout"));
		override.put("DEATH", getConfig().getString("override.Death"));
		override.put("CONNECT", getConfig().getString("override.Connect"));
		override.put("DISCONNECT", getConfig().getString("override.Disconnect"));
		override.put("ACTION", getConfig().getString("override.Action"));
		override.put("BROADCAST", getConfig().getString("override.Broadcast"));
		override.put("SOCIALSPY", getConfig().getString("override.SocialSpy"));
		override.put("HELPOP", getConfig().getString("override.HelpOp"));
		override.put("OPCHAT", getConfig().getString("override.OpChat"));
		override.put("CHANNEL", getConfig().getString("override.Channel"));

		forwardedCommands = (HashMap)getConfig().getConfigurationSection("forwardedCommands").getValues(false);
		setupPermissions();

		if (isHost)
		{
			LogManager.getInstance().info("THIS SERVER IS HOST");
			startServer();
		}

		startClient();

		getServer().getPluginManager().registerEvents(cl, this);
	}

	String s = "";

	public void onDisable()
	{

		hostdc = false;
		netActive = false;
		dc = false;

		if (restartMode == PacketTypes.DC_TYPE_RELOAD)
		{
			s = "Reload";
		} else if (restartMode == PacketTypes.DC_TYPE_STOP)
		{
			s = " Shutting Down";
		}
		dc();
		if (isHost)
			dcServer();

	}

	public void startClient()
	{
		if (!dc)
		{
			client = new Client(this, ip, port);
			client.openConnection();
			cl.setClient(client);
		}
	}

	public void dc()
	{
		if (!dc)
		{
			client.stopClient();
			this.client.sendLocalMessage(aColor + pre + "Disconnecting from host", "xserver.admin", true);

		}

	}

	public void reloadClient()
	{
		dc = false;
		dc();
		startClient();
	}

	public void startServer()
	{
		if (!hostdc)
		{
			LogManager.getInstance().info("Starting as Host");
			server = new Server();
			server.start();
			netActive = true;
		}

	}

	public void dcServer()
	{
		if (!hostdc)
		{
			server.closeConnections();
		}
	}

	public void reloadServer()
	{
		hostdc = false;
		netActive = false;
		dc();
		dcServer();
		startClient();
		startServer();
	}

	public boolean onCommand(CommandSender sender, Command cmd1,
			String commandLabel, String[] args)
	{
		String cmd = cmd1.getName();
		Player player = null;
		if (sender instanceof Player)
		{
			player = (Player) sender;
		}

		if (cmd.equalsIgnoreCase("xserver") || cmd.equalsIgnoreCase("x"))
		{
			if (args.length == 0)
			{
				if (XServer.checkPerm(player, "xserver.admin"))
				{
					player.sendMessage(xpre + ChatColor.YELLOW +"Version: " + version);
					player.sendMessage(xpre + ChatColor.GREEN +"Made by: "+authors.toString());
					player.sendMessage(xpre + "/x list - List packets sent/received");
					player.sendMessage(xpre + "/x dc - Disconnect from the host");
					player.sendMessage(xpre + "/x rc - Connect to the host");
					player.sendMessage(xpre + "/x v - Display the version");
					player.sendMessage(xpre + "/x reload - Reloads the configuration");
					player.sendMessage(xpre + "/x host - List commands for host servers");
					player.sendMessage(xpre + "/x cmd - Run the given command across all servers");
					return true;
				}
				else
				{
					player.sendMessage(xpre + "Version: " + version);
					return true;
				}
			}
			if (XServer.checkPerm(player, "xserver.admin"))
			{
				if (args[0].equalsIgnoreCase("list"))
				{
					stat_req = player;
					getStats();
					return true;
				}
				if (args[0].equalsIgnoreCase("dc") || args[0].equalsIgnoreCase("disconnect"))
				{
					if (dc)
					{
						player.sendMessage(xpre + "Already Disconnected!");
						return true;
					}
					else
					{
						dc();
						dc = true;

						player.sendMessage(xpre	+ "Disconnected. You will be reconnected on next restart or with: /x rc");
						return true;
					}
				}
				if (args[0].equalsIgnoreCase("rc") || args[0].equalsIgnoreCase("reloadclient"))
				{
					reloadClient();
					player.sendMessage(xpre + "Client restarted");
					return true;
				}
				if (args[0].equalsIgnoreCase("v") || args[0].equalsIgnoreCase("version"))
				{
					player.sendMessage(xpre + ChatColor.YELLOW +"Version: " + version);
					return true;
				}

                if (args[0].equalsIgnoreCase("reload"))
				{
                    reloadConfig();
					player.sendMessage(xpre + ChatColor.YELLOW +"Configuration reloaded.");
					return true;
				}

				if (args[0].equalsIgnoreCase("host") || args[0].equalsIgnoreCase("server"))
				{
					if(args.length == 1)
					{
						player.sendMessage(xpre + "Host Server Commands");
						player.sendMessage(xpre + "/x host dc - Disconnect all servers & shutdown the host");
						player.sendMessage(xpre + "/x host rc - Start the host & make available to other servers");
						return true;
					}
					if (args[1].equalsIgnoreCase("dc") || args[1].equalsIgnoreCase("disconnect"))
					{
						if (!isHost)
						{
							player.sendMessage(xpre + "You are not host!");
							return true;
						}
						else if (hostdc)
						{
							player.sendMessage(xpre + "Already disconnected!");
							return true;
						}
						else
						{
							hostdc = true;
							dcServer();
							player.sendMessage(xpre + "Server shutdown! Restarting on next restart or with: /x host rc");
							return true;
						}
					}
					if (args[1].equalsIgnoreCase("rc") || args[1].equalsIgnoreCase("reload") || args[1].equalsIgnoreCase("reconnect"))
					{
						if (!isHost)
						{
							player.sendMessage(xpre + "You are not host!");
							return true;
						}
						else
						{
							reloadServer();
							player.sendMessage(xpre + "Server restarted!");
							return true;
						}

					}
				}

				if (args[0].equalsIgnoreCase("cmd") || args[0].equalsIgnoreCase("command")) {
					String argsString = String.join(" ", args);
					HashMap<String, String> f = new HashMap<String, String>();
					f.put("USERNAME", player.getDisplayName());
					f.put("SERVERNAME", serverName);
					f.put("MESSAGE", argsString.substring(argsString.indexOf(" ") + 1));
					client.send(new Packet(PacketTypes.PACKET_SERVER_COMMAND, f));
					this.getServer().getScheduler().runTask(this, new Runnable()
					{
						public void run()
						{
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), f.get("MESSAGE"));
						}
					});
					return true;
				}

			}
			else
			{
				player.sendMessage(xpre + ChatColor.RED + "You don't have permission to do that!");
				return true;
			}
		}

		if (cmd.equalsIgnoreCase("a") || cmd.equalsIgnoreCase("opchat")) {
			if (!checkPerm(player, "xserver.opchat.send")) {
				player.sendMessage(xpre + ChatColor.RED + "You don't have permission to do that!");
				return true;
			}
			HashMap<String, String> f = new HashMap<String, String>();
			f.put("USERNAME", player.getDisplayName());
			f.put("SERVERNAME", serverName);
			f.put("MESSAGE", String.join(" ", args));
			client.send(new Packet(PacketTypes.PACKET_PLAYER_OPCHAT, f));
			client.sendLocalMessage(format(formats, f, "OPCHAT"), "xserver.opchat.receive", true);
			return true;
		}

		return false;
	}

	public void getStats()
	{
		client.send(new Packet(PacketTypes.PACKET_STATS_REQ, null));
	}

	public static void msgStats(Object[][] stats)
	{
		stat_req.sendMessage(pColor
				+ "--------------xServer Chat Stats----------------");
		stat_req.sendMessage(pColor
				+ "Server      Active      Packets Sent            Packets Recived");
		for (Object[] o : stats)
		{
			String name = addspaces((String) o[0], 25);
			String active = addspaces((Boolean) (o[1]) ? "true" : "false", 30);
			String sent = addspaces(o[2] + "", 40);
			String rec = addspaces(o[3] + "", 7);
			stat_req.sendMessage(pColor + name + active + sent + rec);
		}

	}

	public static String addspaces(String s, int sp)
	{
		for (int a = 0; a < sp - s.length(); a++)
		{
			s = s + " ";
		}
		return s;
	}

	public static String format(HashMap<String, String> format, HashMap<String, String> val, String key) {
		return format(format, (Map<String, String>) val, key);
	}

	public static String format(HashMap<String, String> format, Map<String, String> val, String key)
	{
		String str = "";
		if (!formatoveride)
		{
			str = format.get(key);
		} else
		{
			str = override.get(key);
		}

		if (str == "") {
			return "";
		}

		if (val.get("MESSAGE") != null) {
			str = str.replaceAll("\\{message\\}", Matcher.quoteReplacement(val.get("MESSAGE")));
		}

		if (val.get("USERNAME") != null) {
			str = str.replaceAll("\\{username\\}", Matcher.quoteReplacement(val.get("USERNAME")));
		}

		if (val.get("SERVERNAME") != null) {
			str = str.replaceAll("\\{server\\}", Matcher.quoteReplacement(val.get("SERVERNAME")));
		}

		str = str.replaceAll("(&([a-fl-or0-9]))", "\u00A7$2");

		return str;
	}

	private void setupPermissions()
	{
		// Check for Vault
		Plugin vltpl = this.getServer().getPluginManager().getPlugin("Vault");

		if (!(vltpl == null))
		{
			RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
			if (permissionProvider != null)
			{
				permission = permissionProvider.getProvider();
				log.info("Hooked into Vault for Permissions!");
				permsMode = 1;
			}
			else
			{
				log.error("Couldn't hook into Vault for permissions!");
				log.error("Using Bukkit Perms (Superperms) instead");
				permsMode = 2;
			}
		}
		else
		{
			// Vault not found. Use Superperms instead
			log.info("Vault was not found :(");
			log.info("Using Bukkit Perms (Superperms)");
			permsMode = 2;
		}
	}

	   public static boolean checkPerm(Player plr, String node)
	   {
		   if(permsMode == 1)
		   {
			   // Mode is Vault
			   return permission.has(plr, node);
		   }
		   else if(permsMode == 2)
		   {
			   // Mode is SuperPerms
			   return plr.hasPermission(node);
		   }
		   else if(plr.isOp())
		   {
			   // Or Player is Op
			   return true;
		   }
		   return false;
	   }

}
