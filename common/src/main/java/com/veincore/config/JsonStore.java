package com.veincore.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** 配置文件读写：不存在/损坏时回退默认值。 */
public final class JsonStore {

    private JsonStore() {
    }

    public static Map<String, Object> load(Path file, Map<String, Object> defaults) {
        if (file == null || !Files.isRegularFile(file)) {
            return defaults;
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            Object parsed = MiniJson.parse(text);
            if (parsed instanceof Map<?, ?> map) {
                Map<String, Object> out = new LinkedHashMap<>(defaults);
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    out.put(String.valueOf(e.getKey()), e.getValue());
                }
                return out;
            }
        } catch (Exception ignored) {
            // 损坏配置回退默认，不阻断启动
        }
        return defaults;
    }

    public static void save(Path file, Map<String, Object> map) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, MiniJson.stringify(map), StandardCharsets.UTF_8);
    }
}
