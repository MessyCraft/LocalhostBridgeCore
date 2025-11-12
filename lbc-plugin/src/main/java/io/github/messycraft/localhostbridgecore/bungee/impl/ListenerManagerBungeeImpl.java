package io.github.messycraft.localhostbridgecore.bungee.impl;

import io.github.messycraft.localhostbridgecore.api.subscribe.ChannelListener;
import io.github.messycraft.localhostbridgecore.api.subscribe.ListenerManager;
import io.github.messycraft.localhostbridgecore.api.subscribe.Replyable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ListenerManagerBungeeImpl implements ListenerManager {

    private final Map<String, PriorityQueue<ChannelListener>> listeners = new ConcurrentHashMap<>();

    public String call(String from, String namespace, String seq, String data, boolean needReply) {
        PriorityQueue<ChannelListener> q = listeners.get(namespace);
        String[] ret = new String[1];
        Replyable replyable = new ReplyableBungeeImpl(needReply ? ret : null, seq);
        if (q != null) {
            for (ChannelListener listener : q) {
                listener.onMessageReceive(from, namespace, seq, data, needReply, replyable);
            }
        }
        return ret[0];
    }

    public void callSelf(String namespace, String seq, String data, boolean needReply, Consumer<String> reply) {
        PriorityQueue<ChannelListener> q = listeners.get(namespace);
        Replyable replyable = new ReplyableBungeeImpl.Own(needReply ? reply : null, seq);
        if (q != null) {
            for (ChannelListener listener : q) {
                listener.onMessageReceive("BC", namespace, seq, data, needReply, replyable);
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
