package io.github.messycraft.localhostbridgecore.bungee;

import io.github.messycraft.localhostbridgecore.bungee.manager.UpdaterManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class UpdaterProperties {

    private UpdaterProperties() {}

    public static final List<String> ACCESS_PLUGINS = new ArrayList<>();
    public static final Map<String, List<String>> SERVER_IGNORE_PLUGINS = new HashMap<>();
    public static final Map<String, List<String>> RELOAD_CONFIG_COMMAND = new HashMap<>();
    public static final Map<String, Map<String, String>> SERVER_PLACEHOLDER_MAPPING = new HashMap<>();

    public static void fromFile() {
        if (!Properties.PLUGIN_UPDATER_ENABLE) {
            return;
        }

        UpdaterManager updaterManager = LocalhostBridgeCore.getInstance().getUpdaterManager();
        if (updaterManager == null || !updaterManager.isRunning()) {
            return;
        }

        String sharedPath = Properties.PLUGIN_UPDATER_SHARED_PATH;
        File configFile = new File(sharedPath, "lbc-updater-config.yml");
        
        if (!configFile.exists()) {
            updaterManager.terminate();
            LocalhostBridgeCore.getInstance().getLogger().warning("Updater config file not found: " + configFile.getAbsolutePath());
            return;
        }

        try {
            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
            
            ACCESS_PLUGINS.clear();
            List<String> accessPlugins = config.getStringList("access-plugins");
            ACCESS_PLUGINS.addAll(accessPlugins);

            SERVER_IGNORE_PLUGINS.clear();
            Configuration ignoreSection = config.getSection("server-ignore-plugins");
            if (ignoreSection != null) {
                for (String serverName : ignoreSection.getKeys()) {
                    List<String> plugins = ignoreSection.getStringList(serverName);
                    if (plugins != null && !plugins.isEmpty()) {
                        SERVER_IGNORE_PLUGINS.put(serverName, new ArrayList<>(plugins));
                    }
                }
            }

            RELOAD_CONFIG_COMMAND.clear();
            Configuration reloadSection = config.getSection("reload-config-command");
            if (reloadSection != null) {
                for (String pluginName : reloadSection.getKeys()) {
                    List<String> commands = reloadSection.getStringList(pluginName);
                    if (commands != null && !commands.isEmpty()) {
                        RELOAD_CONFIG_COMMAND.put(pluginName, new ArrayList<>(commands));
                    }
                }
            }
            
            SERVER_PLACEHOLDER_MAPPING.clear();
            Configuration mappingSection = config.getSection("server-placeholder-mapping");
            if (mappingSection != null) {
                for (String pluginName : mappingSection.getKeys()) {
                    Configuration pluginSection = mappingSection.getSection(pluginName);
                    if (pluginSection != null) {
                        Map<String, String> mapping = new HashMap<>();
                        for (String serverName : pluginSection.getKeys()) {
                            String placeholderValue = pluginSection.getString(serverName);
                            if (placeholderValue != null) {
                                mapping.put(serverName, placeholderValue);
                            }
                        }
                        if (!mapping.isEmpty()) {
                            SERVER_PLACEHOLDER_MAPPING.put(pluginName, mapping);
                        }
                    }
                }
            }
            
            LocalhostBridgeCore.getInstance().getLogger().info("Updater configuration loaded successfully.");
            LocalhostBridgeCore.getInstance().getLogger().info("Access plugins: " + ACCESS_PLUGINS.size());
            LocalhostBridgeCore.getInstance().getLogger().info("Server ignore plugins: " + SERVER_IGNORE_PLUGINS.size());
            LocalhostBridgeCore.getInstance().getLogger().info("Reload commands: " + RELOAD_CONFIG_COMMAND.size());
            LocalhostBridgeCore.getInstance().getLogger().info("Server placeholder mappings: " + SERVER_PLACEHOLDER_MAPPING.size());
            
        } catch (IOException e) {
            updaterManager.terminate();
            LocalhostBridgeCore.getInstance().getLogger().log(Level.SEVERE, "Failed to load updater config", e);
        }
    }

}
