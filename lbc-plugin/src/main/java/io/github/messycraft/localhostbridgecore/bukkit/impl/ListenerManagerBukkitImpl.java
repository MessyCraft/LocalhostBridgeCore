package io.github.messycraft.localhostbridgecore.bukkit.impl;

import com.github.retrooper.packetevents.protocol.player.User;
import io.github.messycraft.localhostbridgecore.api.subscribe.ChannelListener;
import io.github.messycraft.localhostbridgecore.api.subscribe.ListenerManager;
import io.github.messycraft.localhostbridgecore.api.subscribe.Replyable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ListenerManagerBukkitImpl implements ListenerManager {

    private final Map<String, PriorityQueue<ChannelListener>> listeners = new ConcurrentHashMap<>();

    public void call(String from, String namespace, String seq, String data, boolean needReply, User packetUser) {
        PriorityQueue<ChannelListener> q = listeners.get(namespace);
        Replyable replyable = new ReplyableBukkitImpl(needReply ? packetUser : null, seq);
        if (q != null) {
            for (ChannelListener listener : q) {
                listener.onMessageReceive(from, namespace, seq, data, needReply, replyable);
            }
        }
    }

    @Override
    public void subscribe(ChannelListener listener, String... namespaces) {
        for (String namespace : namespaces) {
            if (!listeners.containsKey(namespace)) {
                listeners.put(namespace, new PriorityQueue<>(Comparator.comparingInt(o -> o.priority)));
            }
            listeners.get(namespace).offer(listener);
        }
    }

    @Override
    public void unsubscribe(String namespace) {
        if (listeners.containsKey(namespace)) {
            listeners.get(namespace).clear();
            listeners.remove(namespace);
        }
    }

    @Override
    public void unsubscribe(ChannelListener listener, String namespace) {
        if (listeners.containsKey(namespace)) {
            listeners.get(namespace).remove(listener);
            if (listeners.get(namespace).isEmpty()) {
                listeners.remove(namespace);
            }
        }
    }

    @Override
    public void unregister(ChannelListener listener) {
        List<String> empty = new ArrayList<>();
        for (Map.Entry<String, PriorityQueue<ChannelListener>> e : listeners.entrySet()) {
            e.getValue().remove(listener);
            if (e.getValue().isEmpty()) {
                empty.add(e.getKey());
            }
        }
        for (String k : empty) {
            listeners.remove(k);
        }
    }

}
