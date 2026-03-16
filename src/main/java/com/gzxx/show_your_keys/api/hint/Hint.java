package com.gzxx.show_your_keys.api.hint;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 单条按键提示数据，携带优先级、前缀、按键名和动作说明四个字段。
 *
 * <p>{@code Hint} 本身不包含槽位信息——槽位由外层的 {@link SlotContainer} 决定。</p>
 *
 * <h3>优先级规则</h3>
 * <p>在同一 {@link SlotContainer} 内，{@code priority} 数值越小，提示越靠上显示。<br>
 * 当两条提示的 {@link #keyLabel} 文本相同时，
 * {@link SlotContainer#add(Hint)} 只保留 priority 数值更小（更高优先级）的那条，
 * 以防止同一按键显示多个互相矛盾的动作说明。</p>
 *
 * <h3>工厂方法命名规律</h3>
 * <pre>
 *  of(key, action)                        字面量按键，无前缀，priority = 0
 *  of(priority, key, action)              指定优先级
 *  of(prefix, key, action)                带前缀，priority = 0
 *  of(priority, prefix, key, action)      全参数
 *  fromMapping(mapping, action)           KeyMapping 按键（随玩家改键更新），priority = 0
 *  fromMapping(priority, mapping, action)
 *  fromMapping(prefix, mapping, action)
 *  fromMapping(priority, prefix, mapping, action)
 * </pre>
 *
 * <h3>of 与 fromMapping 的选择</h3>
 * <ul>
 *   <li>固定按键（如 {@code "Space"}、{@code "W / A / S / D"}）→ 使用 {@code of}</li>
 *   <li>可由玩家改键的按键（攻击键、使用键等）→ 使用 {@code fromMapping}</li>
 * </ul>
 *
 * @param priority    槽内显示优先级，数值越小越靠上；相同按键时决定保留哪条
 * @param prefix      警告前缀（红色文字），可为 {@code null}
 * @param keyLabel    按键显示名
 * @param actionLabel 动作说明文字
 */
public record Hint(
        int priority,
        @Nullable Component prefix,
        Component keyLabel,
        Component actionLabel
) {

    /** 默认优先级（未显式指定时使用） */
    public static final int DEFAULT_PRIORITY = 0;

    /** 是否携带前缀 */
    public boolean hasPrefix() {
        return prefix != null;
    }

    // ── 字面量按键名工厂方法 ──────────────────────────────────────────────────

    /**
     * 创建基础提示（priority = 0，无前缀）。
     *
     * @param keyLiteral      按键名字面量，例如 {@code "Space"}、{@code "W / A / S / D"}
     * @param actionTranslKey 动作翻译键
     */
    public static Hint of(String keyLiteral, String actionTranslKey) {
        return new Hint(DEFAULT_PRIORITY, null,
                Component.literal(keyLiteral),
                Component.translatable(actionTranslKey));
    }

    /**
     * 创建带优先级的提示（无前缀）。
     *
     * @param priority        槽内优先级
     * @param keyLiteral      按键名字面量
     * @param actionTranslKey 动作翻译键
     */
    public static Hint of(int priority, String keyLiteral, String actionTranslKey) {
        return new Hint(priority, null,
                Component.literal(keyLiteral),
                Component.translatable(actionTranslKey));
    }

    /**
     * 创建带前缀的提示（priority = 0）。
     *
     * @param prefixTranslKey 前缀翻译键（红色警告文字）
     * @param keyLiteral      按键名字面量
     * @param actionTranslKey 动作翻译键
     */
    public static Hint of(String prefixTranslKey, String keyLiteral, String actionTranslKey) {
        return new Hint(DEFAULT_PRIORITY,
                Component.translatable(prefixTranslKey),
                Component.literal(keyLiteral),
                Component.translatable(actionTranslKey));
    }

    /**
     * 创建带优先级和前缀的提示（全参数版）。
     *
     * @param priority        槽内优先级
     * @param prefixTranslKey 前缀翻译键
     * @param keyLiteral      按键名字面量
     * @param actionTranslKey 动作翻译键
     */
    public static Hint of(int priority, String prefixTranslKey, String keyLiteral, String actionTranslKey) {
        return new Hint(priority,
                Component.translatable(prefixTranslKey),
                Component.literal(keyLiteral),
                Component.translatable(actionTranslKey));
    }

    // ── KeyMapping 工厂方法 ───────────────────────────────────────────────────

    /**
     * 根据 {@link KeyMapping} 创建提示（priority = 0，无前缀）。
     * <p>按键名会随玩家改键设置自动更新。</p>
     *
     * @param mapping         按键映射
     * @param actionTranslKey 动作翻译键
     */
    public static Hint fromMapping(KeyMapping mapping, String actionTranslKey) {
        return new Hint(DEFAULT_PRIORITY, null,
                mapping.getTranslatedKeyMessage(),
                Component.translatable(actionTranslKey));
    }

    /**
     * 根据 {@link KeyMapping} 创建带优先级的提示（无前缀）。
     *
     * @param priority        槽内优先级
     * @param mapping         按键映射
     * @param actionTranslKey 动作翻译键
     */
    public static Hint fromMapping(int priority, KeyMapping mapping, String actionTranslKey) {
        return new Hint(priority, null,
                mapping.getTranslatedKeyMessage(),
                Component.translatable(actionTranslKey));
    }

    /**
     * 根据 {@link KeyMapping} 创建带前缀的提示（priority = 0）。
     *
     * @param prefixTranslKey 前缀翻译键
     * @param mapping         按键映射
     * @param actionTranslKey 动作翻译键
     */
    public static Hint fromMapping(String prefixTranslKey, KeyMapping mapping, String actionTranslKey) {
        return new Hint(DEFAULT_PRIORITY,
                Component.translatable(prefixTranslKey),
                mapping.getTranslatedKeyMessage(),
                Component.translatable(actionTranslKey));
    }

    /**
     * 根据 {@link KeyMapping} 创建带优先级和前缀的提示（全参数版）。
     *
     * @param priority        槽内优先级
     * @param prefixTranslKey 前缀翻译键
     * @param mapping         按键映射
     * @param actionTranslKey 动作翻译键
     */
    public static Hint fromMapping(int priority, String prefixTranslKey, KeyMapping mapping, String actionTranslKey) {
        return new Hint(priority,
                Component.translatable(prefixTranslKey),
                mapping.getTranslatedKeyMessage(),
                Component.translatable(actionTranslKey));
    }
}