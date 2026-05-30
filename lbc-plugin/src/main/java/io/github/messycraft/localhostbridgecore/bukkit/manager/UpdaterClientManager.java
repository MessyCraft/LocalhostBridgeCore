package io.github.messycraft.localhostbridgecore.bukkit.manager;

import io.github.messycraft.localhostbridgecore.api.LocalhostBridgeCoreAPIProvider;
import io.github.messycraft.localhostbridgecore.api.subscribe.ChannelListener;
import io.github.messycraft.localhostbridgecore.api.subscribe.Replyable;
import io.github.messycraft.localhostbridgecore.bukkit.LocalhostBridgeCore;
import io.github.messycraft.localhostbridgecore.bukkit.util.SimpleUtil;
import io.github.messycraft.localhostbridgecore.common.dto.UpdaterCallbackDTO;
import io.github.messycraft.localhostbridgecore.common.dto.UpdaterResultDTO;
import io.github.messycraft.localhostbridgecore.common.util.GsonUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class UpdaterClientManager {

    private static final Method GET_PLUGIN_FILE_METHOD;
    private static final long MAX_CONFIG_FILE_SIZE = 1024 * 1024; // 1MB

    static {
        try {
            GET_PLUGIN_FILE_METHOD = JavaPlugin.class.getDeclaredMethod("getFile");
            GET_PLUGIN_FILE_METHOD.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private Listener delayRebootListener = null;

    public void init() {
        ChannelListener pushListener = new ChannelListener() {
            @Override
            public void onMessageReceive(String from, String namespace, String seq, String data, boolean needReply, Replyable replyable) {
                if (!"BC".equals(from)) {
                    return;
                }
                if (!SimpleUtil.isPluginUpdaterEnable()) {
                    if (needReply) {
                        replyable.reply(GsonUtil.GSON.toJson(new UpdaterCallbackDTO()));
                    }
                    return;
                }

                try {
                    UpdaterResultDTO resultDTO = GsonUtil.GSON.fromJson(data, UpdaterResultDTO.class);
                    UpdaterCallbackDTO callback = handleUpdateSyncWithCallback(resultDTO, false, true);
                    if (needReply) {
                        replyable.reply(GsonUtil.GSON.toJson(callback));
                    }
                } catch (Exception e) {
                    LocalhostBridgeCore.getInstance().getLogger().log(Level.SEVERE, "Failed to handle push update", e);
                    if (needReply) {
                        replyable.reply(GsonUtil.GSON.toJson(new UpdaterCallbackDTO(false, false, new ArrayList<>(), new ArrayList<>())));
                    }
                }
            }
        };
        ChannelListener rebootListener = new ChannelListener() {
            @Override
            public void onMessageReceive(String from, String namespace, String seq, String data, boolean needReply, Replyable replyable) {
                if (!"BC".equals(from)) {
                    return;
                }
                if (!SimpleUtil.isPluginUpdaterEnable()) {
                    if (needReply) {
                        replyable.reply(GsonUtil.GSON.toJson(new UpdaterCallbackDTO()));
                    }
                    return;
                }

                try {
                    UpdaterResultDTO resultDTO = GsonUtil.GSON.fromJson(data, UpdaterResultDTO.class);
                    UpdaterCallbackDTO callback = handleUpdateSyncWithCallback(resultDTO, true, true);
                    if (needReply) {
                        replyable.reply(GsonUtil.GSON.toJson(callback));
                    }
                } catch (Exception e) {
                    LocalhostBridgeCore.getInstance().getLogger().log(Level.SEVERE, "Failed to handle reboot update", e);
                    if (needReply) {
                        replyable.reply(GsonUtil.GSON.toJson(new UpdaterCallbackDTO(false, false, new ArrayList<>(), new ArrayList<>())));
                    }
                }
            }
        };
        LocalhostBridgeCoreAPIProvider.getAPI().getListenerManager().subscribe(pushListener, "lbc:PushPluginUpdater");
        LocalhostBridgeCoreAPIProvider.getAPI().getListenerManager().subscribe(rebootListener, "lbc:RebootPluginUpdater");

        if (SimpleUtil.isPluginUpdaterEnable()) {
            LocalhostBridgeCoreAPIProvider.getAPI().sendForReply("BC" ,"lbc:PluginUpdater", "", r -> {
                try {
                    UpdaterResultDTO resultDTO = GsonUtil.GSON.fromJson(r, UpdaterResultDTO.class);
                    check(resultDTO);
                } catch (Exception e) {
                    LocalhostBridgeCore.getInstance().getLogger().log(Level.SEVERE, "Failed to parse updater result from BC", e);
                }
            });
        }
    }

    private void check(UpdaterResultDTO resultDTO) {
        Bukkit.getScheduler().runTaskAsynchronously(LocalhostBridgeCore.getInstance(), () -> {
            if (resultDTO == null || !resultDTO.isEnable()) {
                LocalhostBridgeCore.getInstance().getLogger().info("Updater is not enabled.");
                return;
            }

            String sharedPath = resultDTO.getSharedPath();
            if (sharedPath == null || sharedPath.trim().isEmpty()) {
                LocalhostBridgeCore.getInstance().getLogger().warning("Updater shared-path is not configured!");
                return;
            }

            File repoDir = new File(sharedPath, "repo");

            if (!repoDir.exists()) {
                LocalhostBridgeCore.getInstance().getLogger().warning("Updater repo directory does not exist: " + repoDir.getAbsolutePath());
                return;
            }

            if (!repoDir.isDirectory()) {
                LocalhostBridgeCore.getInstance().getLogger().warning("Updater repo path is not a directory: " + repoDir.getAbsolutePath());
                return;
            }

            if (!repoDir.canRead()) {
                LocalhostBridgeCore.getInstance().getLogger().warning("Updater repo directory is not readable: " + repoDir.getAbsolutePath());
                return;
            }

            if (!repoDir.canWrite()) {
                LocalhostBridgeCore.getInstance().getLogger().warning("Updater repo directory is not writable: " + repoDir.getAbsolutePath());
                return;
            }

            LocalhostBridgeCore.getInstance().getLogger().info("Updater repo directory verified: " + repoDir.getAbsolutePath());

            handleUpdateSyncWithCallback(resultDTO, true, false);
        });
    }

    private Path getBackupPath(String sharedPath) {
        String serverName = LocalhostBridgeCoreAPIProvider.getAPI().getCurrentChannelName();
        return new File(sharedPath, "backup").toPath().resolve(serverName);
    }

    private UpdaterCallbackDTO handleUpdateSyncWithCallback(UpdaterResultDTO resultDTO, boolean updateJar, boolean trackFiles) {
        if (resultDTO == null || !resultDTO.isEnable()) {
            return new UpdaterCallbackDTO(false, false, new ArrayList<>(), new ArrayList<>());
        }
        
        File repoDir = new File(resultDTO.getSharedPath(), "repo");
        if (!repoDir.exists() || !repoDir.isDirectory()) {
            return new UpdaterCallbackDTO(false, false, new ArrayList<>(), new ArrayList<>());
        }
        
        boolean reboot = false;
        List<String> updatedConfigFiles = new ArrayList<>();
        List<String> updatedPluginFiles = new ArrayList<>();
        List<String> pluginsToReload = new ArrayList<>();
        
        for (Map.Entry<String, String> pluginEntry : resultDTO.getAccessPlugins().entrySet()) {
            String pluginName = pluginEntry.getKey();
            try {
                if (Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
                    Plugin currentPlugin = Bukkit.getPluginManager().getPlugin(pluginName);

                    // 更新 JAR
                    if (updateJar) {
                        if (updatePluginJar(currentPlugin, pluginName, pluginEntry.getValue(), repoDir, resultDTO.getSharedPath(), resultDTO.isBackupOnReplace())) {
                            reboot = true;
                            if (trackFiles && pluginEntry.getValue() != null) {
                                updatedPluginFiles.add(pluginEntry.getValue());
                            }
                        }
                    } else if (trackFiles) {
                        // 检查是否有可更新的 JAR（但不执行）
                        if (currentPlugin instanceof JavaPlugin && pluginEntry.getValue() != null) {
                            File usingJar = (File) GET_PLUGIN_FILE_METHOD.invoke(currentPlugin);
                            if (usingJar != null) {
                                File comparedJar = new File(repoDir, pluginEntry.getValue());
                                if (comparedJar.exists() && !sha256(comparedJar.toPath()).equals(sha256(usingJar.toPath()))) {
                                    updatedPluginFiles.add(pluginEntry.getValue());
                                }
                            }
                        }
                    }

                    // 同步插件文件夹
                    File comparedPluginFolder = new File(repoDir, pluginName);
                    if (comparedPluginFolder.exists() && comparedPluginFolder.isDirectory()) {
                        File currentPluginFolder = currentPlugin.getDataFolder();
                        if (!currentPluginFolder.exists()) {
                            // noinspection ResultOfMethodCallIgnored
                            currentPluginFolder.mkdirs();
                        }
                        
                        String placeholderValue = resultDTO.getServerPlaceholderMapping().getOrDefault(
                            pluginName, 
                            LocalhostBridgeCoreAPIProvider.getAPI().getCurrentChannelName()
                        );
                        
                        List<String> trackList = trackFiles ? new ArrayList<>() : null;
                        if (syncPluginFolder(comparedPluginFolder, currentPluginFolder, pluginName, placeholderValue, resultDTO.getSharedPath(), resultDTO.isBackupOnReplace(), trackList)) {
                            pluginsToReload.add(pluginName);
                            if (trackList != null) {
                                updatedConfigFiles.addAll(trackList);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LocalhostBridgeCore.getInstance().getLogger().log(Level.WARNING, "Failed to update plugin: " + pluginName, e);
            }
        }
        
        // 在主线程执行重载命令
        if (!pluginsToReload.isEmpty()) {
            Bukkit.getScheduler().runTask(LocalhostBridgeCore.getInstance(), () -> executeReloadCommands(pluginsToReload, resultDTO.getReloadConfigCommand()));
        }
        
        if (reboot) {
            boolean hasPlayers = !Bukkit.getOnlinePlayers().isEmpty();
            if (hasPlayers) {
                reboot = false;
                if (delayRebootListener == null) {
                    delayRebootListener = new Listener() {
                        @EventHandler
                        public void onQuit(PlayerQuitEvent event) {
                            Bukkit.getScheduler().runTaskLater(LocalhostBridgeCore.getInstance(), () -> {
                                if (Bukkit.getOnlinePlayers().isEmpty()) {
                                    HandlerList.unregisterAll(this);
                                    delayRebootListener = null;
                                    scheduleReboot();
                                }
                            }, 2L);
                        }
                    };
                    Bukkit.getScheduler().runTask(LocalhostBridgeCore.getInstance(), () -> {
                        Bukkit.getPluginManager().registerEvents(delayRebootListener, LocalhostBridgeCore.getInstance());
                        LocalhostBridgeCore.getInstance().getLogger().info("[UPDATER] 监听器已设置，待服务器内无玩家时自动重启更新");
                    });
                }
            }
            else scheduleReboot();
        }
        
        boolean hasUpdate = !updatedConfigFiles.isEmpty() || !updatedPluginFiles.isEmpty();
        return new UpdaterCallbackDTO(hasUpdate, reboot, updatedConfigFiles, updatedPluginFiles);
    }

    private void scheduleReboot() {
        LocalhostBridgeCore.getInstance().getLogger().info("[UPDATER] 服务器内无玩家，准备重启服务器以应用插件更新");
        // Bukkit.getScheduler().runTask(LocalhostBridgeCore.getInstance(), Bukkit.spigot()::restart);
        Bukkit.getScheduler().runTask(LocalhostBridgeCore.getInstance(), Bukkit::shutdown);
    }

    private boolean updatePluginJar(Plugin currentPlugin, String pluginName, String jarFileName, File repo, String sharedPath, boolean backupOnReplace) throws Exception {
        if (!(currentPlugin instanceof JavaPlugin)) {
            return false;
        }

        File usingJar = (File) GET_PLUGIN_FILE_METHOD.invoke(currentPlugin);
        
        if (usingJar == null) {
            LocalhostBridgeCore.getInstance().getLogger().warning("Failed to get JAR file for plugin: " + pluginName);
            return false;
        }
        
        // 若没有相关的最新插件文件则跳过
        if (jarFileName == null) {
            return false;
        }

        File comparedJar = new File(repo, jarFileName);
        if (!comparedJar.exists()) {
            LocalhostBridgeCore.getInstance().getLogger().warning("File " + jarFileName + " does not exist!");
            return false;
        }

        // 比较 SHA256，如果相同则无需更新
        if (sha256(comparedJar.toPath()).equals(sha256(usingJar.toPath()))) {
            return false;
        }

        // 如果已经在待重启队列了直接返回成功
        if (delayRebootListener != null) {
            return true;
        }

        // 执行更新
        String newFileName = comparedJar.getName().replace(".jar", "-" + System.currentTimeMillis() + ".jar");
        String backupInfo = "No backup";
        
        if (backupOnReplace) {
            Path backupPath = getBackupPath(sharedPath);
            Files.createDirectories(backupPath);
            Path backupJarPath = backupPath.resolve(usingJar.getName() + ".backup." + System.currentTimeMillis());
            Files.copy(usingJar.toPath(), backupJarPath, StandardCopyOption.REPLACE_EXISTING);
            backupInfo = "Backup: " + backupJarPath;
        }
        
        Files.copy(comparedJar.toPath(), usingJar.toPath().getParent().resolve(newFileName), StandardCopyOption.REPLACE_EXISTING);
        usingJar.deleteOnExit();
        
        LocalhostBridgeCore.getInstance().getLogger().info(String.format(
            "[UPDATER] JAR Updated: %s | Old: %s | New: %s | %s | Reboot required",
            pluginName, usingJar.getName(), newFileName, backupInfo
        ));
        
        return true;
    }

    private boolean syncPluginFolder(File comparedFolder, File currentFolder, String pluginName, String placeholderValue, String sharedPath, boolean backupOnReplace, List<String> updatedFiles) throws Exception {
        return syncFolderRecursive(comparedFolder, currentFolder, comparedFolder, pluginName, placeholderValue, sharedPath, backupOnReplace, updatedFiles);
    }

    private boolean syncFolderRecursive(File comparedRoot, File currentRoot, File comparedFolder, String pluginName, String placeholderValue, String sharedPath, boolean backupOnReplace, List<String> updatedFiles) throws Exception {
        File[] comparedFiles = comparedFolder.listFiles();
        if (comparedFiles == null) {
            return false;
        }

        boolean hasUpdates = false;

        for (File comparedFile : comparedFiles) {
            String relativePath = comparedRoot.toPath().relativize(comparedFile.toPath()).toString();
            File currentFile = new File(currentRoot, relativePath);

            if (comparedFile.isDirectory()) {
                // 如果是目录，递归处理
                if (!currentFile.exists()) {
                    // noinspection ResultOfMethodCallIgnored
                    currentFile.mkdirs();
                }
                if (syncFolderRecursive(comparedRoot, currentRoot, comparedFile, pluginName, placeholderValue, sharedPath, backupOnReplace, updatedFiles)) {
                    hasUpdates = true;
                }
            } else {
                // 如果是文件，检查是否需要更新
                if (currentFile.exists()) {
                    // 文件存在于两边，比较内容
                    if (!isFileContentEqual(comparedFile, currentFile, placeholderValue)) {
                        // 文件内容不同，需要更新
                        String backupInfo = "No backup";
                        if (backupOnReplace) {
                            Path backupPath = getBackupPath(sharedPath);
                            Files.createDirectories(backupPath);
                            Path backupFilePath = backupPath.resolve(pluginName + "_" + relativePath.replace(File.separator, "_") + ".backup." + System.currentTimeMillis());
                            Files.copy(currentFile.toPath(), backupFilePath, StandardCopyOption.REPLACE_EXISTING);
                            backupInfo = "Backup: " + backupFilePath;
                        }
                        copyFileWithPlaceholderReplacement(comparedFile, currentFile, placeholderValue);
                        hasUpdates = true;
                        if (updatedFiles != null) {
                            updatedFiles.add(pluginName + "/" + relativePath);
                        }
                        LocalhostBridgeCore.getInstance().getLogger().info(String.format(
                            "[UPDATER] File Updated: %s | Path: %s | %s",
                            pluginName, relativePath, backupInfo
                        ));
                    }
                } else {
                    // 文件只存在于 comparedFolder，复制到 currentFolder
                    Files.createDirectories(currentFile.toPath().getParent());
                    copyFileWithPlaceholderReplacement(comparedFile, currentFile, placeholderValue);
                    hasUpdates = true;
                    if (updatedFiles != null) {
                        updatedFiles.add(pluginName + "/" + relativePath);
                    }
                    LocalhostBridgeCore.getInstance().getLogger().info(String.format(
                        "[UPDATER] File Added: %s | Path: %s | New file from repo",
                        pluginName, relativePath
                    ));
                }
            }
        }
        
        return hasUpdates;
    }

    /**
     * 比较两个文件内容是否相等
     * 对于小于 32KB 的 YAML/JSON 文件，会先对源文件进行占位符替换后再进行字符串比较
     * 其他文件使用 SHA-256 比较
     */
    private boolean isFileContentEqual(File comparedFile, File currentFile, String placeholderValue) throws Exception {
        String fileName = comparedFile.getName().toLowerCase();
        
        // 对于小于 32KB 的 YAML/JSON 文件，使用字符串比较（支持占位符替换）
        if ((fileName.endsWith(".yml") || fileName.endsWith(".yaml") || fileName.endsWith(".json"))
            && Files.size(comparedFile.toPath()) <= MAX_CONFIG_FILE_SIZE
            && Files.size(currentFile.toPath()) <= MAX_CONFIG_FILE_SIZE) {
            
            String comparedContent = new String(Files.readAllBytes(comparedFile.toPath()), StandardCharsets.UTF_8);
            comparedContent = comparedContent.replace("$$LBC_SERVER_NAME$$", placeholderValue);
            
            String currentContent = new String(Files.readAllBytes(currentFile.toPath()), StandardCharsets.UTF_8);
            
            return comparedContent.equals(currentContent);
        }
        
        // 其他文件使用 SHA-256 比较
        return sha256(comparedFile.toPath()).equals(sha256(currentFile.toPath()));
    }

    private void copyFileWithPlaceholderReplacement(File sourceFile, File targetFile, String placeholderValue) throws Exception {
        String fileName = sourceFile.getName().toLowerCase();
        
        // 对于小于 32KB 的 YAML/JSON 文件，统一进行占位符替换处理
        if ((fileName.endsWith(".yml") || fileName.endsWith(".yaml") || fileName.endsWith(".json"))
            && Files.size(sourceFile.toPath()) <= MAX_CONFIG_FILE_SIZE) {
            
            String content = new String(Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);
            content = content.replace("$$LBC_SERVER_NAME$$", placeholderValue);
            Files.write(targetFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
        } else {
            // 其他文件直接复制
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void executeReloadCommands(List<String> pluginsToReload, Map<String, List<String>> reloadConfigCommand) {
        for (String pluginName : pluginsToReload) {
            List<String> commands = reloadConfigCommand.get(pluginName);
            if (commands != null && !commands.isEmpty()) {
                for (String command : commands) {
                    try {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                        LocalhostBridgeCore.getInstance().getLogger().info(String.format(
                            "[UPDATER] Reload Command Executed: %s | Command: %s",
                            pluginName, command
                        ));
                    } catch (Exception e) {
                        LocalhostBridgeCore.getInstance().getLogger().log(Level.WARNING, 
                            "Failed to execute reload command for plugin: " + pluginName + ", command: " + command, e);
                    }
                }
            }
        }
    }

    private String sha256(Path jarPath) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = new BufferedInputStream(
                Files.newInputStream(jarPath), 65536)) { // 64KB buffer
            byte[] buf = new byte[65536];
            int n;
            while ((n = is.read(buf)) != -1) {
                digest.update(buf, 0, n);
            }
        }
        // Java 8 没有 HexFormat，手动转十六进制
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder(hashBytes.length * 2);
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
