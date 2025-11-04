package io.github.messycraft.localhostbridgecore.bungee.util;

import io.github.messycraft.localhostbridgecore.bungee.LocalhostBridgeCore;
import io.github.messycraft.localhostbridgecore.bungee.entity.LChannel;
import lombok.Getter;
import net.md_5.bungee.config.Configuration;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class ChannelRegistrationUtil {

    @Getter
    private static Map<String, LChannel> registeredChannel = new HashMap<>();

    private ChannelRegistrationUtil() {}

    public static void loadData() {
        Configuration registered = YamlConfigurationUtil.loadDataFile();
        registeredChannel.clear();
        Collection<String> keys = registered.getKeys();
        for (String s : keys) {
            if ("BC".equals(s)) {
                LocalhostBridgeCore.getInstance().getLogger().warning("已跳过重复的频道: BC");
                continue;
            }
            registeredChannel.put(s, new LChannel(s, registered.getSection(s).getInt("port")));
        }
    }

    private static void saveData() {
        Configuration registered = new Configuration();
        for (LChannel c : registeredChannel.values()) {
            registered.set(c.getUnique() + ".port", c.getPort());
        }
        YamlConfigurationUtil.saveDataFile(registered);
    }

    public static boolean registerChannel(String unique, int port) {
        if (!SimpleUtil.nameMatches(unique) || port > 65535) {
            return false;
        }
        if ("BC".equals(unique)) {
            return false;
        }
        if (registeredChannel.containsKey(unique)) {
            return registeredChannel.get(unique).getPort() == port;
        }
        registeredChannel.put(unique, new LChannel(unique, port));
        LocalhostBridgeCore.getInstance().getLogger().info("[ChannelRecord - NEW] " + unique + ":" + port);
        saveData();
        return true;
    }

    public static boolean unregisterChannel(String unique) {
        LChannel c;
        if ((c = registeredChannel.remove(unique)) != null) {
            LocalhostBridgeCore.getInstance().getLogger().info("[ChannelRecord - DELETE] " + unique);
            saveData();
            c.setValid(false);
            return true;
        }
        return false;
    }

    public static boolean isRegistered(String unique) {
        return "BC".equals(unique) || registeredChannel.containsKey(unique);
    }

    public static boolean isRegisteredOfBukkit(String unique, String portStr) {
        LChannel c = registeredChannel.get(unique);
        return c != null && String.valueOf(c.getPort()).equals(portStr);
    }

}
