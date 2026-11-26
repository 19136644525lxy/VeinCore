package com.veincore.core.pattern;

import com.veincore.core.Face;
import com.veincore.core.Pos;

import java.util.ArrayList;
import java.util.List;

/** 隧道 3×3：沿触发面法线向内部推进，截面 3×3，遇整层不可挖即停。 */
public final class Tunnel3x3Pattern implements IMiningPattern {

    @Override
    public PatternId id() {
        return PatternId.TUNNEL_3X3;
    }

    @Override
    public List<Pos> collect(PatternContext ctx) {
        List<Pos> out = new ArrayList<>();
        Face dir = ctx.face().opposite();
        int main = ctx.face().axis;
        int axisU = (main + 1) % 3;
        int axisV = (main + 2) % 3;
        Pos layerCenter = ctx.origin();
        while (out.size() < ctx.maxBlocks()) {
            boolean any = false;
            for (int du = -1; du <= 1 && out.size() < ctx.maxBlocks(); du++) {
                for (int dv = -1; dv <= 1 && out.size() < ctx.maxBlocks(); dv++) {
                    Pos candidate = offsetOnAxes(layerCenter, axisU, du, axisV, dv);
                    if (ctx.canMine().test(candidate)) {
                        out.add(candidate);
                        any = true;
                    }
                }
            }
            if (!any) {
                break;
            }
            layerCenter = layerCenter.offset(dir.dx, dir.dy, dir.dz);
        }
        return out;
    }

    static Pos offsetOnAxes(Pos p, int axisA, int da, int axisB, int db) {
        int x = p.x();
        int y = p.y();
        int z = p.z();
        switch (axisA) {
            case 0 -> x += da;
            case 1 -> y += da;
            default -> z += da;
        }
        switch (axisB) {
            case 0 -> x += db;
            case 1 -> y += db;
            default -> z += db;
        }
        return new Pos(x, y, z);
    }
}
