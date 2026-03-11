package com.gzxx.show_your_keys.hint;

import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 按键提示责任链引擎（单例）。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>管理所有已注册的 {@link IKeyHintProvider}，保持按优先级排序</li>
 *   <li>每帧由渲染事件调用 {@link #compute(HintContext)}，
 *       依次查询 Provider，第一个返回非空结果的获胜</li>
 *   <li>对外暴露 {@link #getCurrentHints()} 供渲染层读取</li>
 * </ol>
 */
public class HintEngine {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final HintEngine INSTANCE = new HintEngine();

    public static HintEngine getInstance() {
        return INSTANCE;
    }

    private final List<IKeyHintProvider> providers = new ArrayList<>();
    private List<HintEntry> currentHints = Collections.emptyList();

    private HintEngine() {}

    // ── 注册 ──────────────────────────────────────────────────────────────

    /**
     * 注册一个 Provider，注册后立即按优先级重新排序。
     * 应在 {@code FMLClientSetupEvent} 中调用。
     */
    public void register(IKeyHintProvider provider) {
        providers.add(provider);
        providers.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
        LOGGER.debug("[ShowYourKeys] Registered provider: {} (priority={})",
                provider.getClass().getSimpleName(), provider.getPriority());
    }

    /** 返回已注册的 Provider 数量，供日志输出使用。 */
    public int getProviderCount() {
        return providers.size();
    }

    // ── 计算 ──────────────────────────────────────────────────────────────

    /**
     * 根据当前帧上下文，遍历责任链计算应显示的提示。
     * 每帧由 {@link com.gzxx.show_your_keys.render.HudRenderer} 调用。
     *
     * <p>任何 Provider 抛出的异常会被捕获并跳过，不影响其他 Provider。</p>
     *
     * @param ctx 当前帧玩家状态快照；若为 null（玩家不在游戏中），清空提示
     */
    public void compute(@Nullable HintContext ctx) {
        if (ctx == null) {
            currentHints = Collections.emptyList();
            return;
        }

        for (IKeyHintProvider provider : providers) {
            try {
                Optional<List<HintEntry>> result = provider.getHints(ctx);
                if (result.isPresent()) {
                    currentHints = result.get();
                    return;
                }
            } catch (Exception e) {
                LOGGER.warn("[ShowYourKeys] Provider '{}' threw an exception, skipping.",
                        provider.getClass().getSimpleName(), e);
            }
        }

        currentHints = Collections.emptyList();
    }

    // ── 读取 ──────────────────────────────────────────────────────────────

    /**
     * 获取上一次计算出的提示列表，供渲染层每帧读取。
     * 返回值不为 null（可能为空列表）。
     */
    public List<HintEntry> getCurrentHints() {
        return currentHints;
    }
}