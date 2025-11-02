package io.github.messycraft.localhostbridgecore.bukkit.impl;

import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.messycraft.localhostbridgecore.api.subscribe.Replyable;

@SuppressWarnings("SpellCheckingInspection")
public class ReplyableBukkitImpl implements Replyable {

    private final User user;
    private boolean valid = true;

    ReplyableBukkitImpl(User user) {
        this.user = user;
    }

    @Override
    public synchronized boolean reply(String content) {
        if (user == null) {
            throw new UnsupportedOperationException();
        }
        if (!valid || !ChannelHelper.isOpen(user.getChannel())) {
            return false;
        }
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
