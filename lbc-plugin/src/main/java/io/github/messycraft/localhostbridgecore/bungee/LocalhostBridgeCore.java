package io.github.messycraft.localhostbridgecore.bungee;

import io.github.messycraft.localhostbridgecore.api.LocalhostBridgeCoreAPIProvider;
import io.github.messycraft.localhostbridgecore.bungee.command.MainCommand;
import io.github.messycraft.localhostbridgecore.bungee.impl.LBCAPIBungeeImpl;
import io.github.messycraft.localhostbridgecore.bungee.manager.SessionManager;
import io.github.messycraft.localhostbridgecore.bungee.util.ChannelRegistrationUtil;
import io.github.messycraft.localhostbridgecore.bungee.manager.HttpServerManager;
import io.github.messycraft.localhostbridgecore.bungee.util.SimpleUtil;
import io.github.messycraft.localhostbridgecore.bungee.util.YamlConfigurationUtil;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;

public final class LocalhostBridgeCore extends Plugin {

    @Getter
    private static LocalhostBridgeCore instance;

    @Override
    public void onEnable() {
        instance = this;
        YamlConfigurationUtil.saveDefaultConfig();
        YamlConfigurationUtil.reloadConfig();
        Properties.fromFile();
        ChannelRegistrationUtil.loadData();

        getProxy().getPluginManager().registerCommand(this, new MainCommand());

        getLogger().info("Plugin has been enabled. Loaded " + (ChannelRegistrationUtil.getRegisteredChannel().size() + 1) + " channels.");
        getLogger().info("Type 'lbc help' on console to get more!");

        SimpleUtil.runAsyncAsLBC(() -> HttpServerManager.start(getLogger()));
        LocalhostBridgeCoreAPIProvider.setAPI(new LBCAPIBungeeImpl());
    }

    @Override
    public void onDisable() {
        LocalhostBridgeCoreAPIProvider.setAPI(null);
        HttpServerManager.shutdown();
        instance = null;
        ChannelRegistrationUtil.getRegisteredChannel().values().forEach(c -> c.setValid(false));
        ChannelRegistrationUtil.getRegisteredChannel().clear();
        getLogger().info("Done.");
    }

}
