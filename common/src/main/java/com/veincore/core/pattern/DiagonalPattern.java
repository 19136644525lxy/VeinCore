package com.veincore.core.pattern;

import com.veincore.core.Face;
import com.veincore.core.Pos;

import java.util.ArrayList;
import java.util.List;

/** 斜向：沿触发面反向做 45° 阶梯（每前进 1 格下降 1 格）；竖直面退化为沿面方向。 */
public final class DiagonalPattern implements IMiningPattern {

    @Override
    public PatternId id() {
        return PatternId.DIAGONAL;
    }

    @Override
    public List<Pos> collect(PatternContext ctx) {
        List<Pos> out = new ArrayList<>();
        Face dir = ctx.face().opposite();
        int dy = dir.dy != 0 ? dir.dy : -1;
        Pos cur = ctx.origin();
        out.add(cur);
        while (out.size() < ctx.maxBlocks()) {
            cur = cur.offset(dir.dx, dy, dir.dz);
            if (!ctx.canMine().test(cur)) {
                break;
            }
            out.add(cur);
        }
        return out;
    }
}
