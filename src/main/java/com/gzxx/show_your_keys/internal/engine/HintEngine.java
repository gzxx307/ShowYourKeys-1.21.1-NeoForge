package com.gzxx.show_your_keys.internal.engine;

import com.gzxx.show_your_keys.api.hint.HintContext;
import com.gzxx.show_your_keys.api.hint.HintEntry;
import com.gzxx.show_your_keys.api.hint.IKeyHintProvider;
import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 按键提示责任链引擎（内部实现，外部请勿直接使用）。
 *
 * <p>负责按优先级顺序遍历所有已注册的 {@link IKeyHintProvider}，
 * 收集提示并缓存至当前帧，供渲染器读取。</p>
 *
 * <h3>外部 Mod 须知</h3>
 * <p>请使用 {@link com.gzxx.show_your_keys.api.ShowYourKeysAPI#providers()} 注册 Provider，
 * 而非直接调用此类的方法。</p>
 */
public final class HintEngine {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 单例 */
    private static final HintEngine INSTANCE = new HintEngine();

    public static HintEngine getInstance() {
        return INSTANCE;
    }

    private final List<IKeyHintProvider> providers = new ArrayList<>();
    private List<HintEntry> currentHints = Collections.emptyList();

    private HintEngine() {}

    // ── Provider 管理 ──────────────────────────────────────────────────────────

    /**
     * 注册 Provider 并按优先级重排。
     *
     * @param provider 要注册的 Provider
     */
    public void register(IKeyHintProvider provider) {
        providers.add(provider);
        providers.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
        LOGGER.debug("[ShowYourKeys] Provider registered: {} (priority={}, additive={})",
                provider.getClass().getSimpleName(), provider.getPriority(), provider.isAdditive());
    }

    /** 获取已注册的 Provider 数量 */
    public int getProviderCount() {
        return providers.size();
    }

    // ── 每帧计算 ───────────────────────────────────────────────────────────────

    /**
     * 根据当前帧上下文，遍历责任链并缓存计算结果。
     *
     * <p>由渲染器每帧调用一次。</p>
     *
     * @param ctx 当前帧快照，为 {@code null} 时清空缓存
     */
    public void compute(@Nullable HintContext ctx) {
        if (ctx == null) {
            currentHints = Collections.emptyList();
            return;
        }

        List<HintEntry> collected = new ArrayList<>();

        for (IKeyHintProvider provider : providers) {
            try {
                Optional<List<HintEntry>> result = provider.getHints(ctx);
                if (result.isPresent()) {
                    collected.addAll(result.get());
                    // 终止模式：收集后立即停止
                    if (!provider.isAdditive()) {
                        currentHints = collected;
                        return;
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("[ShowYourKeys] Provider '{}' threw an exception, skipping.",
                        provider.getClass().getSimpleName(), e);
            }
        }

        // 所有 Provider 均为叠加模式，或均返回 empty
        currentHints = collected.isEmpty() ? Collections.emptyList() : collected;
    }

    /**
     * 获取当前帧已计算的提示列表（只读）。
     *
     * @return 提示列表，可能为空列表
     */
    public List<HintEntry> getCurrentHints() {
        return currentHints;
    }
}