package io.github.messycraft.localhostbridgecore.bungee.impl;

import io.github.messycraft.localhostbridgecore.api.subscribe.ChannelListener;
import io.github.messycraft.localhostbridgecore.api.subscribe.ListenerManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ListenerManagerBungeeImpl implements ListenerManager {

    private final Map<String, PriorityQueue<ChannelListener>> listeners = new ConcurrentHashMap<>();

    public void call(String from, String namespace, String seq, String data, boolean needReply) {
        // TODO sth
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
