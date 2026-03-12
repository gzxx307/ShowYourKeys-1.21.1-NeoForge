package com.gzxx.show_your_keys.hint;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 单条按键提示类
 */
public record HintEntry(
        // 所属的槽位ID
        String slotId,
        // 优先级
        int slotPriority,
        // 前缀文字
        @Nullable Component prefix,
        // 按键名
        Component keyLabel,
        // 动作说明
        Component actionLabel
) {

    /**
     * 计算排序键，用于 HintEngine 排序
     * 
     * @return 排序键 = 槽位顺序 × 1000 + 槽内优先级
     */
    public int sortKey() {
        return HintSlot.getOrder(slotId) * 1000 + slotPriority;
    }

    // 是否有前缀
    public boolean hasPrefix() {
        return prefix != null;
    }

    /**
     * 创建基础提示（无前缀，槽内优先级 0）
     * 
     * @param slot 槽位ID
     * @param keyLiteral 按键字面量
     * @param actionTranslKey 动作翻译键
     * @return 新的 HintEntry 实例
     */
    public static HintEntry of(String slot, String keyLiteral, String actionTranslKey) {
        return new HintEntry(slot, 0, null,
                Component.literal(keyLiteral),
                Component.translatable(actionTranslKey));
    }

    /**
     * 创建带槽内优先级的提示（无前缀）
     * 
     * @param slot 槽位ID
     * @param priority 槽内优先级
     * @param keyLiteral 按键字面量
     * @param actionTranslKey 动作翻译键
     * @return 新的 HintEntry 实例
     */
    public static HintEntry of(String slot, int priority, String keyLiteral, String actionTranslKey) {
        return new HintEntry(slot, priority, null,
                Component.literal(keyLiteral),
                Component.translatable(actionTranslKey));
    }

    /**
     * 根据 KeyMapping 创建基础提示（无前缀，槽内优先级 0）
     * 
     * @param slot 槽位ID
     * @param mapping 按键映射
     * @param actionTranslKey 动作翻译键
     * @return 新的 HintEntry 实例
     */
    public static HintEntry fromMapping(String slot, KeyMapping mapping, String actionTranslKey) {
        return new HintEntry(slot, 0, null,
                mapping.getTranslatedKeyMessage(),
                Component.translatable(actionTranslKey));
    }

    /**
     * 根据 KeyMapping 创建带槽内优先级的提示（无前缀）
     * 
     * @param slot 槽位ID
     * @param priority 槽内优先级
     * @param mapping 按键映射
     * @param actionTranslKey 动作翻译键
     * @return 新的 HintEntry 实例
     */
    public static HintEntry fromMapping(String slot, int priority, KeyMapping mapping, String actionTranslKey) {
        return new HintEntry(slot, priority, null,
                mapping.getTranslatedKeyMessage(),
                Component.translatable(actionTranslKey));
    }

    /**
     * 创建带前缀的提示（槽内优先级 0）
     * 
     * @param slot 槽位ID
     * @param prefixTranslKey 前缀翻译键
     * @param keyLiteral 按键字面量
     * @param actionTranslKey 动作翻译键
     * @return 新的 HintEntry 实例
     */
    public static HintEntry of(String slot, String prefixTranslKey, String keyLiteral, String actionTranslKey) {
        return new HintEntry(slot, 0,
                Component.translatable(prefixTranslKey),
                Component.literal(keyLiteral),
                Component.translatable(actionTranslKey));
    }

    /**
     * 创建带前缀和槽内优先级的提示
     * 
     * @param slot 槽位ID
     * @param priority 槽内优先级
     * @param prefixTranslKey 前缀翻译键
     * @param keyLiteral 按键字面量
     * @param actionTranslKey 动作翻译键
     * @return 新的 HintEntry 实例
     */
    public static HintEntry of(String slot, int priority, String prefixTranslKey, String keyLiteral, String actionTranslKey) {
        return new HintEntry(slot, priority,
                Component.translatable(prefixTranslKey),
                Component.literal(keyLiteral),
                Component.translatable(actionTranslKey));
    }

    /**
     * 根据 KeyMapping 创建带前缀的提示（槽内优先级 0）
     * 
     * @param slot 槽位ID
     * @param prefixTranslKey 前缀翻译键
     * @param mapping 按键映射
     * @param actionTranslKey 动作翻译键
     * @return 新的 HintEntry 实例
     */
    public static HintEntry fromMapping(String slot, String prefixTranslKey, KeyMapping mapping, String actionTranslKey) {
        return new HintEntry(slot, 0,
                Component.translatable(prefixTranslKey),
                mapping.getTranslatedKeyMessage(),
                Component.translatable(actionTranslKey));
    }

    /**
     * 根据 KeyMapping 创建带前缀和槽内优先级的提示
     * 
     * @param slot 槽位ID
     * @param priority 槽内优先级
     * @param prefixTranslKey 前缀翻译键
     * @param mapping 按键映射
     * @param actionTranslKey 动作翻译键
     * @return 新的 HintEntry 实例
     */
    public static HintEntry fromMapping(String slot, int priority, String prefixTranslKey, KeyMapping mapping, String actionTranslKey) {
        return new HintEntry(slot, priority,
                Component.translatable(prefixTranslKey),
                mapping.getTranslatedKeyMessage(),
                Component.translatable(actionTranslKey));
    }
}