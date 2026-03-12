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
 * <h3>优先级约定</h3>
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
     * 根据当前帧的玩家状态，返回应显示的按键提示列表
     * 
     * @param ctx 当前帧玩家状态快照
     * @return 包含提示列表的 Optional 返回空则表示存在错误
     */
    Optional<List<HintEntry>> getHints(HintContext ctx);

    // 此 Provider 的优先级，数字越小越先被调用。默认 100。
    default int getPriority() {
        return 100;
    }

    /**
     * 是否为叠加模式
     * 
     * <p>终止模式（默认）：提供者返回非空结果后，责任链立即停止。</p>
     * <p>叠加模式：提供者返回非空结果后，结果被收集，责任链继续。</p>
     * 
     * @return 如果是叠加模式返回 true，否则返回 false
     */
    default boolean isAdditive() {
        return false;
    }
}