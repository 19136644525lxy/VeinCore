package com.veincore.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 方块组索引：同组方块可互相连锁（例如 iron_ore 与 deepslate_iron_ore）。
 *
 * <p>内置默认组覆盖常见矿石的深板岩/下界变种；配置 `block_groups` 可按组名覆盖或追加。</p>
 */
public final class BlockGroupIndex {

    private static final Map<String, String> DEFAULTS = buildDefaults();

    private final Map<String, String> blockToGroup;

    private BlockGroupIndex(Map<String, String> blockToGroup) {
        this.blockToGroup = blockToGroup;
    }

    public static BlockGroupIndex withDefaults() {
        return from(null);
    }

    /** 内置默认组 + 配置覆盖（配置优先）。 */
    public static BlockGroupIndex from(Map<String, List<String>> config) {
        Map<String, String> map = new HashMap<>(DEFAULTS);
        if (config != null) {
            for (Map.Entry<String, List<String>> e : config.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) {
                    continue;
                }
                for (String block : e.getValue()) {
                    if (block != null) {
                        map.put(block, e.getKey());
                    }
                }
            }
        }
        return new BlockGroupIndex(map);
    }

    /** 两方块是否同组（id 完全相同也算同组）。 */
    public boolean sameGroup(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.equals(b)) {
            return true;
        }
        String ga = blockToGroup.get(a);
        return ga != null && ga.equals(blockToGroup.get(b));
    }

    private static Map<String, String> buildDefaults() {
        Map<String, String> m = new HashMap<>();
        add(m, "coal", "minecraft:coal_ore", "minecraft:deepslate_coal_ore");
        add(m, "iron", "minecraft:iron_ore", "minecraft:deepslate_iron_ore");
        add(m, "copper", "minecraft:copper_ore", "minecraft:deepslate_copper_ore");
        add(m, "gold", "minecraft:gold_ore", "minecraft:deepslate_gold_ore", "minecraft:nether_gold_ore");
        add(m, "redstone", "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore");
        add(m, "emerald", "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore");
        add(m, "lapis", "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore");
        add(m, "diamond", "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore");
        add(m, "quartz", "minecraft:nether_quartz_ore");
        add(m, "debris", "minecraft:ancient_debris");
        return m;
    }

    private static void add(Map<String, String> map, String group, String... blocks) {
        for (String b : blocks) {
            map.put(b, group);
        }
    }
}
