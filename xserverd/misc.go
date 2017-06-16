package main

import (
	"fmt"
	"os"
	"strings"
	"time"
)

func colorize(s string) string {
	s = strings.Replace(s, "\u00a70", "\x1b[0;30m", -1) // Black
	s = strings.Replace(s, "\u00a71", "\x1b[0;34m", -1) // Blue
	s = strings.Replace(s, "\u00a72", "\x1b[0;32m", -1) // Green
	s = strings.Replace(s, "\u00a73", "\x1b[0;36m", -1) // Cyan
	s = strings.Replace(s, "\u00a74", "\x1b[0;31m", -1) // Red
	s = strings.Replace(s, "\u00a75", "\x1b[0;35m", -1) // Purple
	s = strings.Replace(s, "\u00a76", "\x1b[0;33m", -1) // Gold
	s = strings.Replace(s, "\u00a77", "\x1b[0;37m", -1) // Gray
	s = strings.Replace(s, "\u00a78", "\x1b[1;30m", -1) // Dark Gray
	s = strings.Replace(s, "\u00a79", "\x1b[1;34m", -1) // Light Blue
	s = strings.Replace(s, "\u00a7a", "\x1b[1;32m", -1) // Light Green
	s = strings.Replace(s, "\u00a7b", "\x1b[1;36m", -1) // Light Cyan
	s = strings.Replace(s, "\u00a7c", "\x1b[1;31m", -1) // Light Red
	s = strings.Replace(s, "\u00a7d", "\x1b[1;35m", -1) // Light Purple
	s = strings.Replace(s, "\u00a7e", "\x1b[1;33m", -1) // Yellow
	s = strings.Replace(s, "\u00a7f", "\x1b[1;37m", -1) // White
	s = strings.Replace(s, "\u00a7k", "\x1b[0m", -1)    // Obfuscated (Just invert original)
	s = strings.Replace(s, "\u00a7l", "\x1b[1m", -1)    // Bold (Might not play nicely on some terminals)
	s = strings.Replace(s, "\u00a7m", "", -1)           // Strikethrough (No Support)
	s = strings.Replace(s, "\u00a7n", "\x1b[4m", -1)    // Underline
	s = strings.Replace(s, "\u00a7o", "", -1)           // Italic (No support)
	s = strings.Replace(s, "\u00a7r", "\x1b[0m", -1)    // Reset
	return s
}

// Pretty-prints a line in the console log, based on the configuration's selected logging level
func writeLog(text string, level int) {
	if LogLevel > level {
		return
	}
	logLevel := []string{"DEBUG", "INFO", "WARNING", "ERROR", "SEVERE"}
	if level >= len(logLevel) {
		level = len(logLevel) - 1
	}
	if level < 0 {
		level = 0
	}
	timestamp := time.Now().UTC().Format("2006/01/02 15:04:05")
	fmt.Println(timestamp+" ["+logLevel[level]+"]", colorize(text))
	if LogFile != "" { // TODO: Sloppy, should be rewritten eventually
		f, err := os.OpenFile(LogFile, os.O_APPEND|os.O_WRONLY|os.O_CREATE, 0600)
		if err == nil {
			f.WriteString(text+"\n")
			f.Close()
		}
	}
}