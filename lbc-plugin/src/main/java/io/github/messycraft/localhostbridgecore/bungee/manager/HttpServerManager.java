package io.github.messycraft.localhostbridgecore.bungee.manager;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import io.github.messycraft.localhostbridgecore.bungee.Properties;
import com.sun.net.httpserver.HttpServer;
import io.github.messycraft.localhostbridgecore.bungee.util.ChannelRegistrationUtil;
import io.github.messycraft.localhostbridgecore.bungee.util.SimpleUtil;
import lombok.Getter;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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
            server.createContext("/broadcast", httpExchange -> {

            });
            server.createContext("/send", httpExchange -> {
                // TODO 11/7
            });
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
        if (unique == null || port == null) {
            closeWithoutBody(400, httpExchange);
            return;
        }
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
