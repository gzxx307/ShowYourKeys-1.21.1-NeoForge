package com.gzxx.show_your_keys.hint;

import com.gzxx.show_your_keys.hint.provider.IKeyHintProvider;
import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 按键提示引擎，负责管理所有 Provider 并给出所有的按键提示
 */
public class HintEngine {

    // 获取日志系统
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 单例实例
    private static final HintEngine INSTANCE = new HintEngine();

    // 获取单例实例
    public static HintEngine getInstance() {
        return INSTANCE;
    }

    // 已注册的 Provider
    private final List<IKeyHintProvider> providers = new ArrayList<>();
    
    //当前帧的对应提示
    private List<HintEntry> currentHints = Collections.emptyList();

    private HintEngine() {}

    // 注册 Provider
    public void register(IKeyHintProvider provider) {
        providers.add(provider);
        // 按优先级排序
        providers.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
        LOGGER.debug("[ShowYourKeys] Registered provider: {} (priority={}, additive={})",
                provider.getClass().getSimpleName(), provider.getPriority(), provider.isAdditive());
    }

    // 获取已注册的 Provider 数量
    public int getProviderCount() {
        return providers.size();
    }

    // 生成所有提示
    public void compute(@Nullable HintContext ctx) {
        if (ctx == null) {
            currentHints = Collections.emptyList();
            return;
        }

        List<HintEntry> collected = new ArrayList<>();

        // 按优先级顺序遍历 Provider
        for (IKeyHintProvider provider : providers) {
            try {
                Optional<List<HintEntry>> result = provider.getHints(ctx);
                if (result.isPresent()) {
                    collected.addAll(result.get());
                    // 如果是终止模式，收集结果后立即停止遍历
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

        // 所有 Provider 都是叠加模式 或 均返回 empty，取收集到的全部结果
        currentHints = collected.isEmpty() ? Collections.emptyList() : collected;
    }

    // 获取当前帧计算出的提示列表
    public List<HintEntry> getCurrentHints() {
        return currentHints;
    }
}