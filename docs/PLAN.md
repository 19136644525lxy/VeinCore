# VeinCore 项目计划 v2.1：两端连锁破坏模组（1.21.1 / LGPL-3.0 / 无附魔 / 双模式指示器 + 轮廓渲染）

| 项 | 内容 |
|---|---|
| 文档版本 | v2.1 |
| 状态 | 已批准，待执行 |
| 项目名 | **VeinCore**（Modrinth 搜索无重名；若 CurseForge 冲突可改名） |
| 许可证 | **LGPL-3.0** |
| 目标 MC 版本 | **Minecraft 1.21.1**（Fabric / Forge 两端） |
| 发布渠道 | 未定，由作者自行决定（不写死） |

## 0. 版本变更记录

### v2 → v2.1（本次）

| 项 | v2 | v2.1（本版） |
|---|---|---|
| 指示器 | 无 | **GUI 模式（默认）+ 文本模式**，样式参考 [FTB Ultimine Indicator](https://www.mcmod.cn/class/19945.html) |
| 轮廓渲染 | 仅有「高亮轮廓」一句话 | **独立功能**：预览集合渲染、距离裁剪、截断、防抖刷新 |
| 挖掘上限 | `maxBlocks` 默认 64 | **支持 64–1280 任意自定义**；**>600 放行 + 卡顿自负警告**；服主可封顶 |
| 权限操作 | 被授权玩家可用 `/veinminer optout` 自助取消 | **allow/deny 仅房主/服主/控制台可执行；玩家不能自授权，取消授权也仅限房主/服主/控制台** |
| 指示器命令 | 无 | 客户端 `/veinminer indicator mode <gui\|text>` |
| 里程碑 | M0–M5 | **M0–M6**（M4 轮廓/HUD 基础，M5 指示器完善与性能调优，M6 测试/文档/发布） |

### v1 → v2（历史）

- 许可证 MIT → **LGPL-3.0**；发布渠道不写死；**无附魔**；触发只判断绑定热键（默认 `~`）；自定义权限组独立于原版权限。

## 1. 背景与目标

### 1.1 竞品痛点（为什么要重做）

- **误挖**：`mustSneak` 默认关闭，站着挖就连锁，常挖穿建筑/矿道。
- **卡顿**：大矿脉瞬间刷出大量掉落物与方块更新，客户端/服务端掉帧甚至崩溃。
- **配置不生效/不持久**：方块组重启后丢失或不生效；服务器上轮廓不显示。
- **版本裂隙**：老牌模组停在 1.21/1.20；数据包版在 Realms 上不工作。
- **指示信息弱**：多数连锁模组只给一行纯文本或什么都不给，看不出「将挖多少、什么形状、是否可用」。

### 1.2 目标（成功标准）

- **前期即用**：装好即生效（服务端装即用），默认热键一键连锁。
- **后期舒服**：模式/方块组/限流/指示器/数据包/API 全部可配可调且持久。
- 两端同构：Fabric / Forge，MC 1.21.1，**零 Architectury 依赖**。
- 默认不误挖、大矿脉不卡、配置可持久可覆盖。
- **权限可控**：默认只有房主/服主/控制台能用，其他人需显式授权。
- **所见即所得**：按下热键即可在准星旁看到「形状 + 预计数量 + 状态」，并看到轮廓预览。

### 1.3 非目标（本版不做）

附魔、跨维度、机械动力联动、自动合成、经济系统、可鼠标交互的配置 GUI 菜单、26.x 移植、发布渠道运营。

## 2. 总体设计

### 2.1 版本锁定（1.21.1）

| 项 | 值 |
|---|---|
| Minecraft | 1.21.1 |
| Java | 21（toolchain） |
| Forge | 52.0.x |
| Fabric Loader / API | 0.16.x / 0.102.x+1.21.1 |
| 映射 | common 用 **Mojmap（official）**；Fabric 侧 Loom `officialMojangMappings()` + `remapJar` |
| 构建 | Gradle 8.x；Fabric Loom、ForgeGradle 6 各自子项目 |
| 额外依赖 | **零**（不引 Architectury、不引 Cloth Config；Fabric 仅 Fabric API） |

### 2.2 不依赖 Architectury 的 common 方案（不变）

采用 **MultiLoader-Template 式结构**：

- `common`：普通 Java 子项目，直接依赖 Mojmap 的 MC 类，承载 90% 逻辑。
- `fabric` / `forge`：薄胶水层（事件、入口、网络、配置目录、渲染）。
- 依赖方向单向：`fabric|forge → common`。
- Fabric 侧关键点：common 以 `implementation project(':common')` 引入，`remapJar` 必须把 common 类一起 remap（`shadowJar` 打 fat jar 再 remap），**全项目最高风险点**（§7）。
- 备选 Plan B：common 只放纯 Java 抽象，平台层传 `long` 编码的 BlockPos；零 remap 风险。

### 2.3 模块结构（新增 `client/`）

```text
common/
  core/    VeinEngine(BFS/模式/限流)、BlockGroupRegistry、PlayerState、DropMerger、PreviewScanner
  perm/    AccessManager(授权名单/owner 判定/持久化)、AccessEntry
  api/     VeinMinerAPI（第三方注册方块组/模式/回调）
  config/  VeinConfig + JSON 读写 + 数据包覆盖
  client/  IndicatorState、IndicatorLayout、OutlineState（平台无关的状态与布局计算）
  platform/IPlatform、IEventRegistrar、INetwork、IClientRenderer
  net/     VeinPayload + VeinPreviewPayload + StreamCodec
fabric|forge/
  entry、事件注册、命令注册、payload 注册、config 目录、HUD/轮廓渲染、按键
```

平台接口最小化（6 个）：

- `IPlatform`：configDir、isClient、isSingleplayer、isOwner(player)、isConsole(source)
- `IEventRegistrar`：`onBlockBreakPre/Post`、`onPlayerTick`、`onServerTick`、`onHudRender`
- `INetwork`：`sendToPlayer(player, payload)`
- `IClientRenderer`：`renderIndicator(state)`、`renderOutline(blocks)`、高亮渲染
- `IProtectionProbe`：逐块破坏前的「模拟玩家破坏/保护检测」
- `IClientConfig`：读/写 `config/veinminer-client.json`

### 2.4 两端事件映射（新增「HUD 渲染」与「资源重载」行）

| 能力 | Fabric 1.21.1 | Forge 1.21.1 |
|---|---|---|
| 破坏前拦截 | `PlayerBlockBreakEvents.BEFORE` | `BlockEvent.BreakEvent`（setCanceled） |
| 命令注册 | `CommandRegistrationCallback` | `RegisterCommandsEvent` |
| 玩家 tick | `ServerTickEvents`/`ClientTickEvents` | `TickEvent.PlayerTickEvent` |
| 按键 | `KeyBindingHelper` | `RegisterKeyMappingsEvent` |
| 网络 | `PayloadTypeRegistry` | `RegisterPayloadHandlersEvent` |
| **HUD 渲染** | `HudRenderCallback` | `RenderGuiEvent.Post` / `RegisterGuiOverlaysEvent`（M0 实测确认） |
| 世界轮廓 | `WorldRenderEvents.AFTER_TRANSLUCENT` | `RenderLevelStageEvent`（AFTER_TRANSLUCENT） |
| **资源重载监听** | `ResourceManagerHelper.registerReloadListener` | `RegisterClientReloadListenersEvent`（M0 实测确认） |

### 2.5 一次连锁的完整时序（含预览与指示器）

1. 玩家按住热键并开始挖方块 → 平台捕获 BreakEvent → `VeinEngine.tryBreak(ctx)`。
2. **权限判定**：`AccessManager.canUse(player)`；未授权 → 放行原版 + 提示一次 + 指示器显示「未授权」。
3. **热键判定**：客户端上报热键状态（`VeinPayload`）；未按 → 放行原版。
4. **上限判定**：取玩家 `maxBlocks`（64–1280）→ 与 `server_max_blocks` 取小；>600 时置 `OVER_600_WARNING`。
5. **预览扫描**：`PreviewScanner` BFS 收集目标（不破坏，限频 1 次/10 tick）→ 下发 `VeinPreviewPayload`。
6. 客户端：按 `renderBlocks` 渲染**轮廓**；指示器显示模式/预计数/状态。
7. 逐块执行：`IProtectionProbe.tryBreak` → 失败即停；扣耐久/饥饿；掉落物按策略；每 tick 最多 `blocksPerTick` 块。
8. 首块由引擎接管并取消原事件（避免双倍掉落）；执行中每 tick 递减，指示器刷新剩余数。
9. 结束时清理预览与指示器状态。

## 3. 功能规格

### 3.1 触发（只保留热键）

- 默认热键 `~`（GRAVE），玩家可在按键设置里改；按住热键 + 破坏方块 = 连锁。
- **按住/切换两种模式**：`indicator.hold_mode: true`（默认按住）；可改为切换式（学参考模组的 `toggle`）。
- 不判断潜行、不判断附魔、无「常开」模式；配置可整体关闭。
- 服务端权威：客户端只上报「热键按下状态」，服务端校验权限与方块，防伪造。

### 3.2 指示器（GUI / 文本双模式）

**模式清单**

| pattern_id | 显示名 | 算法一句话 |
|---|---|---|
| `vein` | 矿脉 | 以触发方块为首，向 6 邻接的同组方块做 BFS（默认只挖同种/同标签）。 |
| `chain` | 连锁 | 同 BFS，但允许跨类型按白名单/黑名单连续破坏（如全部原木 + 树叶）。 |
| `square_3x3` | 3×3 小方块 | 以触发方块为中心，在垂直于玩家视线主轴的平面内扩展 ±1，共 9 格、深度仅 1 层。 |
| `tunnel_1x1` | 隧道 1×1 | 以触发面法线为方向，沿视线推进的 1×1 截面通道。 |
| `tunnel_3x3` | 隧道 3×3 | 以触发面法线为方向，沿视线推进的 3×3 截面通道。 |
| `diagonal` | 斜向 | 沿视线方向 45° 斜向延伸（贴面走斜线）。 |
| `plane` | 平面 | 以触发方块所在高度的水平面扩展（同层）。 |
| — | 回退 | 无对应 `pattern_id` 时使用 `unknown.png`。 |

**参考分析**：FTB Ultimine Indicator 在按下连锁键（默认 `~`）后于**十字准星右侧**显示当前形状的**名称 + 图标 + 连锁状态**，替代原版纯文本菜单；支持资源包覆盖图标；配置项有 `enable/show_shape_name/show_status/toggle/show_plain_text_menu/require_sneak_for_menu`；仅 1.20.1 已停更。

**双模式设计**

| 模式 | 形态 | 内容 |
|---|---|---|
| **GUI 模式（默认）** | 准星右侧面板（默认无底板、文字带阴影，可选半透明底板）：图标 + 名称 + 状态行 | 模式图标、本地化模式名、`就绪 · 预计 N 格` / `冷却中 3s` / `未授权` / `超过 600 格，卡顿自负` |
| **文本模式** | 无背景纯文本行，自动深/浅色反色 | 如 `[矿脉] 预计 64 格 · 就绪` |

- 客户端本地配置 `config/veinminer-client.json`：`indicator.mode: "gui" | "text"`（默认 `gui`）。
- 本地命令 `/veinminer indicator mode <gui|text>`（只影响自己，不需要服务端权限）。
- 状态色：就绪=白/绿、冷却=黄、未授权=红、>600 警告=橙红。

**显示时机与位置**

- 默认**按住热键时显示**；可改为切换式（`hold_mode=false` 时按一下开/关）。
- 布局：`indicator.anchor`（3×3 锚点，默认 CENTER 即准星右侧区域）+ 各元素 `insetX/insetY` 与 `anchorX/anchorY`，全部在 GUI 缩放坐标下计算（详见下方参考实现映射）。
- 显示内容由服务端状态驱动，客户端只渲染。

**图标与资源包（复用参考素材）**

- 内置 8 张图标（7 种模式 + 1 张回退），**复用 FTB Ultimine Indicator 自带的形状图标**，按原尺寸原字节复制、仅重命名，不改像素：`assets/veincore/textures/pattern_icon/<pattern_id>.png`；`pattern_id` ∈ {`vein`, `chain`, `square_3x3`, `tunnel_1x1`, `tunnel_3x3`, `diagonal`, `plane`}，回退 `unknown.png`。
- 映射：`vein ← shapeless`(17×15)、`chain ← mining_tunnel`(15×15)、`square_3x3 ← small_square`(13×13)、`tunnel_1x1 ← small_tunnel`(13×13)、`tunnel_3x3 ← large_tunnel`(17×17)、`diagonal ← escape_tunnel`(15×15)、`plane ← layer`(17×12)、`unknown ← missing`(12×17)。
- 渲染时整体 alpha 0.5（与参考一致）；设计稿与 8× 预览：`docs/assets/pattern_icon/`（`preview.png`）；M0 建好 `common/` 后复制到资源目录。
- 资源包可覆盖同路径；资源缺失时回退 `unknown.png`，再缺回退纯文字。
- 背景面板（可选，默认关）用 `GuiGraphics.fill`；M5 可换 9-slice 圆角。

**素材许可**

- 参考项目 FTB Ultimine Indicator 的仓库许可证为 **LGPL-3.0**（仓库根目录 `LICENSE`，无单独素材声明）；其形状图标随仓库以 LGPL-3.0 分发，本项目按用户要求复用其图标像素（仅重命名、未修改）。
- 复用义务：保留版权与许可声明 → 仓库根 `NOTICE.md` 记录来源（https://github.com/Tidy-Bear/FTB-Ultimine-Indicator）、版权（© Tidy-Bear）、许可证（LGPL-3.0）与「复制并重命名、未修改像素」；仓库 `LICENSE` 保留本项目 LGPL-3.0 全文。
- 对玩家/游戏内：**不显示任何来源提示**（符合「不用告知」的要求），声明仅存在于仓库文件中。
- 若未来引入其他第三方素材，按同样方式在 `NOTICE.md` 署名并确认与 LGPL-3.0 兼容。

**性能**

- 指示器仅 2D 绘制，状态按 tick 缓存，不每帧扫描；文本模式无纹理绑定开销。

**参考实现映射（吸收自 FTB Ultimine Indicator 1.20.1 源码）**

1. **图标图集注入**：`assets/minecraft/atlases/blocks.json` 添加 `directory` source（`veincore/pattern_icon`，`prefix` 同路径），客户端资源重载时从 `InventoryMenu.BLOCK_ATLAS` 取 `TextureAtlasSprite`；缺失回退 `unknown.png`；资源包可覆盖同路径。
2. **布局算法**：3×3 锚点 + `insetX/insetY` 像素偏移 + `anchorX/anchorY`（0=左/1=中/2=右，比例 0–2），在 GUI 缩放坐标下计算面板位置。默认对齐参考：图标 `pos=CENTER, inset(30,-3), anchor(1,1)`；名称 `inset(45,-1), anchor(0,1)`；状态 `inset(30,9), anchor(1,0)`。
3. **样式**：与参考一致——**无背景底板、文字带阴影**；半透明底板仅作可选开关（默认关），避免遮挡视野。
4. **显示条件**：`indicator.enable && 热键激活`；按住模式按住显示，切换模式按一下开、再按关。
5. **状态色**：就绪绿 `#A3BE8C`、冷却黄 `#BFBF8C`、未授权红 `#BF616A`；VeinCore 追加「>600 格」橙红 `#D08770`。
6. **与参考的差异**：不依赖 FTB Library / FTB Ultimine，不引入 mixin；状态由自研 `VeinPreviewPayload` 与服务端同步；文本模式是独立的一行纯文本（参考只有纯文本菜单开关）。

### 3.3 轮廓渲染（新增）

- **数据来源**：服务端预览扫描（BFS 只收集不破坏）→ `VeinPreviewPayload.renderBlocks`（截断后的渲染集合）。
- **渲染**：每个方块画白色 1px 线框（自绘 Tesselator，深度测试开启、不写深度），可配颜色/透明度。
- **时机**：按住热键且方块筛选通过时显示；开始挖掘后轮廓随方块逐个消失；松开/结束立即清理。
- **性能保护**：
  - `outline.max_rendered`（默认 256，可配 0–1024；0=不渲染轮廓，只显示指示器数字）。
  - 距离裁剪：只渲染距玩家 64 格内的方块。
  - 每 **10 tick** 或状态变化才刷新预览，不逐帧请求。
- **配置**：`outline.enabled`（客户端可关；服务端可强制禁用，防泄露方块信息）、`outline.color`、`outline.alpha`。

### 3.4 挖掘上限与调度（64–1280）

| 项 | 规则 |
|---|---|
| `maxBlocks` | 范围 **[64, 1280]**，默认 **64**；玩家 `/veinminer settings maxblocks <n>` 可设任意值 |
| 服务器封顶 | `server_max_blocks`（默认 0=不限制）；服主可限制玩家上限 |
| >600 | 放行，但**聊天警告一次** + 指示器红字「超过 600 格，卡顿自负」；`allow_over_600: false` 时直接拒绝 |
| `blocksPerTick` | 默认 16；大上限时保持，靠调度消化 |
| 全局预算 | 服务器每 tick 总方块预算（默认 128），多玩家同时连锁不雪崩 |
| `maxRadius` | 由 `maxBlocks` 推导：`ceil(sqrt(maxBlocks)) + 4`（可手填，上限 64） |
| 预览扫描 | 每次热键激活最多一次扫描 / 10 tick；1280 格扫描限流，不拖慢 tick |

### 3.5 限制与消耗

| 项 | 默认 | 说明 |
|---|---|---|
| durability | 每块扣 1（可配「只首块」/关闭） | 剩 1 耐久自动停 |
| hunger | 0.005/块（原版为 0） | 可关闭 |
| experience | 合并到源方块，可配倍率 | — |
| minToolTier | 铁（可配） | 木/石工具默认不触发，防刷 |
| cooldown | 10 ticks | 防连点 |

### 3.6 方块筛选与数据包

- 白名单/黑名单（方块 ID + 标签，如 `#c:ores`、`#minecraft:logs`）。
- 方块组：JSON 定义别名组，支持**数据包覆盖**（`data/<ns>/veinminer/groups/*.json`）。
- 默认拒挖：容器、刷怪笼、命令方块/结构方块/屏障、Boss 方块。
- 需要正确工具（可关）。

### 3.7 自定义权限组（v2.1 收紧）

**定位**：一套完全独立于原版 OP / LuckPerms / fabric-permissions-api 的访问控制，只服务本模组。

**角色与规则**

| 角色 | 获得方式 | 权限 | 能否被取消 |
|---|---|---|---|
| 控制台 | 命令源为服务器控制台 | allow/deny/list/status 全部放行 | 否 |
| 房主（单人/LAN 主机） | 集成服务器判定：单人存档玩家 | 全部放行 | **否** |
| 服主（专用服） | OP 等级 ≥ 4 自动 + 配置 `owners` 手动追加 | 全部放行 | **否** |
| 已授权玩家 | 房主/服主/控制台 `/veinminer allow <player>` | 仅使用连锁、查自己状态 | 只能由 owner 取消 |
| 未授权玩家 | 默认 | 不可连锁 | — |

**存储**：`config/veinminer/access.json`

```json
{ "version": 1, "owners": ["<uuid 或 name>"], "allowed": ["<uuid 或 name>"] }
```

- 优先 UUID；离线模式/无 UUID 用 name 兜底并提示。
- 原子写入；变更即存；`/veinminer reload` 重读。

**命令**

| 命令 | 权限 |
|---|---|
| `/veinminer allow <player>` | **仅 owner / 控制台**；玩家执行直接拒绝并提示 |
| `/veinminer deny <player>` | **仅 owner / 控制台**；对 owner 拒绝并提示「不可取消」 |
| `/veinminer list` | owner/控制台看全部；普通玩家只看自己 |
| `/veinminer status` | 任何玩家看自己是否可用 |
| `/veinminer settings maxblocks <n>` | 玩家设自己的上限（64–1280，受服务器封顶） |
| `/veinminer mode/settings/stats` | 已授权玩家；`reload` 仅 owner/控制台 |
| `/veinminer indicator mode <gui\|text>` | 本地指示器模式（客户端） |

**删除项**：**取消玩家自助 `optout`**——被取消授权只能由房主/服主/控制台执行，玩家不可自行恢复或取消。

**判定顺序**：控制台 → owner（自动判定 ∪ 配置 owners）→ allowed 名单 → 拒绝。

**边界**

- 单人开 LAN：主机玩家是 owner；LAN 其他玩家默认无权限，需房主 allow。
- 专用服无 OP：只能控制台授权，或手写 `access.json` 的 owners。
- 换用户名：UUID 优先，不失效。
- 所有判定在服务端执行；客户端只做提示与热键上报。

### 3.8 命令与权限

- 管理命令（blocks/groups/settings）限已授权玩家；`reload` 限 owner/控制台。
- `allow/deny` 限 owner/控制台。
- 不依赖 LuckPerms / fabric-permissions-api；权限自管（见 §3.7）。

### 3.9 配置

**服务端** `config/veinminer/veinminer.json`：

- 玩法参数（Gson，字段带默认值与 `_comment`）：`maxBlocks` 范围、`server_max_blocks`、`allow_over_600`、`blocksPerTick`、`cooldown`、`outline_enabled` 等。
- `config/veinminer/access.json`：权限名单。

**客户端** `config/veinminer-client.json`：

- `indicator.mode`（gui/text）、`indicator.hold_mode`、`indicator.offsetX/offsetY`、`indicator.always_show`。
- `outline.enabled/color/alpha/max_rendered`。

- 均支持 `/veinminer reload`（服务端）与客户端热重载（重新进服/存档生效）。
- 数据包可覆盖方块组与黑白名单（整合包友好）。

### 3.10 开发者 API

- `VeinMinerAPI.registerBlockGroup(id, blocks)`、`registerPattern(id, BiFunction)`、`registerBreakListener`。
- 供 KubeJS（v0.2 示例脚本）、CraftTweaker 接入；发布 API javadoc。

### 3.11 差异化亮点

1. **零误挖默认** + 只热键触发，不打扰正常挖掘。
2. **限流 + 掉落物合并默认开**，专治卡顿。
3. **配置持久 + 数据包覆盖**，专治「重启丢失/整合包改不动」。
4. **独立权限组**：房主/服主/控制台永久可用且不可取消；玩家不能自授权，取消授权只能由 owner 执行——服务器管理友好。
5. **双模式指示器 + 轮廓预览**：按住热键即看到形状、预计数量、状态与方块轮廓（参考 FTB Ultimine Indicator，但内置文本模式与 1.21.1 两端支持）。
6. **64–1280 自定义上限**：玩家按需调高，>600 有明确风险提示，服主可封顶。
7. **渐进式破坏可选**（借鉴 Crackt：逐块崩裂动画）。
8. **服务器预算**：全局每 tick 方块预算，多玩家同时连锁不雪崩。
9. **纯服务端兼容**：服务端装即用，原版客户端也可触发。
10. **中英双语**，后续社区翻译。

## 4. 里程碑（单人全职估算，兼职 ×1.5–2）

| 阶段 | 内容 | 验收 | 工期 |
|---|---|---|---|
| M0 脚手架 | 两端空 mod 可加载；common remap 跑通；HUD/渲染事件实测；CI | 两端 `runClient` 无报错；`dependencies` 无 Architectury | 1–2 周 |
| M1 核心引擎 | BFS/模式/限流/64–1280 上限/600 警告/预览扫描 | 单元测试通过；单机 1280 格连锁可用 | 2–3 周 |
| M2 两端事件 + 权限 | 破坏拦截/取消/保护探测 + 收紧后的 AccessManager/命令 | 未授权不触发；玩家不能 allow；owner 不可取消 | 2 周 |
| M3 配置/命令/数据包 | 全套命令、access 持久化、server_max_blocks、数据包覆盖 | 重启不丢配置；datapack 生效；reload 生效 | 1–2 周 |
| **M4 客户端基础** | 热键、**轮廓渲染**、HUD 骨架、状态同步 | 轮廓正确显示/清理；1280 格 FPS 可接受 | 1–2 周 |
| **M5 指示器完善** | **GUI/文本双模式**、图标、锚点偏移、资源包覆盖、性能调优 | 两种模式可切换且显示正确 | 1–2 周 |
| M6 测试/文档/发布 | GameTest + 性能矩阵 + README/Wiki | 验收清单全绿 | 2 周 |

**合计约 10–14 周**（v0.1 MVP 可在 M3 后先行发布）。

## 5. 测试计划

- 单元测试（JUnit，common 纯逻辑）：BFS 边界、上限截断、配置序列化、组匹配、**AccessManager（玩家不能自授权/不能 optout）**。
- GameTest（两端各跑）：矿石/树木连锁、容器拒挖、保护区域、未授权不触发、allow/deny 仅 owner、owner 不可取消、限流不丢块。
- 指示器：GUI/文本切换即时生效；按住/切换；锚点偏移；图标资源包覆盖与缺失回退；GUI scale 不错位。
- 轮廓：预览集合渲染正确；>256 截断不崩；松开即清；1280 格 FPS ≥ 60（最低 30）。
- 上限：64/600/601/1280 设置生效；>600 警告一次；`server_max_blocks` 封顶；1280 格 TPS ≥ 19。
- 手动矩阵：单机、LAN 主机+客机、专用服（OP4/无 OP/控制台授权）、离线模式、多玩家并发。
- 兼容：FTB Chunks、FTB Teams、JEI/REI、Create（仅测试不挖其方块）、Apotheosis 工具。

## 6. 兼容性矩阵

| 对象 | 态度 |
|---|---|
| FTB Chunks / FTB Teams | 软兼容：保护区内不连锁 |
| LuckPerms / fabric-permissions-api | **不依赖**（权限自管），不主动桥接 |
| KubeJS / CraftTweaker | 提供 API + 示例（v0.2） |
| Create / AE2 等 | 默认不进其方块白名单；不主动兼容 |
| 原版客户端连服务端 | 支持（服务端装即可） |
| FTB Ultimine Indicator | 只参考样式，不依赖、不联动 |
| 26.x | v2 评估，不在 v1 范围 |

## 7. 风险与对策

| 风险 | 等级 | 对策 |
|---|---|---|
| Fabric remap（common 类未 remap 导致 `NoSuchMethodError`） | **高** | M0 首先验证 shadow+remap；备选 Plan B |
| 两端事件取消时机不一致（双倍掉落/递归） | 高 | 统一「引擎接管首块 + 取消原事件」；`Set<BlockPos>` 处理中标记 |
| **owner 判定差异**（集成服/LAN/专用服/离线 UUID） | 中 | 平台接口 `isOwner` + 配置 owners 兜底 + 三种场景测试 |
| 1280 格轮廓渲染掉帧 | 中 | renderCap 截断 + 距离裁剪 + 每 10 tick 刷新；M5 性能验收 |
| 两端 HUD 事件差异（1.21.1 较老） | 中 | M0 实测两个事件名并锁版本；抽象层隔离 |
| 预览扫描 + 破坏并发 | 中 | 每玩家单扫描、加锁、去重 |
| 1.21.1 工具链老化 | 中 | 锁 1.21.1 时代组合；CI 固定 JDK 21 |
| 保护模组漏判 | 中 | `IProtectionProbe` 逐块探测 + 文档声明 |
| 玩家自行调高上限滥用 | 低 | `server_max_blocks` 封顶；>600 警告留痕 |
| 指示器图标 atlas 注册失败 / 资源包覆盖不生效 | 中 | M4 先做最小 atlas 冒烟测试；缺失回退 `unknown.png` 图标 |
| 复用参考图标的上游许可变更 | 低 | `NOTICE.md` 固定记录复用时的许可证与来源；上游若改许可，已取得的 LGPL-3.0 授权副本继续有效；M0 复核一次 |
| GUI 缩放/分辨率下锚点偏移错位 | 中 | 用 GUI 缩放坐标计算 + M5 多分辨率截图矩阵验证 |

## 8. 交付物

- 仓库结构：`VeinCore/{build.gradle, settings.gradle, common/, fabric/, forge/, docs/PLAN.md, README.md, LICENSE}`，`.gitignore` 排除 `.gradle/ build/ run/`。
- 发布渠道由作者自行决定；本项目只保证产出 Fabric/Forge 两个标准 jar。
- 文档：README（安装/配置/命令/权限/API）、Wiki（方块组与数据包格式）。

## 9. 明确假设

- 默认 `maxBlocks=64`、默认 **GUI 模式**、默认**按住**触发热键；玩家可自行在 64–1280 调整自己的上限（受服主封顶），符合「这个范围可以自定义任何挖掘格数」。
- >600 默认**放行 + 警告**（不是直接拒绝），因为你说「自行承担卡顿后果」。
- 指示器与轮廓是**纯客户端渲染功能**（不装客户端不影响连锁）；GUI 模式是显示面板，不做可鼠标点击的菜单。
- 图标路径 `assets/veincore/textures/pattern_icon/<id>.png`，M5 时按实际需要微调。
- 项目名 **VeinCore** 定名：Modrinth 搜索 0 命中；若 CurseForge 有冲突，改名只需同步文件夹名与 `mod_id`。
- 版本冻结：只做 1.21.1 两端；26.x 移植不在本版范围。
- Git：仓库已建（`main` 分支），首次提交后推送到 `https://github.com/19136644525lxy/VeinCore.git`；`temp/` 参考源码仅本地保留、不入库。
