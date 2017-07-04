<?php

namespace xServerPM;

use pocketmine\Thread;

class Client extends Thread {
	
	const PACKET_NOOP             = 0;
	const PACKET_SERVER_DC        = 1;
	const PACKET_SERVER_NAME      = 2;
	const PACKET_AUTH             = 3;
	const PACKET_MESSAGE          = 4;
	const PACKET_STATS_REQ        = 5;
	const PACKET_STATS_REPLY      = 6;
	const PACKET_PING             = 7;
	const PACKET_PONG             = 8;
	const PACKET_CLIENT_DC        = 9;
	const PACKET_PLAYER_JOIN      = 10;
	const PACKET_PLAYER_LEAVE     = 11;
	const PACKET_CC               = 12;
	const PACKET_PLAYER_DEATH     = 13;
	const PACKET_CLIENT_CONNECTED = 14;
	const PACKET_PLAYER_ACTION    = 15;
	const PACKET_PLAYER_BROADCAST = 16;
	const PACKET_PLAYER_SOCIALSPY = 17;
	const PACKET_PLAYER_HELPOP    = 18;
	const PACKET_SERVER_COMMAND   = 19;
	const PACKET_PLAYER_OPCHAT    = 20;
	
	public $messageData, $messageType, $messagePerms;
	private $socket;
	private $address, $port, $name, $formats;
	
	public function __construct($address, $port, $name, $formats) {
		$this->address = $address;
		$this->port = $port;
		$this->name = $name;
		$this->formats = $formats;
		$this->messageType = -1;
		$this->start();
	}
	
	public function sendMessage($messageData, $messagePerms = "xserver.message.recieve", $messageType = 0) {
		$this->messageData = $messageData;
		$this->messagePerms = $messagePerms;
		$this->messageType = $messageType;
		$this->synchronized(function ($thread) {
			$thread->wait();
		}, $this);
		$this->messageData = "";
		$this->messagePerms = "";
		$this->messageType = -1;
	}
	
	public function format($packet, $type) {
		$message = $packet->getFormat($type);
		$message = str_replace("{message}", $packet->getArg("MESSAGE"), $message);
		$message = str_replace("{username}", $packet->getArg("USERNAME"), $message);
		$message = str_replace("{server}", $packet->getArg("SERVERNAME"), $message);
		$message = preg_replace("/&([0-9a-frlmnok])/i", "§$1", $message);
		return $message;
	}
	
	public function send(Packet $packet) {
		$packet->setFormats($this->formats);
		socket_write($this->socket, $packet->toString());
	}
	
	public function run() {
		$this->socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
		socket_set_option($this->socket, SOL_SOCKET, SO_KEEPALIVE, 1);
		if (!socket_connect($this->socket, $this->address, $this->port)) {
			$this->sendMessage("Failed to connect to " . $this->address . ":" . $this->port . ". (" . socket_last_error() . ")", "", 0);
			return;
		}
		//socket_set_nonblock($this->socket);
		$this->sendMessage("Connected to " . $this->address . ":" . $this->port . ".", "", 0);
		$this->send(new Packet(self::PACKET_CLIENT_CONNECTED, ["SERVERNAME" => $this->name]));
		while (true) {
			$packets = socket_read($this->socket, 65535);
			if ($packets == "") {
				usleep(1000);
				continue;
			}

			$packets = explode("\0", $packets);
			foreach ($packets as $packet) {
				if ($packet == "") {
					continue;
				}
				
				$packet = new Packet(json_decode($packet));
				if (json_last_error() != JSON_ERROR_NONE) {
					$this->sendMessage("Failed to decode packet!", "", 0);
					continue;
				}
				
				switch ($packet->type) {
				case self::PACKET_NOOP:
					continue;

				case self::PACKET_SERVER_DC:
					continue;

				case self::PACKET_SERVER_NAME:
					continue;

				case self::PACKET_AUTH:
					continue;

				case self::PACKET_MESSAGE:
					if ($packet->getArg("CHANNEL") == "true") {
						break;
					}
					if ($packet->getArg("CANCELLED") == "true") {
						if ($packet->getArg("CHANNEL") == "true") {
							$this->sendMessage($this->format($packet, "CHANNEL"), "xserver.message.channel", 1);
						} else {
							$this->sendMessage("§c[Cancelled] " . preg_replace("/§[0-9a-frlmnok]/i", "", $this->format($packet, "MESSAGE")), "xserver.message.cancelled", 1);
						}
						break;
					}
					$this->sendMessage($this->format($packet, "MESSAGE"), "xserver.message.receive", 1);
					break;

				case self::PACKET_STATS_REQ:
					continue;

				case self::PACKET_STATS_REPLY:
					continue;

				case self::PACKET_PING:
					continue;

				case self::PACKET_PONG:
					continue;

				case self::PACKET_CLIENT_DC:
					$this->sendMessage($this->format($packet, "DISCONNECT"), "xserver.message.receive", 1);
					break;

				case self::PACKET_PLAYER_JOIN:
					$this->sendMessage($this->format($packet, "LOGIN"), "xserver.message.receive", 1);
					break;

				case self::PACKET_PLAYER_LEAVE:
					$this->sendMessage($this->format($packet, "LOGOUT"), "xserver.message.receive", 1);
					break;

				case self::PACKET_CC:
					continue;

				case self::PACKET_PLAYER_DEATH:
					$this->sendMessage($this->format($packet, "DEATH"), "xserver.message.receive", 1);
					break;

				case self::PACKET_CLIENT_CONNECTED:
					$this->sendMessage($this->format($packet, "CONNECT"), "xserver.message.receive", 1);
					break;

				case self::PACKET_PLAYER_ACTION:
					$this->sendMessage($this->format($packet, "ACTION"), "xserver.message.receive", 1);
					break;

				case self::PACKET_PLAYER_BROADCAST:
					$this->sendMessage($this->format($packet, "BROADCAST"), "xserver.message.receive", 1);
					break;

				case self::PACKET_PLAYER_SOCIALSPY:
					$this->sendMessage($this->format($packet, "SOCIALSPY"), "essentials.socialspy", 1);
					break;

				case self::PACKET_PLAYER_HELPOP:	
					$this->sendMessage($this->format($packet, "HELPOP"), "essentials.helpop.receive", 1);
					break;
				
				case self::PACKET_PLAYER_OPCHAT:	
					$this->sendMessage($this->format($packet, "OPCHAT"), "xserver.opchat.receive", 1);
					break;

				case self::PACKET_SERVER_COMMAND:
					$this->sendMessage($packet->args["MESSAGE"], "", 2);
					break;
				
				default:
					$this->sendMessage("Unhandled packet type!", "", 0);
					continue;
				}
			}
		}
	}
	
}

class Packet {
	
	public $args;
	public $type;
	public $formats;
	
	public function __construct() {
		if (func_num_args() == 1) {
			$this->type = func_get_arg(0)->type;
			$this->args = (array) func_get_arg(0)->args;
			$this->formats = (array) func_get_arg(0)->formats;
			return;
		}
		$this->type = func_get_arg(0);
		$this->args = func_get_arg(1);
		$this->formats = [];
	}
	
	public function getType() {
		return $this->type;
	}
	
	public function setType($type) {
		$this->type = $type;
	}
	
	public function getArg($arg) {
		return $this->args[$arg];
	}
	
	public function setArg($arg, $value) {
		$this->args[$arg] = $value;
	}
	
	public function getArgs() {
		return $this->args;
	}
	
	public function setArgs($args) {
		$this->args = $args;
	}
	
	public function getFormat($format) {
		return $this->formats[$format];
	}
	
	public function setFormat($format, $value) {
		$this->formats[$format] = $value;
	}
	
	public function getFormats() {
		return $this->formats;
	}
	
	public function setFormats($formats) {
		$this->formats = $formats;
	}
	
	public function toString() {
		return json_encode($this);
	}

}
