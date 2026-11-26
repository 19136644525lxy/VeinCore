package com.veincore.core;

/** 挖掘上限：玩家可设 64–1280，服务器封顶；超过 600 格放行但警告。 */
public final class MaxBlocksLimiter {

    public static final int MIN = 64;
    public static final int MAX = 1280;
    public static final int WARN_THRESHOLD = 600;
    public static final int SERVER_FALLBACK = 1280;

    private MaxBlocksLimiter() {
    }

    /** 玩家设定值与服务器封顶取较小，并夹在 [MIN, MAX]。 */
    public static int effective(int playerSet, int serverMax) {
        int cap = serverMax <= 0 ? SERVER_FALLBACK : serverMax;
        int v = Math.min(playerSet, cap);
        return Math.max(MIN, Math.min(MAX, v));
    }

    public static boolean overWarn(int planned) {
        return planned > WARN_THRESHOLD;
    }
}
