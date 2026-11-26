package com.veincore.forge.net;

import com.veincore.forge.VeinCoreForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/** Forge 网络（M2）：热键状态与模式选择（C2S）。 */
public final class ForgeNetwork {

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(VeinCoreForge.MOD_ID, "main"))
            .networkProtocolVersion(1)
            .simpleChannel();

    private ForgeNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(KeyStatePayload.class, 0)
                .encoder(KeyStatePayload::encode)
                .decoder(KeyStatePayload::decode)
                .consumerMainThread((msg, ctx) -> ctx.enqueueWork(() -> {
                    ServerPlayer sp = ctx.getSender();
                    if (sp != null && VeinCoreForge.service() != null) {
                        VeinCoreForge.service().setPressed(sp.getUUID(), msg.pressed());
                    }
                }))
                .add();
        CHANNEL.messageBuilder(PatternSelectPayload.class, 1)
                .encoder(PatternSelectPayload::encode)
                .decoder(PatternSelectPayload::decode)
                .consumerMainThread((msg, ctx) -> ctx.enqueueWork(() -> {
                    ServerPlayer sp = ctx.getSender();
                    if (sp != null && VeinCoreForge.service() != null) {
                        VeinCoreForge.service().setPattern(sp.getUUID(), msg.pattern());
                    }
                }))
                .add();
    }

    /** 客户端 → 服务端：热键按住状态。 */
    public static void sendKeyState(boolean pressed) {
        CHANNEL.send(new KeyStatePayload(pressed), PacketDistributor.SERVER.noArg());
    }

    /** 客户端 → 服务端：切换后的模式。 */
    public static void sendPattern(String pattern) {
        CHANNEL.send(new PatternSelectPayload(pattern), PacketDistributor.SERVER.noArg());
    }
}
