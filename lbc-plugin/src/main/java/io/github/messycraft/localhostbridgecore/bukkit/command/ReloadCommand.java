package io.github.messycraft.localhostbridgecore.bukkit.command;

import io.github.messycraft.localhostbridgecore.bukkit.LocalhostBridgeCore;
import io.github.messycraft.localhostbridgecore.bukkit.util.SimpleUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public class ReloadCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            SimpleUtil.sendConsoleOnly(sender);
            return true;
        }
        sender.sendMessage("Reloading...");
        Bukkit.getScheduler().runTaskAsynchronously(LocalhostBridgeCore.getInstance(), () -> LocalhostBridgeCore.getInstance().reloadConfig());
        return true;
    }

}
