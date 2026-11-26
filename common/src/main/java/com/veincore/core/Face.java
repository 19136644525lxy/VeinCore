package com.veincore.core;

/** 触发面（玩家瞄准的方块面）。 */
public enum Face {
    UP(0, 1, 0, 1),
    DOWN(0, -1, 0, 1),
    NORTH(0, 0, -1, 2),
    SOUTH(0, 0, 1, 2),
    EAST(1, 0, 0, 0),
    WEST(-1, 0, 0, 0);

    public final int dx;
    public final int dy;
    public final int dz;
    /** 面法线所在主轴：0=X, 1=Y, 2=Z。 */
    public final int axis;

    Face(int dx, int dy, int dz, int axis) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.axis = axis;
    }

    public Face opposite() {
        return switch (this) {
            case UP -> DOWN;
            case DOWN -> UP;
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }
}
