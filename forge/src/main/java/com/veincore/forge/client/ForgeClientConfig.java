package com.veincore.forge.client;

import com.veincore.config.ClientConfig;
import com.veincore.config.JsonStore;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Path;

/** 客户端配置读写（config/veincore-client.json，Forge 端）。 */
public final class ForgeClientConfig {

    private static final String FILE = "veincore-client.json";
    private static ClientConfig cached;

    private ForgeClientConfig() {
    }

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE);
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
