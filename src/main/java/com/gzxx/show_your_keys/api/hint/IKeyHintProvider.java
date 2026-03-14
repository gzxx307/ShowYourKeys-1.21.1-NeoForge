package com.gzxx.show_your_keys.api.hint;

import java.util.List;
import java.util.Optional;

/**
 * 按键提示 Provider 接口。
 *
 * <p>实现此接口并通过 {@link com.gzxx.show_your_keys.api.ShowYourKeysAPI#providers()} 注册，
 * 即可向 HUD 注入自定义按键提示。</p>
 *
 * <h3>责任链机制</h3>
 * <p>所有 Provider 按 {@link #getPriority()} 从小到大排序后依次调用。</p>
 * <ul>
 *   <li><b>终止模式（默认）</b>：Provider 返回非空结果后，责任链立即停止。
 *       优先级更低的 Provider 不会被调用。</li>
 *   <li><b>叠加模式</b>（{@link #isAdditive()} 返回 {@code true}）：
 *       结果被收集后，责任链继续向下传递。
 *       适用于需要"叠加"显示的提示（如移动状态、工具能力）。</li>
 * </ul>
 *
 * <h3>优先级参考</h3>
 * <pre>
 *  ItemAbilityHintProvider   65  叠加模式  工具能力（去皮、耕地等）
 *  MovementHintProvider      80  叠加模式  移动状态（蹲下、疾跑等）
 *  VanillaHintProvider       81  终止模式  原版交互（方块、实体、物品）
 *  FallbackHintProvider     100  终止模式  兜底（始终有内容显示）
 * </pre>
 *
 * <h3>最简实现示例</h3>
 * <pre>{@code
 * public class MyProvider implements IKeyHintProvider {
 *
 *     @Override
 *     public int getPriority() { return 75; }
 *
 *     @Override
 *     public Optional<List<HintEntry>> getHints(HintContext ctx) {
 *         if (!ctx.isLookingAtBlock()) return Optional.empty();
 *
 *         List<HintEntry> hints = new ArrayList<>();
 *         hints.add(HintEntry.fromMapping(
 *                 HintSlot.USE,
 *                 Minecraft.getInstance().options.keyUse,
 *                 "mymod.hint.my_action"
 *         ));
 *         return Optional.of(hints);
 *     }
 * }
 * }</pre>
 */
public interface IKeyHintProvider {

    /**
     * 根据当前帧玩家状态返回要显示的按键提示。
     *
     * @param ctx 当前帧状态快照（只读）
     * @return 非空 Optional 表示有结果；空 Optional 表示本 Provider 无适用提示，
     *         责任链继续向下（无论模式）
     */
    Optional<List<HintEntry>> getHints(HintContext ctx);

    /**
     * 此 Provider 的全局优先级。数值越小越先被调用，默认 {@code 100}。
     *
     * <p>建议范围：
     * <ul>
     *   <li>叠加型（状态修饰）：10 ~ 79</li>
     *   <li>主要逻辑：80 ~ 90</li>
     *   <li>兜底：91+</li>
     * </ul>
     * </p>
     */
    default int getPriority() {
        return 100;
    }

    /**
     * 是否为叠加模式。
     *
     * <ul>
     *   <li>{@code false}（默认）：终止模式，返回结果后责任链停止。</li>
     *   <li>{@code true}：叠加模式，返回结果后责任链继续。</li>
     * </ul>
     */
    default boolean isAdditive() {
        return false;
    }
}