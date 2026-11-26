package com.veincore.fabric.client;

import com.veincore.config.ClientConfig;
import com.veincore.config.JsonStore;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Path;

/** 客户端配置读写（config/veincore-client.json）。 */
public final class ClientConfigStore {

    private static final String FILE = "veincore-client.json";
    private static ClientConfig cached;

    private ClientConfigStore() {
    }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE);
    }

    public static ClientConfig get() {
        if (cached == null) {
            cached = ClientConfig.fromMap(JsonStore.load(path(), new ClientConfig().toMap()));
        }
        return cached;
    }

    public static void save() {
        try {
            JsonStore.save(path(), get().toMap());
        } catch (IOException ignored) {
            // 写失败不阻断游戏
        }
    }
}
