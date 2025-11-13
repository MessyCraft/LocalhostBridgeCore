package io.github.messycraft.localhostbridgecore.bukkit.util;

import io.github.messycraft.localhostbridgecore.bukkit.LocalhostBridgeCore;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.regex.Pattern;

public final class SimpleUtil {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\w-]+$");

    private SimpleUtil() {}

    public static void debug(String text) {
        if (LocalhostBridgeCore.getInstance().getConfig().getBoolean("debug")) {
            LocalhostBridgeCore.getInstance().getLogger().info("[DEBUG] " + text);
        }
    }

    public static void runtimeWarning(String text) {
        if (
                LocalhostBridgeCore.getInstance().getConfig().getBoolean("show-warnings", true) ||
                LocalhostBridgeCore.getInstance().getConfig().getBoolean("debug")
        ) {
            LocalhostBridgeCore.getInstance().getLogger().warning("[!] " + text);
        }
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void sendConsoleOnly(CommandSender sender) {
        sender.sendMessage(color("&c这是一个控制台指令, 你不能使用它."));
    }

    public static boolean nameMatches(String test) {
        return test != null && !test.isEmpty() && NAME_PATTERN.matcher(test).matches();
    }

    public static String getUnique() {
        return LocalhostBridgeCore.getInstance().getConfig().getString("unique", "default");
    }

    public static int getAccessPort() {
        return LocalhostBridgeCore.getInstance().getConfig().getInt("access-port", -1);
    }

    public static int getTimeout() {
        return LocalhostBridgeCore.getInstance().getConfig().getInt("timeout", 50);
    }

    public static int getReadTimeout() {
        return LocalhostBridgeCore.getInstance().getConfig().getInt("read-timeout", 10000);
    }

}
