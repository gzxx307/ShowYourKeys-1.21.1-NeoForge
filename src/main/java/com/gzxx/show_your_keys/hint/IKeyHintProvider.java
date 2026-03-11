package com.gzxx.show_your_keys.hint;

import java.util.List;
import java.util.Optional;

/**
 * 按键提示提供者接口，所有兼容层均需实现此接口。
 *
 * <h3>责任链协议</h3>
 * <p>所有已注册的 Provider 按优先级排序，依次被调用：</p>
 * <ul>
 *   <li>若当前 Provider 能处理该上下文，返回 {@link Optional} 包含的提示列表（允许空列表）</li>
 *   <li>若当前 Provider 无法判断，返回 {@link Optional#empty()}，引擎将继续调用下一个 Provider</li>
 * </ul>
 *
 * <h3>优先级约定（{@link #getPriority()} 返回值越小越先执行）</h3>
 * <pre>
 *   1  ~ 10   用户/整合包 JSON 配置文件
 *   11 ~ 30   第三方 Mod 通过 API 主动注册的 Provider
 *   31 ~ 60   本 Mod 内置的大型 Mod 硬编码 Compat（如 Create、Mekanism）
 *   61 ~ 80   基于 ItemAbility / ToolAction 的通用推断
 *   81 ~ 90   原版规则（VanillaHintProvider）
 *   91 ~ 99   反射兜底推断
 *   100       最终 Fallback（"右键 交互" / "左键 攻击"）
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
     * 此 Provider 的优先级，数字越小越先被调用。
     * 默认值 100（最低优先级，作为 Fallback）。
     */
    default int getPriority() {
        return 100;
    }
}
