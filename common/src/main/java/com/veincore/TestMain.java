package com.veincore;

import com.veincore.auth.AccessManager;
import com.veincore.config.ClientConfig;
import com.veincore.config.JsonStore;
import com.veincore.config.MiniJson;
import com.veincore.config.ServerConfig;
import com.veincore.core.BreakPlan;
import com.veincore.core.Face;
import com.veincore.core.MaxBlocksLimiter;
import com.veincore.core.Pos;
import com.veincore.core.ViewAxis;
import com.veincore.core.pattern.ChainPattern;
import com.veincore.core.pattern.DiagonalPattern;
import com.veincore.core.pattern.PatternContext;
import com.veincore.core.pattern.PlanePattern;
import com.veincore.core.pattern.Square3x3Pattern;
import com.veincore.core.pattern.Tunnel1x1Pattern;
import com.veincore.core.pattern.Tunnel3x3Pattern;
import com.veincore.core.pattern.VeinPattern;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** M0 本地自测：不依赖 Minecraft、不依赖 JUnit，javac 编译后 java 运行。 */
public final class TestMain {

    private static int passed = 0;

    public static void main(String[] args) throws Exception {
        testPos();
        testViewAxis();
        testLimiter();
        testMiniJson();
        testConfigs();
        testAccess();
        testJsonStore();
        testSquare3x3();
        testVein();
        testChain();
        testTunnel1x1();
        testTunnel3x3();
        testDiagonal();
        testPlane();
        System.out.println("ALL TESTS PASSED (" + passed + " checks)");
    }

    private static void testPos() {
        check(new Pos(1, 2, 3).offset(1, -1, 0).equals(new Pos(2, 1, 3)), "Pos.offset 三参");
        check(new Pos(0, 0, 0).offset(new Pos(1, 0, 0)).equals(new Pos(1, 0, 0)), "Pos.offset 坐标");
        check(Pos.NEIGHBOR_OFFSETS.length == 6, "6 邻接表");
    }

    private static void testViewAxis() {
        check(ViewAxis.fromLook(1, 0, 0).equals(new ViewAxis(0, 2, 1)), "X 主轴");
        check(ViewAxis.fromLook(0, 1, 0).equals(new ViewAxis(1, 0, 2)), "Y 主轴");
        check(ViewAxis.fromLook(0, 0, 1).equals(new ViewAxis(2, 0, 1)), "Z 主轴");
        check(ViewAxis.fromLook(0.5, 0.4, 0.3).equals(new ViewAxis(0, 2, 1)), "斜视取最大轴");
    }

    private static void testLimiter() {
        check(MaxBlocksLimiter.effective(10, 1280) == 64, "下限 64");
        check(MaxBlocksLimiter.effective(1280, 640) == 640, "服务器封顶");
        check(MaxBlocksLimiter.effective(700, 640) == 640, "玩家值超封顶夹取");
        check(MaxBlocksLimiter.effective(512, 0) == 512, "0 用回退值");
        check(MaxBlocksLimiter.overWarn(601), "601 超限警告");
        check(!MaxBlocksLimiter.overWarn(600), "600 不警告");
    }

    private static void testMiniJson() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("name", "vein");
        source.put("max", 1280);
        source.put("enabled", true);
        source.put("list", List.of("a", "b"));
        String text = MiniJson.stringify(source);
        Object parsed = MiniJson.parse(text);
        check(parsed instanceof Map, "解析为对象");
        Map<?, ?> map = (Map<?, ?>) parsed;
        check("vein".equals(map.get("name")), "字符串往返");
        check(((Number) map.get("max")).intValue() == 1280, "数字往返");
        check(Boolean.TRUE.equals(map.get("enabled")), "布尔往返");
        check(List.of("a", "b").equals(map.get("list")), "数组往返");
        check("he said \"hi\"\nline2".equals(MiniJson.parse("\"he said \\\"hi\\\"\\nline2\"")), "转义解析");
    }

    private static void testConfigs() {
        ServerConfig server = ServerConfig.fromMap(Map.of());
        check(server.maxBlocks == 1280, "服务器默认上限");
        check(server.cooldownTicks == 10, "默认冷却");
        ServerConfig roundTrip = ServerConfig.fromMap(server.toMap());
        check(roundTrip.maxBlocks == 1280 && "vein".equals(roundTrip.defaultPattern), "服务器配置往返");

        ClientConfig client = ClientConfig.fromMap(Map.of(
                "indicator_mode", "text", "offset_x", 40, "preview_render_cap", 128));
        check("text".equals(client.indicatorMode), "客户端模式读取");
        check(client.offsetX == 40, "客户端偏移读取");
        ClientConfig clientRoundTrip = ClientConfig.fromMap(client.toMap());
        check("text".equals(clientRoundTrip.indicatorMode) && clientRoundTrip.previewRenderCap == 128,
                "客户端配置往返");
    }

    private static void testAccess() {
        AccessManager access = new AccessManager();
        access.addOwner("owner1");
        check(access.isOwner("owner1"), "owner 识别");
        check(access.isAllowed("owner1"), "owner 天然可用");
        check(!access.deny("owner1"), "owner 不可 deny");
        check(access.allow("player2"), "allow 被授权玩家");
        check(access.isAllowed("player2"), "被授权玩家可用");
        check(access.deny("player2"), "deny 被授权玩家");
        check(!access.isAllowed("player2"), "deny 后不可用");
        AccessManager copy = AccessManager.fromMap(access.toMap());
        check(copy.isOwner("owner1"), "权限组持久化");
    }

    private static void testJsonStore() throws Exception {
        Path tmp = Files.createTempFile("veincore-test", ".json");
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("k", "v");
            data.put("n", 42);
            JsonStore.save(tmp, data);
            Map<String, Object> loaded = JsonStore.load(tmp, Map.of());
            check("v".equals(loaded.get("k")), "JsonStore 保存/读取");
            check(((Number) loaded.get("n")).intValue() == 42, "JsonStore 数字");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static void testSquare3x3() {
        PatternContext ctx = new PatternContext(new Pos(0, 0, 0), Face.SOUTH,
                new ViewAxis(2, 0, 1), 64, p -> true, p -> true);
        List<Pos> blocks = new Square3x3Pattern().collect(ctx);
        check(blocks.size() == 9, "square_3x3 共 9 格");
        check(blocks.get(0).equals(new Pos(0, 0, 0)), "origin 首位");
        check(blocks.contains(new Pos(1, 1, 0)), "square 覆盖 +u +v");
        check(blocks.contains(new Pos(-1, -1, 0)), "square 覆盖 -u -v");
    }

    private static void testVein() {
        PatternContext ctx = new PatternContext(new Pos(0, 0, 0), Face.SOUTH,
                new ViewAxis(2, 0, 1), 5, p -> p.y() <= 0, p -> p.y() <= 0);
        List<Pos> blocks = new VeinPattern().collect(ctx);
        check(blocks.size() == 5, "vein 遵守 maxBlocks");
        check(blocks.contains(new Pos(0, -1, 0)), "vein 同组扩展");
    }

    private static void testChain() {
        PatternContext ctx = new PatternContext(new Pos(0, 0, 0), Face.SOUTH,
                new ViewAxis(2, 0, 1), 3, p -> true, p -> true);
        List<Pos> blocks = new ChainPattern().collect(ctx);
        check(blocks.size() == 3, "chain 遵守 maxBlocks");
    }

    private static void testTunnel1x1() {
        PatternContext ctx = new PatternContext(new Pos(0, 0, 0), Face.SOUTH,
                new ViewAxis(2, 0, 1), 10, p -> p.z() >= -3, p -> true);
        List<Pos> blocks = new Tunnel1x1Pattern().collect(ctx);
        check(blocks.size() == 4, "tunnel_1x1 沿法线推进");
        check(blocks.get(3).equals(new Pos(0, 0, -3)), "tunnel_1x1 末格");
    }

    private static void testTunnel3x3() {
        PatternContext ctx = new PatternContext(new Pos(0, 0, 0), Face.SOUTH,
                new ViewAxis(2, 0, 1), 100,
                p -> p.z() >= -1 && Math.abs(p.x()) <= 1 && Math.abs(p.y()) <= 1, p -> true);
        List<Pos> blocks = new Tunnel3x3Pattern().collect(ctx);
        check(blocks.size() == 18, "tunnel_3x3 两层 3×3");
        check(blocks.contains(new Pos(1, 1, -1)), "tunnel_3x3 截面覆盖");
    }

    private static void testDiagonal() {
        PatternContext ctx = new PatternContext(new Pos(0, 0, 0), Face.SOUTH,
                new ViewAxis(2, 0, 1), 10, p -> p.z() >= -3, p -> true);
        List<Pos> blocks = new DiagonalPattern().collect(ctx);
        check(blocks.size() == 4, "diagonal 45° 阶梯");
        check(blocks.get(2).equals(new Pos(0, -2, -2)), "diagonal 每步下降一格");
    }

    private static void testPlane() {
        PatternContext ctx = new PatternContext(new Pos(0, 5, 0), Face.SOUTH,
                new ViewAxis(2, 0, 1), 7, p -> p.y() == 5, p -> true);
        List<Pos> blocks = new PlanePattern().collect(ctx);
        check(blocks.size() == 7, "plane 遵守 maxBlocks");
        check(blocks.stream().allMatch(p -> p.y() == 5), "plane 仅同层");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("FAIL: " + message);
        }
        passed++;
    }
}
