package com.gzxx.show_your_keys.api.hint;

import java.util.List;
import java.util.Optional;

/**
 * 按键提示 Provider 接口。
 *
 * <p>实现此接口并通过 {@link com.gzxx.show_your_keys.api.ShowYourKeysAPI#providers()} 注册，
 * 即可向 HUD 注入自定义按键提示。</p>
 *
 * <h3>Slot 级别优先级机制</h3>
 * <p>引擎调用所有 Provider，对 <b>每个 SlotContainer 独立竞争</b>：</p>
 * <ul>
 *   <li>对于某个槽位，只有 {@link #getPriority()} 数值最小的 Provider 返回的
 *       {@link SlotContainer} 会被使用。</li>
 *   <li>不同槽位之间相互独立，一个槽位的覆盖不会影响其他槽位。</li>
 *   <li>返回 {@link Optional#empty()} 表示本 Provider 对当前上下文无提示，
 *       所有槽位均不参与竞争。</li>
 * </ul>
 *
 * <h3>SlotContainer 内部去重</h3>
 * <p>每个 {@link SlotContainer} 在 {@link SlotContainer#add(Hint)} 时自动按按键名去重：
 * 同一槽位中相同按键只保留优先级最高（{@link Hint#priority()} 数值最小）的那条提示，
 * 从根本上消除"同一按键显示多个互相矛盾动作"的问题。</p>
 *
 * <h3>优先级规划参考</h3>
 * <pre>
 *  ItemAbilityHintProvider   65   工具能力（去皮、耕地…）  仅覆盖 USE 槽
 *  RedstoneHintProvider      79   红石元件专属提示         覆盖 USE、ATTACK 槽
 *  MovementHintProvider      80   移动状态                覆盖 SHIFT、SPRINT、DROP 槽
 *  VanillaHintProvider       81   原版交互（通用）         覆盖多个槽
 *  FallbackHintProvider     100   兜底                   仅在上游无提示时生效
 * </pre>
 *
 * <h3>示例：在中继器上完全自定义 USE 和 ATTACK 槽</h3>
 * <pre>{@code
 * public class RedstoneHintProvider implements IKeyHintProvider {
 *
 *     @Override
 *     public int getPriority() { return 79; }
 *
 *     @Override
 *     public Optional<List<SlotContainer>> getHints(HintContext ctx) {
 *         if (!ctx.isLookingAtBlock()) return Optional.empty();
 *         BlockState state = ctx.getTargetBlockState();
 *         if (!(state.getBlock() instanceof RepeaterBlock)) return Optional.empty();
 *
 *         Minecraft mc = Minecraft.getInstance();
 *         return Optional.of(List.of(
 *             SlotContainer.of(HintSlot.USE,
 *                 Hint.fromMapping(mc.options.keyUse, "hint.repeater_delay")),
 *             SlotContainer.of(HintSlot.ATTACK,
 *                 Hint.fromMapping(mc.options.keyAttack, "hint.show_your_keys.mine"))
 *         ));
 *     }
 * }
 * }</pre>
 */
public interface IKeyHintProvider {

    /**
     * 根据当前帧玩家状态返回要显示的按键提示。
     *
     * <p>每个 {@link SlotContainer} 代表一个槽位及其包含的提示。
     * 返回 {@link Optional#empty()} 表示本 Provider 对当前上下文无任何提示，
     * 其他 Provider 会正常处理所有槽位。</p>
     *
     * <p><b>注意</b>：不要返回 {@code Optional.of(Collections.emptyList())}，
     * 这等同于"宣告参与竞争但没有任何槽位条目"，通常不是你想要的行为。
     * 请使用 {@code Optional.empty()} 表示放弃。</p>
     *
     * @param ctx 当前帧状态快照（只读）
     * @return 非空 Optional 表示有槽位参与竞争；空 Optional 表示本 Provider 弃权
     */
    Optional<List<SlotContainer>> getHints(HintContext ctx);

    /**
     * 此 Provider 的全局优先级。数值越小优先级越高，默认 {@code 100}。
     *
     * <p>对于某个槽位，优先级最高（数值最小）的 Provider 的 {@link SlotContainer} 会被使用，
     * 其余 Provider 对该槽位的容器将被忽略。</p>
     *
     * <p>建议范围：
     * <ul>
     *   <li>专项覆盖特定槽（工具能力、红石等）：60 ~ 79</li>
     *   <li>主要通用逻辑：80 ~ 90</li>
     *   <li>兜底：91+</li>
     * </ul>
     * </p>
     */
    default int getPriority() {
        return 100;
    }
}