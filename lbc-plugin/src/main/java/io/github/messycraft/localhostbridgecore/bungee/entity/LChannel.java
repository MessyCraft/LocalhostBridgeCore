package io.github.messycraft.localhostbridgecore.bungee.entity;

import io.github.messycraft.localhostbridgecore.bungee.util.ServerListPingUtil;
import io.github.messycraft.localhostbridgecore.bungee.util.SimpleUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.UUID;
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
     * 发送Hello消息
     * @param nanos 回调，单位纳秒 (若失败则无回调，自行限制超时时长即可)
     */
    public void sendHello(Consumer<Long> nanos) {
        if (!valid) {
            throw new IllegalStateException("valid == false");
        }
        SimpleUtil.runAsyncAsLBC(() -> {
            String seq = UUID.randomUUID().toString().substring(0, 6);
            long begin = System.nanoTime();
            ServerListPingUtil.sendCustomData("BC", this, "", "", true, seq, (s) -> nanos.accept(System.nanoTime() - begin), null);
        });
    }

    public void increasePingFailCount() {
        pingFailCount++;
    }

    public void increaseSessionExpireCount() {
        sessionExpireCount++;
    }

}
