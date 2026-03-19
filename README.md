# Show Your Keys

[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.219-blue)](https://neoforged.net/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green)](https://www.minecraft.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

## 前言

本来以为这样的模组开发起来可能会比较难，没想到会这么难T_T。

不但要先考虑怎么适配原版，还要考虑可扩展性，不然别的mod无论是我亲自适配还是其他作者适配都会特别麻烦。

## 模组简介

Show Your Keys 是一个轻量级的客户端模组，它会在游戏界面上显示当前可用的按键操作提示。当你手持特定工具或面对特定方块时，模组会自动检测可用的交互动作并显示相应的按键提示。

## 为什么要开发此mod？

拉舍友入坑的时候想起来"四个游戏凑不出一个新手教程"，所以就开发了（草）。

1. 为新手进行操作指导，例如驯服马、游泳、与方块交互等不那么明显的可交互选项。
2. 在游玩一个全新的玩法扩展mod时，可能出现因为操作复杂或教程指引不明确的情况。该模组可以提示基本的交互选项，也为各大mod开发者和整合包作者准备了易用的适配方式。

### 主要特性

- **上下文感知提示**：根据玩家当前状态自动显示相关按键提示
- **多种交互类型支持**：
  - 方块交互（挖掘、放置、互动）
  - 工具能力（斧头去皮、铲子铲平、锄头耕地等）
  - 物品使用（吃东西、喝药水、拉弓等）
  - 载具操作（骑马、划船、矿车加速等）
  - 移动状态（潜行、疾跑、游泳等）
- **便于 mod 适配的 Provider 系统**：通过注册自定义 Provider，支持自定义条件下的自定义按键提示
- **多语言支持**：继承自 Minecraft 源码的多语言适配

## 版本更新日志

_目前无测试与正式版本_

## 写给玩家的使用说明

_哪有啥使用说明啊轮椅完了（_

## 写给开发者

### 开发环境

- Windows 10/11
- Jetbrains IDEA （其他IDE应该也可以）
- JDK 21
- Minecraft 1.21.1
- NeoForge 21.1.219

### 项目结构

```
src/main/java/com/gzxx/show_your_keys/
│
├── api/                                   ← 对外公开 API（第三方 Mod 只需关注此处）
│   ├── ShowYourKeysAPI.java               #   统一入口，所有注册从这里开始
│   ├── hint/
│   │   ├── IKeyHintProvider.java          #   Provider 接口（需要实现）
│   │   ├── HintContext.java               #   当前帧状态快照（只读输入）
│   │   ├── Hint.java                      #   单条提示数据（工厂方法构造）
│   │   ├── SlotContainer.java             #   单个槽位的提示容器（内置去重）
│   │   └── HintSlot.java                  #   内置槽位 ID 常量
│   └── registry/
│       ├── ProviderRegistry.java          #   Provider 注册表
│       ├── SlotRegistry.java              #   槽位注册表
│       └── AbilityHintRegistry.java       #   ItemAbility 映射注册表
│
├── internal/                              ← 内部实现（请勿直接引用，随时可能变动）
│   ├── engine/
│   │   └── HintEngine.java               #   按键提示引擎，负责每帧计算与优先级竞争
│   ├── provider/
│   │   ├── ItemAbilityHintProvider.java   #   工具能力提示（去皮、耕地…）  priority=65
│   │   ├── RedstoneHintProvider.java      #   红石元件专属提示              priority=79
│   │   ├── MovementHintProvider.java      #   移动状态提示                  priority=80
│   │   ├── VanillaHintProvider.java       #   原版通用交互提示              priority=81
│   │   └── FallbackHintProvider.java      #   兜底提示                     priority=100
│   └── render/
│       └── HudRenderer.java              #   HUD 渲染器
│
├── Config.java                            # 模组配置
├── ShowYourKeys.java                      # Mod 主类
└── ShowYourKeysClient.java                # 客户端初始化入口
```

**黄金法则：只导入 `api` 包下的类，永远不要导入 `internal` 包下的类。**

### 如何扩展或适配该模组

详见 [Introduction.md](Introduction.md)，其中包含：

- 三种扩展方式（自定义 Provider、ItemAbility 映射、自定义槽位）
- Hint 工厂方法速查表
- SlotContainer 用法与去重规则说明
- Provider 优先级规划参考
- 完整适配示例
- 常见问题 FAQ

快速示例：

```java
// 1. 实现 IKeyHintProvider 接口
public class MyHintProvider implements IKeyHintProvider {
    @Override
    public int getPriority() { return 75; }

    @Override
    public Optional<List<SlotContainer>> getHints(HintContext ctx) {
        if (!ctx.isLookingAtBlock()) return Optional.empty();
        if (!(ctx.heldItem().getItem() instanceof MySpecialTool)) return Optional.empty();

        Minecraft mc = Minecraft.getInstance();
        return Optional.of(List.of(
            SlotContainer.of(HintSlot.USE,
                Hint.fromMapping(mc.options.keyUse, "mymod.hint.process"))
        ));
    }
}

// 2. 在 FMLClientSetupEvent 中注册
modEventBus.addListener((FMLClientSetupEvent event) -> {
    ShowYourKeysAPI.providers().register(new MyHintProvider());
});
```

### 构建项目

使用 Gradle 构建项目：

```bash
./gradlew build
```

输出文件位于 `build/libs/show_your_keys-{版本}.jar`，放入 `mods/` 文件夹即可运行。

### 多语言支持

模组支持多种语言，当前包含：

- 英语 (en_us)
- 简体中文 (zh_cn)

与大多数基于 NeoForge 的 Mod 一样，支持多语言需要在 `resources/assets/show_your_keys/lang/` 文件夹中进行配置。

> **完整的项目介绍与扩展方式请移步 [Introduction.md](Introduction.md)**

### 开发过程中的问题

由于代码本身的限制，我们无法直接通过源码获取玩家的所有交互选项。
目前的功能实现基本靠硬编码，即"若玩家手持哪类工具，射线获取到哪类方块，判断工具是否合法"等。
但这显然只能止步于在MC原版中使用，Mod的数量过于庞大，我们无法为每一个物品的每一种状态与对象都进行硬编码。

目前根据 ItemAbility / ToolAction 通用适配工具能力与等级的方式，虽然能够减少代码量，但仍然无法解决根本问题。

考虑到社区友好以及各位Mod开发者的头发（但我的头发快无了），我希望从多个方向来进行行为的注册与判断：
（以下为拥有优先级的获取按键提示的方法，优先级高的方法会覆盖优先级低的方法获取的结果，1为优先级最高）

1. 硬编码软依赖：自己适配某些热门常见模组，如机械动力（Create），Tweakroo等。另外，之后的模组开发者也能够做对我的模组的适配，甚至让热门模组也来主动适配（~~野心~~。
2. 对于大多数玩法扩展不多的Mod，仍然使用 ItemAbility / ToolAction 以保障适配大部分内容。
3. 通过反射的方式获取到逻辑或 ID注册名 / 元数据，再进行通用推断
4. ~~现在AI都这么发达了，为什么不能直接用AI批量适配mod呢（~~

## 更新计划

**代码部分**

- [x] 注册系统结构化重构
- [ ] 基础反射系统实现
- [ ] 数据管理与生成脚本

**内容部分**

- [ ] 原版内容补全（目前方块相关操作全是交互）

**用户交互部分**

- [ ] UI 美化与自定义支持
- [ ] Mod 加载部分的进度条

_更多计划计划中_

## 加入我们（只有我（悲）

欢迎提交 Issue 和 PR 来帮助改进这个模组！（支持我这个完全没有任何大佬认识我的入门程序吧QwQ）

希望各位大佬能够来提建议甚至一起来维护我的小Mod~~来考打我~~，我绝对会非常乐意学习大佬们的经验和知识的！

各位玩家的话，与我一起聊一聊这个小Mod，提一提问题，或者来告诉我找到哪些Bug，这些都算贡献！

## 许可证

本项目采用 MIT 许可证 - [LICENSE](LICENSE.txt)

## 致谢

- **[NeoForge](https://neoforged.net/)** - Minecraft mod 加载器框架
- **[Minecraft](https://www.minecraft.net/)** - 游戏本体

感谢 Minecraft modding 社区的宝贵资源和经验分享。

以及所有关注及支持该项目的各位，你们的支持是我持续更新与维护本Mod的最大动力！

## 相关链接

- [GitHub 仓库](https://github.com/gzxx307/ShowYourKeys-1.21.1-NeoForge)
- [问题追踪](https://github.com/gzxx307/ShowYourKeys-1.21.1-NeoForge/issues)
- [NeoForge 文档](https://docs.neoforged.net/)
- [Minecraft 官网](https://www.minecraft.net/)

以及：

- [Github 个人主页](https://github.com/gzxx307)
- [bilibili 个人主页](https://space.bilibili.com/372575779)
- 个人博客（暂无）

## 联系我

关于此Mod有任何问题或建议，欢迎通过以下方式联系：

- GitHub Issues -> [提交问题](https://github.com/gzxx307/ShowYourKeys-1.21.1-NeoForge/issues) <-
- 邮箱: [QQ] 3581544162@qq.com / [Gmail] gzxx307@gmail.com

## 最后

如果觉得该Mod为你带来了方便，或者认为该Mod未来可期，不妨点点免费的Star，这对一个学生来说无疑是莫大的鼓励！

我也会不断的更新该Mod，希望能够得到各位Mod开发大佬的认可以及玩家的肯定，这就足够了！