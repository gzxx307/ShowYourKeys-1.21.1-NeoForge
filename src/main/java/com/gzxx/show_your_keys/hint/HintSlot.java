package com.gzxx.show_your_keys.hint;

import java.util.HashMap;
import java.util.Map;

/**
 * 按键槽位注册表：将每个逻辑按键映射到一个显示顺序值。
 *
 * <h3>设计目标</h3>
 * <p>HUD 上每一行提示都属于某个槽位。槽位决定该行在屏幕上从上到下的位置，
 * 保证无论哪些 Provider 触发、以何种顺序触发，视觉布局始终一致。</p>
 *
 * <h3>工作原理</h3>
 * <pre>
 *  每条 {@link HintEntry} 携带一个 slotId（字符串，如 "use"、"attack"）
 *  和一个 slotPriority（槽内顺序，int）。
 *
 *  HintEngine 收集完所有 Provider 的结果后，按以下 sortKey 排序再渲染：
 *    sortKey = HintSlot.getOrder(slotId) × 1000 + slotPriority
 *
 *  同一槽位的多条提示（例如右键既可「去皮」又可「交互」）会紧挨着显示，
 *  slotPriority 越小越靠前。
 * </pre>
 *
 * <h3>内置槽位（显示顺序从上到下）</h3>
 * <pre>
 *  MOVE    100  W/A/S/D  移动/操控
 *  JUMP    200  Space    跳跃/蓄力
 *  USE     300  右键      交互/放置/使用物品
 *  ATTACK  400  左键      攻击/挖掘
 *  SHIFT   500  Shift    蹲下/下马/下潜
 *  SPRINT  600  Sprint   疾跑/疾游
 *  DROP    700  Q        丢出物品
 *  SWAP    800  F        切换副手（预留）
 * </pre>
 *
 * <h3>第三方 Mod 扩展</h3>
 * <p>调用 {@link #register(String, int)} 可注册自定义槽位：</p>
 * <pre>
 *  HintSlot.register("mymod.wrench", 350);   // 显示在 USE 与 ATTACK 之间
 * </pre>
 */
public final class HintSlot {

    // WASD
    public static final String MOVE = "move";
    // 空格
    public static final String JUMP = "jump";
    // 右键
    public static final String USE = "use";
    // 键
    public static final String ATTACK = "attack";
    // Shift
    public static final String SHIFT = "shift";
    // 疾跑（双击W）
    public static final String SPRINT = "sprint";
    // Q
    public static final String DROP = "drop";
    // F
    public static final String SWAP = "swap";

    // 注册时附带优先级，为其进行排序
    private static final Map<String, Integer> ORDER = new HashMap<>();
    // 为每个行为添加优先级
    static {
        ORDER.put(MOVE,100);
        ORDER.put(JUMP,200);
        ORDER.put(USE,300);
        ORDER.put(ATTACK,400);
        ORDER.put(SHIFT,500);
        ORDER.put(SPRINT,600);
        ORDER.put(DROP,700);
        ORDER.put(SWAP,800);
    }

    //获取槽位的基础显示顺序值。未注册的槽位默认为最低级 9999
    public static int getOrder(String slotId) {
        return ORDER.getOrDefault(slotId, 9999);
    }

    /**
     * 注册自定义槽位
     * 
     * @param slotId 槽位ID（建议加 modid 前缀）
     * @param order 显示顺序（值越小越靠上）
     */
    public static void register(String slotId, int order) {
        ORDER.put(slotId, order);
    }

    private HintSlot() {}
}
