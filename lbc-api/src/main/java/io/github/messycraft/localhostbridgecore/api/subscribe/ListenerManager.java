package io.github.messycraft.localhostbridgecore.api.subscribe;

/**
 * 监听器管理器，用于注册或注销监听器以完成数据交流。
 */
public interface ListenerManager {

    /**
     * 订阅一种或多种消息。建议在插件的{@code onDisable()}中注销监听器或取消订阅频道以及时释放资源，这也是插件支持热重载的关键。
     *
     * @param namespaces 订阅的命名空间，如果为空则不会收到任何内容。
     * @throws IllegalArgumentException 任意参数为<tt>null</tt>
     * @see #unsubscribe(String)
     * @see #unsubscribe(ChannelListener, String)
     * @see #unregister(ChannelListener)
     */
    void subscribe(ChannelListener listener, String... namespaces);

    /**
     * 取消订阅内容。如果使用此方法，最好确认只有你的插件在使用此{@code namespace}。
     *
     * @throws IllegalArgumentException 任意参数为<tt>null</tt>
     */
    void unsubscribe(String namespace);

    /**
     * 取消订阅内容。使{@code listener}取消订阅{@code namespace}的内容。
     *
     * @throws IllegalArgumentException 任意参数为<tt>null</tt>
     */
    void unsubscribe(ChannelListener listener, String namespace);

    /**
     * 注销一个监听器，使所有关联其的订阅不再生效。
     *
     * @throws IllegalArgumentException 任意参数为<tt>null</tt>
     */
    void unregister(ChannelListener listener);

}
