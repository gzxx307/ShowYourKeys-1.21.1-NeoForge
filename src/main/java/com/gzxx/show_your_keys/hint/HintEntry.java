package com.gzxx.show_your_keys.hint;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 一条按键提示，由四部分组成：
 *
 * <pre>
 *  [前缀]  [按键框]  动作说明
 *   红色   深灰背景   浅灰色
 * </pre>
 *
 * <ul>
 *   <li>{@code slotId}      —— 归属槽位，决定在 HUD 中从上到下的显示位置（见 {@link HintSlot}）</li>
 *   <li>{@code slotPriority}—— 槽内顺序，同槽多条提示时值越小越靠前（默认 0）</li>
 *   <li>{@code prefix}      —— 按键框左侧的红色警告文字，例如「工具错误」「蓄力未完成」（可为 null）</li>
 *   <li>{@code keyLabel}    —— 按键名称，显示在深灰背景框中，例如「[左键]」</li>
 *   <li>{@code actionLabel} —— 动作说明，显示在按键框右侧，例如「挖掘」「放置」</li>
 * </ul>
 *
 * <h3>排序键</h3>
 * <p>{@link #sortKey()} = {@code HintSlot.getOrder(slotId) × 1000 + slotPriority}，
 * 由 {@link HintEngine} 用于将所有 Provider 的结果排序后再渲染，
 * 确保 HUD 布局与 Provider 执行顺序无关。</p>
 */
public record HintEntry(
        String slotId,
        int slotPriority,
        @Nullable Component prefix,
        Component keyLabel,
        Component actionLabel
) {

    /** 用于 HintEngine 排序的综合键：先按槽位顺序，再按槽内优先级。 */
    public int sortKey() {
        return HintSlot.getOrder(slotId) * 1000 + slotPriority;
    }

    public boolean hasPrefix() {
        return prefix != null;
    }

    // ── 工厂方法：字符串字面量按键标签，无前缀 ───────────────────────────

    /** 基础版：slot + 字面量按键 + 动作，槽内优先级 0。 */
    public static HintEntry of(String slot, String keyLiteral, String actionTranslKey) {
        return new HintEntry(slot, 0, null,
                Component.literal(keyLiteral),
                Component.translatable(actionTranslKey));
    }

    /** 带槽内优先级版。 */
    public static HintEntry of(String slot, int priority, String keyLiteral, String actionTranslKey) {
        return new HintEntry(slot, priority, null,
                Component.literal(keyLiteral),
                Component.translatable(actionTranslKey));
    }

    // ── 工厂方法：KeyMapping 按键标签，无前缀 ────────────────────────────

    /** 基础版：slot + KeyMapping + 动作，槽内优先级 0。 */
    public static HintEntry fromMapping(String slot, KeyMapping mapping, String actionTranslKey) {
        return new HintEntry(slot, 0, null,
                mapping.getTranslatedKeyMessage(),
                Component.translatable(actionTranslKey));
    }

    /** 带槽内优先级版。 */
    public static HintEntry fromMapping(String slot, int priority, KeyMapping mapping, String actionTranslKey) {
        return new HintEntry(slot, priority, null,
                mapping.getTranslatedKeyMessage(),
                Component.translatable(actionTranslKey));
    }

    // ── 工厂方法：字符串字面量按键标签，带红色前缀 ──────────────────────

    /** 带前缀版：slot + 前缀翻译 key + 字面量按键 + 动作，槽内优先级 0。 */
    public static HintEntry of(String slot, String prefixTranslKey, String keyLiteral, String actionTranslKey) {
        return new HintEntry(slot, 0,
                Component.translatable(prefixTranslKey),
                Component.literal(keyLiteral),
                Component.translatable(actionTranslKey));
    }

    /** 带前缀 + 优先级版。 */
    public static HintEntry of(String slot, int priority, String prefixTranslKey, String keyLiteral, String actionTranslKey) {
        return new HintEntry(slot, priority,
                Component.translatable(prefixTranslKey),
                Component.literal(keyLiteral),
                Component.translatable(actionTranslKey));
    }

    // ── 工厂方法：KeyMapping 按键标签，带红色前缀 ────────────────────────

    /** 带前缀版：slot + 前缀翻译 key + KeyMapping + 动作，槽内优先级 0。 */
    public static HintEntry fromMapping(String slot, String prefixTranslKey, KeyMapping mapping, String actionTranslKey) {
        return new HintEntry(slot, 0,
                Component.translatable(prefixTranslKey),
                mapping.getTranslatedKeyMessage(),
                Component.translatable(actionTranslKey));
    }

    /** 带前缀 + 优先级版。 */
    public static HintEntry fromMapping(String slot, int priority, String prefixTranslKey, KeyMapping mapping, String actionTranslKey) {
        return new HintEntry(slot, priority,
                Component.translatable(prefixTranslKey),
                mapping.getTranslatedKeyMessage(),
                Component.translatable(actionTranslKey));
    }
}