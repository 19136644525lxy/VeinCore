package com.veincore.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Forge /veinminer 命令（M2）：allow / deny / list / status，仅 owner/控制台可授权。 */
public final class ForgeCommands {

    private ForgeCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("veinminer")
                .then(Commands.literal("allow")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> allow(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("deny")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> deny(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("status")
                        .executes(ctx -> status(ctx.getSource()))));
    }

    private static int allow(CommandSourceStack source, ServerPlayer target) {
        VeinCoreForgeService service = VeinCoreForge.service();
        if (service == null) {
            source.sendFailure(Component.literal("VeinCore 尚未加载"));
            return 0;
        }
        if (!isOperator(source)) {
            source.sendFailure(Component.literal("仅房主/服主/控制台可授权"));
            return 0;
        }
        if (service.isOwner(target)) {
            source.sendFailure(Component.literal("该玩家已是 owner，无需授权"));
            return 0;
        }
        if (!service.access().allow(target.getUUID().toString())) {
            source.sendFailure(Component.literal("该玩家已在授权列表中"));
            return 0;
        }
        service.saveAccess();
        source.sendSuccess(() -> Component.literal("已授权 " + target.getGameProfile().getName()), true);
        return 1;
    }

    private static int deny(CommandSourceStack source, ServerPlayer target) {
        VeinCoreForgeService service = VeinCoreForge.service();
        if (service == null) {
            source.sendFailure(Component.literal("VeinCore 尚未加载"));
            return 0;
        }
        if (!isOperator(source)) {
            source.sendFailure(Component.literal("仅房主/服主/控制台可取消授权"));
            return 0;
        }
        if (!service.access().deny(target.getUUID().toString())) {
            source.sendFailure(Component.literal("该玩家不在授权列表中（owner 不可取消）"));
            return 0;
        }
        service.saveAccess();
        source.sendSuccess(() -> Component.literal("已取消授权 " + target.getGameProfile().getName()), true);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        VeinCoreForgeService service = VeinCoreForge.service();
        if (service == null) {
            source.sendFailure(Component.literal("VeinCore 尚未加载"));
            return 0;
        }
        if (!isOperator(source)) {
            source.sendFailure(Component.literal("仅房主/服主/控制台可查看全部列表；用 /veinminer status 查看自己"));
            return 0;
        }
        String owners = String.join(", ", service.access().owners());
        String allowed = String.join(", ", service.access().allowed());
        source.sendSuccess(() -> Component.literal("owner: " + (owners.isEmpty() ? "（无）" : owners)), false);
        source.sendSuccess(() -> Component.literal("allowed: " + (allowed.isEmpty() ? "（无）" : allowed)), false);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        ServerPlayer caller;
        try {
            caller = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendSuccess(() -> Component.literal("控制台：全权可用"), false);
            return 1;
        }
        VeinCoreForgeService service = VeinCoreForge.service();
        if (service == null) {
            source.sendFailure(Component.literal("VeinCore 尚未加载"));
            return 0;
        }
        boolean ok = service.isOwner(caller) || service.isAllowed(caller.getUUID());
        source.sendSuccess(() -> Component.literal(ok ? "VeinCore: 已授权，可用连锁" : "VeinCore: 未授权"), false);
        return 1;
    }

    private static boolean isOperator(CommandSourceStack source) {
        try {
            ServerPlayer caller = source.getPlayerOrException();
            return VeinCoreForge.service() != null && VeinCoreForge.service().isOwner(caller);
        } catch (CommandSyntaxException e) {
            return true; // 控制台
        }
    }
}
