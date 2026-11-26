package com.veincore.fabric.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：连锁热键按住状态。 */
public record KeyStatePayload(boolean pressed) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<KeyStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("veincore", "key_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KeyStatePayload> CODEC =
            StreamCodec.of(KeyStatePayload::encode, KeyStatePayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, KeyStatePayload payload) {
        buf.writeBoolean(payload.pressed);
    }

    private static KeyStatePayload decode(RegistryFriendlyByteBuf buf) {
        return new KeyStatePayload(buf.readBoolean());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
