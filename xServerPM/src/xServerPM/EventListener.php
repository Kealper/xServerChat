<?php

namespace xServerPM;

use pocketmine\event\Listener;
use pocketmine\event\player\PlayerChatEvent;
use pocketmine\event\player\PlayerQuitEvent;
use pocketmine\event\player\PlayerDeathEvent;
use pocketmine\event\player\PlayerCommandPreprocessEvent;
use SimpleAuth\event\PlayerAuthenticateEvent;

class EventListener implements Listener {
	
	private $plugin;
	
	public function __construct(xServerPM $plugin) {
		$this->plugin = $plugin;
	}
	
	/**
	* @ignoreCancelled true
	* @priority HIGHEST
	*/
	/*public function onPlayerChat(PlayerChatEvent $event) {
		$args = [
			"SERVERNAME" => $this->plugin->getClientName(),
			"MESSAGE" => $event->getMessage(),
			"USERNAME" => $this->plugin->getFormattedName($event->getPlayer()),
			"CANCELLED" => "false",
			"CHANNEL" => "false",
		];
		if ($event->isCancelled()) {
			$args["CANCELLED"] = "true";
		}
		$packet = new Packet($this->plugin->getClient()::PACKET_MESSAGE, $args);
		$this->plugin->getClient()->send($packet);
	}*/
	
	/**
	* @priority HIGHEST
	*/
	public function onPlayerAuth(PlayerAuthenticateEvent $event) {
		if ($this->plugin->hasPermission($event->getPlayer(), "essentials.silentjoin", true)) {
			return;
		}
		$args = [
			"SERVERNAME" => $this->plugin->getClientName(),
			"USERNAME" => $event->getPlayer()->getDisplayName(),
		];
		$packet = new Packet($this->plugin->getClient()::PACKET_PLAYER_JOIN, $args);
		$this->plugin->getClient()->send($packet);
	}
	
	/**
	* @priority LOWEST
	*/
	public function onPlayerQuit(PlayerQuitEvent $event) {
		if ($this->plugin->hasPermission($event->getPlayer(), "essentials.silentquit")) {
			return;
		}
		$args = [
			"SERVERNAME" => $this->plugin->getClientName(),
			"USERNAME" => $event->getPlayer()->getDisplayName(),
		];
		$packet = new Packet($this->plugin->getClient()::PACKET_PLAYER_LEAVE, $args);
		$this->plugin->getClient()->send($packet);
	}
	
	/**
	* @priority HIGHEST
	*/
	public function onPlayerDeath(PlayerDeathEvent $event) {
		$args = [
			"SERVERNAME" => $this->plugin->getClientName(),
			"MESSAGE" => $event->getDeathMessage()->getText(),
			"USERNAME" => $event->getPlayer()->getDisplayName(),
		];
		$packet = new Packet($this->plugin->getClient()::PACKET_PLAYER_DEATH, $args);
		$this->plugin->getClient()->send($packet);
	}
	
	/**
	* @ignoreCancelled true
	* @priority HIGHEST
	*/
	public function onPlayerCommand(PlayerCommandPreprocessEvent $event) {
		// This is to handle all chat, including cancelled chat, since PlayerChatEvent doesn't
		if (substr($event->getMessage(), 0, 1) !== "/") {
			$args = [
				"SERVERNAME" => $this->plugin->getClientName(),
				"MESSAGE" => $event->getMessage(),
				"USERNAME" => $this->plugin->getFormattedName($event->getPlayer()),
				"CANCELLED" => "false",
				"CHANNEL" => "false",
			];
			if ($event->isCancelled()) {
				$args["CANCELLED"] = "true";
			}
			$packet = new Packet($this->plugin->getClient()::PACKET_MESSAGE, $args);
			$this->plugin->getClient()->send($packet);
			return;
		}
		
		// Regular command processing below here
		if ($event->isCancelled()) {
			return;
		}

		$args = explode(" ", $event->getMessage());
		$command = array_shift($args);

		switch (strtolower($command)) {
		case "/me":
			$message = [
				"SERVERNAME" => $this->plugin->getClientName(),
				"USERNAME" => $this->plugin->getFormattedName($event->getPlayer()),
				"MESSAGE" => implode(" ", $args),
			];
			$packet = new Packet($this->plugin->getClient()::PACKET_PLAYER_ACTION, $message);
			$this->plugin->getClient()->send($packet);
			return;
		}

		foreach ($this->plugin->getForwardedCommands() as $command => $permission) {
			if (!preg_match("/\/" . $command . "/i", $event->getMessage())) {
				continue;
			}

			if (!$this->plugin->hasPermission($event->getPlayer(), $permission)) {
				continue;
			}

			$message = [
				"SERVERNAME" => $this->plugin->getClientName(),
				"USERNAME" => $event->getPlayer()->getDisplayName(),
				"MESSAGE" => substr($event->getMessage(), 1),
			];
			$packet = new Packet($this->plugin->getClient()::PACKET_SERVER_COMMAND, $message);
			$this->plugin->getClient()->send($packet);
		}
	}
	
}
