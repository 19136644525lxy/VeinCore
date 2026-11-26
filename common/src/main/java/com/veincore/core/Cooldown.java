package com.veincore.core;

/** 冷却：以 tick 计数判断是否可再次触发。 */
public final class Cooldown {

    private final int ticks;
    private long lastTick = Long.MIN_VALUE;

    public Cooldown(int ticks) {
        this.ticks = Math.max(0, ticks);
    }

    /** @return 冷却是够结束（现在可以触发） */
    public boolean ready(long nowTick) {
        if (nowTick - lastTick >= ticks) {
            lastTick = nowTick;
            return true;
        }
        return false;
    }

    public int ticks() {
        return ticks;
    }

    /** 剩余冷却 tick（0 = 可用）。只读，不更新 lastTick。 */
    public int remaining(long nowTick) {
        long left = (long) ticks - (nowTick - lastTick);
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, left));
    }
}
