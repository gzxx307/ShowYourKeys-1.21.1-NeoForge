package com.gzxx.show_your_keys.internal.engine;

import com.gzxx.show_your_keys.api.hint.HintContext;
import com.gzxx.show_your_keys.api.hint.HintEntry;
import com.gzxx.show_your_keys.api.hint.IKeyHintProvider;
import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 按键提示引擎（内部实现，外部请勿直接使用）。
 *
 * <p>每帧调用所有已注册的 {@link IKeyHintProvider}，按 <b>Slot 级别优先级竞争</b>
 * 规则计算最终提示列表并缓存，供渲染器读取。</p>
 *
 * <h3>Slot 级别优先级规则</h3>
 * <p>引擎遍历全部 Provider，对每个 Slot 独立竞争：
 * 只保留该 Slot 上 <b>优先级数值最小</b>（即最高优先级）的 Provider 所提供的条目。
 * 不同 Slot 之间互不干扰，不再有全局的"终止/叠加"概念。</p>
 *
 * <pre>
 * 示例场景：准心对准中继器，未蹲下
 *
 *  Provider               priority  提供的 Slot
 *  RedstoneHintProvider      79     USE, ATTACK
 *  MovementHintProvider       80     SHIFT, SPRINT, DROP
 *  VanillaHintProvider        81     USE, ATTACK   ← 被 79 覆盖，不显示
 *  FallbackHintProvider      100     USE, ATTACK   ← 被 79 覆盖，不显示
 *
 *  最终：USE=切换挡位, ATTACK=挖掘（来自 79），SHIFT/SPRINT/DROP（来自 80）
 * </pre>
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
        providers.sort(Comparator.comparingInt(IKeyHintProvider::getPriority));
        LOGGER.debug("[ShowYourKeys] Provider registered: {} (priority={})",
                provider.getClass().getSimpleName(), provider.getPriority());
    }

    /** 获取已注册的 Provider 数量 */
    public int getProviderCount() {
        return providers.size();
    }

    // ── 每帧计算 ───────────────────────────────────────────────────────────────

    /**
     * 根据当前帧上下文，按 Slot 级别优先级规则计算并缓存结果。
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

        // slotId -> 赢得该 Slot 的最小 priority 值
        Map<String, Integer> slotWinnerPriority = new HashMap<>();
        // slotId -> 该 priority 下的所有 HintEntry
        Map<String, List<HintEntry>> slotEntries = new LinkedHashMap<>();

        for (IKeyHintProvider provider : providers) {
            try {
                Optional<List<HintEntry>> result = provider.getHints(ctx);
                if (result.isEmpty()) continue;

                int priority = provider.getPriority();

                for (HintEntry hint : result.get()) {
                    String slotId = hint.slotId();
                    int currentBest = slotWinnerPriority.getOrDefault(slotId, Integer.MAX_VALUE);

                    if (priority < currentBest) {
                        // 此 Provider 优先级更高，替换该 Slot 的所有已收集条目
                        slotWinnerPriority.put(slotId, priority);
                        List<HintEntry> list = new ArrayList<>();
                        list.add(hint);
                        slotEntries.put(slotId, list);
                    } else if (priority == currentBest) {
                        // 同一 Provider 的多条同 Slot 条目（如工具能力），追加
                        slotEntries.get(slotId).add(hint);
                    }
                    // priority > currentBest：更低优先级，忽略
                }

            } catch (Exception e) {
                LOGGER.warn("[ShowYourKeys] Provider '{}' threw an exception, skipping.",
                        provider.getClass().getSimpleName(), e);
            }
        }

        if (slotEntries.isEmpty()) {
            currentHints = Collections.emptyList();
            return;
        }

        // 展平并按 sortKey 排序（槽位 order × 1000 + 槽内 priority）
        currentHints = slotEntries.values().stream()
                .flatMap(Collection::stream)
                .sorted(Comparator.comparingInt(HintEntry::sortKey))
                .collect(Collectors.toList());
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