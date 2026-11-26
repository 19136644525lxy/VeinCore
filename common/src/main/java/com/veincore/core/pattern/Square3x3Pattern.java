package com.veincore.core.pattern;

import com.veincore.core.Pos;
import com.veincore.core.ViewAxis;

import java.util.ArrayList;
import java.util.List;

/** 3×3 小方块：垂直于视线主轴的一层 3×3（共 9 格，深度 1）。 */
public final class Square3x3Pattern implements IMiningPattern {

    @Override
    public PatternId id() {
        return PatternId.SQUARE_3X3;
    }

    @Override
    public List<Pos> collect(PatternContext ctx) {
        List<Pos> out = new ArrayList<>();
        out.add(ctx.origin());
        ViewAxis axis = ctx.viewAxis();
        for (int du = -1; du <= 1 && out.size() < ctx.maxBlocks(); du++) {
            for (int dv = -1; dv <= 1 && out.size() < ctx.maxBlocks(); dv++) {
                if (du == 0 && dv == 0) {
                    continue;
                }
                Pos candidate = at(ctx.origin(), du, dv, axis);
                if (ctx.canMine().test(candidate)) {
                    out.add(candidate);
                }
            }
        }
        return out;
    }

    private static Pos at(Pos origin, int du, int dv, ViewAxis axis) {
        int x = origin.x();
        int y = origin.y();
        int z = origin.z();
        switch (axis.u()) {
            case 0 -> x += du;
            case 1 -> y += du;
            default -> z += du;
        }
        switch (axis.v()) {
            case 0 -> x += dv;
            case 1 -> y += dv;
            default -> z += dv;
        }
        return new Pos(x, y, z);
    }
}
