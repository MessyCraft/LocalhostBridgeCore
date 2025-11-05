package io.github.messycraft.localhostbridgecore.bukkit.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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

public final class HttpClientUtil {

    private static int CONNECT_TIMEOUT = 50;
    private static int READ_TIMEOUT = 500;

    private HttpClientUtil() {}

    /**
     * @throws IllegalArgumentException if {@code context} be null
     */
    public static ResponseStruct doPost(String context, String namespace, String data) {
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
            if (namespace != null && SimpleUtil.nameMatches(namespace)) conn.setRequestProperty("namespace", namespace);

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
            return new ResponseStruct(code, result);
        }
        catch (IOException e) {
            return new ResponseStruct(-1, null);
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
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
        int code = doPost("/register", null, null).code;
        switch (code) {
            case 200: return SimpleUtil.color("&a频道注册成功!");
            case 403: return SimpleUtil.color("&c此标识已被不同的端口注册或不合法, 请在配置文件中更换频道名.");
            case 503: return SimpleUtil.color("&3服务器未开启自动注册(如已注册请忽略此消息). &b@see &n/lbc register");
            case -1: return SimpleUtil.color("&c服务器请求超时, 访问不可达.");
            default: return SimpleUtil.color("&4未知错误(显然作者完全没有意识到这个情况)");
        }
    }

    /**
     * 发送Hello信息，必须外部异步使用否则抛出异常!
     * @param target 目标频道，为null时代表所有
     * @return 键表示目标频道名，值表示对应延时毫秒数(-1表示不可达)
     * @throws RuntimeException if {@link Bukkit#isPrimaryThread()} be <tt>true</tt>
     * @deprecated 蠢人灵机一动，然而多余的设计，连接端不需要主动发起延迟测试
     */
    @Deprecated
    public static Map<String, Integer> hello(String target) throws RuntimeException {
        if (Bukkit.isPrimaryThread()) {
            throw new RuntimeException("Send hello ping in primary thread");
        }
        ResponseStruct resp = doPost("/hello", null, target);
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
        READ_TIMEOUT = SimpleUtil.getSessionLifetime();
    }

    @AllArgsConstructor
    public static class ResponseStruct {
        public final int code;
        public final String data;
    }

}
