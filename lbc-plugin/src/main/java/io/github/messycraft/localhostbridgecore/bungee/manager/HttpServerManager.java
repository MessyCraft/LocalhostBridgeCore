package io.github.messycraft.localhostbridgecore.bungee.manager;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.messycraft.localhostbridgecore.api.LocalhostBridgeCoreAPIProvider;
import io.github.messycraft.localhostbridgecore.bungee.Properties;
import io.github.messycraft.localhostbridgecore.bungee.entity.LChannel;
import io.github.messycraft.localhostbridgecore.bungee.impl.ListenerManagerBungeeImpl;
import io.github.messycraft.localhostbridgecore.bungee.util.ChannelRegistrationUtil;
import io.github.messycraft.localhostbridgecore.bungee.util.ServerListPingUtil;
import io.github.messycraft.localhostbridgecore.bungee.util.SimpleUtil;
import lombok.Getter;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HttpServerManager {

    private static Logger logger;
    private static HttpServer server = null;

    @Getter
    private static long startMillis;

    private HttpServerManager() {}

    public static void start(Logger logger) {
        HttpServerManager.logger = logger;
        try {
            int port = Properties.BIND_PORT;
            logger.info("--------------------");
            logger.info("Start server on 127.0.0.1:" + port);
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            server.setExecutor(null);
            server.createContext("/broadcast", HttpServerManager::handleBroadcast);
            server.createContext("/send", HttpServerManager::handleDataReceive);
            server.createContext("/register", HttpServerManager::handleRegister);
            server.createContext("/list", HttpServerManager::handleGetList);
            server.start();
            startMillis = System.currentTimeMillis();
            logger.info("--------------------");
        } catch (IOException ex) {
            server = null;
            logger.log(Level.SEVERE, "HttpServerUtil$start", ex);
            logger.severe("启动未完成, 这可能是因为端口被占用等因素导致的, 可稍后使用\"lbc restart\"重试.");
        }
    }

    public static void shutdown() {
        if (isRunning()) {
            logger.info("--------------------");
            logger.info("Shutdown server on 127.0.0.1:" + server.getAddress().getPort());
            server.stop(0);
            server = null;
            logger.info("--------------------");
        }
    }

    public static boolean isRunning() {
        return server != null;
    }

    private static String readFirstLine(InputStream is) throws IOException {
        try (
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr)
        ) {
            return br.readLine();
        }
    }

    private static void closeWithoutBody(int code, HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(code, 0);
        exchange.close();
    }

    private static void closeWithBody(int code, String body, HttpExchange exchange) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, data.length);
        OutputStream os = exchange.getResponseBody();
        os.write(data, 0, data.length);
        os.flush();
        exchange.close();
    }

    private static void handleBroadcast(HttpExchange httpExchange) throws IOException {
        if (!httpExchange.getRequestMethod().equals("POST")) {
            closeWithoutBody(405, httpExchange);
            return;
        }
        Headers headers = httpExchange.getRequestHeaders();
        if (!accessHeaders(headers)) {
            closeWithoutBody(403, httpExchange);
            return;
        }
        String from = getHeaderValue(headers, "unique");
        String seq = getHeaderValue(headers, "seq");
        String namespace = getHeaderValue(headers, "namespace");
        boolean needReply = "reply".equals(headers.getFirst("extra"));
        if (!SimpleUtil.nameMatches(namespace)) {
            closeWithoutBody(400, httpExchange);
            return;
        }
        String data = readFirstLine(httpExchange.getRequestBody());
        if (data == null) {
            closeWithoutBody(400, httpExchange);
            return;
        }
        SimpleUtil.debug(String.format("Broadcast -> {(FROM) %s, %s, %s, %s, %s}", from, namespace, needReply, seq, data));
        assert from != null;

        Map<String, String> answer = new ConcurrentHashMap<>();
        final int count = ChannelRegistrationUtil.getRegisteredChannel().size();  // contains "BC"
        AtomicInteger completed = new AtomicInteger();

        // send to "BC"
        SimpleUtil.runAsyncAsLBC(() -> {
            String r = ((ListenerManagerBungeeImpl) LocalhostBridgeCoreAPIProvider.getAPI().getListenerManager()).call(from, namespace, seq, data, needReply);
            if (needReply) {
                if (r != null) {
                    answer.put("BC", r);
                }
                if (completed.incrementAndGet() == count) {
                    try {
                        String json = new Gson().toJson(answer);
                        closeWithBody(200, json, httpExchange);
                        SimpleUtil.debug("Reply(broadcast) [" + seq + "] -> " + json);
                    } catch (IOException e) {
                        SimpleUtil.runtimeWarning("Reply(broadcast) [FAILURE][" + seq + "]");
                    }
                }
            }
        });

        // send to every channel
        for (LChannel c : ChannelRegistrationUtil.getRegisteredChannel().values()) {
            if (from.equals(c.getUnique())) continue;
            SimpleUtil.runAsyncAsLBC(() -> ServerListPingUtil.sendCustomData(from, c, namespace, data, needReply, seq, r -> {
                answer.put(c.getUnique(), r);
                if (completed.incrementAndGet() == count) {
                    try {
                        String json = new Gson().toJson(answer);
                        closeWithBody(200, json, httpExchange);
                        SimpleUtil.debug("Reply(broadcast) [" + seq + "] -> " + json);
                    } catch (IOException e) {
                        SimpleUtil.runtimeWarning("Reply(broadcast) [FAILURE][" + seq + "]");
                    }
                }
            }, () -> {
                if (needReply && completed.incrementAndGet() == count) {
                    try {
                        String json = new Gson().toJson(answer);
                        closeWithBody(200, json, httpExchange);
                        SimpleUtil.debug("Reply(broadcast) [" + seq + "] -> " + json);
                    } catch (IOException e) {
                        SimpleUtil.runtimeWarning("Reply(broadcast) [FAILURE][" + seq + "]");
                    }
                }
            }));
        }

        // immediately close if !needReply
        if (!needReply) {
            closeWithoutBody(200, httpExchange);
        }
    }

    private static void handleDataReceive(HttpExchange httpExchange) throws IOException {
        if (!httpExchange.getRequestMethod().equals("POST")) {
            closeWithoutBody(405, httpExchange);
            return;
        }
        Headers headers = httpExchange.getRequestHeaders();
        if (!accessHeaders(headers)) {
            closeWithoutBody(403, httpExchange);
            return;
        }
        String from = getHeaderValue(headers, "unique");
        String seq = getHeaderValue(headers, "seq");
        String namespace = getHeaderValue(headers, "namespace");
        String target = getHeaderValue(headers, "target");
        boolean needReply = "reply".equals(headers.getFirst("extra"));
        if (!SimpleUtil.nameMatches(namespace) || !SimpleUtil.nameMatches(target)) {
            closeWithoutBody(400, httpExchange);
            return;
        }
        String data = readFirstLine(httpExchange.getRequestBody());
        if (data == null) {
            closeWithoutBody(400, httpExchange);
            return;
        }
        if (target.equals("BC")) {
            SimpleUtil.debug(String.format("Receive -> {%s, %s, %s, %s, %s}", from, namespace, needReply, seq, data));
            String ret = ((ListenerManagerBungeeImpl) LocalhostBridgeCoreAPIProvider.getAPI().getListenerManager()).call(from, namespace, seq, data, needReply);
            if (ret != null) {
                closeWithBody(200, ret, httpExchange);
            }
            else {
                closeWithoutBody(200, httpExchange);
            }
            return;
        }
        SimpleUtil.debug(String.format("Forward " + target + " -> {%s, %s, %s, %s, %s}", from, namespace, needReply, seq, data));
        LChannel lChannel = ChannelRegistrationUtil.getRegisteredChannel().get(target);
        if (lChannel == null) {
            closeWithoutBody(404, httpExchange);
            return;
        }
        assert from != null;
        AtomicReference<String> ret = new AtomicReference<>(null);
        ServerListPingUtil.sendCustomData(from, lChannel, namespace, data, needReply, seq, ret::set, null);
        if (ret.get() != null) {
            closeWithBody(200, ret.get(), httpExchange);
        }
        else {
            closeWithoutBody(200, httpExchange);
        }
    }

    private static void handleRegister(HttpExchange httpExchange) throws IOException {
        if (!httpExchange.getRequestMethod().equals("POST")) {
            closeWithoutBody(405, httpExchange);
            return;
        }
        if (!Properties.ALLOW_REGISTER) {
            closeWithoutBody(503, httpExchange);
            return;
        }
        Headers headers = httpExchange.getRequestHeaders();
        String unique = getHeaderValue(headers, "unique");
        String port = getHeaderValue(headers, "port");
        if (!SimpleUtil.nameMatches(unique) || !SimpleUtil.nameMatches(port)) {
            closeWithoutBody(400, httpExchange);
            return;
        }
        int registerPort;
        try {
            registerPort = Integer.parseInt(port);
        } catch (NumberFormatException ex) {
            closeWithoutBody(400, httpExchange);
            return;
        }
        if (ChannelRegistrationUtil.registerChannel(unique, registerPort)) {
            closeWithoutBody(200, httpExchange);
        }
        else {
            closeWithoutBody(403, httpExchange);
        }
    }

    private static void handleGetList(HttpExchange httpExchange) throws IOException {
        if (!httpExchange.getRequestMethod().equals("POST")) {
            closeWithoutBody(405, httpExchange);
            return;
        }
        Headers headers = httpExchange.getRequestHeaders();
        if (!accessHeaders(headers)) {
            closeWithoutBody(403, httpExchange);
            return;
        }
        closeWithBody(200, "BC$" + String.join("$", ChannelRegistrationUtil.getRegisteredChannel().keySet()), httpExchange);
    }

    private static String getHeaderValue(Headers headers, String key) {
        return headers.containsKey(key) ? (headers.get(key).isEmpty() ? null : headers.get(key).get(0)) : null;
    }

    private static boolean accessHeaders(Headers headers) {
        String unique = getHeaderValue(headers, "unique");
        String port = getHeaderValue(headers, "port");
        return unique != null && port != null && ChannelRegistrationUtil.isRegisteredOfBukkit(unique, port);
    }

}
