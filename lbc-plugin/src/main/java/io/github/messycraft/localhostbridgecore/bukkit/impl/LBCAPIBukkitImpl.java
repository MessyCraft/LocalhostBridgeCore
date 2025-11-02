package io.github.messycraft.localhostbridgecore.bukkit.impl;

import io.github.messycraft.localhostbridgecore.api.LocalhostBridgeCoreAPI;
import io.github.messycraft.localhostbridgecore.api.subscribe.ListenerManager;
import io.github.messycraft.localhostbridgecore.bukkit.util.HttpClientUtil;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class LBCAPIBukkitImpl implements LocalhostBridgeCoreAPI {

    private Plugin plugin;

    public LBCAPIBukkitImpl(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getRegisteredChannels() {
        HttpClientUtil.ResponseStruct resp = HttpClientUtil.doPost("/list", null, plugin.getConfig().getString("unique"));
        return resp.code == 200 && resp.data != null ? new ArrayList<>(Arrays.asList(resp.data.split("\\$"))) : null;
    }

    @Override
    public ListenerManager getListenerManager() {
        return null;
    }

    @Override
    public void send(String channel, String namespace, String body) {

    }

    @Override
    public void sendForReply(String channel, String namespace, String body, Consumer<String> reply) {

    }

    @Override
    public void sendForReply(String channel, String namespace, String body, Consumer<String> reply, Runnable noReply) {

    }

    @Override
    public void broadcast(String namespace, String body) {

    }

    @Override
    public void broadcastForWaitReply(String namespace, String body, Consumer<Map<String, String>> reply) {

    }

}
