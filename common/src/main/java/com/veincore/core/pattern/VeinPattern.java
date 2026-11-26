package com.veincore.core.pattern;

import com.veincore.core.Pos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 矿脉：6 邻接 BFS，只沿同一方块组扩展。 */
public final class VeinPattern implements IMiningPattern {

    @Override
    public PatternId id() {
        return PatternId.VEIN;
    }

    @Override
    public List<Pos> collect(PatternContext ctx) {
        List<Pos> out = new ArrayList<>();
        Set<Pos> seen = new HashSet<>();
        Deque<Pos> queue = new ArrayDeque<>();
        out.add(ctx.origin());
        seen.add(ctx.origin());
        queue.add(ctx.origin());
        while (!queue.isEmpty() && out.size() < ctx.maxBlocks()) {
            Pos cur = queue.poll();
            for (int[] d : Pos.NEIGHBOR_OFFSETS) {
                Pos next = cur.offset(d[0], d[1], d[2]);
                if (!seen.add(next)) {
                    continue;
                }
                if (!ctx.sameGroup().test(next) || !ctx.canMine().test(next)) {
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
