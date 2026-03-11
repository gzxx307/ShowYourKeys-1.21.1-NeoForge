package com.gzxx.show_your_keys.hint;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

/**
 * 一条按键提示，包含两部分：
 * - keyLabel：显示给玩家的按键名（如 "[LMB]"、"[Shift]"）
 * - actionLabel：对应的动作说明（如 "挖掘"、"脱离坐骑"）
 */
public record HintEntry(Component keyLabel, Component actionLabel) {

    /**
     * 使用字符串字面量创建按键标签。
     * 适用于 Shift、Space、W/A/S/D 等无法用 KeyMapping 表示的键。
     *
     * @param keyLiteral        按键名称字符串，例如 "Shift"
     * @param actionTranslKey   动作的翻译键，例如 "hint.show_your_keys.dismount"
     */
    public static HintEntry of(String keyLiteral, String actionTranslKey) {
        return new HintEntry(
                Component.literal(keyLiteral),
                Component.translatable(actionTranslKey)
        );
    }

    /**
     * 使用 Minecraft 的 KeyMapping 创建，会自动读取玩家在设置中绑定的实际键位。
     * 适用于左键、右键、潜行等可由玩家自定义的键。
     *
     * @param mapping           玩家可自定义的键位绑定
     * @param actionTranslKey   动作的翻译键
     */
    public static HintEntry fromMapping(KeyMapping mapping, String actionTranslKey) {
        return new HintEntry(
                mapping.getTranslatedKeyMessage(),
                Component.translatable(actionTranslKey)
        );
    }
}
