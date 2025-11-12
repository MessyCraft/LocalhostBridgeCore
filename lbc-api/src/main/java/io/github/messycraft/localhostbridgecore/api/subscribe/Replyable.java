package io.github.messycraft.localhostbridgecore.api.subscribe;

@SuppressWarnings("SpellCheckingInspection")
public interface Replyable {

    /**
     * 回复一条消息。只有最先回复的内容会被发送，此后将无效。
     *
     * @throws UnsupportedOperationException 如果具体内容附带的{@code needReply}为<tt>false</tt>
     * @return 是否回复成功。超时或多次回复都会导致失败。
     */
    boolean reply(String content);

}
