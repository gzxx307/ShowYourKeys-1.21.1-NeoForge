package com.gzxx.show_your_keys.api.hint;

import com.gzxx.show_your_keys.api.registry.SlotRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 单条按键提示，携带槽位、排序、前缀、按键名、动作说明五个字段。
 *
 * <p>通常不需要手动调用构造器，推荐使用静态工厂方法：</p>
 * <ul>
 *   <li>{@link #of} —— 用字面量按键名创建提示</li>
 *   <li>{@link #fromMapping} —— 用 {@link KeyMapping} 创建提示（自动随玩家改键更新）</li>
 * </ul>
 *
 * <h3>工厂方法命名规律</h3>
 * <pre>
 *  of(slot, key, action)                     基础（无前缀，优先级 0）
 *  of(slot, priority, key, action)           带槽内优先级
 *  of(slot, prefix, key, action)             带警告前缀
 *  of(slot, priority, prefix, key, action)   带优先级 + 前缀
 *  fromMapping(...)                           同上，key 来自 KeyMapping
 * </pre>
 *
 * @param slotId       所属槽位 ID（见 {@link HintSlot}）
 * @param slotPriority 槽内显示顺序（越小越靠上，同槽多条时生效）
 * @param prefix       警告前缀组件，可为 {@code null}
 * @param keyLabel     按键显示名
 * @param actionLabel  动作说明文字
 */
public record HintEntry(
        String slotId,
        int slotPriority,
        @Nullable Component prefix,
        Component keyLabel,
        Component actionLabel
) {

    // ── 排序 ─────────────────────────────────────────────────────────────────

    /**
     * 计算全局排序键，供渲染器排序使用。
     * <p>sortKey = 槽位 order × 1000 + 槽内优先级</p>
     */
    public int sortKey() {
        return SlotRegistry.INSTANCE.getOrder(slotId) * 1000 + slotPriority;
    }

    /** 是否携带前缀 */
    public boolean hasPrefix() {
        return prefix != null;
    }

    // ── 字面量按键名工厂方法 ──────────────────────────────────────────────────

    /**
     * 创建基础提示（无前缀，槽内优先级 0）。
     *
     * @param slot             槽位 ID
     * @param keyLiteral       按键名（如 "Space"、"W / A / S / D"）
     * @param actionTranslKey  动作翻译键
     */
    public static HintEntry of(String slot, String keyLiteral, String actionTranslKey) {
        return new HintEntry(slot, 0, null,
                Component.literal(keyLiteral),
                Component.translatable(actionTranslKey));
    }

    /**
     * 创建带槽内优先级的提示（无前缀）。
     *
     * @param slot             槽位 ID
     * @param priority         槽内优先级
     * @param keyLiteral       按键名
     * @param actionTranslKey  动作翻译键
     */
    public static HintEntry of(String slot, int priority, String keyLiteral, String actionTranslKey) {
        return new HintEntry(slot, priority, null,
                Component.literal(keyLiteral),
                Component.translatable(actionTranslKey));
    }

    /**
     * 创建带前缀的提示（槽内优先级 0）。
     *
     * @param slot             槽位 ID
     * @param prefixTranslKey  前缀翻译键（用于显示警告，如"工具错误"）
     * @param keyLiteral       按键名
     * @param actionTranslKey  动作翻译键
     */
    public static HintEntry of(String slot, String prefixTranslKey, String keyLiteral, String actionTranslKey) {
        return new HintEntry(slot, 0,
                Component.translatable(prefixTranslKey),
                Component.literal(keyLiteral),
                Component.translatable(actionTranslKey));
    }

    /**
     * 创建带前缀和槽内优先级的提示。
     *
     * @param slot             槽位 ID
     * @param priority         槽内优先级
     * @param prefixTranslKey  前缀翻译键
     * @param keyLiteral       按键名
     * @param actionTranslKey  动作翻译键
     */
    public static HintEntry of(String slot, int priority, String prefixTranslKey, String keyLiteral, String actionTranslKey) {
        return new HintEntry(slot, priority,
                Component.translatable(prefixTranslKey),
                Component.literal(keyLiteral),
                Component.translatable(actionTranslKey));
    }

    // ── KeyMapping 工厂方法 ───────────────────────────────────────────────────

    /**
     * 根据 {@link KeyMapping} 创建基础提示（无前缀，槽内优先级 0）。
     * <p>按键名会随玩家改键设置自动变化。</p>
     *
     * @param slot             槽位 ID
     * @param mapping          按键映射
     * @param actionTranslKey  动作翻译键
     */
    public static HintEntry fromMapping(String slot, KeyMapping mapping, String actionTranslKey) {
        return new HintEntry(slot, 0, null,
                mapping.getTranslatedKeyMessage(),
                Component.translatable(actionTranslKey));
    }

    /**
     * 根据 {@link KeyMapping} 创建带槽内优先级的提示（无前缀）。
     *
     * @param slot             槽位 ID
     * @param priority         槽内优先级
     * @param mapping          按键映射
     * @param actionTranslKey  动作翻译键
     */
    public static HintEntry fromMapping(String slot, int priority, KeyMapping mapping, String actionTranslKey) {
        return new HintEntry(slot, priority, null,
                mapping.getTranslatedKeyMessage(),
                Component.translatable(actionTranslKey));
    }

    /**
     * 根据 {@link KeyMapping} 创建带前缀的提示（槽内优先级 0）。
     *
     * @param slot             槽位 ID
     * @param prefixTranslKey  前缀翻译键
     * @param mapping          按键映射
     * @param actionTranslKey  动作翻译键
     */
    public static HintEntry fromMapping(String slot, String prefixTranslKey, KeyMapping mapping, String actionTranslKey) {
        return new HintEntry(slot, 0,
                Component.translatable(prefixTranslKey),
                mapping.getTranslatedKeyMessage(),
                Component.translatable(actionTranslKey));
    }

    /**
     * 根据 {@link KeyMapping} 创建带前缀和槽内优先级的提示。
     *
     * @param slot             槽位 ID
     * @param priority         槽内优先级
     * @param prefixTranslKey  前缀翻译键
     * @param mapping          按键映射
     * @param actionTranslKey  动作翻译键
     */
    public static HintEntry fromMapping(String slot, int priority, String prefixTranslKey, KeyMapping mapping, String actionTranslKey) {
        return new HintEntry(slot, priority,
                Component.translatable(prefixTranslKey),
                mapping.getTranslatedKeyMessage(),
                Component.translatable(actionTranslKey));
    }
}