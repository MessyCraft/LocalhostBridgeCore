package io.github.messycraft.localhostbridgecore.bungee.entity;

import io.github.messycraft.localhostbridgecore.bungee.util.ServerListPingUtil;
import io.github.messycraft.localhostbridgecore.bungee.util.SimpleUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Data
@RequiredArgsConstructor
public class LChannel {

    private final String unique;
    private final int port;
    private boolean valid = true;
    private int pingFailCount = 0;
    private int sessionExpireCount = 0;

    /**
     * 发送频道消息
     * @param namespace 命名空间
     * @param data 数据内容
     * @param needReply 是否需要回复
     */
    public void sendChannelData(String namespace, String data, boolean needReply, Consumer<String> reply, Runnable noReply) {
        if (!valid) {
            throw new IllegalStateException("valid == false");
        }
        SimpleUtil.runAsyncAsLBC(() -> {
            String seq = UUID.randomUUID().toString().substring(0, 6);
            ServerListPingUtil.sendCustomData("BC", this, namespace, data, needReply, seq, reply, noReply);
        });
    }

    /**
     * 发送Hello消息 (会阻塞线程)
     * @return 延时，单位纳秒 (若超时则返回-1)
     */
    public long sendHelloInCurrentThread() {
        if (!valid) {
            throw new IllegalStateException("valid == false");
        }
        String seq = UUID.randomUUID().toString().substring(0, 6);
        AtomicLong ret = new AtomicLong(-1);
        long begin = System.nanoTime();
        ServerListPingUtil.sendCustomData("BC", this, "", "", true, seq, s -> ret.set(System.nanoTime() - begin), null);
        return ret.get();
    }

    public void increasePingFailCount() {
        pingFailCount++;
    }

    public void increaseSessionExpireCount() {
        sessionExpireCount++;
    }

}
