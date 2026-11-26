package com.veincore.forge;

import com.veincore.forge.net.ForgeNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Forge 入口（M2）：网络注册、事件总线、服务端 service 生命周期。 */
@Mod(VeinCoreForge.MOD_ID)
public final class VeinCoreForge {

    public static final String MOD_ID = "veincore";

    private static VeinCoreForgeService service;

    public VeinCoreForge(FMLJavaModLoadingContext context) {
        ForgeNetwork.register();
    }

    public static VeinCoreForgeService service() {
        return service;
    }

    public static void setService(VeinCoreForgeService newService) {
        service = newService;
    }
}
