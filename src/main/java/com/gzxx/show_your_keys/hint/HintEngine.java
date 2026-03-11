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
 * <h3>职责</h3>
 * <ol>
 *   <li>管理所有已注册的 {@link IKeyHintProvider}，保持按优先级排序</li>
 *   <li>每帧由渲染事件调用 {@link #compute(HintContext)}，
 *       依次查询 Provider，支持终止模式与叠加模式</li>
 *   <li>对外暴露 {@link #getCurrentHints()} 供渲染层读取</li>
 * </ol>
 *
 * <h3>遍历逻辑</h3>
 * <pre>
 *  for each provider (按优先级从小到大):
 *    result = provider.getHints(ctx)
 *    if result 为空 (Optional.empty()):
 *        → 跳过，继续下一个（无论叠加/终止模式）
 *    if result 非空:
 *        → 将 result 中的 HintEntry 收集进 collected 列表
 *        if provider.isAdditive() == false（终止模式）:
 *            → 停止遍历，返回 collected
 *        if provider.isAdditive() == true（叠加模式）:
 *            → 继续遍历下一个 Provider
 * </pre>
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
        LOGGER.debug("[ShowYourKeys] Registered provider: {} (priority={}, additive={})",
                provider.getClass().getSimpleName(), provider.getPriority(), provider.isAdditive());
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
     * <p>遍历规则：</p>
     * <ul>
     *   <li>Provider 返回 {@code Optional.empty()} → 跳过，继续遍历</li>
     *   <li>Provider 返回非空且为<b>终止模式</b>（{@code isAdditive()==false}）→
     *       将其结果追加至已收集列表后停止</li>
     *   <li>Provider 返回非空且为<b>叠加模式</b>（{@code isAdditive()==true}）→
     *       将其结果追加至已收集列表后<b>继续</b>遍历</li>
     * </ul>
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

        List<HintEntry> collected = new ArrayList<>();

        for (IKeyHintProvider provider : providers) {
            try {
                Optional<List<HintEntry>> result = provider.getHints(ctx);
                if (result.isPresent()) {
                    collected.addAll(result.get());
                    if (!provider.isAdditive()) {
                        // 终止模式：收集结果后停止责任链
                        currentHints = collected;
                        return;
                    }
                    // 叠加模式：收集结果后继续遍历
                }
            } catch (Exception e) {
                LOGGER.warn("[ShowYourKeys] Provider '{}' threw an exception, skipping.",
                        provider.getClass().getSimpleName(), e);
            }
        }

        // 所有 Provider 都是叠加模式 或 均返回 empty，取收集到的全部结果
        currentHints = collected.isEmpty() ? Collections.emptyList() : collected;
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