package io.github.messycraft.localhostbridgecore.bungee;

import io.github.messycraft.localhostbridgecore.bungee.util.YamlConfigurationUtil;

public class Properties {

    private Properties() {}

    public static int BIND_PORT = -1;
    public static boolean ALLOW_REGISTER;
    public static int TIMEOUT = -1;
    public static int SESSION_LIFETIME = -1;
    public static boolean DEBUG;
    public static boolean SHOW_WARNINGS = true;


    public static void fromFile() {
        BIND_PORT = YamlConfigurationUtil.getConfig().getInt("bind-port", -1);
        ALLOW_REGISTER = YamlConfigurationUtil.getConfig().getBoolean("allow-register", false);
        TIMEOUT = YamlConfigurationUtil.getConfig().getInt("timeout", -1);
        SESSION_LIFETIME = YamlConfigurationUtil.getConfig().getInt("session-lifetime", -1);
        DEBUG = YamlConfigurationUtil.getConfig().getBoolean("debug", false);
        SHOW_WARNINGS = YamlConfigurationUtil.getConfig().getBoolean("show-warnings", true);
    }

}
