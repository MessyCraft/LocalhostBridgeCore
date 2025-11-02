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
            ServerListPingUtil.sendCustomData(this, namespace, data, needReply, seq, reply, noReply);
        });
    }

    public void increasePingFailCount() {
        pingFailCount++;
    }

    public void increaseSessionExpireCount() {
        sessionExpireCount++;
    }

}
