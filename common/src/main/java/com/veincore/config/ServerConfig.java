package com.veincore.config;

import com.veincore.core.MaxBlocksLimiter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 服务端配置（server / config 目录下的 veincore-server.json）。 */
public final class ServerConfig {

    public String defaultPattern = "vein";
    /** 服务器封顶，玩家上限不能超过它。 */
    public int maxBlocks = MaxBlocksLimiter.SERVER_FALLBACK;
    /** 玩家默认挖掘上限（玩家没有 /veinminer settings maxblocks 个人设置时生效）。 */
    public int defaultPlayerBlocks = MaxBlocksLimiter.MIN;
    public boolean allowOver600 = true;
    /** 每破坏一格增加的疲劳值（exhaustion；4 点 = 1 点饥饿值）。默认 20 = 每格 5 点饥饿值。 */
    public int hungerPerBlock = 20;
    public int durabilityPerBlock = 1;
    public int cooldownTicks = 10;
    public int serverMaxRadius = 64;
    public boolean includeDataPack = true;
    public int rescanIntervalTicks = 200;
    public List<String> blacklist = new ArrayList<>();
    public List<String> whitelist = new ArrayList<>();
    public Map<String, List<String>> blockGroups = new LinkedHashMap<>();

    public static ServerConfig fromMap(Map<String, Object> map) {
        ServerConfig c = new ServerConfig();
        if (map == null) {
            return c;
        }
        c.defaultPattern = str(map.get("default_pattern"), c.defaultPattern);
        c.maxBlocks = intVal(map.get("max_blocks"), c.maxBlocks);
        c.defaultPlayerBlocks = intVal(map.get("default_player_blocks"), c.defaultPlayerBlocks);
        c.allowOver600 = boolVal(map.get("allow_over_600"), c.allowOver600);
        c.hungerPerBlock = intVal(map.get("hunger_per_block"), c.hungerPerBlock);
        c.durabilityPerBlock = intVal(map.get("durability_per_block"), c.durabilityPerBlock);
        c.cooldownTicks = intVal(map.get("cooldown_ticks"), c.cooldownTicks);
        c.serverMaxRadius = intVal(map.get("server_max_radius"), c.serverMaxRadius);
        c.includeDataPack = boolVal(map.get("include_data_pack"), c.includeDataPack);
        c.rescanIntervalTicks = intVal(map.get("rescan_interval_ticks"), c.rescanIntervalTicks);
        c.blacklist = strList(map.get("blacklist"));
        c.whitelist = strList(map.get("whitelist"));
        c.blockGroups = groups(map.get("block_groups"));
        return c;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("default_pattern", defaultPattern);
        m.put("max_blocks", maxBlocks);
        m.put("default_player_blocks", defaultPlayerBlocks);
        m.put("allow_over_600", allowOver600);
        m.put("hunger_per_block", hungerPerBlock);
        m.put("durability_per_block", durabilityPerBlock);
        m.put("cooldown_ticks", cooldownTicks);
        m.put("server_max_radius", serverMaxRadius);
        m.put("include_data_pack", includeDataPack);
        m.put("rescan_interval_ticks", rescanIntervalTicks);
        m.put("blacklist", new ArrayList<>(blacklist));
        m.put("whitelist", new ArrayList<>(whitelist));
        m.put("block_groups", blockGroups);
        return m;
    }

    private static String str(Object o, String dft) {
        return o instanceof String s ? s : dft;
    }

    private static int intVal(Object o, int dft) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        if (o instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                return dft;
            }
        }
        return dft;
    }

    private static boolean boolVal(Object o, boolean dft) {
        if (o instanceof Boolean b) {
            return b;
        }
        if (o instanceof String s) {
            return Boolean.parseBoolean(s.trim());
        }
        return dft;
    }

    private static List<String> strList(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List<?> list) {
            for (Object e : list) {
                if (e instanceof String s) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    private static Map<String, List<String>> groups(Object o) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        if (o instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> e : map.entrySet()) {
                out.put(String.valueOf(e.getKey()), strList(e.getValue()));
            }
        }
        return out;
    }
}
