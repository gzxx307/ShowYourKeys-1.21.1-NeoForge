package com.gzxx.show_your_keys.hint;

import java.util.List;
import java.util.Optional;

/**
 * 按键提示提供者接口，所有兼容层均需实现此接口。
 *
 * <h3>责任链协议（含叠加模式）</h3>
 * <p>所有已注册的 Provider 按优先级排序，依次被调用：</p>
 * <ul>
 *   <li>若当前 Provider 能处理该上下文，返回 {@link Optional} 包含的提示列表（允许空列表）</li>
 *   <li>若当前 Provider 无法判断，返回 {@link Optional#empty()}，引擎继续调用下一个</li>
 * </ul>
 *
 * <h3>终止模式 vs 叠加模式（{@link #isAdditive()}）</h3>
 * <pre>
 *  终止模式（默认）：Provider 返回非空结果后，责任链立即停止。
 *  叠加模式：        Provider 返回非空结果后，结果被收集，责任链继续向后传递。
 * </pre>
 *
 * <h3>Slot 系统与提示排序</h3>
 * <p>每条 {@link HintEntry} 携带 {@code slotId} 和 {@code slotPriority}。
 * {@link HintEngine} 收集完所有 Provider 的结果后，按 {@link HintEntry#sortKey()}
 * 统一排序再渲染。提示的显示顺序与 Provider 执行顺序无关，始终由槽位决定。</p>
 *
 * <h3>优先级约定（{@link #getPriority()} 越小越先执行）</h3>
 * <pre>
 *   1  ~ 10   用户/整合包 JSON 配置文件
 *   11 ~ 30   第三方 Mod 通过 API 主动注册的 Provider
 *   31 ~ 60   本 Mod 内置的大型 Mod 硬编码 Compat（如 Create、Mekanism）
 *   61 ~ 80   通用推断层（ItemAbility、移动按键等，叠加模式）
 *   81 ~ 90   原版规则（VanillaHintProvider，终止模式）
 *   91 ~ 99   反射兜底推断
 *   100       最终 Fallback（终止模式）
 * </pre>
 */
public interface IKeyHintProvider {

    /**
     * 根据当前帧的玩家状态，返回应显示的按键提示列表。
     *
     * @param ctx 当前帧玩家状态快照，永远不为 null
     * @return 包含提示列表的 Optional；或 {@link Optional#empty()} 表示本 Provider 无法处理
     */
    Optional<List<HintEntry>> getHints(HintContext ctx);

    /**
     * 此 Provider 的优先级，数字越小越先被调用。默认 100。
     */
    default int getPriority() {
        return 100;
    }

    /**
     * 是否为叠加模式。
     * <ul>
     *   <li>{@code false}（默认，终止模式）：返回非空结果后责任链停止。</li>
     *   <li>{@code true}（叠加模式）：返回非空结果后，结果被收集，责任链继续。</li>
     * </ul>
     * <p>叠加模式 Provider 应在无贡献时返回 {@link Optional#empty()}，
     * 而非空列表，避免提前终止链。</p>
     */
    default boolean isAdditive() {
        return false;
    }
}