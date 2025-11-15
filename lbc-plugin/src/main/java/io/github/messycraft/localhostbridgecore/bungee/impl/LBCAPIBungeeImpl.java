package io.github.messycraft.localhostbridgecore.bungee.impl;

import io.github.messycraft.localhostbridgecore.api.LocalhostBridgeCoreAPI;
import io.github.messycraft.localhostbridgecore.api.subscribe.ListenerManager;
import io.github.messycraft.localhostbridgecore.bungee.Properties;
import io.github.messycraft.localhostbridgecore.bungee.entity.LChannel;
import io.github.messycraft.localhostbridgecore.bungee.util.ChannelRegistrationUtil;
import io.github.messycraft.localhostbridgecore.bungee.util.SimpleUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LBCAPIBungeeImpl implements LocalhostBridgeCoreAPI {

    private final ListenerManager listenerManager = new ListenerManagerBungeeImpl();

    @Override
    public List<String> getRegisteredChannels() {
        return Stream.concat(Stream.of("BC"), ChannelRegistrationUtil.getRegisteredChannel().keySet().stream()).collect(Collectors.toList());
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
        if ("BC".equals(channel)) {
            String seq = UUID.randomUUID().toString().substring(0, 6);
            SimpleUtil.runAsyncAsLBC(() -> ((ListenerManagerBungeeImpl) listenerManager).callSelf(namespace, seq, body, false, null));
            return;
        }
        LChannel c = ChannelRegistrationUtil.getRegisteredChannel().get(channel);
        if (c != null) {
            c.sendChannelData(namespace, body, false, null, null);
        }
        else {
            SimpleUtil.runtimeWarning("Channel '" + channel + "' does not exist.");
        }
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
        if ("BC".equals(channel)) {
            String seq = UUID.randomUUID().toString().substring(0, 6);
            SimpleUtil.runAsyncAsLBC(() -> ((ListenerManagerBungeeImpl) listenerManager).callSelf(namespace, seq, body, true, reply));
            return;
        }
        LChannel c = ChannelRegistrationUtil.getRegisteredChannel().get(channel);
        if (c != null) {
            c.sendChannelData(namespace, body, true, reply, noReply);
        }
        else {
            SimpleUtil.runtimeWarning("Channel '" + channel + "' does not exist.");
        }
    }

    @Override
    public void broadcast(String namespace, String body) {
        if (!SimpleUtil.nameMatches(namespace)) {
            throw new IllegalArgumentException("namespace contains illegal characters");
        }
        String bcSeq = UUID.randomUUID().toString().substring(0, 6);
        SimpleUtil.runAsyncAsLBC(() -> ((ListenerManagerBungeeImpl) listenerManager).callSelf(namespace, bcSeq, body, false, null));
        for (LChannel c : ChannelRegistrationUtil.getRegisteredChannel().values()) {
            SimpleUtil.runAsyncAsLBC(() -> c.sendChannelData(namespace, body, false, null, null));
        }
    }

    @Override
    public void broadcastForWaitReply(String namespace, String body, Consumer<Map<String, String>> reply) {
        if (!SimpleUtil.nameMatches(namespace)) {
            throw new IllegalArgumentException("namespace contains illegal characters");
        }
        Map<String, String> answer = new ConcurrentHashMap<>();
        String bcSeq = UUID.randomUUID().toString().substring(0, 6);
        SimpleUtil.runAsyncAsLBC(() -> ((ListenerManagerBungeeImpl) listenerManager).callSelf(namespace, bcSeq, body, true, r -> answer.put("BC", r)));
        for (LChannel c : ChannelRegistrationUtil.getRegisteredChannel().values()) {
            SimpleUtil.runAsyncAsLBC(() -> c.sendChannelData(namespace, body, true, r -> answer.put(c.getUnique(), r), null));
        }
        // TODO: 当所有频道都完成回复时提早回调，避免多余等待
        SimpleUtil.runAsyncDelayAsLBC(() -> reply.accept(answer), Properties.SESSION_LIFETIME);
    }

}
