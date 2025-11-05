package io.github.messycraft.localhostbridgecore.bungee.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.messycraft.localhostbridgecore.bungee.Properties;
import io.github.messycraft.localhostbridgecore.bungee.entity.LChannel;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class ServerListPingUtil {

    private ServerListPingUtil() {}

    /**
     * Do it async, please, or proxy may cause a lag.
     * @param channel 127.0.0.1:port
     */
    public static void sendCustomData(LChannel channel, String namespace, String data, boolean needReply, String seq, Consumer<String> reply, Runnable noReply) {
        String logSuffix = "[" + seq + "]";
        String unique = channel.getUnique();
        int port = channel.getPort();
        if (unique.length() + namespace.length() + data.length() + 15 > 255) {
            SimpleUtil.debug(String.format("IGNORE TOO LONG -> {%s, %s, %s..., %s, %s}", unique, namespace, needReply, seq, data.substring(0, 20)));
            return;
        }
        SimpleUtil.debug(String.format("Send -> {%s, %s, %s, %s, %s}", unique, namespace, needReply, seq, data));
        boolean connected = false;
        String resp = null;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), Properties.TIMEOUT);
            connected = true;
            socket.setSoTimeout(Properties.SESSION_LIFETIME);
            try (
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    DataInputStream in = new DataInputStream(socket.getInputStream())
            ) {
                // 握手处理
                try (
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        DataOutputStream handshake = new DataOutputStream(buffer)
                ) {
                    writeVarInt(handshake, 0x00);
                    writeVarInt(handshake, -1);
                    writeString(handshake, spawnHeaders(unique, namespace, needReply, seq) + data);
                    handshake.writeShort(port);
                    writeVarInt(handshake, 1);
                    handshake.flush();

                    writeVarInt(out, buffer.size());
                    out.write(buffer.toByteArray());
                    out.flush();
                }

                if (needReply) {
                    int packetLength = readVarInt(in);
                    byte[] packetData = new byte[packetLength];
                    in.readFully(packetData);
                    try (DataInputStream packetIn = new DataInputStream(new ByteArrayInputStream(packetData))) {
                        int packetId = readVarInt(packetIn);
                        if (packetId != 0x00) {
                            SimpleUtil.debug("Send [ERROR]" + logSuffix);
                            return;
                        }
                        resp = new Gson().fromJson(readString(packetIn), JsonObject.class).get("d").getAsString();
                        SimpleUtil.debug("Response -> " + resp + " " + logSuffix);
                    }
                }
            }
        } catch (Exception ex) {
            if (connected && needReply) {
                SimpleUtil.debug((ex instanceof SocketTimeoutException ? "Send [REPLY TIMEOUT]" : "Send [CLOSED WITHOUT REPLY]") + logSuffix);
                channel.increaseSessionExpireCount();
            }
            else {
                SimpleUtil.debug("Send [FAILURE]" + logSuffix);
                channel.increasePingFailCount();
            }
        }
        if (resp != null) {
            if (reply != null) {
                reply.accept(resp);
            }
        }
        else if (noReply != null) {
            noReply.run();
        }
    }

    private static void writeVarInt(DataOutputStream out, int i) throws IOException {
        while ((i & -128) != 0) {
            out.writeByte(i & 127 | 128);
            i >>>= 7;
        }
        out.writeByte(i);
    }

    private static void writeString(DataOutputStream out, String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        int position = 0;
        byte currentByte;
        while (true) {
            currentByte = in.readByte();
            value |= (currentByte & 0x7F) << position;
            if ((currentByte & 0x80) == 0) break;
            position += 7;
            if (position >= 32) throw new RuntimeException("VarInt is too big");
        }
        return value;
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = readVarInt(in);
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String spawnHeaders(String unique, String namespace, boolean needReply, String seq) {
        if (!SimpleUtil.nameMatches(unique)) {
            throw new IllegalArgumentException("unique contains illegal characters");
        }
        if ((namespace == null || !namespace.isEmpty()) && SimpleUtil.nameMatches(namespace)) {
            throw new IllegalArgumentException("namespace contains illegal characters");
        }
        return String.format("LBC$%s$%s$%s$%s$", unique, namespace, needReply ? "1" : "0", seq);
    }

}
