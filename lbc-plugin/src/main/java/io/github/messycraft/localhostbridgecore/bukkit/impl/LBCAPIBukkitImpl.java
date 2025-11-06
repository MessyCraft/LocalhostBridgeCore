package io.github.messycraft.localhostbridgecore.bukkit.impl;

import io.github.messycraft.localhostbridgecore.api.LocalhostBridgeCoreAPI;
import io.github.messycraft.localhostbridgecore.api.subscribe.ListenerManager;
import io.github.messycraft.localhostbridgecore.bukkit.util.HttpClientUtil;
import io.github.messycraft.localhostbridgecore.bukkit.util.SimpleUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class LBCAPIBukkitImpl implements LocalhostBridgeCoreAPI {

    private final ListenerManager listenerManager = new ListenerManagerBukkitImpl();

    @Override
    public List<String> getRegisteredChannels() {
        HttpClientUtil.ResponseStruct resp = HttpClientUtil.doPost("/list", null, null, SimpleUtil.getUnique());
        return resp.code == 200 && resp.data != null ? new ArrayList<>(Arrays.asList(resp.data.split("\\$"))) : null;
    }

    @Override
    public ListenerManager getListenerManager() {
        return listenerManager;
    }

    @Override
    public void send(String channel, String namespace, String body) {
        if (!SimpleUtil.nameMatches(channel) || !SimpleUtil.nameMatches(namespace)) {
            throw new IllegalArgumentException("unique or namespace contains illegal characters");
        }
        HttpClientUtil.sendDataAsync(channel, namespace, body, false, null, null);
    }

    @Override
    public void sendForReply(String channel, String namespace, String body, Consumer<String> reply) {
        sendForReply(channel, namespace, body, reply, null);
    }

    @Override
    public void sendForReply(String channel, String namespace, String body, Consumer<String> reply, Runnable noReply) {
        if (!SimpleUtil.nameMatches(channel) || !SimpleUtil.nameMatches(namespace)) {
            throw new IllegalArgumentException("unique or namespace contains illegal characters");
        }
        HttpClientUtil.sendDataAsync(channel, namespace, body, true, reply, noReply);
    }

    @Override
    public void broadcast(String namespace, String body) {

    }

    @Override
    public void broadcastForWaitReply(String namespace, String body, Consumer<Map<String, String>> reply) {

    }

}
