package com.veincore.core.pattern;

import com.veincore.core.Pos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 平面：锁定触发方块所在 Y 层，沿水平 4 邻接连锁扩展。 */
public final class PlanePattern implements IMiningPattern {

    private static final int[][] HORIZONTAL = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    @Override
    public PatternId id() {
        return PatternId.PLANE;
    }

    @Override
    public List<Pos> collect(PatternContext ctx) {
        List<Pos> out = new ArrayList<>();
        Set<Pos> seen = new HashSet<>();
        Deque<Pos> queue = new ArrayDeque<>();
        int y = ctx.origin().y();
        out.add(ctx.origin());
        seen.add(ctx.origin());
        queue.add(ctx.origin());
        while (!queue.isEmpty() && out.size() < ctx.maxBlocks()) {
            Pos cur = queue.poll();
            for (int[] d : HORIZONTAL) {
                Pos next = new Pos(cur.x() + d[0], y, cur.z() + d[1]);
                if (!seen.add(next)) {
                    continue;
                }
                if (!ctx.canMine().test(next)) {
                    continue;
                }
                out.add(next);
                queue.add(next);
                if (out.size() >= ctx.maxBlocks()) {
                    break;
                }
            }
        }
        return out;
    }
}
