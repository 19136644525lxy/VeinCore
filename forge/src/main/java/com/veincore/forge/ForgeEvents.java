package com.veincore.forge;

import com.veincore.core.Pos;
import com.veincore.platform.PlayerHandle;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge 事件（M2）：破坏拦截、服务端生命周期、登出清理、命令注册。 */
@Mod.EventBusSubscriber(modid = VeinCoreForge.MOD_ID)
public final class ForgeEvents {

    private ForgeEvents() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        VeinCoreForgeService service = VeinCoreForge.service();
        if (service == null) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer sp)) {
            return;
        }
        PlayerHandle handle = new PlayerHandle(sp.getUUID().toString());
        Pos pos = new Pos(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());
        if (!service.handleBreak(handle, pos)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        VeinCoreForgeService service = new VeinCoreForgeService(new ForgePlatform(event.getServer()));
        service.loadDataPack(event.getServer().getResourceManager());
        VeinCoreForge.setService(service);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        VeinCoreForgeService service = VeinCoreForge.service();
        if (service != null) {
            service.savePlayers();
        }
        VeinCoreForge.setService(null);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        VeinCoreForgeService service = VeinCoreForge.service();
        if (service != null && event.getEntity() instanceof ServerPlayer sp) {
            service.forget(new PlayerHandle(sp.getUUID().toString()));
        }
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        ForgeCommands.register(event.getDispatcher());
    }
}
