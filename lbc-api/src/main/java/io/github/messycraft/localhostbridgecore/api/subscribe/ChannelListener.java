package io.github.messycraft.localhostbridgecore.api.subscribe;


/**
 * 代表一个频道监听器。使用者需实现此类的抽象方法。
 */
public abstract class ChannelListener {

    public final byte priority;

    /**
     * 使用默认优先级初始化。优先级小的监听器优先被广播，优先级相同的监听器不保证广播顺序。
     */
    public ChannelListener() {
        this.priority = 0;
    }

    /**
     * 使用指定优先级初始化。优先级小的监听器优先被广播，优先级相同的监听器不保证广播顺序。
     * @param priority 优先级
     */
    public ChannelListener(byte priority) {
        this.priority = priority;
    }

    /**
     * 订阅的内容接收后将在此处理，注意这是非主线程调用的。
     * @param from 消息来源，即来源服务器配置中的唯一标识符
     * @param namespace 命名空间
     * @param seq 一串随机字符，对每条消息唯一
     * @param data 接收的数据
     * @param needReply 来源方是否要求需要回复
     * @param replyable 仅当{@code needReply}为<tt>true</tt>时使用
     */
    public abstract void onMessageReceive(String from, String namespace, String seq, String data, boolean needReply, Replyable replyable);

}
