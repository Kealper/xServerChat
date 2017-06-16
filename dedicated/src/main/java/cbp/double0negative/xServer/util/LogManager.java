package cbp.double0negative.xServer.util;

import java.time.LocalTime;

public class LogManager
{
	public static void info(String msg)
	{
		println("[INFO] " + msg);
	}

	public static void warn(String msg)
	{
		println("[WARN] " + msg);
	}

	public static void error(String msg)
	{
		println("[ERROR] " + msg);
	}

	public static void println(String msg) {
		LocalTime now = LocalTime.now();
		System.out.printf("%02d:%02d:%02d %s\n", now.getHour(), now.getMinute(), now.getSecond(), colorize(msg));
	}

	private static String colorize(String msg) {
		msg = msg.replaceAll("\u00A70", (char) 27 + "[0;30m"); // Black
		msg = msg.replaceAll("\u00A71", (char) 27 + "[0;34m"); // Blue
		msg = msg.replaceAll("\u00A72", (char) 27 + "[0;32m"); // Green
		msg = msg.replaceAll("\u00A73", (char) 27 + "[0;36m"); // Cyan
		msg = msg.replaceAll("\u00A74", (char) 27 + "[0;31m"); // Red
		msg = msg.replaceAll("\u00A75", (char) 27 + "[0;35m"); // Purple
		msg = msg.replaceAll("\u00A76", (char) 27 + "[0;33m"); // Gold
		msg = msg.replaceAll("\u00A77", (char) 27 + "[0;37m"); // Gray
		msg = msg.replaceAll("\u00A78", (char) 27 + "[1;30m"); // Dark Gray
		msg = msg.replaceAll("\u00A79", (char) 27 + "[1;34m"); // Light Blue
		msg = msg.replaceAll("(?i)\u00A7a", (char) 27 + "[1;32m"); // Light Green
		msg = msg.replaceAll("(?i)\u00A7b", (char) 27 + "[1;36m"); // Light Cyan
		msg = msg.replaceAll("(?i)\u00A7c", (char) 27 + "[1;31m"); // Light Red
		msg = msg.replaceAll("(?i)\u00A7d", (char) 27 + "[1;35m"); // Light Purple
		msg = msg.replaceAll("(?i)\u00A7e", (char) 27 + "[1;33m"); // Yellow
		msg = msg.replaceAll("(?i)\u00A7f", (char) 27 + "[1;37m"); // White
		msg = msg.replaceAll("(?i)\u00A7k", (char) 27 + "[0m"); // Obfuscated (Just invert original)
		msg = msg.replaceAll("(?i)\u00A7l", (char) 27 + "[1m"); // Bold (Might not play nicely on some terminals)
		msg = msg.replaceAll("(?i)\u00A7m", ""); // Strikethrough (No Support)
		msg = msg.replaceAll("(?i)\u00A7n", (char) 27 + "[4m"); // Underline
		msg = msg.replaceAll("(?i)\u00A7o", ""); // Italic (No support)
		msg = msg.replaceAll("(?i)\u00A7r", (char) 27 + "[0m"); // Reset
		return msg;
	}

	public static String stripFormat(String msg) {
		return msg.replaceAll("\u00A7[0-9a-fklmnor]", "");
	}

}
