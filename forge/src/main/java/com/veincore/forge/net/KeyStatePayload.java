package com.veincore.forge.net;

import net.minecraft.network.FriendlyByteBuf;

/** Forge 端：客户端 → 服务端的热键状态。 */
public record KeyStatePayload(boolean pressed) {

    public static void encode(KeyStatePayload msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.pressed());
    }

    public static KeyStatePayload decode(FriendlyByteBuf buf) {
        return new KeyStatePayload(buf.readBoolean());
    }
}
