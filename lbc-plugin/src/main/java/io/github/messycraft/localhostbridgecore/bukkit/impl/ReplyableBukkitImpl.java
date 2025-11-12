package io.github.messycraft.localhostbridgecore.bukkit.impl;

import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.messycraft.localhostbridgecore.api.subscribe.Replyable;
import io.github.messycraft.localhostbridgecore.bukkit.util.SimpleUtil;

@SuppressWarnings("SpellCheckingInspection")
public class ReplyableBukkitImpl implements Replyable {

    private final User user;
    private final String logSuffix;
    private boolean valid = true;

    ReplyableBukkitImpl(User user, String seq) {
        this.user = user;
        this.logSuffix = "[" + seq + "]";
    }

    @Override
    public synchronized boolean reply(String content) {
        if (user == null) {
            throw new UnsupportedOperationException();
        }
        if (!valid || !ChannelHelper.isOpen(user.getChannel())) {
            return false;
        }
        SimpleUtil.debug("Reply " + logSuffix + " -> " + content);
        user.sendPacketSilently(new WrapperStatusServerResponse(wrapData(content)));
        valid = false;
        return true;
    }

    private static JsonObject wrapData(String data) {
        JsonObject json = new JsonObject();
        json.add("d", new JsonPrimitive(data));
        return json;
    }

}
