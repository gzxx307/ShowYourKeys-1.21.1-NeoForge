# Show Your Keys 文档

> 本文档面向希望为 Show Your Keys 添加适配的 **Mod 开发者** 或 **整合包作者**。  
> 如果你只是普通玩家，请看 [README.md](README.md)。

---

## 目录

1. [项目结构速览](#1-项目结构速览)
2. [核心概念](#2-核心概念)
3. [API 入口：ShowYourKeysAPI](#3-api-入口showyourkeysapi)
4. [三种扩展方式](#4-三种扩展方式)
    - [方式 A：注册自定义 Provider](#方式-a注册自定义-provider推荐)
    - [方式 B：注册 ItemAbility 映射（工具扩展 Mod 专用）](#方式-b注册-itemability-映射工具扩展-mod-专用)
    - [方式 C：注册自定义槽位](#方式-c注册自定义槽位)
5. [HintEntry 工厂方法速查](#5-hintentry-工厂方法速查)
6. [责任链机制与优先级](#6-责任链机制与优先级)
7. [HintContext 速查](#7-hintcontext-速查)
8. [HintSlot 内置槽位速查](#8-hintslot-内置槽位速查)
9. [多语言适配](#9-多语言适配)
10. [完整示例：机械动力压力机适配](#10-完整示例机械动力压力机适配)
11. [常见问题 FAQ](#11-常见问题-faq)

---

## 1. 项目结构速览

```
com.gzxx.show_your_keys/
│
├── api/                            ← ★ 你只需要关注这里
│   ├── ShowYourKeysAPI.java        #   唯一对外入口，所有注册从这里开始
│   ├── hint/
│   │   ├── IKeyHintProvider.java  #   Provider 接口（需要实现）
│   │   ├── HintContext.java       #   当前帧状态快照（只读输入）
│   │   ├── HintEntry.java         #   单条提示（工厂方法构造）
│   │   └── HintSlot.java          #   内置槽位 ID 常量
│   └── registry/
│       ├── ProviderRegistry.java  #   Provider 注册表
│       ├── SlotRegistry.java      #   槽位注册表
│       └── AbilityHintRegistry.java # ItemAbility 映射注册表
│
└── internal/                       ← ✗ 请勿直接引用，随时可能变动
    ├── engine/HintEngine.java
    ├── provider/
    │   ├── VanillaHintProvider.java
    │   ├── MovementHintProvider.java
    │   ├── ItemAbilityHintProvider.java
    │   └── FallbackHintProvider.java
    └── render/HudRenderer.java
```

**黄金法则：只导入 `api` 包下的类，永远不要导入 `internal` 包下的类。**

---

## 2. 核心概念

### Provider（提示提供者）

Provider 是按键提示的生产者。每帧，引擎按优先级顺序调用所有已注册的 Provider，
收集它们返回的 `HintEntry` 列表，最终交给渲染器显示在 HUD 上。

### HintContext（帧快照）

每帧采集一次的玩家状态只读快照，包含：
- 玩家实例（状态查询）
- 主手物品
- 准心射线命中结果（方块/实体/空气）
- 当前世界

Provider 的 `getHints(ctx)` 方法接收此快照作为唯一输入。

### HintEntry（单条提示）

HUD 上的一行显示，包含：
- **槽位**（决定上下位置）
- **前缀**（可选，红色警告文字，如"工具错误"）
- **按键名**（灰色方框内的文字）
- **动作说明**（按键框右侧的文字）

### HintSlot（槽位）

每个 `HintEntry` 属于一个槽位（如 `use`、`attack`）。槽位决定该行在面板中的排列顺序。
同一槽位可以有多条提示，按槽内优先级（`slotPriority`）排序。

---

## 3. API 入口：ShowYourKeysAPI

所有对外功能均通过 `ShowYourKeysAPI` 的三个静态方法访问：

```java
ShowYourKeysAPI.providers()   // ProviderRegistry  注册 Provider
ShowYourKeysAPI.slots()       // SlotRegistry      注册自定义槽位
ShowYourKeysAPI.abilities()   // AbilityHintRegistry 注册 ItemAbility 映射
```

**注册时机**：必须在 `FMLClientSetupEvent` 中注册。

```java
// 在你的 Mod 主类或客户端类中：
modEventBus.addListener((FMLClientSetupEvent event) -> {
    ShowYourKeysAPI.providers().register(new MyProvider());
});
```

---

## 4. 三种扩展方式

### 方式 A：注册自定义 Provider

适用于大多数场景：需要根据复杂条件显示自定义提示。

**第一步：实现 `IKeyHintProvider` 接口**

```java
import com.gzxx.show_your_keys.api.hint.*;

public class MyMachineHintProvider implements IKeyHintProvider {

    /**
     * 优先级：数值越小越先执行。
     * 如果你的 Provider 是"终止模式"，需要比 VanillaHintProvider(81) 更高优先级，
     * 则设置为 80 以下的值。
     * 如果是"叠加模式"（追加提示），任何值均可，通常 65~79。
     */
    @Override
    public int getPriority() { return 75; }

    /**
     * 叠加模式：返回结果后，责任链继续向下。
     * 终止模式（默认 false）：返回结果后，责任链停止。
     */
    @Override
    public boolean isAdditive() { return false; }

    @Override
    public Optional<List<HintEntry>> getHints(HintContext ctx) {
        // 示例：只在准心对准方块，且手持我的 Mod 的特殊工具时显示提示
        if (!ctx.isLookingAtBlock()) return Optional.empty();
        if (!(ctx.heldItem().getItem() instanceof MySpecialTool)) return Optional.empty();

        Minecraft mc = Minecraft.getInstance();
        List<HintEntry> hints = new ArrayList<>();

        // 使用 fromMapping 可随玩家改键自动更新按键名
        hints.add(HintEntry.fromMapping(
                HintSlot.USE,                    // 显示在 USE 槽（右键区域）
                mc.options.keyUse,              // 跟随玩家改键设置
                "mymod.hint.process"            // lang 文件中的翻译键
        ));

        return Optional.of(hints);
    }
}
```

**第二步：注册**

```java
modEventBus.addListener((FMLClientSetupEvent event) -> {
    ShowYourKeysAPI.providers().register(new MyMachineHintProvider());
});
```

---

### 方式 B：注册 ItemAbility 映射（工具扩展 Mod 专用）

如果你的 Mod 只是给现有方块添加了新的 `ItemAbility`（如"磁化"、"镀层"），
只需注册一行映射，无需编写 Provider。

内部的 `ItemAbilityHintProvider` 会自动检测并显示提示。

```java
modEventBus.addListener((FMLClientSetupEvent event) -> {
    ShowYourKeysAPI.abilities().register(
            MyItemAbilities.MAGNETIZE,   // 你的 ItemAbility
            "mymod.hint.magnetize"       // lang 文件翻译键
    );
});
```

**工作原理**：每帧对准方块时，引擎会调用
`Block.getToolModifiedState(state, ctx, ability, true)` 模拟操作，
若结果方块与原方块不同，则说明该能力可用，对应提示会显示在 USE 槽。

---

### 方式 C：注册自定义槽位

当内置槽位（USE、ATTACK 等）无法满足需求时，可注册新槽位。

```java
modEventBus.addListener((FMLClientSetupEvent event) -> {
    // 在 USE(300) 和 ATTACK(400) 之间插入自定义槽位
    ShowYourKeysAPI.slots().register("mymod.charge", 350);
});
```

然后在 `HintEntry` 中使用该槽位：

```java
hints.add(HintEntry.fromMapping("mymod.charge", mc.options.keyAttack, "mymod.hint.charge"));
```

---

## 5. HintEntry 工厂方法速查

`HintEntry` 提供两组静态工厂方法，覆盖所有常见场景：

| 方法签名 | 说明 |
|---|---|
| `of(slot, key, action)` | 最简单，按键名为字面量（如 `"Space"`） |
| `of(slot, priority, key, action)` | 带槽内优先级 |
| `of(slot, prefix, key, action)` | 带警告前缀（红色文字） |
| `of(slot, priority, prefix, key, action)` | 带优先级 + 前缀 |
| `fromMapping(slot, mapping, action)` | 按键名来自 `KeyMapping`（随改键更新） |
| `fromMapping(slot, priority, mapping, action)` | 带槽内优先级 |
| `fromMapping(slot, prefix, mapping, action)` | 带前缀 |
| `fromMapping(slot, priority, prefix, mapping, action)` | 全参数 |

**`of` vs `fromMapping` 如何选择？**

- 如果按键是**固定的**（如 `"W / A / S / D"`、`"Space"`）：使用 `of`
- 如果按键**可由玩家自定义**（如攻击键、使用键、跳跃键）：使用 `fromMapping`，
  传入 `mc.options.keyUse` 等，这样当玩家改键后显示会自动更新

**槽内优先级（`slotPriority`）是什么？**

当同一个槽位（如 USE）有多条提示时，`slotPriority` 决定它们的上下顺序。
值越小越靠上。不传时默认为 `0`。

---

## 6. 责任链机制与优先级

引擎按 `priority` 从小到大遍历所有 Provider：

```
priority=65  ItemAbilityHintProvider  叠加模式  ─┐
priority=80  MovementHintProvider     叠加模式  ─┤ 结果合并
priority=81  VanillaHintProvider      终止模式  ─┘→ 停止
priority=100 FallbackHintProvider     终止模式  （上游都为 empty 时才执行）
```

**终止模式（`isAdditive()=false`，默认）**：
- Provider 返回非空列表 → 引擎将此结果（含之前叠加的结果）作为最终结果，停止遍历
- Provider 返回 `Optional.empty()` → 引擎继续调用下一个 Provider

**叠加模式（`isAdditive()=true`）**：
- Provider 返回非空列表 → 结果被收集，引擎继续调用下一个 Provider
- Provider 返回 `Optional.empty()` → 什么都不收集，继续

**常见用法**：

| 场景 | 推荐模式 | priority |
|---|---|---|
| 完全替代原版提示（自定义机器） | 终止，priority < 81 | 70~80 |
| 在原版提示基础上追加额外行 | 叠加，priority < 80 | 65~79 |
| 状态性提示（始终显示） | 叠加，任意 | 65~79 |
| 完全替换所有提示 | 终止，priority < 65 | < 65 |

---

## 7. HintContext 速查

| 方法 | 返回值 | 说明 |
|---|---|---|
| `ctx.player()` | `LocalPlayer` | 玩家实例，可查询各种状态 |
| `ctx.heldItem()` | `ItemStack` | 主手物品 |
| `ctx.level()` | `Level` | 当前世界 |
| `ctx.isLookingAtBlock()` | `boolean` | 准心是否对准方块 |
| `ctx.isLookingAtEntity()` | `boolean` | 准心是否对准实体 |
| `ctx.getTargetBlockState()` | `BlockState?` | 准心方块的状态 |
| `ctx.getBlockHitResult()` | `BlockHitResult?` | 含命中面、坐标等信息 |
| `ctx.getTargetEntity()` | `Entity?` | 准心指向的实体 |

**常用玩家状态**（通过 `ctx.player()` 访问）：

```java
ctx.player().isCrouching()    // 是否蹲下
ctx.player().isSprinting()    // 是否疾跑
ctx.player().isSwimming()     // 是否游泳
ctx.player().isPassenger()    // 是否骑乘载具
ctx.player().getVehicle()     // 载具实体（可能为 null）
ctx.player().getAttackStrengthScale(0f) // 攻击蓄力进度 0.0~1.0
```

---

## 8. HintSlot 内置槽位速查

| 常量 | ID 字符串 | order | 默认含义 |
|---|---|---|---|
| `HintSlot.MOVE` | `"move"` | 100 | W/A/S/D 移动 |
| `HintSlot.JUMP` | `"jump"` | 200 | Space 跳跃 |
| `HintSlot.USE` | `"use"` | 300 | 右键 交互/使用/放置 |
| `HintSlot.ATTACK` | `"attack"` | 400 | 左键 攻击/挖掘 |
| `HintSlot.SHIFT` | `"shift"` | 500 | Shift 蹲下 |
| `HintSlot.SPRINT` | `"sprint"` | 600 | Ctrl 疾跑 |
| `HintSlot.DROP` | `"drop"` | 700 | Q 丢出物品 |
| `HintSlot.SWAP` | `"swap"` | 800 | F 切换副手（预留） |

自定义槽位的 `order` 值可以插入到任意位置。
例如要在 USE 和 ATTACK 之间新增一行：注册 `order=350`。

---

## 9. 多语言适配

`HintEntry` 的动作说明和前缀均使用翻译键，需在 lang 文件中添加对应条目。

**`en_us.json`**（英文，必须提供）：

```json
{
  "mymod.hint.process": "Process",
  "mymod.hint.magnetize": "Magnetize",
  "mymod.hint.prefix.no_power": "No Power"
}
```

**`zh_cn.json`**（简体中文，可选）：

```json
{
  "mymod.hint.process": "加工",
  "mymod.hint.magnetize": "磁化",
  "mymod.hint.prefix.no_power": "无能量"
}
```

翻译键命名建议：
- 动作说明：`modid.hint.动作名`
- 警告前缀：`modid.hint.prefix.条件名`

---

## 10. 完整示例：机械动力压力机适配

假设你想为"机械动力"（Create）的压力机（Press）添加提示：
当玩家用铁锤对准压力机时，显示"右键 → 加工"。

**`MyCreateHintProvider.java`**

```java
package com.example.mymod.compat;

import com.gzxx.show_your_keys.api.hint.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MyCreateHintProvider implements IKeyHintProvider {

    // 优先级 79，终止模式：在 VanillaHintProvider(81) 之前触发，完全替换其结果
    @Override
    public int getPriority() { return 79; }

    @Override
    public Optional<List<HintEntry>> getHints(HintContext ctx) {
        // 条件 1：准心对准方块
        if (!ctx.isLookingAtBlock()) return Optional.empty();

        // 条件 2：手持铁锤
        if (!(ctx.heldItem().getItem() instanceof HammerItem)) return Optional.empty();

        // 条件 3：对准的是压力机
        BlockState state = ctx.getTargetBlockState();
        if (state == null || !(state.getBlock() instanceof MechanicalPressBlock)) {
            return Optional.empty();
        }

        // 满足所有条件，构建提示
        Minecraft mc = Minecraft.getInstance();
        List<HintEntry> hints = new ArrayList<>();

        hints.add(HintEntry.fromMapping(
                HintSlot.USE,
                mc.options.keyUse,
                "mymod.hint.press_process"   // lang: "Press"
        ));
        hints.add(HintEntry.fromMapping(
                HintSlot.ATTACK,
                mc.options.keyAttack,
                "hint.show_your_keys.mine"   // 复用内置翻译键
        ));

        return Optional.of(hints);
    }
}
```

**注册（在你的客户端初始化类中）**

```java
modEventBus.addListener((FMLClientSetupEvent event) -> {
    ShowYourKeysAPI.providers().register(new MyCreateHintProvider());
});
```

**lang 文件**

```json
// en_us.json
{ "mymod.hint.press_process": "Press" }

// zh_cn.json  
{ "mymod.hint.press_process": "冲压" }
```

---

## 11. 常见问题 FAQ

**Q：我的 Provider 注册了，但提示没有显示？**

A：按以下顺序检查：
1. 注册是否在 `FMLClientSetupEvent` 中进行？（不能在静态块或构造函数中）
2. `getHints()` 是否在满足条件时返回了非空列表？（调试时加日志确认是否被调用）
3. 优先级是否被其他 Provider 的终止模式拦截？（尝试降低 priority 值）
4. `Optional.empty()` 和 `Optional.of(emptyList)` 是两回事——后者会触发终止！

**Q：如何在原版提示基础上追加额外行，而不是替换？**

A：让你的 Provider 返回 `isAdditive() = true`，并设置 `priority < 81`。
引擎会先收集你的结果，再继续调用 `VanillaHintProvider`，两者合并显示。

**Q：如何在提示前加红色警告文字（如"无能量"）？**

A：使用带 `prefixTranslKey` 参数的工厂方法：

```java
HintEntry.fromMapping(
    HintSlot.USE,
    "mymod.hint.prefix.no_power",   // 红色前缀翻译键
    mc.options.keyUse,
    "mymod.hint.process"
)
```

**Q：`Optional.empty()` 和返回空列表 `Optional.of(List.of())` 有什么区别？**

A：有根本区别。
- `Optional.empty()` → 本 Provider 没有结果，责任链**继续**
- `Optional.of(List.of())` → 本 Provider 有结果（只是空列表），若为终止模式则**停止**链

通常应返回 `Optional.empty()` 而非空列表。

**Q：我能修改内置 Provider 的行为吗（如让它不显示攻击提示）？**

A：不建议直接修改内置 Provider。推荐做法是注册一个更高优先级的终止 Provider，
在你关心的条件下返回自定义结果，其他情况返回 `Optional.empty()` 让内置 Provider 处理。

**Q：我的 Provider 抛出了异常会怎样？**

A：引擎会捕获异常并打印 WARN 日志，跳过该 Provider，继续执行后续 Provider。
不会导致游戏崩溃，但你的提示不会显示，请检查日志。

---

> 有任何问题或建议，欢迎在 [GitHub Issues](https://github.com/gzxx307/ShowYourKeys-1.21.1-NeoForge/issues) 中反馈！