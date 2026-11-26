# VeinCore

> 一个为 Minecraft **1.21.1** 设计的两端（Fabric / Forge）连锁破坏模组。
> 零 Architectury 依赖 · **LGPL-3.0** · 无附魔 · **双模式指示器** · **轮廓预览** · **64–1280 自定义上限**

当前状态：**M2 完成（Fabric + Forge 两端完整）**。两端均已具备：连锁破坏、Shift+滚轮模式切换、权限组（owner/OP/授权名单）、深板岩方块组、**每格消耗 1 点耐久（受耐久附魔影响）**、**可配置饱食度消耗（默认 0 = 关闭）**、`/veinminer allow/deny/list/status` 命令。预览与指示器在 M4。

## 为什么做它

现有连锁模组普遍存在这些问题：

- **误挖**：默认不要求潜行，站着挖就连锁，常挖穿建筑。
- **卡顿**：大矿脉瞬间刷出大量掉落物与方块更新，甚至导致崩溃。
- **配置不持久**：方块组重启后丢失或不生效；服务器上轮廓不显示。
- **版本裂隙**：老牌模组停在 1.21/1.20；数据包版在 Realms 上不工作。
- **指示信息弱**：看不出将挖多少、什么形状、是否可用。

VeinCore 的目标：**默认保守不误挖、大矿脉不卡、配置可持久可覆盖、权限可控、所见即所得**。

## 目标版本

| 端 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| Fabric | Loader 0.16.x + Fabric API 0.102.x+1.21.1 |
| Forge | 52.0.x |
| Java | 21 |

## 核心特性（v2.1）

1. **热键触发**：默认 `~`，按住触发，不判断潜行/附魔；服务端权威。**模式切换**：按住 `~` + Shift + 鼠标滚轮上下，FTB Ultimine 同款交互（客户端即时切换并同步服务端）。
2. **双模式指示器**：准星右侧面板（图标 + 名称 + 状态）或纯文本模式；按下热键即可看到「预计挖掘数 / 冷却 / 授权状态 / 超过 600 格警告」。
3. **轮廓预览渲染**：连锁前用白色线框预览将被挖的方块，开始挖掘后随方块逐个消失；带距离裁剪与截断保护。
4. **自定义挖掘上限**：**64–1280 任意可设**（默认 64）；超过 **600 格**放行但警告「卡顿自负」，服务端可设置封顶。
5. **7 种模式**：矿脉 / 连锁 / 3×3 小方块 / 隧道 1×1 / 隧道 3×3 / 斜向 / 平面；均可由玩家选（权限可限）。
6. **独立权限组**：房主 / 服主 / 控制台默认可用且**不可取消**；其他玩家需显式授权；**玩家不能自授权，取消授权也只能由房主/服主/控制台执行**。
7. **配置持久 + 数据包覆盖**：`config/veinminer/*.json` 原子写入，方块组可由数据包覆盖。
8. **限流与掉落物合并**：每 tick 执行上限、冷却、全局预算，默认合并掉落物，专治卡顿。
9. **开发者 API**：第三方注册方块组 / 模式 / 回调（KubeJS、CraftTweaker 示例见发布说明）。

## 权限模型（简表）

| 角色 | 如何获得 | 能做什么 | 能否被取消 |
|---|---|---|---|
| 控制台 | 服务器控制台 | 全部 | 否 |
| 房主 | 单人 / LAN 主机 | 全部 | 否 |
| 服主 | OP 等级 ≥ 4 或配置 `owners` | 全部 | 否 |
| 已授权玩家 | `/veinminer allow <player>`（仅 owner/控制台） | 使用连锁、查自己状态 | 是（仅 owner/控制台） |
| 未授权玩家 | 默认 | 不可连锁 | — |

## 安装

- **服务端**：放入 `mods/` 即可（Fabric 需 Fabric API）。服务端装好即生效。
- **客户端**：可选。装上后可用指示器与轮廓；不装客户端也不影响连锁。
- 单人游戏：直接把 jar 放进游戏即可。

## 命令速查（v2.1）

| 命令 | 说明 | 权限 |
|---|---|---|
| `/veinminer allow <player>` | 授权玩家 | 仅房主/服主/控制台 |
| `/veinminer deny <player>` | 取消授权 | 仅房主/服主/控制台 |
| `/veinminer list` | 查看授权列表 | owner/控制台看全部，玩家只看自己 |
| `/veinminer status` | 查看自己是否可用 | 所有人 |
| `/veinminer mode <pattern>` | 切换模式 | 已授权玩家 |
| `/veinminer settings maxblocks <n>` | 设置自己的挖掘上限（64–1280） | 已授权玩家 |
| `/veinminer settings <key> <value>` | 调整个人选项 | 已授权玩家 |
| `/veinminer stats` | 统计信息 | 已授权玩家 |
| `/veinminer blocks add/remove` | 方块白名单 | 已授权玩家 |
| `/veinminer groups list` | 内置方块组 | 已授权玩家 |
| `/veinminer reload` | 重读配置 | 仅房主/服主/控制台 |
| `/veinminer indicator mode <gui\|text>` | 切换指示器模式 | 客户端本地 |

> 注：`mode` / `settings` / `stats` / `blocks` / `groups` / `reload` / `indicator` 属于 M3/M4 功能，当前 M2 构建仅提供 `allow` / `deny` / `list` / `status`。

## 配置文件

- 服务端：`config/veinminer/veinminer.json`（玩法参数与上限）
- 服务端：`config/veinminer/access.json`（房主/授权名单）
- 客户端：`config/veinminer-client.json`（指示器模式/偏移/轮廓开关）

## 开发进度

| 里程碑 | 内容 | 状态 |
|---|---|---|
| M0 | Fabric+Forge 脚手架 + common remap 验证 | ✅ 完成（javac 自测 50 项通过） |
| M1 | 核心引擎（BFS/模式/限流/上限/预览扫描） | ✅ 完成（Fabric 事件链路接通：热键包→模式扫描→接管破坏；`javac` 50 项 + `:fabric:build` 通过；预览/指示器归 M4） |
| M2 | 两端事件 + 权限组/命令 | ✅ 完成（两端：事件/网络/命令/权限组/模式切换/耐久/饱食度；`javac` 50 项 + `:fabric:build` + `:forge:build` 通过） |
| M3 | 配置/命令/数据包 | ⏳ 待开始 |
| M4 | 客户端基础（热键/轮廓/HUD 骨架） | ⏳ 待开始 |
| M5 | 指示器完善（GUI/文本双模式） | ⏳ 待开始 |
| M6 | 测试/文档/发布 | ⏳ 待开始 |

## 目录规划

```text
fabric/       Fabric 适配（入口、事件、网络、HUD/轮廓渲染）
forge/        Forge 适配
common/       跨平台核心（引擎、模式、配置、权限、API、网络包）
docs/PLAN.md  完整项目计划
```

## 本地验证（无需 Gradle）

```powershell
$out = 'build/test-classes'
New-Item -ItemType Directory -Force $out | Out-Null
javac -encoding UTF-8 -d $out (Get-ChildItem -Recurse -Filter *.java common\src\main\java | ForEach-Object FullName)
java -cp $out com.veincore.TestMain
```

预期输出 `ALL TESTS PASSED (50 checks)`。

实机试用：

- Fabric：`.\gradlew.bat :fabric:build` 后，把 `fabric/build/libs/veincore-fabric-0.1.0.jar` 放进 `mods/`（需 Fabric API 0.102.x+1.21.1 + Java 21）。
- Forge：`.\gradlew.bat :forge:build` 后，把 `forge/build/libs/veincore-forge-0.1.0.jar` 放进 `mods/`（需 Forge 52.0.x + Java 21）。

进游戏**按住 `~` 左键挖方块**即可连锁同种方块；按住 `~` + Shift + 滚轮切换模式；不按 `~` 时保持原版单块破坏。

## 素材

- 模式图标**复用 FTB Ultimine Indicator 的形状图标**（按原尺寸原字节复制、仅重命名，未修改像素），映射见 PLAN.md「图标与资源包」；预览见 `docs/assets/pattern_icon/preview.png`。
- 参考项目为 LGPL-3.0；本项目同样以 LGPL-3.0 发布，复用来源与版权见仓库根 `NOTICE.md`。游戏内不显示来源提示。

## 仓库

- GitHub：https://github.com/19136644525lxy/VeinCore
- 默认分支：`main`
- 参考源码 `temp/` 仅本地保留、不入库。

## 许可证

**LGPL-3.0**，见 [LICENSE](LICENSE)。
