# Show Your Keys 开发者文档

> 本文档面向希望为 Show Your Keys 添加适配的 **Mod 开发者** 或 **整合包作者**。  
> 如果你只是普通玩家，请看 [README.md](README.md)。

---

## 目录

1. [项目结构速览](#1-项目结构速览)
2. [核心概念](#2-核心概念)
3. [API 入口：ShowYourKeysAPI](#3-api-入口showyourkeysapi)
4. [三种扩展方式](#4-三种扩展方式)
    - [方式 A：注册自定义 Provider（推荐）](#方式-a注册自定义-provider推荐)
    - [方式 B：注册 ItemAbility 映射（工具扩展 Mod 专用）](#方式-b注册-itemability-映射工具扩展-mod-专用)
    - [方式 C：注册自定义槽位](#方式-c注册自定义槽位)
5. [Hint 工厂方法速查](#5-hint-工厂方法速查)
6. [SlotContainer 用法速查](#6-slotcontainer-用法速查)
7. [Slot 级别优先级机制](#7-slot-级别优先级机制)
8. [HintContext 速查](#8-hintcontext-速查)
9. [HintSlot 内置槽位速查](#9-hintslot-内置槽位速查)
10. [多语言适配](#10-多语言适配)
11. [完整示例：机械动力压力机适配](#11-完整示例机械动力压力机适配)
12. [常见问题 FAQ](#12-常见问题-faq)

---

## 1. 项目结构速览

```
com.gzxx.show_your_keys/
│
├── api/                              ← ★ 你只需要关注这里
│   ├── ShowYourKeysAPI.java          #   唯一对外入口，所有注册从这里开始
│   ├── hint/
│   │   ├── IKeyHintProvider.java    #   Provider 接口（需要实现）
│   │   ├── HintContext.java         #   当前帧状态快照（只读输入）
│   │   ├── Hint.java                #   单条提示数据（工厂方法构造）
│   │   ├── SlotContainer.java       #   单个槽位的提示容器（内置去重）
│   │   └── HintSlot.java            #   内置槽位 ID 常量
│   └── registry/
│       ├── ProviderRegistry.java    #   Provider 注册表
│       ├── SlotRegistry.java        #   槽位注册表
│       └── AbilityHintRegistry.java #   ItemAbility 映射注册表
│
└── internal/                         ← ✗ 请勿直接引用，随时可能变动
    ├── engine/HintEngine.java
    ├── provider/
    │   ├── VanillaHintProvider.java
    │   ├── MovementHintProvider.java
    │   ├── ItemAbilityHintProvider.java
    │   ├── RedstoneHintProvider.java
    │   └── FallbackHintProvider.java
    └── render/HudRenderer.java
```

**黄金法则：只导入 `api` 包下的类，永远不要导入 `internal` 包下的类。**

---

## 2. 核心概念

### Provider（提示提供者）

Provider 是按键提示的生产者。每帧，引擎调用所有已注册的 Provider，
按 **Slot 级别优先级竞争** 规则计算最终结果，交给渲染器显示在 HUD 上。

### HintContext（帧快照）

每帧采集一次的玩家状态只读快照，包含：
- 玩家实例（状态查询）
- 主手物品
- 准心射线命中结果（方块 / 实体 / 空气）
- 当前世界

Provider 的 `getHints(ctx)` 方法接收此快照作为唯一输入。

### Hint（单条提示数据）

HUD 上一行显示的数据载体，包含四个字段：
- **priority**（槽内优先级）：数值越小越靠上；相同按键冲突时决定保留哪条
- **prefix**（可选，红色警告文字，如"工具错误"）
- **keyLabel**（灰色方框内的按键名）
- **actionLabel**（按键框右侧的动作说明）

`Hint` **不包含槽位信息**，槽位由外层的 `SlotContainer` 决定。

### SlotContainer（槽位容器）

每个 `SlotContainer` 对应一个槽位（如 `HintSlot.USE`），内部持有该槽位下的所有 `Hint`。

**核心特性——同键去重**：调用 `add(Hint)` 时，以按键名为键自动去重：

- 若该按键尚无提示 → 直接插入
- 若已存在相同按键的提示，且新提示的 `priority` 更小（优先级更高）→ 替换
- 否则 → 忽略（保留已有的高优先级提示）

这从根本上消除了"同一按键在同一槽位显示多个互相矛盾的动作"的问题。

### HintSlot（槽位 ID 常量）

槽位决定该行在 HUD 面板中从上到下的位置，由 `SlotRegistry` 管理排序值（order）。
同一槽位可以有多条提示，按 `Hint#priority()` 升序排列（数值越小越靠上）。

---

## 3. API 入口：ShowYourKeysAPI

所有对外功能均通过 `ShowYourKeysAPI` 的三个静态方法访问：

```java
ShowYourKeysAPI.providers()   // ProviderRegistry     注册 Provider
ShowYourKeysAPI.slots()       // SlotRegistry         注册自定义槽位
ShowYourKeysAPI.abilities()   // AbilityHintRegistry  注册 ItemAbility 映射
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

### 方式 A：注册自定义 Provider（推荐）

适用于大多数场景：需要根据复杂条件显示自定义提示。

**第一步：实现 `IKeyHintProvider` 接口**

```java
import com.gzxx.show_your_keys.api.hint.*;
import net.minecraft.client.Minecraft;
import java.util.List;
import java.util.Optional;

public class MyMachineHintProvider implements IKeyHintProvider {

    /**
     * 优先级：数值越小优先级越高。
     * 对于你返回的 SlotContainer 所在槽位，priority 低于 VanillaHintProvider(81) 的
     * Provider 会覆盖该槽位上的原版提示。不同槽位之间独立竞争，互不影响。
     */
    @Override
    public int getPriority() { return 75; }

    @Override
    public Optional<List<SlotContainer>> getHints(HintContext ctx) {
        // 条件判断：只在准心对准方块，且手持特殊工具时显示
        if (!ctx.isLookingAtBlock()) return Optional.empty();
        if (!(ctx.heldItem().getItem() instanceof MySpecialTool)) return Optional.empty();

        Minecraft mc = Minecraft.getInstance();

        // 创建 USE 槽容器，添加提示
        // fromMapping 可随玩家改键自动更新按键名
        SlotContainer useSlot = SlotContainer.of(HintSlot.USE,
                Hint.fromMapping(mc.options.keyUse, "mymod.hint.process"));

        return Optional.of(List.of(useSlot));
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

内部的 `ItemAbilityHintProvider` 会自动检测并在 USE 槽显示提示。

```java
modEventBus.addListener((FMLClientSetupEvent event) -> {
    ShowYourKeysAPI.abilities().register(
            MyItemAbilities.MAGNETIZE,   // 你的 ItemAbility
            "mymod.hint.magnetize"       // lang 文件翻译键
    );
});
```

**工作原理**：每帧对准方块时，引擎调用
`Block.getToolModifiedState(state, ctx, ability, true)` 模拟操作，
若结果方块与原方块不同，则说明该能力可用，对应提示显示在 USE 槽。

---

### 方式 C：注册自定义槽位

当内置槽位（USE、ATTACK 等）无法满足需求时，可注册新槽位。

```java
modEventBus.addListener((FMLClientSetupEvent event) -> {
    // 在 USE(300) 和 ATTACK(400) 之间插入自定义槽位
    ShowYourKeysAPI.slots().register("mymod.charge", 350);
});
```

然后在 Provider 中使用该槽位：

```java
SlotContainer chargeSlot = SlotContainer.of("mymod.charge",
        Hint.fromMapping(mc.options.keyAttack, "mymod.hint.charge"));
```

---

## 5. Hint 工厂方法速查

`Hint` 提供两组静态工厂方法，覆盖所有常见场景。
`Hint` 本身不含槽位信息，槽位由外层 `SlotContainer` 指定。

| 方法签名 | 说明 |
|---|---|
| `Hint.of(key, action)` | 字面量按键名，priority=0，无前缀 |
| `Hint.of(priority, key, action)` | 指定槽内优先级 |
| `Hint.of(prefix, key, action)` | 带警告前缀（红色文字），priority=0 |
| `Hint.of(priority, prefix, key, action)` | 优先级 + 前缀，全参数 |
| `Hint.fromMapping(mapping, action)` | 按键名来自 `KeyMapping`（随改键更新），priority=0 |
| `Hint.fromMapping(priority, mapping, action)` | 指定优先级 |
| `Hint.fromMapping(prefix, mapping, action)` | 带前缀 |
| `Hint.fromMapping(priority, prefix, mapping, action)` | 全参数 |

**`of` vs `fromMapping` 如何选择？**

- 按键**固定不变**（如 `"W / A / S / D"`、`"Space"`）→ 使用 `of`
- 按键**可由玩家自定义**（攻击键、使用键、跳跃键等）→ 使用 `fromMapping`，
  传入 `mc.options.keyUse` 等，改键后显示自动更新

**`priority`（槽内优先级）是什么？**

同一 `SlotContainer` 内有多条提示时，`priority` 决定上下顺序（数值越小越靠上）。
更重要的是，**当两条提示的按键名相同时，`priority` 更小的那条会保留，另一条被丢弃**，
以防止同一按键显示两个互相矛盾的动作。不传时默认为 `0`。

---

## 6. SlotContainer 用法速查

`SlotContainer` 是槽位的提示容器，也是 `getHints()` 的返回单元。

**创建方式**

```java
// 方式 1：工厂方法，一次性创建并填入提示（推荐，简洁）
SlotContainer useSlot = SlotContainer.of(HintSlot.USE,
        Hint.fromMapping(mc.options.keyUse, "mymod.hint.process"));

// 方式 2：逐步构建，适合条件判断多的场景
SlotContainer useSlot = new SlotContainer(HintSlot.USE);
if (someCondition) {
    useSlot.add(Hint.fromMapping(mc.options.keyUse, "mymod.hint.process"));
}
if (!useSlot.isEmpty()) {
    result.add(useSlot);
}
```

**同键去重规则（自动处理，无需手动干预）**

```java
SlotContainer slot = new SlotContainer(HintSlot.USE);
// 假设两个 Hint 的 keyLabel 都是右键
slot.add(Hint.fromMapping(0, mc.options.keyUse, "mymod.hint.action_a")); // priority=0
slot.add(Hint.fromMapping(5, mc.options.keyUse, "mymod.hint.action_b")); // priority=5，被丢弃
// 结果：只保留 priority=0 的 action_a
```

**不要返回空容器**：引擎会自动过滤掉空的 `SlotContainer`，
但为了代码清晰，建议在添加前做好条件判断，或在返回前检查 `isEmpty()`。

---

## 7. Slot 级别优先级机制

引擎调用所有 Provider，对 **每个槽位独立进行优先级竞争**：

```
场景示例：准心对准中继器，未蹲下

Provider                priority   返回的槽位
─────────────────────────────────────────────────────
ItemAbilityHintProvider   65       （无适用能力，返回 empty）
RedstoneHintProvider      79       USE, ATTACK     ← 这两槽获胜
MovementHintProvider      80       SHIFT, SPRINT, DROP ← 无竞争，直接获胜
VanillaHintProvider       81       USE, ATTACK     ← 被 79 覆盖，忽略
FallbackHintProvider     100       USE, ATTACK     ← 被 79 覆盖，忽略

最终 HUD：
  [右键] 切换挡位      ← RedstoneHintProvider (79) 的 USE 槽
  [左键] 挖掘          ← RedstoneHintProvider (79) 的 ATTACK 槽
  [Shift] 蹲下         ← MovementHintProvider (80) 的 SHIFT 槽
  [Ctrl] 疾跑          ← MovementHintProvider (80) 的 SPRINT 槽
```

**三层去重机制**

系统通过三个层次共同保证提示不重复、不矛盾：

| 层次 | 位置 | 作用 |
|---|---|---|
| **Provider 间** | `HintEngine` | 对每个槽位，只保留 `provider.getPriority()` 最小的 Provider 的 `SlotContainer` |
| **同键去重** | `SlotContainer.add()` | 同一槽位中，相同按键只保留 `hint.priority()` 最小的一条 |
| **逻辑互斥** | Provider 代码中的 `else-if` | 从代码结构上确保同场景下同槽位不会有重叠的分支 |

**返回 `Optional.empty()` 的含义**

Provider 对当前帧放弃所有槽位的竞争，不影响其他 Provider 对任何槽位的处理。

**内置 Provider 覆盖的槽位一览**

| Provider | priority | 主要覆盖的槽位 | 备注 |
|---|---|---|---|
| `ItemAbilityHintProvider` | 65 | USE | 无可用工具能力时返回 empty |
| `RedstoneHintProvider` | 79 | USE, ATTACK | 非红石元件时返回 empty |
| `MovementHintProvider` | 80 | SHIFT, SPRINT, DROP | 骑乘/游泳时部分槽位不返回 |
| `VanillaHintProvider` | 81 | USE, ATTACK, MOVE, JUMP, SHIFT | 通用原版逻辑 |
| `FallbackHintProvider` | 100 | USE, ATTACK | 兜底，无其他 Provider 时生效 |

**常见用法参考**

| 场景 | 建议 priority | 说明 |
|---|---|---|
| 覆盖特定方块的 USE 和 ATTACK 槽 | 70 ~ 80 | 低于 VanillaHintProvider(81) 即可 |
| 追加始终显示的状态提示（新槽位） | 任意 | 使用内置 Provider 未覆盖的槽位 ID |
| 覆盖所有场景下的所有槽位 | < 65 | 低于所有内置 Provider |

---

## 8. HintContext 速查

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
ctx.player().isCrouching()              // 是否蹲下
ctx.player().isSprinting()              // 是否疾跑
ctx.player().isSwimming()               // 是否游泳
ctx.player().isPassenger()              // 是否骑乘载具
ctx.player().getVehicle()               // 载具实体（可能为 null）
ctx.player().getAttackStrengthScale(0f) // 攻击蓄力进度 0.0~1.0
```

---

## 9. HintSlot 内置槽位速查

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

## 10. 多语言适配

`Hint` 的动作说明和前缀均使用翻译键，需在 lang 文件中添加对应条目。

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

## 11. 完整示例：机械动力压力机适配

假设你想为"机械动力"（Create）的压力机（Press）添加提示：
当玩家手持铁锤对准压力机时，显示"右键 → 冲压"。

**`MyCreateHintProvider.java`**

```java
package com.example.mymod.compat;

import com.gzxx.show_your_keys.api.hint.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import java.util.Optional;

public class MyCreateHintProvider implements IKeyHintProvider {

    // priority=79：低于 VanillaHintProvider(81)。
    // 本 Provider 返回 USE 和 ATTACK 槽时，这两个槽上的原版提示会被覆盖。
    // MovementHintProvider(80) 负责的 SHIFT/SPRINT/DROP 槽不受任何影响。
    @Override
    public int getPriority() { return 79; }

    @Override
    public Optional<List<SlotContainer>> getHints(HintContext ctx) {
        if (!ctx.isLookingAtBlock()) return Optional.empty();
        if (!(ctx.heldItem().getItem() instanceof HammerItem)) return Optional.empty();

        BlockState state = ctx.getTargetBlockState();
        if (state == null || !(state.getBlock() instanceof MechanicalPressBlock)) {
            return Optional.empty();
        }

        Minecraft mc = Minecraft.getInstance();

        return Optional.of(List.of(
                SlotContainer.of(HintSlot.USE,
                        Hint.fromMapping(mc.options.keyUse, "mymod.hint.press_process")),
                SlotContainer.of(HintSlot.ATTACK,
                        Hint.fromMapping(mc.options.keyAttack, "hint.show_your_keys.mine"))
        ));
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

## 12. 常见问题 FAQ

**Q：我的 Provider 注册了，但提示没有显示？**

A：按以下顺序排查：
1. 注册是否在 `FMLClientSetupEvent` 中进行？不能在静态块或构造函数中注册。
2. `getHints()` 是否在条件满足时返回了非空列表？可加日志确认是否被调用。
3. 是否有更高优先级（数值更小）的 Provider 也为同一槽位返回了 `SlotContainer`？参见第 7 节的内置 Provider 表。
4. 返回的 `SlotContainer` 是否为空（调用了 `add()` 但条件不满足导致实际没有提示）？引擎会忽略空容器。
5. 确保使用 `Optional.empty()` 表示弃权，而非 `Optional.of(List.of())`——后者相当于"声明参与竞争但不提供任何槽位"，可能导致其他 Provider 在某些槽位上也无法显示。

**Q：如何在某个槽位上与原版提示共存（而非覆盖）？**

A：将你的提示放在原版 Provider 未覆盖的 **其他槽位**，或使用比 `VanillaHintProvider`（81）**更大**的 priority（如 90），这样只有当 Vanilla 不为该槽位返回内容时你的提示才会显示。

**Q：如何在提示前加红色警告文字（如"无能量"）？**

A：使用带 `prefixTranslKey` 参数的工厂方法：

```java
SlotContainer.of(HintSlot.USE,
    Hint.fromMapping(
        "mymod.hint.prefix.no_power",   // 红色前缀翻译键
        mc.options.keyUse,
        "mymod.hint.process"
    )
)
```

**Q：同一槽位的同一按键出现了两条提示，怎么处理？**

A：这种情况在新系统中由 `SlotContainer` 自动处理——相同按键只保留 `priority` 更小（优先级更高）的那条。如果结果不符合预期，检查两条提示的 `priority` 值，确保你希望显示的那条 `priority` 更小。

**Q：`Optional.empty()` 和返回空列表 `Optional.of(List.of())` 有什么区别？**

A：有实质区别。
- `Optional.empty()` → 本 Provider 对当前帧完全弃权，所有槽位竞争与本 Provider 无关，其他 Provider 正常处理所有槽位。
- `Optional.of(List.of())` → 本 Provider 声明"参与竞争"但不提供任何槽位。由于引擎以 Provider 返回值是否为 `empty()` 判断是否弃权，返回空列表仍被视为"参与了"，但对最终输出无实际贡献。

几乎在所有情况下都应使用 `Optional.empty()` 表示"本 Provider 无适用提示"。

**Q：我能压制内置 Provider 对某个槽位的提示吗？**

A：可以。注册一个 `priority` 数值小于目标内置 Provider 的自定义 Provider，在你想接管的条件下返回该槽位的 `SlotContainer`，其余条件返回 `Optional.empty()` 让内置 Provider 正常处理即可。

**Q：我的 Provider 抛出了异常会怎样？**

A：引擎会捕获异常并打印 WARN 日志，跳过该 Provider，继续处理后续 Provider。不会导致游戏崩溃，但该帧该 Provider 的提示不会显示，请检查日志排查原因。

---

> 有任何问题或建议，欢迎在 [GitHub Issues](https://github.com/gzxx307/ShowYourKeys-1.21.1-NeoForge/issues) 中反馈！