package com.veincore.core.pattern;

import com.veincore.core.Face;
import com.veincore.core.Pos;

import java.util.ArrayList;
import java.util.List;

/** 隧道 1×1：沿触发面法线向方块内部推进，截面 1 格。 */
public final class Tunnel1x1Pattern implements IMiningPattern {

    @Override
    public PatternId id() {
        return PatternId.TUNNEL_1X1;
    }

    @Override
    public List<Pos> collect(PatternContext ctx) {
        List<Pos> out = new ArrayList<>();
        Face dir = ctx.face().opposite();
        Pos cur = ctx.origin();
        out.add(cur);
        while (out.size() < ctx.maxBlocks()) {
            cur = cur.offset(dir.dx, dir.dy, dir.dz);
            if (!ctx.canMine().test(cur)) {
                break;
            }
            out.add(cur);
        }
        return out;
    }
}
