package io.github.messycraft.localhostbridgecore.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class GsonUtil {

    public static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private GsonUtil() {}

}
