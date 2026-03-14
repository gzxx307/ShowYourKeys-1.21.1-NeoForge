package com.gzxx.show_your_keys.api.registry;

import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link ItemAbility}（工具行为）→ 动作翻译键 映射注册表。
 *
 * <p>通过 {@link com.gzxx.show_your_keys.api.ShowYourKeysAPI#abilities()} 访问单例。</p>
 *
 * <p>内部 {@code ItemAbilityHintProvider} 会遍历此注册表，
 * 对准心所指方块检测工具能否执行对应行为（如去皮、耕地），并在 HUD 上显示提示。</p>
 *
 * <h3>工作原理</h3>
 * <p>每帧，Provider 会对注册表中的每个 {@link ItemAbility} 调用
 * {@code Block.getToolModifiedState()}，若结果方块与当前不同，则说明该能力可用，
 * 对应翻译键的提示会显示在 USE 槽。</p>
 *
 * <h3>注册顺序即显示优先级</h3>
 * <p>注册表内部使用 {@link LinkedHashMap}，先注册的能力在同槽内排序更靠前。
 * 建议将最常用的能力先注册。</p>
 *
 * <h3>注册自定义 ItemAbility 示例</h3>
 * <pre>{@code
 * // 假设你的 Mod 定义了一种"磨光"能力
 * ShowYourKeysAPI.abilities().register(
 *         MyItemAbilities.POLISH,
 *         "mymod.hint.polish"          // 对应 lang 文件中的翻译键
 * );
 * }</pre>
 *
 * <h3>内置映射（已预注册，无需重复添加）</h3>
 * <pre>
 *  AXE_STRIP     →  hint.show_your_keys.strip    斧头去皮
 *  AXE_SCRAPE    →  hint.show_your_keys.scrape   斧头刮氧化层
 *  AXE_WAX_OFF   →  hint.show_your_keys.wax_off  斧头去蜡
 *  SHOVEL_FLATTEN→  hint.show_your_keys.flatten  铲子铲平
 *  HOE_TILL      →  hint.show_your_keys.till     锄头耕地
 * </pre>
 */
public final class AbilityHintRegistry {

    /** 全局单例，通过 {@link com.gzxx.show_your_keys.api.ShowYourKeysAPI#abilities()} 访问 */
    public static final AbilityHintRegistry INSTANCE = new AbilityHintRegistry();

    /** LinkedHashMap 保证插入顺序即优先级顺序 */
    private final Map<ItemAbility, String> entries = new LinkedHashMap<>();

    private AbilityHintRegistry() {
        // 注册内置原版工具能力
        entries.put(ItemAbilities.AXE_STRIP,      "hint.show_your_keys.strip");
        entries.put(ItemAbilities.AXE_SCRAPE,     "hint.show_your_keys.scrape");
        entries.put(ItemAbilities.AXE_WAX_OFF,    "hint.show_your_keys.wax_off");
        entries.put(ItemAbilities.SHOVEL_FLATTEN, "hint.show_your_keys.flatten");
        entries.put(ItemAbilities.HOE_TILL,       "hint.show_your_keys.till");
    }

    /**
     * 注册 ItemAbility 到提示翻译键的映射。
     *
     * <p>若 {@code ability} 已注册，新的翻译键会覆盖旧值（顺序保持不变）。</p>
     *
     * @param ability        物品能力（来自 {@link ItemAbilities} 或自定义）
     * @param translationKey 对应动作的翻译键，需在 lang 文件中配置
     */
    public void register(ItemAbility ability, String translationKey) {
        entries.put(ability, translationKey);
    }

    /**
     * 获取只读的能力映射视图，按注册顺序排列。
     *
     * <p>此方法仅供 {@code ItemAbilityHintProvider} 内部使用。</p>
     *
     * @return 不可修改的 {@code ItemAbility → 翻译键} 映射
     */
    public Map<ItemAbility, String> getEntries() {
        return Collections.unmodifiableMap(entries);
    }
}
