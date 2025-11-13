package io.github.messycraft.localhostbridgecore.bukkit.command;

import io.github.messycraft.localhostbridgecore.api.LocalhostBridgeCoreAPIProvider;
import io.github.messycraft.localhostbridgecore.bukkit.LocalhostBridgeCore;
import io.github.messycraft.localhostbridgecore.bukkit.util.SimpleUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class InfoCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            SimpleUtil.sendConsoleOnly(sender);
            return true;
        }
        Bukkit.getScheduler().runTaskAsynchronously(LocalhostBridgeCore.getInstance(), () -> {
            Plugin plugin = LocalhostBridgeCore.getInstance();
            List<String> channels = LocalhostBridgeCoreAPIProvider.getAPI().getRegisteredChannels();
            sender.sendMessage("");
            sender.sendMessage("LocalhostBridgeCore bukkit-side v" + plugin.getDescription().getVersion());
            sender.sendMessage("主机端口: " + SimpleUtil.getAccessPort());
            sender.sendMessage("本服端口: " + Bukkit.getPort());
            sender.sendMessage("当前频道: " + SimpleUtil.getUnique());
            sender.sendMessage("连接状态: " + (channels != null));
            sender.sendMessage("频道列表: " + channels);
            sender.sendMessage("调试模式: " + plugin.getConfig().getBoolean("debug"));
            sender.sendMessage("展示警告: " + plugin.getConfig().getBoolean("show-warnings", true));
            sender.sendMessage("");
        });
        return true;
    }

}
