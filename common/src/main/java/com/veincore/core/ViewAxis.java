package com.veincore.core;

/**
 * 玩家视线主轴与两个正交轴（用于 square_3x3 等面平面模式）。
 * main/u/v 取值 0=X, 1=Y, 2=Z。
 */
public record ViewAxis(int main, int u, int v) {

    /** 由视线向量求主轴：取绝对值最大的坐标轴，避免 45° 斜视歧义。 */
    public static ViewAxis fromLook(double dx, double dy, double dz) {
        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);
        if (ax >= ay && ax >= az) {
            return new ViewAxis(0, 2, 1); // 主轴 X，平面轴 Z/Y
        }
        if (ay >= ax && ay >= az) {
            return new ViewAxis(1, 0, 2); // 主轴 Y，平面轴 X/Z
        }
        return new ViewAxis(2, 0, 1); // 主轴 Z，平面轴 X/Y
    }

    /** 按轴索引给坐标加 1 个单位。 */
    public static int add(int x, int y, int z, int axis, int delta) {
        return switch (axis) {
            case 0 -> x + delta;
            case 1 -> y + delta;
            default -> z + delta;
        };
    }
}
