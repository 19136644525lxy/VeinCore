package com.veincore.fabric.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：玩家当前选择的挖掘模式。 */
public record PatternSelectPayload(String pattern) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PatternSelectPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("veincore", "pattern_select"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PatternSelectPayload> CODEC =
            StreamCodec.of(PatternSelectPayload::encode, PatternSelectPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, PatternSelectPayload payload) {
        buf.writeUtf(payload.pattern);
    }

    private static PatternSelectPayload decode(RegistryFriendlyByteBuf buf) {
        return new PatternSelectPayload(buf.readUtf());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
