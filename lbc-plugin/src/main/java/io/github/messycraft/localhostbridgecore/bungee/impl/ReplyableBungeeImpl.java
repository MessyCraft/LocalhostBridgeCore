package io.github.messycraft.localhostbridgecore.bungee.impl;

import io.github.messycraft.localhostbridgecore.api.subscribe.Replyable;
import io.github.messycraft.localhostbridgecore.bungee.util.SimpleUtil;

import java.util.function.Consumer;

@SuppressWarnings("SpellCheckingInspection")
public class ReplyableBungeeImpl implements Replyable {

    private final String[] res;
    private final String logSuffix;
    private boolean valid = true;

    ReplyableBungeeImpl(String[] res, String seq) {
        this.res = res;
        this.logSuffix = "[" + seq + "]";
    }

    @Override
    public synchronized boolean reply(String content) {
        if (res == null || res.length != 1) {
            throw new UnsupportedOperationException();
        }
        if (!valid) {
            return false;
        }
        valid = false;
        SimpleUtil.debug("Reply " + logSuffix + " -> " + content);
        res[0] = content;
        return true;
    }

    public static class Own implements Replyable {

        private final Consumer<String> consumer;
        private final String logSuffix;
        private boolean valid = true;

        Own(Consumer<String> consumer, String seq) {
            this.consumer = consumer;
            this.logSuffix = "[" + seq + "]";
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
            SimpleUtil.debug("Reply " + logSuffix + " -> " + content);
            SimpleUtil.runAsyncAsLBC(() -> consumer.accept(content));
            return true;
        }

    }

}
