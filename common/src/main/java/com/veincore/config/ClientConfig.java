package com.veincore.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 客户端配置（config/veincore-client.json）。 */
public final class ClientConfig {

    public boolean enabled = true;
    public boolean indicatorEnabled = true;
    /** gui 或 text。 */
    public String indicatorMode = "gui";
    public boolean showStatus = true;
    public boolean showBlockCount = true;
    public boolean outlineEnabled = true;
    public int previewRenderCap = 256;
    public int recentPatternsSize = 5;
    public int iconSize = 10;
    public int offsetX = 30;
    public int offsetY = 12;
    public boolean toggleMode = false;

    public static ClientConfig fromMap(Map<String, Object> map) {
        ClientConfig c = new ClientConfig();
        if (map == null) {
            return c;
        }
        c.enabled = boolVal(map.get("enabled"), c.enabled);
        c.indicatorEnabled = boolVal(map.get("indicator_enabled"), c.indicatorEnabled);
        c.indicatorMode = str(map.get("indicator_mode"), c.indicatorMode);
        c.showStatus = boolVal(map.get("show_status"), c.showStatus);
        c.showBlockCount = boolVal(map.get("show_block_count"), c.showBlockCount);
        c.outlineEnabled = boolVal(map.get("outline_enabled"), c.outlineEnabled);
        c.previewRenderCap = intVal(map.get("preview_render_cap"), c.previewRenderCap);
        c.recentPatternsSize = intVal(map.get("recent_patterns_size"), c.recentPatternsSize);
        c.iconSize = intVal(map.get("icon_size"), c.iconSize);
        c.offsetX = intVal(map.get("offset_x"), c.offsetX);
        c.offsetY = intVal(map.get("offset_y"), c.offsetY);
        c.toggleMode = boolVal(map.get("toggle_mode"), c.toggleMode);
        return c;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", enabled);
        m.put("indicator_enabled", indicatorEnabled);
        m.put("indicator_mode", indicatorMode);
        m.put("show_status", showStatus);
        m.put("show_block_count", showBlockCount);
        m.put("outline_enabled", outlineEnabled);
        m.put("preview_render_cap", previewRenderCap);
        m.put("recent_patterns_size", recentPatternsSize);
        m.put("icon_size", iconSize);
        m.put("offset_x", offsetX);
        m.put("offset_y", offsetY);
        m.put("toggle_mode", toggleMode);
        return Collections.unmodifiableMap(m);
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
}
