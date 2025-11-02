package io.github.messycraft.localhostbridgecore.api;

/**
 * 获取LocalhostBridgeCore API实例。
 */
public final class LocalhostBridgeCoreAPIProvider {

    private static LocalhostBridgeCoreAPI api;

    private LocalhostBridgeCoreAPIProvider() {}

    public static LocalhostBridgeCoreAPI getAPI() {
        if (api == null) {
            throw new IllegalStateException("The API has not been initialized.");
        }
        return api;
    }

    public static void setAPI(LocalhostBridgeCoreAPI api) {
        if (api != null && LocalhostBridgeCoreAPIProvider.api != null) {
            throw new UnsupportedOperationException("The API is already set.");
        }
        LocalhostBridgeCoreAPIProvider.api = api;
    }

}
