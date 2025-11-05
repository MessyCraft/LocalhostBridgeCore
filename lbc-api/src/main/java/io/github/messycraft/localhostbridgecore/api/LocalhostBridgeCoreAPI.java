package io.github.messycraft.localhostbridgecore.api;

import io.github.messycraft.localhostbridgecore.api.subscribe.ListenerManager;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * LocalhostBridge API 的所有操作将从这里开始。
 *
 * @author ImCur_
 */
public interface LocalhostBridgeCoreAPI {

    /**
     * 获取已注册的频道列表。特别地，"BC"为主服务器固定名称。
     * <p>警告: 在Bukkit环境下调用此方法有一个请求的过程，最好还是放在异步线程里。</p>
     * @return 频道列表
     */
    List<String> getRegisteredChannels();

    /**
     * 获取监听器管理器。在这里可以订阅指定命名空间的内容。
     * @return ListenerManager instance
     */
    ListenerManager getListenerManager();

    /**
     * 向指定频道发送一条消息。
     * @param channel 频道名
     * @param namespace 命名空间
     * @param body 消息主体
     */
    void send(String channel, String namespace, String body);

    /**
     * 向指定频道发送一条需要回复的消息。
     * @param channel 频道名
     * @param namespace 命名空间
     * @param body 消息主体
     * @param reply 回调，包含一条回复
     */
    void sendForReply(String channel, String namespace, String body, Consumer<String> reply);

    /**
     * 向指定频道发送一条需要回复的消息。
     * @param channel 频道名
     * @param namespace 命名空间
     * @param body 消息主体
     * @param reply 回调，包含一条回复
     * @param noReply (可选)等待超时后无回复或连接失败时触发
     */
    void sendForReply(String channel, String namespace, String body, Consumer<String> reply, Runnable noReply);

    /**
     * 向群组中广播一条消息。
     * @param namespace 命名空间
     * @param body 消息主体
     */
    void broadcast(String namespace, String body);

    /**
     * 向群组中广播一条需要回复的消息。此方法在所有频道均已回复或超过{session-lifetime}毫秒后进行回调。
     * @param namespace 命名空间
     * @param body 消息主体
     * @param reply 回调，以Map形式传入所有频道的回复内容；Map的键为频道名，值为此频道回复的消息。
     */
    void broadcastForWaitReply(String namespace, String body, Consumer<Map<String, String>> reply);

}