package com.veincore.forge.net;

import net.minecraft.network.FriendlyByteBuf;

/** Forge 端：客户端 → 服务端的模式选择。 */
public record PatternSelectPayload(String pattern) {

    public static void encode(PatternSelectPayload msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.pattern());
    }

    public static PatternSelectPayload decode(FriendlyByteBuf buf) {
        return new PatternSelectPayload(buf.readUtf());
    }
}
