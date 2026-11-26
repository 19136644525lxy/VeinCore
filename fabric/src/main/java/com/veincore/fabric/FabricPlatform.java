package com.veincore.fabric;

import com.veincore.core.Pos;
import com.veincore.platform.IPlatform;
import com.veincore.platform.PlayerHandle;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.UUID;

/** Fabric 平台实现（M1）。 */
public final class FabricPlatform implements IPlatform {

    private static final Logger LOG = LoggerFactory.getLogger("veincore");

    private final MinecraftServer server;

    public FabricPlatform(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer server() {
        return server;
    }

    @Override
    public String loaderId() {
        return "fabric";
    }

    @Override
    public boolean isClient() {
        return false;
    }

    @Override
    public boolean isServer() {
        return true;
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void info(String message) {
        LOG.info(message);
    }

    @Override
    public void warn(String message) {
        LOG.warn(message);
    }

    @Override
    public String uuidOf(PlayerHandle player) {
        return player.id();
    }

    @Override
    public long serverTick() {
        return server.getTickCount();
    }

    @Override
    public void requestBreak(PlayerHandle player, Pos pos) {
        ServerPlayer sp = playerById(player.id());
        if (sp == null) {
            return;
        }
        ServerLevel level = sp.serverLevel();
        BlockPos bp = new BlockPos(pos.x(), pos.y(), pos.z());
        if (level.getBlockState(bp).isAir()) {
            return;
        }
        level.destroyBlock(bp, true);
    }

    /** 按 UUID 字符串取在线玩家。 */
    public ServerPlayer playerById(String id) {
        try {
            return server.getPlayerList().getPlayer(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
