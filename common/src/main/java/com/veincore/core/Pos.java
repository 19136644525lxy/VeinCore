package com.veincore.core;

/** 不可变整型方块坐标（与 Minecraft BlockPos 解耦，common 不依赖 MC 类）。 */
public record Pos(int x, int y, int z) {

    public Pos offset(int dx, int dy, int dz) {
        return new Pos(x + dx, y + dy, z + dz);
    }

    public Pos offset(Pos delta) {
        return new Pos(x + delta.x, y + delta.y, z + delta.z);
    }

    /** 6 邻接方向偏移表（BFS 用）。 */
    public static final int[][] NEIGHBOR_OFFSETS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };
}
