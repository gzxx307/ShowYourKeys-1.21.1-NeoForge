package com.gzxx.show_your_keys.api.registry;

import com.gzxx.show_your_keys.api.hint.HintSlot;

import java.util.HashMap;
import java.util.Map;

/**
 * 按键槽位显示顺序注册表。
 *
 * <p>通过 {@link com.gzxx.show_your_keys.api.ShowYourKeysAPI#slots()} 访问单例。</p>
 *
 * <p>内置槽位（{@link HintSlot} 中的常量）已在构造时预注册，无需重复添加。
 * 自定义槽位只需调用 {@link #register(String, int)} 一次即可。</p>
 *
 * <h3>显示顺序规则</h3>
 * <p>最终排序键 = {@code getOrder(slotId) × 1000 + slotPriority}，
 * 值越小在 HUD 上越靠上。</p>
 *
 * <h3>注册自定义槽位示例</h3>
 * <pre>{@code
 * // 在 FMLClientSetupEvent 中注册，order=350 使其显示在 USE 和 ATTACK 之间
 * ShowYourKeysAPI.slots().register("mymod.charge_slot", 350);
 * }</pre>
 */
public final class SlotRegistry {

    /** 全局单例，通过 {@link com.gzxx.show_your_keys.api.ShowYourKeysAPI#slots()} 访问 */
    public static final SlotRegistry INSTANCE = new SlotRegistry();

    private final Map<String, Integer> orderMap = new HashMap<>();

    private SlotRegistry() {
        // 注册内置槽位
        orderMap.put(HintSlot.MOVE,   100);
        orderMap.put(HintSlot.JUMP,   200);
        orderMap.put(HintSlot.USE,    300);
        orderMap.put(HintSlot.ATTACK, 400);
        orderMap.put(HintSlot.SHIFT,  500);
        orderMap.put(HintSlot.SPRINT, 600);
        orderMap.put(HintSlot.DROP,   700);
        orderMap.put(HintSlot.SWAP,   800);
    }

    /**
     * 注册自定义槽位。
     *
     * <p>若槽位 ID 已存在，新的 order 值会覆盖旧值。</p>
     *
     * @param slotId 槽位 ID，建议使用 {@code "modid.slot_name"} 格式以避免冲突
     * @param order  显示顺序，值越小越靠近屏幕顶部
     */
    public void register(String slotId, int order) {
        orderMap.put(slotId, order);
    }

    /**
     * 获取槽位的显示顺序值。
     *
     * @param slotId 槽位 ID
     * @return 对应的 order 值，未注册的槽位返回 {@code 9999}（显示在最底部）
     */
    public int getOrder(String slotId) {
        return orderMap.getOrDefault(slotId, 9999);
    }
}
