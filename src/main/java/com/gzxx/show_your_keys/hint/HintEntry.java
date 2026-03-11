package com.gzxx.show_your_keys.hint;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 一条按键提示，由三部分组成（后两部分可选）：
 *
 * <pre>
 *  [前缀]  [按键框]  动作说明
 *   红色   深灰背景   浅灰色
 * </pre>
 *
 * <ul>
 *   <li>{@code prefix}     —— 显示在按键框左侧的警告文字（红色），例如 "工具错误"、"蓄力未完成"</li>
 *   <li>{@code keyLabel}   —— 按键名称，显示在深灰背景框中，例如 "[左键]"、"[右键]"</li>
 *   <li>{@code actionLabel}—— 动作说明，显示在按键框右侧，例如 "挖掘"、"放置"</li>
 * </ul>
 */
public record HintEntry(
        @Nullable Component prefix,
        Component keyLabel,
        Component actionLabel
) {

    // ── 工厂方法：不带前缀 ────────────────────────────────────────────────

    /** 用字符串字面量创建按键标签（无前缀）。 */
    public static HintEntry of(String keyLiteral, String actionTranslKey) {
        return new HintEntry(
                null,
                Component.literal(keyLiteral),
                Component.translatable(actionTranslKey)
        );
    }

    /** 用 KeyMapping 创建（无前缀），自动读取玩家自定义键位。 */
    public static HintEntry fromMapping(KeyMapping mapping, String actionTranslKey) {
        return new HintEntry(
                null,
                mapping.getTranslatedKeyMessage(),
                Component.translatable(actionTranslKey)
        );
    }

    // ── 工厂方法：带前缀 ──────────────────────────────────────────────────

    /** 用字符串字面量创建按键标签，并附带红色前缀。 */
    public static HintEntry of(String prefixTranslKey, String keyLiteral, String actionTranslKey) {
        return new HintEntry(
                Component.translatable(prefixTranslKey),
                Component.literal(keyLiteral),
                Component.translatable(actionTranslKey)
        );
    }

    /** 用 KeyMapping 创建并附带红色前缀。 */
    public static HintEntry fromMapping(String prefixTranslKey, KeyMapping mapping, String actionTranslKey) {
        return new HintEntry(
                Component.translatable(prefixTranslKey),
                mapping.getTranslatedKeyMessage(),
                Component.translatable(actionTranslKey)
        );
    }

    /** 是否含有前缀。 */
    public boolean hasPrefix() {
        return prefix != null;
    }
}