package com.gzxx.show_your_keys.api.hint;

/**
 * 内置按键槽位 ID 常量。
 *
 * <p>槽位决定该行提示在 HUD 上从上到下的排列位置。
 * 每个槽位 ID 对应一个整型排序值，由 {@link com.gzxx.show_your_keys.api.registry.SlotRegistry} 管理。</p>
 *
 * <h3>内置槽位显示顺序（从上到下）</h3>
 * <pre>
 *  MOVE    order=100   W/A/S/D  移动 / 操控载具
 *  JUMP    order=200   Space    跳跃 / 马跳蓄力
 *  USE     order=300   右键      交互 / 放置 / 使用物品
 *  ATTACK  order=400   左键      攻击 / 挖掘
 *  SHIFT   order=500   Shift    蹲下 / 下马 / 下潜
 *  SPRINT  order=600   Ctrl     疾跑 / 疾游
 *  DROP    order=700   Q        丢出物品
 *  SWAP    order=800   F        切换副手（预留）
 * </pre>
 *
 * <h3>添加自定义槽位</h3>
 * <pre>{@code
 * // 在 FMLClientSetupEvent 中调用
 * ShowYourKeysAPI.slots().register("mymod.charge", 350);
 * }</pre>
 */
public final class HintSlot {

    /** W / A / S / D 移动或操控载具 */
    public static final String MOVE   = "move";
    /** Space 跳跃 */
    public static final String JUMP   = "jump";
    /** 鼠标右键 交互 / 使用 / 放置 */
    public static final String USE    = "use";
    /** 鼠标左键 攻击 / 挖掘 */
    public static final String ATTACK = "attack";
    /** Shift 蹲下 / 离开载具 */
    public static final String SHIFT  = "shift";
    /** 疾跑键 疾跑 / 疾游 */
    public static final String SPRINT = "sprint";
    /** Q 丢出物品 */
    public static final String DROP   = "drop";
    /** F 切换副手（预留，暂未使用） */
    public static final String SWAP   = "swap";

    private HintSlot() {}
}