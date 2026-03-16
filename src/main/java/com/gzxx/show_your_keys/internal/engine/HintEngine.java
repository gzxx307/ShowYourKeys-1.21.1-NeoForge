package com.gzxx.show_your_keys.internal.engine;

import com.gzxx.show_your_keys.api.hint.HintContext;
import com.gzxx.show_your_keys.api.hint.IKeyHintProvider;
import com.gzxx.show_your_keys.api.hint.SlotContainer;
import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 按键提示引擎（内部实现，外部请勿直接使用）。
 *
 * <p>每帧调用所有已注册的 {@link IKeyHintProvider}，按 <b>Slot 级别优先级竞争</b>
 * 规则计算最终 {@link SlotContainer} 列表并缓存，供渲染器读取。</p>
 *
 * <h3>Slot 级别优先级规则</h3>
 * <p>引擎遍历全部 Provider（已按优先级预排序），对每个槽位独立竞争：</p>
 * <ul>
 *   <li>某槽位的第一个（优先级最高）返回该槽位 {@link SlotContainer} 的 Provider 获胜</li>
 *   <li>同优先级的多个 Provider 返回同一槽位时，容器会被合并（仍遵循同键去重规则）</li>
 *   <li>更低优先级 Provider 对该槽位的容器被完全忽略</li>
 * </ul>
 *
 * <h3>同键去重</h3>
 * <p>由 {@link SlotContainer#add(com.gzxx.show_your_keys.api.hint.Hint)} 在插入时自动处理，
 * 引擎无需额外干预。详见 {@link SlotContainer} 的文档。</p>
 *
 * <pre>
 * 示例场景：准心对准中继器，未蹲下
 *
 *  Provider               priority  返回的槽位
 *  ──────────────────────────────────────────────────────────────
 *  RedstoneHintProvider      79     USE, ATTACK        ← 获胜
 *  MovementHintProvider      80     SHIFT, SPRINT, DROP ← 获胜（无竞争）
 *  VanillaHintProvider       81     USE, ATTACK        ← 被 79 覆盖，忽略
 *  FallbackHintProvider     100     USE, ATTACK        ← 被 79 覆盖，忽略
 *
 *  最终：USE=切换挡位, ATTACK=挖掘（来自 79），SHIFT/SPRINT/DROP（来自 80）
 * </pre>
 */
public final class HintEngine {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final HintEngine INSTANCE = new HintEngine();

    public static HintEngine getInstance() {
        return INSTANCE;
    }

    private final List<IKeyHintProvider> providers = new ArrayList<>();

    /** 当前帧已计算的 SlotContainer 列表，按槽位 order 升序排列 */
    private List<SlotContainer> currentSlots = Collections.emptyList();

    private HintEngine() {}

    // ── Provider 管理 ──────────────────────────────────────────────────────────

    /**
     * 注册 Provider 并按优先级重排（数值越小越靠前）。
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
            currentSlots = Collections.emptyList();
            return;
        }

        // slotId → 赢得该槽位的最高 Provider 优先级值（数值越小越优先）
        Map<String, Integer> slotWinnerPriority = new HashMap<>();
        // slotId → 最终保留的 SlotContainer（可被替换，或与同优先级容器合并）
        Map<String, SlotContainer> winnerContainers = new LinkedHashMap<>();

        for (IKeyHintProvider provider : providers) {
            try {
                Optional<List<SlotContainer>> result = provider.getHints(ctx);
                if (result.isEmpty()) continue;

                int providerPriority = provider.getPriority();

                for (SlotContainer incoming : result.get()) {
                    if (incoming.isEmpty()) continue; // 忽略空容器

                    String slotId = incoming.getSlotId();
                    int currentBest = slotWinnerPriority.getOrDefault(slotId, Integer.MAX_VALUE);

                    if (providerPriority < currentBest) {
                        // 更高优先级的 Provider 胜出：完全替换该槽位的容器
                        // 复制一份以避免外部修改影响引擎状态
                        SlotContainer copy = new SlotContainer(slotId);
                        copy.merge(incoming);
                        winnerContainers.put(slotId, copy);
                        slotWinnerPriority.put(slotId, providerPriority);

                    } else if (providerPriority == currentBest) {
                        // 同优先级（同一 Provider 的多个槽位，或极少数情况下两个同优先级 Provider）：
                        // 合并到已有容器，同键去重由 SlotContainer.add() 自动处理
                        winnerContainers.get(slotId).merge(incoming);
                    }
                    // providerPriority > currentBest：更低优先级，忽略
                }

            } catch (Exception e) {
                LOGGER.warn("[ShowYourKeys] Provider '{}' threw an exception, skipping.",
                        provider.getClass().getSimpleName(), e);
            }
        }

        if (winnerContainers.isEmpty()) {
            currentSlots = Collections.emptyList();
            return;
        }

        // 按槽位 order 升序排列（order 值越小在 HUD 越靠上）
        currentSlots = winnerContainers.values().stream()
                .filter(c -> !c.isEmpty())
                .sorted(Comparator.comparingInt(SlotContainer::getSlotOrder))
                .toList();
    }

    /**
     * 获取当前帧已计算的 {@link SlotContainer} 列表（只读，已按槽位 order 排序）。
     *
     * @return 非空列表，可能为空集合
     */
    public List<SlotContainer> getCurrentSlots() {
        return currentSlots;
    }
}