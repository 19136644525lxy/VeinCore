package com.veincore.data;

import com.veincore.config.JsonStore;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    /** 个人白名单：非空时该玩家只能连锁这些方块。 */
    private final Map<String, List<String>> allowBlocks = new LinkedHashMap<>();

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

    /** 个人白名单方块（空表示不限）。 */
    public List<String> allowBlocks(String id) {
        return allowBlocks.getOrDefault(id, List.of());
    }

    public boolean addAllowBlock(String id, String block) {
        if (id == null || block == null || block.isEmpty()) {
            return false;
        }
        List<String> list = allowBlocks.computeIfAbsent(id, k -> new ArrayList<>());
        if (list.contains(block)) {
            return false;
        }
        list.add(block);
        return true;
    }

    public boolean removeAllowBlock(String id, String block) {
        List<String> list = allowBlocks.get(id);
        if (list == null) {
            return false;
        }
        boolean removed = list.remove(block);
        if (list.isEmpty()) {
            allowBlocks.remove(id);
        }
        return removed;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("patterns", new LinkedHashMap<>(patterns));
        m.put("max_blocks", new LinkedHashMap<>(maxBlocks));
        m.put("chains", new LinkedHashMap<>(chains));
        m.put("blocks", new LinkedHashMap<>(blocks));
        m.put("allow_blocks", new LinkedHashMap<>(allowBlocks));
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
        copyLists(p.allowBlocks, map.get("allow_blocks"));
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

    private static void copyLists(Map<String, List<String>> target, Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return;
        }
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (e.getKey() == null || !(e.getValue() instanceof List<?> list)) {
                continue;
            }
            List<String> values = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof String s && !s.isEmpty()) {
                    values.add(s);
                }
            }
            if (!values.isEmpty()) {
                target.put(String.valueOf(e.getKey()), values);
            }
        }
    }
}
