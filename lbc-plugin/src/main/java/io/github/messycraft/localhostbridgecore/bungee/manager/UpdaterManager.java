package io.github.messycraft.localhostbridgecore.bungee.manager;

import io.github.messycraft.localhostbridgecore.api.LocalhostBridgeCoreAPIProvider;
import io.github.messycraft.localhostbridgecore.api.subscribe.ChannelListener;
import io.github.messycraft.localhostbridgecore.api.subscribe.Replyable;
import io.github.messycraft.localhostbridgecore.bungee.LocalhostBridgeCore;
import io.github.messycraft.localhostbridgecore.bungee.Properties;
import io.github.messycraft.localhostbridgecore.bungee.UpdaterProperties;
import io.github.messycraft.localhostbridgecore.bungee.util.PluginInfoUtil;
import io.github.messycraft.localhostbridgecore.common.dto.UpdaterResultDTO;
import io.github.messycraft.localhostbridgecore.common.util.GsonUtil;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Getter
public class UpdaterManager {

    private boolean isRunning;

    public UpdaterManager() {
        ChannelListener updateRequestListener = new ChannelListener() {
            @Override
            public void onMessageReceive(String from, String namespace, String seq, String data, boolean needReply, Replyable replyable) {
                if (needReply) {
                    replyable.reply(GsonUtil.GSON.toJson(isRunning ? generateUpdaterResult(from) : new UpdaterResultDTO()));
                }
            }
        };
        LocalhostBridgeCoreAPIProvider.getAPI().getListenerManager().subscribe(updateRequestListener, "lbc:PluginUpdater");
    }

    public UpdaterResultDTO generateUpdaterResult(String targetServer) {
        File repoDir = new File(Properties.PLUGIN_UPDATER_SHARED_PATH, "repo");
        Map<String, PluginInfoUtil.PluginInfo> pluginInfoMap = new HashMap<>();
        Map<String, Integer> pluginCountMap = new HashMap<>();

        if (repoDir.exists() && repoDir.isDirectory()) {
            File[] jarFiles = repoDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));

            if (jarFiles != null) {
                for (File jarFile : jarFiles) {
                    PluginInfoUtil.PluginInfo info = PluginInfoUtil.getPluginInfo(jarFile);
                    if (info != null) {
                        String pluginName = info.getName();
                        pluginCountMap.put(pluginName, pluginCountMap.getOrDefault(pluginName, 0) + 1);

                        if (pluginCountMap.get(pluginName) > 1) {
                            pluginInfoMap.remove(pluginName);
                            LocalhostBridgeCore.getInstance().getLogger().warning(
                                    "Multiple jar files found for plugin '" + pluginName + "' in repo directory. This plugin will be skipped."
                            );
                        } else {
                            pluginInfoMap.put(pluginName, info);
                        }
                    }
                    else {
                        LocalhostBridgeCore.getInstance().getLogger().warning("File " + jarFile.getName() + " is not a valid java plugin.");
                    }
                }
            }
        }

        Map<String, String> accessPlugins = new HashMap<>();
        List<String> ignorePlugins = UpdaterProperties.SERVER_IGNORE_PLUGINS.getOrDefault(targetServer, Collections.emptyList());
        for (String pluginName : UpdaterProperties.ACCESS_PLUGINS) {
            if (ignorePlugins.contains(pluginName)) {
                continue;
            }
            PluginInfoUtil.PluginInfo info = pluginInfoMap.get(pluginName);
            if (info != null) {
                accessPlugins.put(pluginName, info.getFileName());
            }
            else {
                accessPlugins.put(pluginName, null);
            }
        }

        Map<String, String> serverPlaceholderMapping = new HashMap<>();
        UpdaterProperties.SERVER_PLACEHOLDER_MAPPING.forEach((plugin, serverAliases) -> {
            if (serverAliases.containsKey(targetServer)) {
                serverPlaceholderMapping.put(plugin, serverAliases.get(targetServer));
            }
        });

        return new UpdaterResultDTO(
                true,
                Properties.PLUGIN_UPDATER_SHARED_PATH,
                Properties.PLUGIN_UPDATER_BACKUP_ON_REPLACE,
                accessPlugins,
                UpdaterProperties.RELOAD_CONFIG_COMMAND,
                serverPlaceholderMapping
        );
    }

    public void init() {
        isRunning = false;

        if (!Properties.PLUGIN_UPDATER_ENABLE) {
            LocalhostBridgeCore.getInstance().getLogger().info("Plugin updater is disabled.");
            return;
        }

        String sharedPath = Properties.PLUGIN_UPDATER_SHARED_PATH;
        if (sharedPath == null || sharedPath.trim().isEmpty()) {
            LocalhostBridgeCore.getInstance().getLogger().warning("Plugin updater is enabled but shared-path is not configured!");
            return;
        }

        File sharedDir = new File(sharedPath);
        
        if (!sharedDir.exists()) {
            LocalhostBridgeCore.getInstance().getLogger().warning("Shared path does not exist: " + sharedPath);
            return;
        }

        if (!sharedDir.isDirectory()) {
            LocalhostBridgeCore.getInstance().getLogger().warning("Shared path is not a directory: " + sharedPath);
            return;
        }

        if (!sharedDir.canRead()) {
            LocalhostBridgeCore.getInstance().getLogger().warning("Shared path is not readable: " + sharedPath);
            return;
        }

        if (!sharedDir.canWrite()) {
            LocalhostBridgeCore.getInstance().getLogger().warning("Shared path is not writable: " + sharedPath);
            return;
        }

        LocalhostBridgeCore.getInstance().getLogger().info("Plugin updater shared path verified: " + sharedPath);

        copyUpdaterConfigToSharedPath(sharedDir);
        // noinspection ResultOfMethodCallIgnored
        new File(sharedDir, "repo").mkdir();

        isRunning = true;
        
        UpdaterProperties.fromFile();
    }

    private void copyUpdaterConfigToSharedPath(File sharedDir) {
        File targetFile = new File(sharedDir, "lbc-updater-config.yml");
        
        if (targetFile.exists()) {
            LocalhostBridgeCore.getInstance().getLogger().info("Updater config file already exists in shared path, skipping copy.");
            return;
        }

        try (InputStream in = LocalhostBridgeCore.getInstance().getResourceAsStream("lbc-updater-config.yml")) {
            if (in == null) {
                LocalhostBridgeCore.getInstance().getLogger().warning("Could not find lbc-updater-config.yml in plugin resources!");
                return;
            }
            Files.copy(in, targetFile.toPath());
            LocalhostBridgeCore.getInstance().getLogger().info("Successfully copied lbc-updater-config.yml to shared path.");
        } catch (IOException e) {
            LocalhostBridgeCore.getInstance().getLogger().log(Level.SEVERE, "Failed to copy updater config to shared path", e);
        }
    }

    public void terminate() {
        isRunning = false;
    }
}
