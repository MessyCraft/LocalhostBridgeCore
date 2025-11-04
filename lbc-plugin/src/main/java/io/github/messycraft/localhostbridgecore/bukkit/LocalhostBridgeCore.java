package io.github.messycraft.localhostbridgecore.bukkit;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.messycraft.localhostbridgecore.api.LocalhostBridgeCoreAPIProvider;
import io.github.messycraft.localhostbridgecore.bukkit.command.InfoCommand;
import io.github.messycraft.localhostbridgecore.bukkit.command.ReloadCommand;
import io.github.messycraft.localhostbridgecore.bukkit.impl.LBCAPIBukkitImpl;
import io.github.messycraft.localhostbridgecore.bukkit.packetlistener.MessageReceiveListener;
import io.github.messycraft.localhostbridgecore.bukkit.util.HttpClientUtil;
import io.github.messycraft.localhostbridgecore.bukkit.util.SimpleUtil;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public final class LocalhostBridgeCore extends JavaPlugin {

    @Getter
    private static LocalhostBridgeCore instance;

    @Override
    @SuppressWarnings("all")
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().checkForUpdates(false).debug(false);
        PacketEvents.getAPI().load();
        // Register events
        PacketEvents.getAPI().getEventManager().registerListener(new MessageReceiveListener(this));
    }

    @Override
    public void onEnable() {
        PacketEvents.getAPI().init();
        instance = this;
        saveDefaultConfig();
        reloadConfig();

        getCommand("lbc-info").setExecutor(new InfoCommand());
        getCommand("lbc-reload").setExecutor(new ReloadCommand());

        getLogger().info("Plugin has been enabled.");
        getLogger().info("Type 'lbc-info' on console to get more!");

        LocalhostBridgeCoreAPIProvider.setAPI(new LBCAPIBukkitImpl());
    }

    @Override
    public void onDisable() {
        LocalhostBridgeCoreAPIProvider.setAPI(null);
        PacketEvents.getAPI().terminate();
        getLogger().info("Done.");
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        String unique = SimpleUtil.getUnique();
        HttpClientUtil.resetTimeoutFromConfig();
        if (unique == null || unique.equals("default")) {
            getLogger().warning("请在配置文件中更改默认频道名后使用 /lbc-reload 重新连接");
        }
        else {
            getLogger().info("配置已重载, 尝试注册中... " + HttpClientUtil.register());
        }
    }

}
