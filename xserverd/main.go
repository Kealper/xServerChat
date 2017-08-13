package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"net"
	"strings"
	"sync"
	"time"
)

type Packet struct {
	Type    int `json:"type"`
	Args    map[string] string `json:"args"`
	Formats map[string] string `json:"formats"`
}

type Client struct {
	UUID       string
	Socket     *net.TCPConn
	Name       string
	SignOn     time.Time
	LastActive time.Time
}

var (
	Version    = "1.2.0"
	Clients    = make(map[string] *Client)
	ClientLock = new(sync.Mutex)
	LogLevel   = 1
	LogFile    = "latest.log"
)

const (
	PACKET_NOOP = iota
	PACKET_SERVER_DC
	PACKET_SERVER_NAME
	PACKET_AUTH
	PACKET_MESSAGE
	PACKET_STATS_REQ
	PACKET_STATS_REPLY
	PACKET_PING
	PACKET_PONG
	PACKET_CLIENT_DC
	PACKET_PLAYER_JOIN
	PACKET_PLAYER_LEAVE
	PACKET_CC
	PACKET_PLAYER_DEATH
	PACKET_CLIENT_CONNECTED
	PACKET_PLAYER_ACTION
	PACKET_PLAYER_BROADCAST
	PACKET_PLAYER_SOCIALSPY
	PACKET_PLAYER_HELPOP
	PACKET_SERVER_COMMAND
	PACKET_PLAYER_OPCHAT
)

func broadcastPacket(p Packet, c *Client) {
	data, err := json.Marshal(p)
	if err != nil {
		fmt.Println("Error:", err.Error())
		return
	}
	data = append(data, byte(0))
	for uuid, client := range Clients {
		if uuid == c.UUID {
			continue
		}
		client.Socket.Write(data)
	}
}

func handleClient(c *net.TCPConn) {
	c.SetKeepAlive(true)
	c.SetKeepAlivePeriod(30 * time.Second)
	c.SetNoDelay(true)

	ClientLock.Lock()
	client := &Client {
		UUID: c.RemoteAddr().String(),
		Socket: c,
		Name: c.RemoteAddr().String(),
		SignOn: time.Now(),
		LastActive: time.Now(),
	}
	Clients[client.UUID] = client
	ClientLock.Unlock()

	writeLog("New connection from " + client.Name, 0)

	for {
		buf := make([]byte, 65535)
		l, err := c.Read(buf)
		if err != nil {
			writeLog(client.Name + " lost connection to the server.", 1)
			c.Close()
			ClientLock.Lock()
			delete(Clients, client.UUID)
			ClientLock.Unlock()
			return
		}

		packets := strings.Split(string(buf[:l]), "\x00")
		for i := 0; i < len(packets); i++ {
			if len(packets[i]) < 1 {
				continue
			}
			packet := Packet{}
			err := json.Unmarshal([]byte(packets[i]), &packet)
			if err != nil {
				writeLog("Unable to read packet data from " + client.Name + ".", 2)
				writeLog(err.Error(), 0)
				writeLog(packets[i], 0)
				continue
			}

			ClientLock.Lock()
			client.LastActive = time.Now()
			Clients[client.UUID].LastActive = client.LastActive
			ClientLock.Unlock()

			switch packet.Type {
			case PACKET_NOOP:
				continue

			case PACKET_SERVER_DC:
				continue

			case PACKET_SERVER_NAME:
				continue

			case PACKET_AUTH:
				continue

			case PACKET_MESSAGE:
				if packet.Args["CANCELLED"] == "true" {
					if packet.Args["CHANNEL"] == "true" {
						writeLog("[" + packet.Args["SERVERNAME"] + "] " + colorize("\u00a78" + decolorize(packet.Args["USERNAME"] + ": " + packet.Args["MESSAGE"])), 1)
					} else {
						writeLog("[" + packet.Args["SERVERNAME"] + "] " + colorize("\u00a7c" + decolorize(packet.Args["USERNAME"] + ": " + packet.Args["MESSAGE"])), 1)
					}
				} else {
					writeLog("[" + packet.Args["SERVERNAME"] + "] " + packet.Args["USERNAME"] + "\u00a7f: " + packet.Args["MESSAGE"], 1)
				}

			case PACKET_STATS_REQ:
				continue

			case PACKET_STATS_REPLY:
				continue

			case PACKET_PING:
				continue

			case PACKET_PONG:
				continue

			case PACKET_CLIENT_DC:
				writeLog(packet.Args["SERVERNAME"] + " has disconnected", 1)
				continue

			case PACKET_PLAYER_JOIN:
				writeLog("" + packet.Args["USERNAME"] + " has joined server " + packet.Args["SERVERNAME"], 1)

			case PACKET_PLAYER_LEAVE:
				writeLog("" + packet.Args["USERNAME"] + " has left server " + packet.Args["SERVERNAME"], 1)

			case PACKET_CC:
				continue

			case PACKET_PLAYER_DEATH:
				writeLog("" + packet.Args["MESSAGE"] + " on server " + packet.Args["SERVERNAME"], 1)

			case PACKET_CLIENT_CONNECTED:
				writeLog(packet.Args["SERVERNAME"] + " has connected", 1)

			case PACKET_PLAYER_ACTION:
				writeLog("[" + packet.Args["SERVERNAME"] + "] * " + packet.Args["USERNAME"] + " " + packet.Args["MESSAGE"], 1)

			case PACKET_PLAYER_BROADCAST:
				writeLog("[BROADCAST] " + packet.Args["MESSAGE"], 1)

			case PACKET_PLAYER_SOCIALSPY:
				switch strings.ToLower(packet.Args["TYPE"]) {
				case "book-quill":
					writeLog("[" + packet.Args["SERVERNAME"] + "] " + colorize("\u00a77" + decolorize(packet.Args["USERNAME"] + "\u00a7f changed book-quill text from \"" + packet.Args["PREVIOUS"] + "\" to \"" + packet.Args["MESSAGE"] +  "\"")), 1)

				case "book-signed":
					writeLog("[" + packet.Args["SERVERNAME"] + "] " + colorize("\u00a77" + decolorize(packet.Args["USERNAME"] + "\u00a7f changed book-signed text from \"" + packet.Args["PREVIOUS"] + "\" to \"" + packet.Args["MESSAGE"] +  "\"")), 1)

				default: // Catch "social" type or older protocol versions that don't send types
					writeLog("[" + packet.Args["SERVERNAME"] + "] " + colorize("\u00a77" + decolorize(packet.Args["USERNAME"] + "\u00a7f: " + packet.Args["MESSAGE"])), 1)
				}

			case PACKET_PLAYER_HELPOP:
				writeLog("[HelpOp] " + colorize(packet.Args["USERNAME"] + "\u00a7f: " + "\u00a7d" + decolorize(packet.Args["MESSAGE"])), 1)

			case PACKET_SERVER_COMMAND:
				writeLog(packet.Args["USERNAME"] + " issued global server command: " + packet.Args["MESSAGE"], 1)

			case PACKET_PLAYER_OPCHAT:
				writeLog("[OpChat] " + colorize(packet.Args["USERNAME"] + "\u00a7f: " + "\u00a7e" + decolorize(packet.Args["MESSAGE"])), 1)

			default:
				writeLog("Unhandled packet from client " + client.Name, 2)
				writeLog(packets[i], 0)
				continue
			}

			broadcastPacket(packet, client)

		}
	}
}

func main() {
	addr := flag.String("a", ":33777", "Local address:port to bind when listening for client connections.")
	logLevel := flag.Int("level", 1, "Log level, ranging from 0 (most talkative, show everything) to 4 (least talkative, only shows severe output)")
	logFile := flag.String("logfile", "latest.log", "File name to log all configured output into.")
	flag.Parse()

	LogLevel = *logLevel
	LogFile = *logFile

	laddr, err := net.ResolveTCPAddr("tcp", *addr)
	if err != nil {
		writeLog("Failed to resolve bind address! (" + *addr + ")", 4)
		writeLog(err.Error(), 0)
		return
	}

	l, err := net.ListenTCP("tcp", laddr)
	if err != nil {
		writeLog("Failed to bind listening socket! (" + *addr + ")", 4)
		writeLog(err.Error(), 0)
		return
	}

	writeLog("xserverd " + Version + " started successfully.", 1)
	writeLog("Listening for connections on " + *addr, 1)

	for {
		c, err := l.AcceptTCP()
		if err != nil {
			writeLog("Unable to accept new connection.", 3)
			writeLog(err.Error(), 0)
			continue
		}
		go handleClient(c)
	}
}
