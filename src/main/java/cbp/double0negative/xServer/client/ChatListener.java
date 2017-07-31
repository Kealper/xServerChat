package cbp.double0negative.xServer.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.ChatColor;

import cbp.double0negative.xServer.XServer;
import cbp.double0negative.xServer.packets.Packet;
import cbp.double0negative.xServer.packets.PacketTypes;

public class ChatListener implements Listener
{

	Client c;

	public void setClient(Client c)
	{
		this.c = c;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void handleChat(AsyncPlayerChatEvent event)
	{
		if (XServer.checkPerm(event.getPlayer(), "xserver.message.send"))
		{
			String msg = event.getMessage();
			if (XServer.checkPerm(event.getPlayer(), "essentials.chat.color")) {
				msg.replaceAll("(&([0-9a-fr]))", "\u00A7$2");
			}
			if (XServer.checkPerm(event.getPlayer(), "essentials.chat.format")) {
				msg.replaceAll("(&([l-or]))", "\u00A7$2");
			}
			HashMap<String, String> f = new HashMap<String, String>();
			f.put("USERNAME", event.getPlayer().getDisplayName());
			f.put("SERVERNAME", XServer.serverName);
			f.put("MESSAGE", msg);
			f.put("CANCELLED", "false");
			f.put("CHANNEL", "false");
			if (event.isCancelled()) {
				f.put("CANCELLED", "true");
				f.put("USERNAME", event.getPlayer().getName());
				if (c.playerIsInChannel(event.getPlayer()) && XServer.ignoreCancelledSCC) {
					f.put("CHANNEL", "true");
				} else {
					if (XServer.notifyCancelledChat) {
						c.sendLocalMessage(ChatColor.RED + ChatColor.stripColor("[Cancelled] " + f.get("USERNAME") + ": " + f.get("MESSAGE")), "xserver.message.cancelled", true);
					}
				}
			}
			c.send(new Packet(PacketTypes.PACKET_MESSAGE, f));
		}

	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void handleCommand(PlayerCommandPreprocessEvent event)
	{
		if (event.getMessage().toLowerCase().startsWith("/. ")) {
			HashMap<String, String> f = new HashMap<String, String>();
			f.put("USERNAME", event.getPlayer().getName());
			f.put("SERVERNAME", XServer.serverName);
			f.put("MESSAGE", event.getMessage().substring(event.getMessage().indexOf(" ") + 1));
			f.put("CANCELLED", "true");
			f.put("CHANNEL", "true");
			c.send(new Packet(PacketTypes.PACKET_MESSAGE, f));
		}

		if ((event.getMessage().toLowerCase().startsWith("/me ")) || (event.getMessage().toLowerCase().startsWith("/action ")))
		{
			if (!XServer.checkPerm(event.getPlayer(), "essentials.me")) {
				return;
			}
			if (!XServer.checkPerm(event.getPlayer(), "xserver.message.send")) {
				return;
			}
			HashMap<String, String> f = new HashMap<String, String>();
			f.put("USERNAME", event.getPlayer().getDisplayName());
			f.put("SERVERNAME", XServer.serverName);
			f.put("MESSAGE", event.getMessage().substring(event.getMessage().indexOf(" ") + 1));
			c.send(new Packet(PacketTypes.PACKET_PLAYER_ACTION, f));
		}

		if ((event.getMessage().toLowerCase().startsWith("/broadcast ")) || (event.getMessage().toLowerCase().startsWith("/bc ")))
		{
			if (!XServer.checkPerm(event.getPlayer(), "essentials.broadcast")) {
				return;
			}
			if (!XServer.checkPerm(event.getPlayer(), "xserver.message.send")) {
				return;
			}
			HashMap<String, String> f = new HashMap<String, String>();
			f.put("USERNAME", event.getPlayer().getDisplayName());
			f.put("SERVERNAME", XServer.serverName);
			f.put("MESSAGE", event.getMessage().substring(event.getMessage().indexOf(" ") + 1));
			c.send(new Packet(PacketTypes.PACKET_PLAYER_BROADCAST, f));
		}

		if (event.getMessage().toLowerCase().matches("^/(.+?:)?(e)?(w|whisper|t|tell|msg|m|r|reply|mail) (.*)$"))
		{
			HashMap<String, String> f = new HashMap<String, String>();
			f.put("USERNAME", event.getPlayer().getName());
			f.put("SERVERNAME", XServer.serverName);
			f.put("MESSAGE", event.getMessage());
			c.send(new Packet(PacketTypes.PACKET_PLAYER_SOCIALSPY, f));
			c.sendLocalMessage(XServer.format(XServer.formats, f, "SOCIALSPY"), "essentials.socialspy", true);
		}

		if ((event.getMessage().toLowerCase().startsWith("/ac ")) || (event.getMessage().toLowerCase().startsWith("/helpop ")))
		{
			if (!XServer.checkPerm(event.getPlayer(), "essentials.helpop")) {
				return;
			}
			HashMap<String, String> f = new HashMap<String, String>();
			f.put("USERNAME", event.getPlayer().getDisplayName());
			f.put("SERVERNAME", XServer.serverName);
			f.put("MESSAGE", event.getMessage().substring(event.getMessage().indexOf(" ") + 1));
			c.send(new Packet(PacketTypes.PACKET_PLAYER_HELPOP, f));
		}

		if (event.getMessage().toLowerCase().matches("^(/v|/vanish) (fj|fakejoin)$"))
		{
			if (!XServer.checkPerm(event.getPlayer(), "vanish.fakeannounce")) {
				return;
			}
			HashMap<String, String> f = new HashMap<String, String>();
			f.put("USERNAME", event.getPlayer().getName());
			f.put("SERVERNAME", XServer.serverName);
			f.put("MESSAGE", "");
			c.send(new Packet(PacketTypes.PACKET_PLAYER_JOIN, f));
		}

		if (event.getMessage().toLowerCase().matches("^(/v|/vanish) (fq|fakequit)$"))
		{
			if (!XServer.checkPerm(event.getPlayer(), "vanish.fakeannounce")) {
				return;
			}
			HashMap<String, String> f = new HashMap<String, String>();
			f.put("USERNAME", event.getPlayer().getName());
			f.put("SERVERNAME", XServer.serverName);
			f.put("MESSAGE", "");
			c.send(new Packet(PacketTypes.PACKET_PLAYER_LEAVE, f));
		}

		Iterator cmds = XServer.forwardedCommands.entrySet().iterator();
		while (cmds.hasNext())
		{
			Map.Entry cmd = (Map.Entry) cmds.next();
			if (Pattern.matches("(?i)^/" + (String) cmd.getKey() + "(.*)", event.getMessage())) {
				if (XServer.checkPerm(event.getPlayer(), ((String) cmd.getValue())))
				{
					HashMap<String, String> f = new HashMap<String, String>();
					f.put("USERNAME", event.getPlayer().getName());
					f.put("SERVERNAME", XServer.serverName);
					f.put("MESSAGE", event.getMessage().substring(1));
					c.send(new Packet(PacketTypes.PACKET_SERVER_COMMAND, f));
					break;
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void handlePlayerJoin(PlayerJoinEvent event)
	{

		if (XServer.checkPerm(event.getPlayer(), "vanish.silentjoin") || XServer.checkPerm(event.getPlayer(), "vanish.joinwithoutannounce") || XServer.checkPerm(event.getPlayer(), "essentials.silentjoin")) {
			return;
		}

		HashMap<String, String> f = new HashMap<String, String>();

		f.put("USERNAME", event.getPlayer().getName());
		f.put("SERVERNAME", XServer.serverName);
		f.put("MESSAGE", event.getPlayer().getAddress().getAddress().getHostAddress());
		c.send(new Packet(PacketTypes.PACKET_PLAYER_JOIN, f));

	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void handlePlayerLeave(PlayerQuitEvent event)
	{

		if (XServer.checkPerm(event.getPlayer(), "vanish.silentquit") || XServer.checkPerm(event.getPlayer(), "vanish.joinwithoutannounce") || XServer.checkPerm(event.getPlayer(), "essentials.silentquit")) {
			return;
		}

		HashMap<String, String> f = new HashMap<String, String>();

		f.put("USERNAME", event.getPlayer().getName());
		f.put("SERVERNAME", XServer.serverName);
		f.put("MESSAGE", "");
		c.send(new Packet(PacketTypes.PACKET_PLAYER_LEAVE, f));
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void handlePlayerDeath(PlayerDeathEvent event)
	{
		// Some plugins blank the death message to stop the death from showing in chat
		if (event.getDeathMessage() == null) {
			return;
		}

		HashMap<String, String> f = new HashMap<String, String>();

		f.put("USERNAME", event.getEntity().getName());
		f.put("SERVERNAME", XServer.serverName);
		f.put("MESSAGE", event.getDeathMessage());
		c.send(new Packet(PacketTypes.PACKET_PLAYER_DEATH, f));
	}

	/*
	 * @EventHandler(priority = EventPriority.HIGH) public void
	 * handleCommand(PlayerCommandPreprocessEvent event){
	 *
	 * if(event.getMessage().equalsIgnoreCase("/reload")){ XServer.restartMode =
	 * PacketTypes.DC_TYPE_RELOAD; } if(event.getMessage().startsWith("/stop")){
	 * XServer.restartMode = PacketTypes.DC_TYPE_STOP; }
	 *
	 * }
	 */
}
