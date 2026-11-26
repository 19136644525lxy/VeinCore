package com.veincore.data;

import com.veincore.config.JsonStore;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 玩家个人设置与统计（config/veincore-players.json）：当前模式、个人挖掘上限、连锁次数与总格数。
 *
 * <p>键统一用玩家 UUID 字符串，避免重名玩家互相覆盖。</p>
 */
public final class PlayerSettings {

    private final Map<String, String> patterns = new LinkedHashMap<>();
    private final Map<String, Integer> maxBlocks = new LinkedHashMap<>();
    private final Map<String, Integer> chains = new LinkedHashMap<>();
    private final Map<String, Integer> blocks = new LinkedHashMap<>();

    public String pattern(String id) {
        return patterns.get(id);
    }

    public void setPattern(String id, String key) {
        if (id == null) {
            return;
        }
        if (key == null) {
            patterns.remove(id);
        } else {
            patterns.put(id, key);
        }
    }

    /** 玩家个人上限；null 表示未设置，使用服务器默认。 */
    public Integer maxBlocks(String id) {
        return maxBlocks.get(id);
    }

    public void setMaxBlocks(String id, Integer value) {
        if (id == null) {
            return;
        }
        if (value == null) {
            maxBlocks.remove(id);
        } else {
            maxBlocks.put(id, value);
        }
    }

    /** 一次连锁结束后累加：连锁次数 +1，破坏格数 +destroyed。 */
    public void recordChain(String id, int destroyed) {
        if (id == null) {
            return;
        }
        chains.merge(id, 1, Integer::sum);
        blocks.merge(id, Math.max(0, destroyed), Integer::sum);
    }

    public int chains(String id) {
        return chains.getOrDefault(id, 0);
    }

    public int blocks(String id) {
        return blocks.getOrDefault(id, 0);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("patterns", new LinkedHashMap<>(patterns));
        m.put("max_blocks", new LinkedHashMap<>(maxBlocks));
        m.put("chains", new LinkedHashMap<>(chains));
        m.put("blocks", new LinkedHashMap<>(blocks));
        return m;
    }

    public static PlayerSettings fromMap(Map<String, Object> map) {
        PlayerSettings p = new PlayerSettings();
        if (map == null) {
            return p;
        }
        copyStrings(p.patterns, map.get("patterns"));
        copyInts(p.maxBlocks, map.get("max_blocks"));
        copyInts(p.chains, map.get("chains"));
        copyInts(p.blocks, map.get("blocks"));
        return p;
    }

    public static PlayerSettings load(Path file) {
        return fromMap(JsonStore.load(file, new PlayerSettings().toMap()));
    }

    public void save(Path file) throws IOException {
        JsonStore.save(file, toMap());
    }

    private static void copyStrings(Map<String, String> target, Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return;
        }
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (e.getKey() != null && e.getValue() instanceof String s) {
                target.put(String.valueOf(e.getKey()), s);
            }
        }
    }

    private static void copyInts(Map<String, Integer> target, Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return;
        }
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (e.getKey() != null && e.getValue() instanceof Number n) {
                target.put(String.valueOf(e.getKey()), n.intValue());
            }
        }
    }
}
