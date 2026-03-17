package io.github.messycraft.localhostbridgecore.bungee.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class PluginInfoUtil {

    private PluginInfoUtil() {}

    /**
     * 从 ZIP/JAR 文件中读取 plugin.yml 的 name 和 version 字段
     * 
     * @param zipFile ZIP/JAR 文件
     * @return PluginInfo 对象，如果获取失败则返回 null
     */
    public static PluginInfo getPluginInfo(File zipFile) {
        if (zipFile == null || !zipFile.exists() || !zipFile.isFile()) {
            return null;
        }

        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry entry = zip.getEntry("plugin.yml");
            if (entry == null) {
                return null;
            }

            try (InputStream in = zip.getInputStream(entry)) {
                Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(in);
                
                String name = config.getString("name");
                String version = config.getString("version");
                
                if (name == null || version == null) {
                    return null;
                }
                
                return new PluginInfo(zipFile.getName(), name, version);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 插件信息结果类
     */
    @Getter
    @AllArgsConstructor
    public static class PluginInfo {
        private final String fileName;
        private final String name;
        private final String version;
    }

}
