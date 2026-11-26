package com.veincore.fabric.net;

import com.veincore.core.Pos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** S2C：预览数据（模式 / 状态 / 总数 / 剩余冷却 / 渲染用方块子集）。 */
public record VeinPreviewPayload(String pattern, String status, int total, int cooldownTicks,
                                 List<Pos> blocks) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("veincore", "preview");
    public static final Type<VeinPreviewPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, VeinPreviewPayload> CODEC = StreamCodec.of(
            VeinPreviewPayload::write, VeinPreviewPayload::read);

    private static void write(FriendlyByteBuf buf, VeinPreviewPayload p) {
        buf.writeUtf(p.pattern, 64);
        buf.writeUtf(p.status, 32);
        buf.writeVarInt(p.total);
        buf.writeVarInt(p.cooldownTicks);
        buf.writeVarInt(p.blocks.size());
        for (Pos pos : p.blocks) {
            buf.writeVarInt(pos.x());
            buf.writeVarInt(pos.y());
            buf.writeVarInt(pos.z());
        }
    }

    private static VeinPreviewPayload read(FriendlyByteBuf buf) {
        String pattern = buf.readUtf(64);
        String status = buf.readUtf(32);
        int total = buf.readVarInt();
        int cooldown = buf.readVarInt();
        int n = buf.readVarInt();
        List<Pos> blocks = new ArrayList<>(Math.min(n, 512));
        for (int i = 0; i < n; i++) {
            blocks.add(new Pos(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        }
        return new VeinPreviewPayload(pattern, status, total, cooldown, List.copyOf(blocks));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
