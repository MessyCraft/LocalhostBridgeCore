package io.github.messycraft.localhostbridgecore.bukkit.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.messycraft.localhostbridgecore.bukkit.LocalhostBridgeCore;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class HttpClientUtil {

    private static int CONNECT_TIMEOUT = 50;
    private static int READ_TIMEOUT = 10000;

    private HttpClientUtil() {}

    /**
     * @param extra 额外的信息，会作为列表加入Header中
     * @throws IllegalArgumentException if {@code context} be null
     */
    public static ResponseStruct doPost(String context, String target, String namespace, String data, String... extra) {
        String seq = UUID.randomUUID().toString().substring(0, 6);
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        String result;
        try {
            if (context == null || !context.startsWith("/")) {
                throw new IllegalArgumentException("context");
            }
            URL url = new URL("http://127.0.0.1:" + SimpleUtil.getAccessPort() + context);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setDoOutput(data != null);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
            conn.setRequestProperty("accept","*/*");

            // Custom Headers
            conn.setRequestProperty("unique", SimpleUtil.getUnique());
            conn.setRequestProperty("port", String.valueOf(Bukkit.getPort()));
            conn.setRequestProperty("seq", seq);
            for (String ext : extra) {
                conn.addRequestProperty("extra", ext);
            }
            if (SimpleUtil.nameMatches(namespace)) conn.setRequestProperty("namespace", namespace);
            if (SimpleUtil.nameMatches(target)) conn.setRequestProperty("target", target);

            if (data != null) {
                byte[] writeBytes = data.getBytes();
                conn.setRequestProperty("Content-Length", String.valueOf(writeBytes.length));
                OutputStream os = conn.getOutputStream();
                os.write(data.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            }
            else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }
            result = reader.readLine();
            return new ResponseStruct(code, result, seq);
        }
        catch (IOException e) {
            return new ResponseStruct(-1, null, seq);
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (Exception e) {
                    LocalhostBridgeCore.getInstance().getLogger().log(Level.SEVERE, "bufferedReader.close()", e);
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * @return 请求结果描述
     */
    public static String register() {
        int code = doPost("/register", null, null, null).code;
        switch (code) {
            case 200: return SimpleUtil.color("&a频道注册成功!");
            case 403: return SimpleUtil.color("&c此标识已被不同的端口注册或不合法, 请在配置文件中更换频道名.");
            case 503: return SimpleUtil.color("&3服务器未开启自动注册(如已注册请忽略此消息). &b@see &n/lbc register");
            case -1: return SimpleUtil.color("&c服务器请求超时, 访问不可达.");
            default: return SimpleUtil.color("&4未知错误(显然作者完全没有意识到这个情况)");
        }
    }

    public static void sendDataAsync(String target, String namespace, String body, boolean needReply, Consumer<String> reply, Runnable fail) {
        Bukkit.getScheduler().runTaskAsynchronously(LocalhostBridgeCore.getInstance(), () -> {
            ResponseStruct resp = doPost("/send", target, namespace, body, needReply ? "reply" : "none");
            String logSuffix = "[" + resp.seq + "]";
            SimpleUtil.debug(String.format("Send -> {%s, %s, %s, %s, %s}", target, namespace, needReply, resp.seq, body));
            if (resp.code == 200) {
                if (needReply) {
                    if (resp.data != null) {
                        SimpleUtil.debug("Response " + logSuffix + " -> " + resp.data);
                        if (reply != null) {
                            reply.accept(resp.data);
                        }
                    }
                    else {
                        SimpleUtil.runtimeWarning("Response [EMPTY]" + logSuffix);
                        if (fail != null) {
                            fail.run();
                        }
                    }
                }
                return;
            }
            switch (resp.code) {
                case -1: SimpleUtil.runtimeWarning("Send [FAILURE]" + logSuffix); break;
                case 400: SimpleUtil.runtimeWarning("Send [WRONG ARG]" + logSuffix); break;
                case 403: SimpleUtil.runtimeWarning("Send [NOT PERMITTED]" + logSuffix); break;
                case 404: SimpleUtil.runtimeWarning("Send [TARGET NOT EXIST]" + logSuffix); break;
                default: SimpleUtil.runtimeWarning("Send [ERROR UNEXPECTED]" + logSuffix); break;
            }
            if (fail != null) {
                fail.run();
            }
        });
    }

    public static void sendBroadcastAsync(String namespace, String body, boolean needReply, Consumer<Map<String, String>> reply) {
        Bukkit.getScheduler().runTaskAsynchronously(LocalhostBridgeCore.getInstance(), () -> {
            ResponseStruct resp = doPost("/broadcast", null, namespace, body, needReply ? "reply" : "none");
            String logSuffix = "[" + resp.seq + "]";
            SimpleUtil.debug(String.format("Broadcast -> {(FROM) %s, %s, %s, %s, %s}", SimpleUtil.getUnique(), namespace, needReply, resp.seq, body));
            if (resp.code == 200 && needReply && resp.data != null) {
                SimpleUtil.debug("Response(broadcast) " + logSuffix + " -> " + resp.data);
                if (reply != null) {
                    reply.accept(new Gson().fromJson(resp.data, TypeToken.getParameterized(Map.class, String.class, String.class).getType()));
                }
                return;
            }
            switch (resp.code) {
                case -1: SimpleUtil.runtimeWarning("Broadcast [FAILURE]" + logSuffix); break;
                case 400: SimpleUtil.runtimeWarning("Broadcast [WRONG ARG]" + logSuffix); break;
                case 403: SimpleUtil.runtimeWarning("Broadcast [NOT PERMITTED]" + logSuffix); break;
                default: SimpleUtil.runtimeWarning("Broadcast [ERROR UNEXPECTED]" + logSuffix); break;  // also match resp == null
            }
        });
    }

    /**
     * 发送Hello信息，必须外部异步使用否则抛出异常!
     * @param target 目标频道，为null时代表所有
     * @return 键表示目标频道名，值表示对应延时毫秒数(-1表示不可达)
     * @throws RuntimeException if {@link org.bukkit.Bukkit#isPrimaryThread()} be <tt>true</tt>
     * @deprecated 蠢人灵机一动，然而多余的设计，连接端不需要主动发起延迟测试
     */
    @Deprecated
    public static Map<String, Integer> hello(String target) throws RuntimeException {
        if (Bukkit.isPrimaryThread()) {
            throw new RuntimeException("Send hello ping in primary thread");
        }
        ResponseStruct resp = doPost("/hello", null, null, target);
        Map<String, Integer> ret = new HashMap<>();
        if (resp.code == 200) {
            ret.putAll(new Gson().fromJson(resp.data, TypeToken.getParameterized(Map.class, String.class, Integer.class).getType()));
        }
        else {
            ret.put(String.valueOf(target), -1);
        }
        return ret;
    }

    public static void resetTimeoutFromConfig() {
        CONNECT_TIMEOUT = SimpleUtil.getTimeout();
        READ_TIMEOUT = SimpleUtil.getReadTimeout();
    }

    @AllArgsConstructor
    public static class ResponseStruct {
        public final int code;
        public final String data;
        public final String seq;
    }

}
