package io.github.messycraft.localhostbridgecore.bungee.impl;

import io.github.messycraft.localhostbridgecore.api.subscribe.Replyable;
import io.github.messycraft.localhostbridgecore.bungee.util.SimpleUtil;

import java.util.concurrent.SynchronousQueue;
import java.util.function.Consumer;

@SuppressWarnings("SpellCheckingInspection")
public class ReplyableBungeeImpl implements Replyable {

    private final SynchronousQueue<String> queue;

    private boolean valid = true;

    ReplyableBungeeImpl(SynchronousQueue<String> queue) {
        this.queue = queue;
    }

    @Override
    public synchronized boolean reply(String content) {
        if (queue == null) {
            throw new UnsupportedOperationException();
        }
        if (!valid) {
            return false;
        }
        valid = false;
        return queue.offer(content);
    }

    public static class Own implements Replyable {

        private final Consumer<String> consumer;

        private boolean valid = true;

        Own(Consumer<String> consumer) {
            this.consumer = consumer;
        }

        @Override
        public synchronized boolean reply(String content) {
            if (consumer == null) {
                throw new UnsupportedOperationException();
            }
            if (!valid) {
                return false;
            }
            valid = false;
            SimpleUtil.runAsyncAsLBC(() -> consumer.accept(content));
            return true;
        }

    }

}
