package io.github.messycraft.localhostbridgecore.bungee.manager;

import io.github.messycraft.localhostbridgecore.bungee.Properties;
import io.github.messycraft.localhostbridgecore.bungee.entity.LSession;
import io.github.messycraft.localhostbridgecore.bungee.util.ChannelRegistrationUtil;
import io.github.messycraft.localhostbridgecore.bungee.util.SimpleUtil;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

@Deprecated
public class SessionManager {

    private final Plugin plugin;

    private Map<String, LSession> waitForReply = new HashMap<>();

    public SessionManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void addNewSession(String unique, String namespace, String seq) {
        waitForReply.put(seq, new LSession(unique, namespace, seq, System.currentTimeMillis()));
        SimpleUtil.runAsyncDelayAsLBC(() -> {
            if (waitForReply.remove(seq) != null && ChannelRegistrationUtil.getRegisteredChannel().containsKey(unique)) {
                ChannelRegistrationUtil.getRegisteredChannel().get(unique).increaseSessionExpireCount();
            }
        }, Properties.SESSION_LIFETIME);
    }

}
