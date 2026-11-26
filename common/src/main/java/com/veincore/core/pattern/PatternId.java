package com.veincore.core.pattern;

/** 挖掘模式 id（与图标文件名一致）。 */
public enum PatternId {
    VEIN("vein"),
    CHAIN("chain"),
    SQUARE_3X3("square_3x3"),
    TUNNEL_1X1("tunnel_1x1"),
    TUNNEL_3X3("tunnel_3x3"),
    DIAGONAL("diagonal"),
    PLANE("plane");

    public final String key;

    PatternId(String key) {
        this.key = key;
    }

    /** 图标/资源回退 id。 */
    public static final String FALLBACK_KEY = "unknown";

    /** 按 key 查找；未知返回 null（由调用方决定 fallback）。 */
    public static PatternId byKey(String key) {
        for (PatternId p : values()) {
            if (p.key.equals(key)) {
                return p;
            }
        }
        return null;
    }
}
