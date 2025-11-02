package io.github.messycraft.localhostbridgecore.bungee.util;

import io.github.messycraft.localhostbridgecore.bungee.LocalhostBridgeCore;
import lombok.Getter;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

public final class YamlConfigurationUtil {

    @Getter
    private static Configuration config;

    private YamlConfigurationUtil() {}

    public static void saveDefaultConfig() {
        if (!LocalhostBridgeCore.getInstance().getDataFolder().exists()) {
            LocalhostBridgeCore.getInstance().getDataFolder().mkdir();
        }
        File file = new File(LocalhostBridgeCore.getInstance().getDataFolder(),"config.yml");
        if (!file.exists()) {
            try (InputStream in = LocalhostBridgeCore.getInstance().getResourceAsStream("config-bungee.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                LocalhostBridgeCore.getInstance().getLogger().log(Level.SEVERE, "saveDefaultConfig", e);
            }
        }
    }

    public static void reloadConfig() {
        File file = new File(LocalhostBridgeCore.getInstance().getDataFolder(),"config.yml");
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            LocalhostBridgeCore.getInstance().getLogger().log(Level.SEVERE, "reloadConfig", e);
        }
    }

    static Configuration loadDataFile() {
        try {
            File file = new File(LocalhostBridgeCore.getInstance().getDataFolder(), "registered.yml");
            if (file.exists()) {
                return ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
            }
        } catch (IOException e) {
            LocalhostBridgeCore.getInstance().getLogger().log(Level.SEVERE, "loadData", e);
        }
        return new Configuration();
    }

    static void saveDataFile(Configuration registered) {
        try {
            File file = new File(LocalhostBridgeCore.getInstance().getDataFolder(), "registered.yml");
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(registered, file);
        } catch (IOException e) {
            LocalhostBridgeCore.getInstance().getLogger().log(Level.SEVERE, "saveData", e);
        }
    }

}
