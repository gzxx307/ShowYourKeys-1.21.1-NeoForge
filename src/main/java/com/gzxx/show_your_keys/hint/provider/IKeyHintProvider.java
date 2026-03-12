package com.gzxx.show_your_keys.hint.provider;

import com.gzxx.show_your_keys.hint.HintContext;
import com.gzxx.show_your_keys.hint.HintEntry;

import java.util.List;
import java.util.Optional;

/**
 * 按键提示 Provider 接口，所有 Provider 均需实现此接口。
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