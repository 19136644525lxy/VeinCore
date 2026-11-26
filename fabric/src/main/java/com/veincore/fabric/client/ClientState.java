package com.veincore.fabric.client;

import com.veincore.fabric.net.VeinPreviewPayload;

/** 客户端预览状态：最新一帧服务端预览 + 过期计时。 */
public final class ClientState {

    /** 超过该 tick 数未刷新则视为过期（服务端每 10 tick 刷新）。 */
    private static final int EXPIRE_TICKS = 40;
    private static volatile VeinPreviewPayload preview;
    private static int age;

    private ClientState() {
    }

    public static void update(VeinPreviewPayload payload) {
        preview = payload;
        age = 0;
    }

    public static void tick() {
        VeinPreviewPayload current = preview;
        if (current == null) {
            return;
        }
        if (++age > EXPIRE_TICKS) {
            preview = null;
        }
    }

    public static VeinPreviewPayload preview() {
        return preview;
    }
}
