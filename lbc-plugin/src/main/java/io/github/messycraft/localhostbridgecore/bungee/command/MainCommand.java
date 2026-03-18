package io.github.messycraft.localhostbridgecore.bungee.command;

import io.github.messycraft.localhostbridgecore.api.LocalhostBridgeCoreAPIProvider;
import io.github.messycraft.localhostbridgecore.bungee.LocalhostBridgeCore;
import io.github.messycraft.localhostbridgecore.bungee.Properties;
import io.github.messycraft.localhostbridgecore.bungee.entity.LChannel;
import io.github.messycraft.localhostbridgecore.bungee.manager.HttpServerManager;
import io.github.messycraft.localhostbridgecore.bungee.util.ChannelRegistrationUtil;
import io.github.messycraft.localhostbridgecore.bungee.util.SimpleUtil;
import io.github.messycraft.localhostbridgecore.bungee.util.YamlConfigurationUtil;
import io.github.messycraft.localhostbridgecore.common.dto.UpdaterCallbackDTO;
import io.github.messycraft.localhostbridgecore.common.dto.UpdaterResultDTO;
import io.github.messycraft.localhostbridgecore.common.util.GsonUtil;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.PluginDescription;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
            SimpleUtil.sendTextMessage(sender, "&7- &f&lBC&7:" + Properties.BIND_PORT + " &3本端");
            for (LChannel c : ChannelRegistrationUtil.getRegisteredChannel().values()) {
                SimpleUtil.sendRichMessage(sender, "&7- &f&l" + c.getUnique() + "&7:" + c.getPort() + " &3连接失败次数[" + c.getPingFailCount() + "] 会话过期次数[" + c.getSessionExpireCount() + "]", "点击测试频道连接: " + c.getUnique(), "/lbc hello " + c.getUnique());
            }
            sendLine(sender);
            return;
        }
        if (subcommand.equalsIgnoreCase("hello")) {
            if (args.length == 1) {
                if (ChannelRegistrationUtil.getRegisteredChannel().isEmpty()) {
                    SimpleUtil.sendTextMessage(sender, "&c发送失败: 你还没有任何可用频道!");
                    return;
                }
                final int count = ChannelRegistrationUtil.getRegisteredChannel().size();
                AtomicInteger completed = new AtomicInteger();
                sendLine(sender);
                for (LChannel c : ChannelRegistrationUtil.getRegisteredChannel().values()) {
                    SimpleUtil.runAsyncAsLBC(() -> {
                        long ping = c.sendHelloInCurrentThread();
                        SimpleUtil.sendTextMessage(sender, "&7- &f&l" + c.getUnique() + "&7:" + c.getPort() + (ping == -1 ? " &c超时" : String.format(" &e%.2fms", ping / 1000_000.0)) + String.format("&3 (%d/%d)", completed.incrementAndGet(), count));
                        if (completed.get() == count) sendLine(sender);
                    });
                }
            }
            else if (args.length == 2) {
                if (args[1].equals("BC")) {
                    sendLine(sender);
                    SimpleUtil.sendTextMessage(sender, "&7- &f&lBC&7:" + Properties.BIND_PORT + " &3本端");
                    sendLine(sender);
                    return;
                }
                LChannel c = ChannelRegistrationUtil.getRegisteredChannel().get(args[1]);
                if (c == null) {
                    SimpleUtil.sendTextMessage(sender, "&c发送失败: 频道 " + args[1] + " 不存在");
                    return;
                }
                SimpleUtil.runAsyncAsLBC(() -> {
                    sendLine(sender);
                    long ping = c.sendHelloInCurrentThread();
                    SimpleUtil.sendTextMessage(sender, "&7- &f&l" + c.getUnique() + "&7:" + c.getPort() + (ping == -1 ? " &c超时" : String.format(" &e%.2fms", ping / 1000_000.0)));
                    sendLine(sender);
                });
            }
            else {
                sendWrongArguments(sender);
            }
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
                LocalhostBridgeCore.getInstance().getUpdaterManager().init();
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
            if (args.length < 4) {
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
            StringBuilder data = new StringBuilder();
            for (int i = 3; i < args.length; i++) {
                if (i > 3) data.append(" ");
                data.append(args[i]);
            }
            LocalhostBridgeCoreAPIProvider.getAPI().send(args[1], args[2], data.toString());
            SimpleUtil.sendTextMessage(sender, "&a已向 " + args[1] + " 发送一条消息");
            return;
        }
        if (subcommand.equalsIgnoreCase("send-r")) {
            if (args.length < 4) {
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
            StringBuilder data = new StringBuilder();
            for (int i = 3; i < args.length; i++) {
                if (i > 3) data.append(" ");
                data.append(args[i]);
            }
            LocalhostBridgeCoreAPIProvider.getAPI().sendForReply(args[1], args[2], data.toString(),
                    r -> SimpleUtil.sendTextMessage(sender, "&b" + args[1] + " 回复 &7-> &e" + r),
                    () -> SimpleUtil.sendRichMessage(sender, "&c" + args[1] + ": 接收回复超时或发送失败 &7(点击测试)", "点击测试频道连接: " + args[1], "/lbc hello " + args[1])
            );
            SimpleUtil.sendTextMessage(sender, "&a已向 " + args[1] + " 发送一条消息 (等待回复中...)");
            return;
        }
        if (subcommand.equalsIgnoreCase("broadcast")) {
            if (args.length < 3) {
                sendWrongArguments(sender);
                return;
            }
            if (!SimpleUtil.nameMatches(args[1])) {
                SimpleUtil.sendTextMessage(sender, "&c发送失败: namespace 包含特殊字符");
                return;
            }
            StringBuilder data = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                if (i > 2) data.append(" ");
                data.append(args[i]);
            }
            LocalhostBridgeCoreAPIProvider.getAPI().broadcast(args[1], data.toString());
            SimpleUtil.sendTextMessage(sender, "&a已在群组中广播一条消息");
            return;
        }
        if (subcommand.equalsIgnoreCase("broadcast-r")) {
            if (args.length < 3) {
                sendWrongArguments(sender);
                return;
            }
            if (!SimpleUtil.nameMatches(args[1])) {
                SimpleUtil.sendTextMessage(sender, "&c发送失败: namespace 包含特殊字符");
                return;
            }
            StringBuilder data = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                if (i > 2) data.append(" ");
                data.append(args[i]);
            }
            LocalhostBridgeCoreAPIProvider.getAPI().broadcastForWaitReply(args[1], data.toString(), r -> {
                sendLine(sender);
                for (Map.Entry<String, String> e : r.entrySet()) {
                    SimpleUtil.sendTextMessage(sender, "&b" + e.getKey() + " 回复 &7-> &e" + r);
                }
                if (r.isEmpty()) {
                    SimpleUtil.sendRichMessage(sender, "&c广播: 未收到任何回复 &7(点击测试)", "点击测试频道连接", "/lbc hello");
                }
                sendLine(sender);
            });
            SimpleUtil.sendTextMessage(sender, "&a已在群组中广播一条消息 (等待各服务端回复中...)");
            return;
        }
        if (subcommand.equalsIgnoreCase("updater")) {
            handleUpdaterCommand(sender, args);
            return;
        }
        SimpleUtil.sendTextMessage(sender, "&4Error: Unknown subcommand '" + subcommand + "'");
    }

    private void handleUpdaterCommand(CommandSender sender, String[] args) {
        if (!LocalhostBridgeCore.getInstance().getUpdaterManager().isRunning()) {
            SimpleUtil.sendTextMessage(sender, "&4Error: 更新器未运行。你可以在确保功能启用的情况下，使用 lbc restart 重启服务。");
            return;
        }

        if (args.length < 2) {
            sendLine(sender);
            SimpleUtil.sendTextMessage(sender, "&b/lbc updater push  &3立即推送插件配置文件更新并执行热重载");
            SimpleUtil.sendTextMessage(sender, "&b/lbc updater push <server>  &3立即推送指定服务器插件配置文件更新并执行热重载");
            SimpleUtil.sendTextMessage(sender, "&b/lbc updater reboot  &3重启所有需要更新的子服以应用更新");
            SimpleUtil.sendTextMessage(sender, "&b/lbc updater reboot <server>  &3重启指定需要更新的子服以应用更新");
            sendLine(sender);
            return;
        }

        String updaterSubcommand = args[1];

        if (updaterSubcommand.equalsIgnoreCase("push")) {
            if (args.length == 2) {
                handleUpdaterPushAll(sender);
            } else if (args.length == 3) {
                handleUpdaterPushServer(sender, args[2]);
            } else {
                sendWrongArguments(sender);
            }
            return;
        }

        if (updaterSubcommand.equalsIgnoreCase("reboot")) {
            if (args.length == 2) {
                handleUpdaterRebootAll(sender);
            } else if (args.length == 3) {
                handleUpdaterRebootServer(sender, args[2]);
            } else {
                sendWrongArguments(sender);
            }
            return;
        }

        SimpleUtil.sendTextMessage(sender, "&4Error: Unknown updater subcommand '" + updaterSubcommand + "'");
    }

    private void handleUpdaterPushAll(CommandSender sender) {
        SimpleUtil.sendTextMessage(sender, "&e[Updater] 正在推送所有服务器的配置文件更新...");
        SimpleUtil.runAsyncAsLBC(() -> {
            for (String server : ChannelRegistrationUtil.getRegisteredChannel().keySet()) {
                UpdaterResultDTO updaterResultDTO = LocalhostBridgeCore.getInstance().getUpdaterManager().generateUpdaterResult(server);
                LocalhostBridgeCoreAPIProvider.getAPI().sendForReply(server, "lbc:PushPluginUpdater", GsonUtil.GSON.toJson(updaterResultDTO), r -> {
                    try {
                        UpdaterCallbackDTO updaterCallbackDTO = GsonUtil.GSON.fromJson(r, UpdaterCallbackDTO.class);
                        sendPushUpdateReport(sender, server, updaterCallbackDTO);
                    } catch (Exception e) {
                        SimpleUtil.sendTextMessage(sender, "&c[Updater] 服务器 &e" + server + "&c: 解析回复数据失败");
                        LocalhostBridgeCore.getInstance().getLogger().log(java.util.logging.Level.WARNING, "Failed to parse callback from " + server, e);
                    }
                }, () -> SimpleUtil.sendTextMessage(sender, "&c[Updater] 服务器 &e" + server + "&c: 推送失败或超时"));
            }
        });
    }

    private void handleUpdaterPushServer(CommandSender sender, String serverName) {
        if (!ChannelRegistrationUtil.isRegistered(serverName)) {
            SimpleUtil.sendTextMessage(sender, "&c[Updater] 服务器 &e" + serverName + "&c 不存在或未注册");
            return;
        }

        SimpleUtil.sendTextMessage(sender, "&e[Updater] 正在推送服务器 " + serverName + " 的配置文件更新...");
        SimpleUtil.runAsyncAsLBC(() -> {
            UpdaterResultDTO updaterResultDTO = LocalhostBridgeCore.getInstance().getUpdaterManager().generateUpdaterResult(serverName);
            LocalhostBridgeCoreAPIProvider.getAPI().sendForReply(serverName, "lbc:PushPluginUpdater", GsonUtil.GSON.toJson(updaterResultDTO), r -> {
                try {
                    UpdaterCallbackDTO updaterCallbackDTO = GsonUtil.GSON.fromJson(r, UpdaterCallbackDTO.class);
                    sendPushUpdateReport(sender, serverName, updaterCallbackDTO);
                } catch (Exception e) {
                    SimpleUtil.sendTextMessage(sender, "&c[Updater] 服务器 &e" + serverName + "&c: 解析回复数据失败");
                    LocalhostBridgeCore.getInstance().getLogger().log(java.util.logging.Level.WARNING, "Failed to parse callback from " + serverName, e);
                }
            }, () -> SimpleUtil.sendTextMessage(sender, "&c[Updater] 服务器 &e" + serverName + "&c: 推送失败或超时"));
        });
    }

    private void handleUpdaterRebootAll(CommandSender sender) {
        SimpleUtil.sendTextMessage(sender, "&e[Updater] 正在准备重启所有需要更新的子服...");
        SimpleUtil.runAsyncAsLBC(() -> {
            for (String server : ChannelRegistrationUtil.getRegisteredChannel().keySet()) {
                UpdaterResultDTO updaterResultDTO = LocalhostBridgeCore.getInstance().getUpdaterManager().generateUpdaterResult(server);
                LocalhostBridgeCoreAPIProvider.getAPI().sendForReply(server, "lbc:RebootPluginUpdater", GsonUtil.GSON.toJson(updaterResultDTO), r -> {
                    try {
                        UpdaterCallbackDTO updaterCallbackDTO = GsonUtil.GSON.fromJson(r, UpdaterCallbackDTO.class);
                        sendRebootUpdateReport(sender, server, updaterCallbackDTO);
                    } catch (Exception e) {
                        SimpleUtil.sendTextMessage(sender, "&c[Updater] 服务器 &e" + server + "&c: 解析回复数据失败");
                        LocalhostBridgeCore.getInstance().getLogger().log(java.util.logging.Level.WARNING, "Failed to parse callback from " + server, e);
                    }
                }, () -> SimpleUtil.sendTextMessage(sender, "&c[Updater] 服务器 &e" + server + "&c: 推送失败或超时"));
            }
        });
    }

    private void handleUpdaterRebootServer(CommandSender sender, String serverName) {
        if (!ChannelRegistrationUtil.isRegistered(serverName)) {
            SimpleUtil.sendTextMessage(sender, "&c[Updater] 服务器 &e" + serverName + "&c 不存在或未注册");
            return;
        }

        SimpleUtil.sendTextMessage(sender, "&e[Updater] 正在准备重启子服: " + serverName);
        SimpleUtil.runAsyncAsLBC(() -> {
            UpdaterResultDTO updaterResultDTO = LocalhostBridgeCore.getInstance().getUpdaterManager().generateUpdaterResult(serverName);
            LocalhostBridgeCoreAPIProvider.getAPI().sendForReply(serverName, "lbc:RebootPluginUpdater", GsonUtil.GSON.toJson(updaterResultDTO), r -> {
                try {
                    UpdaterCallbackDTO updaterCallbackDTO = GsonUtil.GSON.fromJson(r, UpdaterCallbackDTO.class);
                    sendRebootUpdateReport(sender, serverName, updaterCallbackDTO);
                } catch (Exception e) {
                    SimpleUtil.sendTextMessage(sender, "&c[Updater] 服务器 &e" + serverName + "&c: 解析回复数据失败");
                    LocalhostBridgeCore.getInstance().getLogger().log(java.util.logging.Level.WARNING, "Failed to parse callback from " + serverName, e);
                }
            }, () -> SimpleUtil.sendTextMessage(sender, "&c[Updater] 服务器 &e" + serverName + "&c: 推送失败或超时"));
        });
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return SimpleUtil.tabPrefixFilter(Arrays.asList("status", "restart", "hello", "help", "register", "unregister", "send", "send-r", "broadcast", "broadcast-r", "updater"), args[0]);
        }
        else if (args.length == 2) {
            if (Arrays.asList("hello", "register", "unregister", "send", "send-r").contains(args[0])) {
                return SimpleUtil.tabPrefixFilter(ChannelRegistrationUtil.getRegisteredChannel().keySet(), args[1]);
            }
            if (args[0].equalsIgnoreCase("updater")) {
                return SimpleUtil.tabPrefixFilter(Arrays.asList("push", "reboot"), args[1]);
            }
        }
        else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("updater") && (args[1].equalsIgnoreCase("push") || args[1].equalsIgnoreCase("reboot"))) {
                return SimpleUtil.tabPrefixFilter(ChannelRegistrationUtil.getRegisteredChannel().keySet(), args[2]);
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
        SimpleUtil.sendTextMessage(sender, "&b/lbc updater  &3查看更新器子命令");
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

    private void sendPushUpdateReport(CommandSender sender, String serverName, UpdaterCallbackDTO updaterCallbackDTO) {
        if (updaterCallbackDTO != null && updaterCallbackDTO.isHasUpdate()) {
            StringBuilder message = new StringBuilder();
            message.append("&8&m                              &r\n");
            message.append("&a[Updater] 服务器 &e").append(serverName).append("&a 配置更新完成:\n");
            
            if (updaterCallbackDTO.getUpdatedConfigFiles() != null && !updaterCallbackDTO.getUpdatedConfigFiles().isEmpty()) {
                message.append("&7  配置文件更新: &b").append(updaterCallbackDTO.getUpdatedConfigFiles().size()).append(" 个\n");
                for (String file : updaterCallbackDTO.getUpdatedConfigFiles()) {
                    message.append("&7    - &f").append(file).append("\n");
                }
            }
            
            if (updaterCallbackDTO.getUpdatedPluginFiles() != null && !updaterCallbackDTO.getUpdatedPluginFiles().isEmpty()) {
                message.append("&e  检测到JAR更新(未应用): &b").append(updaterCallbackDTO.getUpdatedPluginFiles().size()).append(" 个\n");
                for (String file : updaterCallbackDTO.getUpdatedPluginFiles()) {
                    message.append("&e    - &f").append(file).append("\n");
                }
                message.append("&e  提示: 使用 &b/lbc updater reboot ").append(serverName).append("&e 以应用JAR更新\n");
            }

            if (updaterCallbackDTO.getUpdatedConfigFiles() != null && !updaterCallbackDTO.getUpdatedConfigFiles().isEmpty()) {
                message.append("&2  状态: 配置已热重载\n");
            } else {
                message.append("&2  状态: 未改动\n");
            }
            message.append("&8&m                              &r");
            
            SimpleUtil.sendTextMessage(sender, message.toString());
        } else if (updaterCallbackDTO != null) {
            SimpleUtil.sendTextMessage(sender, "&7[Updater] 服务器 &e" + serverName + "&7: 无需更新");
        }
    }

    private void sendRebootUpdateReport(CommandSender sender, String serverName, UpdaterCallbackDTO updaterCallbackDTO) {
        if (updaterCallbackDTO != null && updaterCallbackDTO.isHasUpdate()) {
            StringBuilder message = new StringBuilder();
            message.append("&8&m                              &r\n");
            message.append("&a[Updater] 服务器 &e").append(serverName).append("&a 更新完成:\n");

            if (updaterCallbackDTO.getUpdatedConfigFiles() != null && !updaterCallbackDTO.getUpdatedConfigFiles().isEmpty()) {
                message.append("&7  配置文件更新: &b").append(updaterCallbackDTO.getUpdatedConfigFiles().size()).append(" 个\n");
                for (String file : updaterCallbackDTO.getUpdatedConfigFiles()) {
                    message.append("&7    - &f").append(file).append("\n");
                }
            }

            if (updaterCallbackDTO.getUpdatedPluginFiles() != null && !updaterCallbackDTO.getUpdatedPluginFiles().isEmpty()) {
                message.append("&7  插件文件更新: &b").append(updaterCallbackDTO.getUpdatedPluginFiles().size()).append(" 个\n");
                for (String file : updaterCallbackDTO.getUpdatedPluginFiles()) {
                    message.append("&7    - &f").append(file).append("\n");
                }
            }

            if (updaterCallbackDTO.isHasReboot()) {
                message.append("&2  状态: 正在重启以应用更新\n");
            } else if (updaterCallbackDTO.getUpdatedPluginFiles() != null && !updaterCallbackDTO.getUpdatedPluginFiles().isEmpty()) {
                if (updaterCallbackDTO.getUpdatedConfigFiles() != null && !updaterCallbackDTO.getUpdatedConfigFiles().isEmpty()) {
                    message.append("&2  状态: Jar更新失败(服务器内有玩家); 配置已热重载\n");
                } else {
                    message.append("&2  状态: Jar更新失败(服务器内有玩家)\n");
                }
            } else {
                message.append("&2  状态: 配置已热重载\n");
            }
            message.append("&8&m                              &r");

            SimpleUtil.sendTextMessage(sender, message.toString());
        } else if (updaterCallbackDTO != null) {
            SimpleUtil.sendTextMessage(sender, "&7[Updater] 服务器 &e" + serverName + "&7: 无需更新");
        }
    }

}
