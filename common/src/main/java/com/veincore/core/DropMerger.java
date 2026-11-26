package com.veincore.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 掉落物合并：同一坐标 + 同物品的掉落计数合并，供平台层一次性弹出。 */
public final class DropMerger {

    public record Entry(Pos pos, String item, int count) {
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>();

    public void add(Pos pos, String item, int count) {
        if (count <= 0) {
            return;
        }
        String key = pos.x() + "," + pos.y() + "," + pos.z() + "|" + item;
        entries.merge(key, new Entry(pos, item, count),
                (a, b) -> new Entry(a.pos(), a.item(), a.count() + b.count()));
    }

    public List<Entry> entries() {
        return List.copyOf(entries.values());
    }

    public void clear() {
        entries.clear();
    }
}
