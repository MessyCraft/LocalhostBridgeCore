package io.github.messycraft.localhostbridgecore.bungee.command;

import io.github.messycraft.localhostbridgecore.api.LocalhostBridgeCoreAPIProvider;
import io.github.messycraft.localhostbridgecore.bungee.LocalhostBridgeCore;
import io.github.messycraft.localhostbridgecore.bungee.Properties;
import io.github.messycraft.localhostbridgecore.bungee.entity.LChannel;
import io.github.messycraft.localhostbridgecore.bungee.util.ChannelRegistrationUtil;
import io.github.messycraft.localhostbridgecore.bungee.manager.HttpServerManager;
import io.github.messycraft.localhostbridgecore.bungee.util.SimpleUtil;
import io.github.messycraft.localhostbridgecore.bungee.util.YamlConfigurationUtil;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.PluginDescription;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Arrays;
import java.util.Collections;

public class MainCommand extends Command implements TabExecutor {

    public MainCommand() {
        super("lbc", "localhostbridgecore.admin", "localhostbridgecore");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendTitle(sender);
            SimpleUtil.sendTextMessage(sender, "输入 &n/lbc help&r 了解更多.");
            return;
        }
        String subcommand = args[0];
        if (subcommand.equalsIgnoreCase("help")) {
            sendHelp(sender);
            return;
        }
        if (subcommand.equalsIgnoreCase("status")) {
            sendLine(sender);
            SimpleUtil.sendTextMessage(sender, "&b运行状态: " + (HttpServerManager.isRunning() ? "&a&lRunning" : "&c&lOffline"));
            SimpleUtil.sendTextMessage(sender, "&b运行时间: " + (HttpServerManager.isRunning() ? String.format("&e%.2f hours", (System.currentTimeMillis() - HttpServerManager.getStartMillis()) / (1000 * 60 * 60.0)) : "&7N/A"));
            SimpleUtil.sendTextMessage(sender, "&b调试模式: " + (Properties.DEBUG ? "&e是" : "&e否"));
            SimpleUtil.sendTextMessage(sender, "&b展示警告: " + (Properties.SHOW_WARNINGS ? "&e是" : "&e否"));
            SimpleUtil.sendTextMessage(sender, "&b已注册频道:");
            SimpleUtil.sendRichMessage(sender, "&7- &f&lBC&7:" + Properties.BIND_PORT + " &3本端", "点击测试频道连接: BC", "/lbc hello BC");
            for (LChannel c : ChannelRegistrationUtil.getRegisteredChannel().values()) {
                SimpleUtil.sendRichMessage(sender, "&7- &f&l" + c.getUnique() + "&7:" + c.getPort() + " &3连接失败次数[" + c.getPingFailCount() + "] 会话过期次数[" + c.getSessionExpireCount() + "]", "点击测试频道连接: " + c.getUnique(), "/lbc hello " + c.getUnique());
            }
            sendLine(sender);
            return;
        }
        if (subcommand.equalsIgnoreCase("hello")) {
            if (args.length == 1) {
                //
            }
            else if (args.length == 2) {
                //
            }
            else {
                sendWrongArguments(sender);
                return;
            }
            // TODO 11/6
            return;
        }
        if (subcommand.equalsIgnoreCase("restart")) {
            SimpleUtil.sendTextMessage(sender, "&c正在重启服务...");
            SimpleUtil.runAsyncAsLBC(() -> {
                YamlConfigurationUtil.reloadConfig();
                Properties.fromFile();
                ChannelRegistrationUtil.loadData();
                HttpServerManager.shutdown();
                HttpServerManager.start(LocalhostBridgeCore.getInstance().getLogger());
                SimpleUtil.sendTextMessage(sender, "&2重启命令已执行完成");
            });
            return;
        }
        if (subcommand.equalsIgnoreCase("register")) {
            if (args.length != 3) {
                sendWrongArguments(sender);
                return;
            }
            if (ChannelRegistrationUtil.isRegistered(args[1])) {
                SimpleUtil.sendTextMessage(sender, "&c注册失败: 频道名 " + args[1] + " 已被注册");
                return;
            }
            int port;
            try {
                port = Integer.parseInt(args[2]);
                if (port < 1024 || port > 49151) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                SimpleUtil.sendTextMessage(sender, "&c注册失败: 端口号 " + args[2] + " 非有效数字(1024~49151)");
                return;
            }
            if (ChannelRegistrationUtil.registerChannel(args[1], port)) {
                SimpleUtil.sendTextMessage(sender, "&2频道 " + args[1] + ":" + port + " 注册成功!");
            }
            else {
                SimpleUtil.sendTextMessage(sender, "&c注册失败: 名称包含特殊字符或其他原因");
            }
            return;
        }
        if (subcommand.equalsIgnoreCase("unregister")) {
            if (args.length != 2) {
                sendWrongArguments(sender);
                return;
            }
            if (ChannelRegistrationUtil.unregisterChannel(args[1])) {
                SimpleUtil.sendTextMessage(sender, "&2频道 " + args[1] + " 已被注销!");
            }
            else {
                SimpleUtil.sendTextMessage(sender, "&c注销失败: 频道 " + args[1] + " 不存在");
            }
            return;
        }
        if (subcommand.equalsIgnoreCase("send")) {
            if (args.length != 4) {
                sendWrongArguments(sender);
                return;
            }
            if (!ChannelRegistrationUtil.isRegistered(args[1])) {
                SimpleUtil.sendTextMessage(sender, "&c发送失败: 频道 " + args[1] + " 不存在");
                return;
            }
            if (!SimpleUtil.nameMatches(args[2])) {
                SimpleUtil.sendTextMessage(sender, "&c发送失败: namespace 包含特殊字符");
                return;
            }
            LocalhostBridgeCoreAPIProvider.getAPI().send(args[1], args[2], args[3]);
            SimpleUtil.sendTextMessage(sender, "&a已向 " + args[1] + " 发送一条消息");
            return;
        }
        if (subcommand.equalsIgnoreCase("send-r")) {
            return;
        }
        if (subcommand.equalsIgnoreCase("broadcast")) {
            return;
        }
        if (subcommand.equalsIgnoreCase("broadcast-r")) {
            return;
        }
        SimpleUtil.sendTextMessage(sender, "&4Error: Unknown subcommand '" + subcommand + "'");
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return SimpleUtil.tabPrefixFilter(Arrays.asList("status", "restart", "hello", "help", "register", "unregister", "send", "send-r", "broadcast", "broadcast-r"), args[0]);
        }
        else if (args.length == 2) {
            if (Arrays.asList("hello", "register", "unregister", "send", "send-r").contains(args[0])) {
                return SimpleUtil.tabPrefixFilter(ChannelRegistrationUtil.getRegisteredChannel().keySet(), args[1]);
            }
        }
        return Collections.emptyList();
    }

    private void sendHelp(CommandSender sender) {
        sendLine(sender);
        sendTitle(sender);
        SimpleUtil.sendTextMessage(sender, "");
        SimpleUtil.sendTextMessage(sender, "&b/lbc status  &3查看运行总览");
        SimpleUtil.sendTextMessage(sender, "&b/lbc restart  &3重启服务(且重载配置文件)");
        SimpleUtil.sendTextMessage(sender, "&b/lbc hello  &3批量检查频道响应状态");
        SimpleUtil.sendTextMessage(sender, "&b/lbc hello <channel>  &3检查指定频道响应状态");
        SimpleUtil.sendTextMessage(sender, "");
        SimpleUtil.sendTextMessage(sender, "&b/lbc register <channel> <port>  &3手动注册频道");
        SimpleUtil.sendTextMessage(sender, "&b/lbc unregister <channel>  &3手动注销频道");
        SimpleUtil.sendTextMessage(sender, "");
        SimpleUtil.sendTextMessage(sender, "&b/lbc send <channel> <namespace> <body>  &3定向发送一条消息");
        SimpleUtil.sendTextMessage(sender, "&b/lbc send-r <channel> <namespace> <body>  &3定向发送一条消息(需要回复)");
        SimpleUtil.sendTextMessage(sender, "&b/lbc broadcast <namespace> <body>  &3广播一条消息");
        SimpleUtil.sendTextMessage(sender, "&b/lbc broadcast-r <namespace> <body>  &3广播一条消息(需要回复)");
        SimpleUtil.sendTextMessage(sender, "");
        SimpleUtil.sendTextMessage(sender, "&3注: &o<port>&3 代表目标Minecraft服务器端口号");
        sendLine(sender);
    }

    private void sendLine(CommandSender sender) {
        SimpleUtil.sendTextMessage(sender, "&8&m                              &r");
    }

    private void sendTitle(CommandSender sender) {
        PluginDescription desc = LocalhostBridgeCore.getInstance().getDescription();
        SimpleUtil.sendTextMessage(sender, "&f&l" + desc.getName() + "&7 v" + desc.getVersion() + " by " + desc.getAuthor());
    }

    private void sendWrongArguments(CommandSender sender) {
        SimpleUtil.sendTextMessage(sender, "&4Error: Wrong arguments");
    }

}
