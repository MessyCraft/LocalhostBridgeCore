package io.github.messycraft.localhostbridgecore.bungee.util;

import io.github.messycraft.localhostbridgecore.bungee.LocalhostBridgeCore;
import io.github.messycraft.localhostbridgecore.bungee.Properties;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class SimpleUtil {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\w-]+$");

    private static final String PREFIX = "&9LBC » &r";

    private SimpleUtil() {}

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void sendTextMessage(CommandSender sender, String msg) {
        sender.sendMessage(new TextComponent(color(PREFIX + msg)));
    }

    public static void sendRichMessage(CommandSender sender, String msg, String hover, String run) {
        TextComponent text = new TextComponent(color(msg));
        text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent(color(hover))}));
        text.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, run));
        sender.sendMessage(new TextComponent(color(PREFIX)), text);
    }

    public static List<String> tabPrefixFilter(Iterable<String> it, String prefix) {
        List<String> ret = new ArrayList<>();
        it.forEach(s -> {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                ret.add(s);
            }
        });
        return ret;
    }

    public static void runAsyncAsLBC(Runnable runnable) {
        ProxyServer.getInstance().getScheduler().runAsync(LocalhostBridgeCore.getInstance(), runnable);
    }

    public static void runAsyncDelayAsLBC(Runnable runnable, long delayMillis) {
        ProxyServer.getInstance().getScheduler().schedule(LocalhostBridgeCore.getInstance(), runnable, delayMillis, TimeUnit.MILLISECONDS);
    }

    public static boolean nameMatches(String test) {
        return NAME_PATTERN.matcher(test).matches();
    }

    public static void debug(String text) {
        if (Properties.DEBUG) {
            LocalhostBridgeCore.getInstance().getLogger().info("[DEBUG] " + text);
        }
    }

}
