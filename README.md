# Pooping 💩

> 一个为 Paper 服务器添加"拉屎"玩法的插件。

[![Version](https://img.shields.io/badge/version-0.4-blue.svg)]()
[![Paper](https://img.shields.io/badge/Paper-1.21.11-orange.svg)]()
[![Java](https://img.shields.io/badge/Java-21-red.svg)]()
[![License](https://img.shields.io/badge/license-MIT-green.svg)]()

## 简介

**Pooping** 是一个轻量级娱乐插件，让玩家可以在服务器里"拉屎"。
通过特定的蹲下操作触发，屏幕会显示进度条，拉完后在脚下留下一坨**不可拾取、不可交互**的"屎"，并悬浮显示它属于谁。


## 玩法

### 触发方式

1. **快速点按两次** Shift（蹲下），每次间隔 ≤ 500ms
2. 然后**第三次按住** Shift 不放（也要在 500ms 内按下）
3. 动作栏立即出现进度条，持续长按 3 秒涨满

> 松开 Shift、或中途受伤，都会**立即取消**，进度清零。

### 效果

- 动作栏显示进度条：`正在拉屎 ||||||||||||||||||||`
  - 初始整条为**绿色**，**红色**从左往右推进填充，拉满时整条变红
- 涨满后在玩家脚下生成一坨"屎"（默认萤石粉）
- 掉落物特性：
  - ❌ 不可拾取、不可被漏斗吸取、不可交互、不受伤害
  - 🏷️ 悬浮名显示为 `<玩家名>的屎`（金色），**靠近 8 格内**才可见
  - ⏳ 生成 **60 秒**后自动消失
- 支持同时存在多坨，各自独立计时

## 安装

1. 下载 `pooping-x.x.jar`
2. 放入服务器 `plugins/` 目录
3. 重启服务器
4. 首次启动会生成 `plugins/Pooping/config.yml`

**环境要求：**
- Paper（或兼容的 Spigot 分支）**1.21+**
- Java **21+**

## 命令与权限

| 命令 | 说明 | 权限 | 默认 |
| --- | --- | --- | --- |
| `/poop reload` | 重载配置文件 | `pooping.reload` | OP |

## 配置文件

配置文件位于 `plugins/Pooping/config.yml`，修改后可用 `/poop reload` 热重载。

```yaml
# ---------- 触发设置 ----------
trigger:
  # 两次快速点按（蹲下）之间的最大间隔，单位毫秒
  tap-window-ms: 500
  # 第二次点按到第三次长按之间的最大间隔，单位毫秒
  hold-window-ms: 500
  # 允许触发拉屎的游戏模式
  game-modes:
    - SURVIVAL
    - ADVENTURE
    - CREATIVE

# ---------- 进度条设置 ----------
progress:
  # 进度条标题文字
  title: "正在拉屎"
  # 竖线总数量
  bar-length: 20
  # 从开始到涨满所需时间，单位毫秒
  duration-ms: 3000
  # 未填充部分的颜色（初始颜色）
  start-color: GREEN
  # 已填充（推进）部分的颜色
  fill-color: RED
  # 玩家受伤时是否打断拉屎
  cancel-on-damage: true

# ---------- 屎的设置 ----------
poop:
  # 掉落物类型（Bukkit Material 名称）
  material: GLOWSTONE_DUST
  # 悬浮名格式，%player% 会被替换为玩家名
  name-format: "%player%的屎"
  # 悬浮名可见半径，单位格
  name-view-radius: 8.0
  # 存活时间，单位秒（到时间后消失）
  lifetime-seconds: 60
  # 是否允许玩家拾取（false = 不可拾取）
  can-pickup: false

  # ---------- 数量限制 ----------
  limit:
    # 是否启用数量限制（true 时下方配置生效）
    enabled: false
    # 单个玩家在同一服务器内同时存在的屎数量上限
    max-per-player: 1
    # 达到上限时显示在动作栏的提示文字
    message: "你拉不出来了！地上的屎太多了"
    # 提示冷却，单位毫秒（防止连按刷屏）
    message-cooldown-ms: 2000
```

### 配置说明

| 配置项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `trigger.tap-window-ms` | 整数 | `500` | 两次点按的最大间隔（毫秒） |
| `trigger.hold-window-ms` | 整数 | `500` | 点按到长按的最大间隔（毫秒） |
| `trigger.game-modes` | 列表 | 生存/冒险/创造 | 允许触发的游戏模式 |
| `progress.title` | 文本 | `正在拉屎` | 进度条标题 |
| `progress.bar-length` | 整数 | `20` | 竖线数量 |
| `progress.duration-ms` | 整数 | `3000` | 涨满耗时（毫秒） |
| `progress.start-color` | 颜色 | `GREEN` | 未填充部分颜色 |
| `progress.fill-color` | 颜色 | `RED` | 填充部分颜色 |
| `progress.cancel-on-damage` | 布尔 | `true` | 受伤是否打断 |
| `poop.material` | 物品 | `GLOWSTONE_DUST` | 掉落物类型 |
| `poop.name-format` | 文本 | `%player%的屎` | 悬浮名格式 |
| `poop.name-view-radius` | 小数 | `8.0` | 悬浮名可见半径 |
| `poop.lifetime-seconds` | 整数 | `60` | 存活秒数 |
| `poop.can-pickup` | 布尔 | `false` | 是否可拾取 |
| `poop.limit.enabled` | 布尔 | `false` | 是否启用数量限制 |
| `poop.limit.max-per-player` | 整数 | `1` | 单玩家数量上限 |
| `poop.limit.message` | 文本 | 见上 | 达到上限的提示文字 |
| `poop.limit.message-cooldown-ms` | 整数 | `2000` | 提示冷却（毫秒） |

> 颜色支持 Bukkit `NamedTextColor` 全部名称：`GREEN`、`RED`、`GOLD`、`AQUA`、`YELLOW`、`WHITE` 等。
> 物品使用 Bukkit `Material` 名称，如 `GLOWSTONE_DUST`、`BROWN_DYE`。

## 从源码构建

需要 **JDK 21+** 和 **Maven**：

```bash
git clone <仓库地址>
cd Pooping
mvn clean package
```

产物位于 `target/pooping-x.x.jar`。

## 作者

**Nice_Leo_**

## 许可

本项目基于 [GPL-3.0 license](LICENSE) 开源。
