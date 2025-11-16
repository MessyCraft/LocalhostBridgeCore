package io.github.messycraft.localhostbridgecore.bukkit.packetlistener;

import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketHandshakeReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.handshaking.client.WrapperHandshakingClientHandshake;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerResponse;
import io.github.messycraft.localhostbridgecore.api.LocalhostBridgeCoreAPIProvider;
import io.github.messycraft.localhostbridgecore.bukkit.impl.ListenerManagerBukkitImpl;
import io.github.messycraft.localhostbridgecore.bukkit.util.SimpleUtil;
import org.bukkit.plugin.Plugin;

import java.net.InetSocketAddress;

public class MessageReceiveListener extends SimplePacketListenerAbstract {

    private Plugin plugin;

    public MessageReceiveListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPacketHandshakeReceive(PacketHandshakeReceiveEvent event) {
        if (((InetSocketAddress) event.getAddress()).getAddress().isLoopbackAddress() && event.getPacketType() == PacketType.Handshaking.Client.HANDSHAKE) {
            WrapperHandshakingClientHandshake packet = new WrapperHandshakingClientHandshake(event);
            String hostname = packet.getServerAddress();
            if (hostname == null || !hostname.startsWith("LBC$")) {
                return;
            }
            event.setCancelled(true);
            String[] reqStr = hostname.split("\\$", -1);
            if (reqStr.length < 6) {
                return;
            }
            String unique = reqStr[1];
            String namespace = reqStr[2];
            boolean needReply = reqStr[3].equals("1");
            String seq = reqStr[4];
            StringBuilder dataSB = new StringBuilder();
            for (int i = 5; i < reqStr.length; i++) {
                if (i > 5) dataSB.append("$");
                dataSB.append(reqStr[i]);
            }
            String data = dataSB.toString();
            if (namespace.isEmpty() && needReply) {
                event.getUser().sendPacketSilently(new WrapperStatusServerResponse("{\"d\": \"Hello\"}"));
                SimpleUtil.debug("Receive -> Hello " + seq);
                return;
            }
            SimpleUtil.debug(String.format("Receive -> {%s, %s, %s, %s, %s}", unique, namespace, needReply, seq, data));
            ((ListenerManagerBukkitImpl) LocalhostBridgeCoreAPIProvider.getAPI().getListenerManager()).call(unique, namespace, seq, data, needReply, event.getUser());
        }
    }

}
