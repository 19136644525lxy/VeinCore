package com.veincore.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.veincore.core.MaxBlocksLimiter;
import com.veincore.core.pattern.PatternId;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/** /veinminer 全套命令（M3）：权限管理、个人上限、模式、统计、方块组、热重载。 */
public final class FabricCommands {

    private FabricCommands() {
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
                        .executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("settings")
                        .executes(ctx -> settings(ctx.getSource()))
                        .then(Commands.literal("maxblocks")
                                .then(Commands.argument("blocks", IntegerArgumentType.integer(1))
                                        .executes(ctx -> settingsMaxBlocks(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "blocks"))))))
                .then(Commands.literal("mode")
                        .then(Commands.argument("pattern", StringArgumentType.word())
                                .executes(ctx -> mode(ctx.getSource(), StringArgumentType.getString(ctx, "pattern")))))
                .then(Commands.literal("stats")
                        .executes(ctx -> stats(ctx.getSource())))
                .then(Commands.literal("groups")
                        .executes(ctx -> groups(ctx.getSource()))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> groupMembers(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("reload")
                        .executes(ctx -> reload(ctx.getSource())))
                .then(Commands.literal("blocks")
                        .executes(ctx -> blocksList(ctx.getSource()))
                        .then(Commands.literal("add")
                                .then(Commands.argument("id", StringArgumentType.greedyString())
                                        .executes(ctx -> blocksAdd(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("id", StringArgumentType.greedyString())
                                        .executes(ctx -> blocksRemove(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))));
    }

    private static int allow(CommandSourceStack source, ServerPlayer target) {
        VeinCoreService service = VeinCoreFabric.service();
        if (service == null) {
            return notReady(source);
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
        VeinCoreService service = VeinCoreFabric.service();
        if (service == null) {
            return notReady(source);
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
        VeinCoreService service = VeinCoreFabric.service();
        if (service == null) {
            return notReady(source);
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
        VeinCoreService service = VeinCoreFabric.service();
        if (service == null) {
            return notReady(source);
        }
        ServerPlayer caller = playerOrNull(source);
        if (caller == null) {
            source.sendSuccess(() -> Component.literal("控制台：全权可用"), false);
            return 1;
        }
        boolean ok = service.isOwner(caller) || service.isAllowed(caller.getUUID());
        int limit = service.maxBlocksFor(caller.getUUID());
        source.sendSuccess(() -> Component.literal(ok
                ? "VeinCore: 已授权，可用连锁（当前上限 " + limit + " 格）"
                : "VeinCore: 未授权"), false);
        return 1;
    }

    private static int settings(CommandSourceStack source) {
        VeinCoreService service = VeinCoreFabric.service();
        if (service == null) {
            return notReady(source);
        }
        ServerPlayer caller = playerOrNull(source);
        if (caller == null) {
            source.sendSuccess(() -> Component.literal("控制台：服务器封顶 max_blocks="
                    + service.config().maxBlocks + "，默认玩家上限 default_player_blocks="
                    + service.config().defaultPlayerBlocks), false);
            return 1;
        }
        if (!authorized(source, service, caller)) {
            return 0;
        }
        UUID id = caller.getUUID();
        Integer personal = service.playerSettings().maxBlocks(id.toString());
        int limit = service.maxBlocksFor(id);
        source.sendSuccess(() -> Component.literal("你的连锁上限：" + limit
                + "（个人设置=" + (personal == null ? "未设置" : personal)
                + "，服务器封顶=" + service.config().maxBlocks
                + "，默认=" + service.config().defaultPlayerBlocks + "）"), false);
        source.sendSuccess(() -> Component.literal("修改：/veinminer settings maxblocks <64–1280>"), false);
        return 1;
    }

    private static int settingsMaxBlocks(CommandSourceStack source, int requested) {
        VeinCoreService service = VeinCoreFabric.service();
        if (service == null) {
            return notReady(source);
        }
        ServerPlayer caller = playerOrNull(source);
        if (caller == null) {
            source.sendFailure(Component.literal("控制台请通过配置文件设置 max_blocks / default_player_blocks"));
            return 0;
        }
        if (!authorized(source, service, caller)) {
            return 0;
        }
        int cap = service.config().maxBlocks;
        if (requested < MaxBlocksLimiter.MIN || requested > MaxBlocksLimiter.MAX) {
            source.sendFailure(Component.literal("范围 " + MaxBlocksLimiter.MIN + "–" + MaxBlocksLimiter.MAX
                    + "（超出部分会被服务器封顶 " + cap + "）"));
            return 0;
        }
        int effective = MaxBlocksLimiter.effective(requested, cap);
        service.playerSettings().setMaxBlocks(caller.getUUID().toString(), effective);
        service.savePlayers();
        source.sendSuccess(() -> Component.literal("你的连锁上限已设为 " + effective
                + (effective < requested ? "（受服务器封顶 " + cap + "）" : "")), false);
        return 1;
    }

    private static int mode(CommandSourceStack source, String key) {
        VeinCoreService service = VeinCoreFabric.service();
        if (service == null) {
            return notReady(source);
        }
        ServerPlayer caller = playerOrNull(source);
        if (caller == null) {
            source.sendFailure(Component.literal("仅玩家可用（也支持 Shift+热键+滚轮切换）"));
            return 0;
        }
        if (!authorized(source, service, caller)) {
            return 0;
        }
        PatternId pattern = PatternId.byKey(key);
        if (pattern == null) {
            source.sendFailure(Component.literal("未知模式：" + key + "，可选："
                    + keys()));
            return 0;
        }
        service.setPattern(caller.getUUID(), pattern.key);
        source.sendSuccess(() -> Component.literal("当前模式：" + pattern.key), false);
        return 1;
    }

    private static int stats(CommandSourceStack source) {
        VeinCoreService service = VeinCoreFabric.service();
        if (service == null) {
            return notReady(source);
        }
        ServerPlayer caller = playerOrNull(source);
        if (caller == null) {
            source.sendFailure(Component.literal("仅玩家可用"));
            return 0;
        }
        if (!authorized(source, service, caller)) {
            return 0;
        }
        String id = caller.getUUID().toString();
        int chains = service.playerSettings().chains(id);
        int blocks = service.playerSettings().blocks(id);
        source.sendSuccess(() -> Component.literal("连锁次数：" + chains + "，累计破坏：" + blocks + " 格"), false);
        return 1;
    }

    private static int groups(CommandSourceStack source) {
        VeinCoreService service = VeinCoreFabric.service();
        if (service == null) {
            return notReady(source);
        }
        if (!isOperator(source) && !isAuthorizedPlayer(source, service)) {
            return 0;
        }
        List<String> names = List.copyOf(service.groups().groupNames());
        names.sort(String::compareTo);
        source.sendSuccess(() -> Component.literal("内置/数据包方块组：" + String.join(", ", names)
                + "（用 /veinminer groups <名称> 查看成员）"), false);
        return 1;
    }

    private static int groupMembers(CommandSourceStack source, String name) {
        VeinCoreService service = VeinCoreFabric.service();
        if (service == null) {
            return notReady(source);
        }
        if (!isOperator(source) && !isAuthorizedPlayer(source, service)) {
            return 0;
        }
        List<String> members = service.groups().members(name);
        if (members.isEmpty()) {
            source.sendFailure(Component.literal("没有名为 " + name + " 的方块组"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(name + ": " + String.join(", ", members)), false);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        VeinCoreService service = VeinCoreFabric.service();
        if (service == null) {
            return notReady(source);
        }
        if (!isOperator(source)) {
            source.sendFailure(Component.literal("仅房主/服主/控制台可重载配置"));
            return 0;
        }
        service.reload(source.getServer().getResourceManager());
        source.sendSuccess(() -> Component.literal("VeinCore 配置已重载（server/access/players/数据包方块组）"), true);
        return 1;
    }

    private static int blocksList(CommandSourceStack source) {
        VeinCoreService service = VeinCoreFabric.service();
        if (service == null) {
            return notReady(source);
        }
        ServerPlayer caller = playerOrNull(source);
        if (caller == null) {
            source.sendFailure(Component.literal("仅玩家可用"));
            return 0;
        }
        if (!authorized(source, service, caller)) {
            return 0;
        }
        List<String> personal = service.playerSettings().allowBlocks(caller.getUUID().toString());
        if (personal.isEmpty()) {
            source.sendSuccess(() -> Component.literal("个人白名单未设置：可连锁所有非黑名单方块"), false);
        } else {
            source.sendSuccess(() -> Component.literal("个人白名单（" + personal.size() + "）："
                    + String.join(", ", personal)), false);
        }
        return 1;
    }

    private static int blocksAdd(CommandSourceStack source, String raw) {
        VeinCoreService service = VeinCoreFabric.service();
        if (service == null) {
            return notReady(source);
        }
        ServerPlayer caller = playerOrNull(source);
        if (caller == null) {
            source.sendFailure(Component.literal("仅玩家可用"));
            return 0;
        }
        if (!authorized(source, service, caller)) {
            return 0;
        }
        String id = normalizeBlock(raw);
        if (id == null) {
            source.sendFailure(Component.literal("未知方块：" + raw + "（示例：minecraft:iron_ore）"));
            return 0;
        }
        if (!service.playerSettings().addAllowBlock(caller.getUUID().toString(), id)) {
            source.sendFailure(Component.literal("该方块已在个人白名单中：" + id));
            return 0;
        }
        service.savePlayers();
        source.sendSuccess(() -> Component.literal("已加入个人白名单：" + id), false);
        return 1;
    }

    private static int blocksRemove(CommandSourceStack source, String raw) {
        VeinCoreService service = VeinCoreFabric.service();
        if (service == null) {
            return notReady(source);
        }
        ServerPlayer caller = playerOrNull(source);
        if (caller == null) {
            source.sendFailure(Component.literal("仅玩家可用"));
            return 0;
        }
        if (!authorized(source, service, caller)) {
            return 0;
        }
        String id = normalizeBlock(raw);
        if (id == null) {
            source.sendFailure(Component.literal("未知方块：" + raw));
            return 0;
        }
        if (!service.playerSettings().removeAllowBlock(caller.getUUID().toString(), id)) {
            source.sendFailure(Component.literal("该方块不在个人白名单中：" + id));
            return 0;
        }
        service.savePlayers();
        source.sendSuccess(() -> Component.literal("已从个人白名单移除：" + id), false);
        return 1;
    }

    /** 规范化方块 id（小写、补 minecraft: 前缀）并校验注册表；未知返回 null。 */
    private static String normalizeBlock(String raw) {
        String id = raw.trim().toLowerCase(java.util.Locale.ROOT);
        if (id.isEmpty()) {
            return null;
        }
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !BuiltInRegistries.BLOCK.containsKey(rl)) {
            return null;
        }
        return id;
    }

    /** 已授权玩家（owner 或 access 列表）。失败时已发提示并返回 false。 */
    private static boolean authorized(CommandSourceStack source, VeinCoreService service, ServerPlayer caller) {        if (service.isOwner(caller) || service.isAllowed(caller.getUUID())) {
            return true;
        }
        source.sendFailure(Component.literal("未授权，无法使用该命令（请房主/服主 /veinminer allow）"));
        return false;
    }

    private static boolean isAuthorizedPlayer(CommandSourceStack source, VeinCoreService service) {
        ServerPlayer caller = playerOrNull(source);
        return caller != null && (service.isOwner(caller) || service.isAllowed(caller.getUUID()));
    }

    private static ServerPlayer playerOrNull(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            return null;
        }
    }

    private static boolean isOperator(CommandSourceStack source) {
        ServerPlayer caller = playerOrNull(source);
        VeinCoreService service = VeinCoreFabric.service();
        return caller == null || (service != null && service.isOwner(caller));
    }

    private static int notReady(CommandSourceStack source) {
        source.sendFailure(Component.literal("VeinCore 尚未加载"));
        return 0;
    }

    private static String keys() {
        StringBuilder sb = new StringBuilder();
        for (PatternId id : PatternId.values()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(id.key);
        }
        return sb.toString();
    }
}
