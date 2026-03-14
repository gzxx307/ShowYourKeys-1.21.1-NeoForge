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
 * <p>引擎调用所有 Provider，对 <b>每个 Slot 独立竞争</b>：</p>
 * <ul>
 *   <li>对于某个 Slot，只有 {@link #getPriority()} 数值最小的 Provider 的条目会显示。</li>
 *   <li>不同 Slot 之间相互独立，一个 Slot 的覆盖不会影响其他 Slot。</li>
 *   <li>返回 {@link Optional#empty()} 表示本 Provider 对当前上下文无提示，
 *       该 Provider 的所有 Slot 均不参与竞争。</li>
 * </ul>
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
 * // priority=79，低于 VanillaHintProvider(81)，
 * // 因此只要本 Provider 返回了 USE/ATTACK 条目，Vanilla 对这两个槽的提示就会被忽略。
 * // MovementHintProvider(80) 提供 SHIFT/SPRINT/DROP，与本 Provider 不冲突。
 * public class RedstoneHintProvider implements IKeyHintProvider {
 *
 *     @Override
 *     public int getPriority() { return 79; }
 *
 *     @Override
 *     public Optional<List<HintEntry>> getHints(HintContext ctx) {
 *         if (!ctx.isLookingAtBlock()) return Optional.empty();
 *         BlockState state = ctx.getTargetBlockState();
 *         if (!(state.getBlock() instanceof RepeaterBlock)) return Optional.empty();
 *
 *         return Optional.of(List.of(
 *             HintEntry.fromMapping(HintSlot.USE,    mc.options.keyUse,    "hint.repeater_delay"),
 *             HintEntry.fromMapping(HintSlot.ATTACK, mc.options.keyAttack, "hint.show_your_keys.mine")
 *         ));
 *     }
 * }
 * }</pre>
 */
public interface IKeyHintProvider {

    /**
     * 根据当前帧玩家状态返回要显示的按键提示。
     *
     * <p>返回 {@link Optional#empty()} 表示本 Provider 对当前上下文无任何提示，
     * 其他 Provider 会正常处理所有 Slot。<br>
     * 返回非空列表后，列表中涉及的每个 Slot 会与其他 Provider 按优先级竞争。</p>
     *
     * <p><b>注意</b>：不要返回 {@code Optional.of(Collections.emptyList())}，
     * 这等同于"宣告参与竞争但没有条目"，通常不是你想要的行为。请使用
     * {@code Optional.empty()} 表示放弃。</p>
     *
     * @param ctx 当前帧状态快照（只读）
     * @return 非空 Optional 表示有条目参与 Slot 竞争；空 Optional 表示本 Provider 弃权
     */
    Optional<List<HintEntry>> getHints(HintContext ctx);

    /**
     * 此 Provider 的全局优先级。数值越小优先级越高，默认 {@code 100}。
     *
     * <p>对于某个 Slot，优先级最高（数值最小）的 Provider 的条目会被显示，
     * 其余 Provider 对该 Slot 的条目将被忽略。</p>
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