<?php

namespace xServerPM;

use pocketmine\plugin\PluginBase;
use pocketmine\command\Command;
use pocketmine\command\CommandSender;
use pocketmine\command\ConsoleCommandSender;
use pocketmine\utils\TextFormat;
use pocketmine\Player;
use SimpleAuth\SimpleAuth;

class xServerPM extends PluginBase {
	
	private $client, $listener;
	private $address, $port, $name, $formats, $forwardedCommands, $ignoredCommands;
	private $pluginTag;
	private $pureChat, $simpleAuth;
	
	public function onLoad() {
		ini_set('mbstring.substitute_character', "none");
	}
	
	public function onEnable() {
		// These are ugly and should be moved out into config options
		$this->address = "bungee.famcraft.com";
		$this->port = 33777;
		$this->name = "Pocket";
		$this->formats = [
			"MESSAGE" => "&7[p] {username}&f: {message}",
			"LOGOUT" => "&e{username}&e has left the game &7(On {server})",
			"ACTION" => "&7[p]&r&a * &r{username}&r&a {message}",
			"BROADCAST" => "&6[&4Broadcast&6]&a {message}",
			"SOCIALSPY" => "&7[{server}] {username}: {message}",
			"LOGIN" => "&e{username}&e has joined the game &7(On {server})",
			"DEATH" => "&e{username} died &7(On {server})",
			"CONNECT" => "",
			"DISCONNECT" => "",
			"HELPOP" => "&e[HelpOp]&r {username}: &d{message}",
		];
		$this->forwardedCommands = [
			"whitelist on" => "pocketmine.command.whitelist",
			"kick" => "pocketmine.command.kick",
			"ban " => "pocketmine.command.ban",
			"banip " => "pocketmine.command.banip",
			"unban " => "pocketmine.command.unban",
			"pardon " => "pocketmine.command.unban",
			"unbanip " => "pocketmine.command.unbanip",
			"pardonip " => "pocketmine.command.unbanip",
			"mute " => "essentials.mute.use",
			"tempban " => "essentials.tempban",
			"togglejail " => "essentials.togglejail",
			"tjail " => "essentials.togglejail",
			"jail " => "essentials.togglejail",
			"unjail " => "essentials.togglejail",
			"clear" => "mycmd.clearscreen",
			"pex " => "pocketmine.command.op",
			"clock" => "clearchat.lock",
			"muteall" => "clearchat.lock",
			"sudo " => "essentials.sudo",
			"ns " => "pocketmine.command.op",
		];
		$this->ignoredCommands = "whitelist |pex |nick |mycmd-reload |spawn ";
		
		$this->pluginTag = TextFormat::YELLOW . "[xServer] " . TextFormat::RESET;
		$this->pureChat = $this->getServer()->getPluginManager()->getPlugin("PureChat");
		$this->simpleAuth = $this->getServer()->getPluginManager()->getPlugin("SimpleAuth");
		$this->listener = new EventListener($this);
		$this->getServer()->getPluginManager()->registerEvents($this->listener, $this);
		$this->getServer()->getScheduler()->scheduleRepeatingTask(new Tasks($this), 1);
		$this->client = new Client($this->address, $this->port, $this->name, $this->formats);
	}
	
	public function onDisable() {
		
	}
	
	public function onCommand(CommandSender $sender, Command $command, $label, array $args) {
		switch ($command->getName()) {
		case "xserver":
			// TODO: This.
			return true;
		
		case "ac":
		case "helpop":
			if (!$this->hasPermission($sender, "essentials.helpop")) {
				$sender->sendMessage($this->pluginTag . TextFormat::RED . "You don't have permission to use this.");
				return false;
			}
			$message = [
				"SERVERNAME" => $this->name,
				"USERNAME" => $this->getFormattedName($sender),
				"MESSAGE" => implode(" ", $args),
			];
			$localMessage = TextFormat::YELLOW . "[HelpOp] " . TextFormat::RESET . $message["USERNAME"] . TextFormat::RESET . ": " . TextFormat::LIGHT_PURPLE . $message["MESSAGE"];
			$this->getServer()->getLogger()->info(TextFormat::toANSI($localMessage));
			foreach ($this->getServer()->getOnlinePlayers() as $player) {
				if (!$this->hasPermission($player, "essentials.helpop.receive")) {
					continue;
				}
				$player->sendMessage($localMessage);
			}
			$packet = new Packet($this->client::PACKET_PLAYER_HELPOP, $message);
			$this->client->send($packet);
			return true;
		}
	}
	
	public function getFormattedName($sender) {
		$name = $sender->getName();
		if ($sender instanceof Player) {
			$name = $this->pureChat->getPrefix($sender) . $sender->getDisplayName() . $this->pureChat->getSuffix($sender) . TextFormat::RESET;
		} else {
			$name = TextFormat::GOLD . $name . TextFormat::RESET;
		}
		return $name;
	}
	
	public function getClient() {
		return $this->client;
	}

	public function getListener() {
		return $this->listener;
	}
	
	public function getClientName() {
		return $this->name;
	}
	
	public function getForwardedCommands() {
		return $this->forwardedCommands;
	}
	
	public function hasPermission($player, $permission, $bypassAuth = false) {
		if (!$this->simpleAuth->isPlayerAuthenticated($player) && !$bypassAuth) {
			return false;
		}
		if ($player->isOp()) {
			return true;
		}
		if ($player->hasPermission($permission)) {
			return true;
		}
		return false;
	}
	
	private function stripFormat($message) {
		return preg_replace("/§[0-9a-frlmnok]/i", "", $message);
	}
	
	private function stripUnicode($message) {
		$message = preg_replace("/§([0-9a-frlmnok])/i", "\x01$1", $message);
		$message = mb_convert_encoding($message, "ASCII", "UTF-8");
		return preg_replace("/\x01([0-9a-frlmnok])/i", "§$1", $message);
	}
	
	public function checkClient() {
		if (!$this->client->isRunning()) {
			return;
		}
		$messageData = $this->client->messageData;
		$messageType = $this->client->messageType;
		$messagePerms = $this->client->messagePerms;
		
		switch ($messageType) {
		case -1:
			return;

		case 0:
			$this->getLogger()->info($messageData);
			break;
		
		case 1:
			$messageData = $this->stripUnicode($messageData);
			$this->getServer()->getLogger()->info(TextFormat::toANSI($messageData));
			foreach ($this->getServer()->getOnlinePlayers() as $player) {
				if (!$this->hasPermission($player, $messagePerms)) {
					continue;
				}
				$player->sendMessage($messageData);
			}
			break;
		
		case 2:
			if (preg_match("/" . $this->ignoredCommands . "/i", $messageData)) {
				break;
			}

			$this->getServer()->dispatchCommand(new ConsoleCommandSender(), $messageData);
			break;
		}
		$this->client->synchronized(function($thread) {
			$thread->notify();
		}, $this->client);
	}
	
}
