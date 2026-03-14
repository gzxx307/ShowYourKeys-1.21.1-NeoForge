package com.gzxx.show_your_keys.api.registry;

import com.gzxx.show_your_keys.api.hint.IKeyHintProvider;
import com.gzxx.show_your_keys.internal.engine.HintEngine;

/**
 * Provider 注册表。
 *
 * <p>通过 {@link com.gzxx.show_your_keys.api.ShowYourKeysAPI#providers()} 访问单例。</p>
 *
 * <p>此类是对内部 {@link HintEngine} 的薄包装，外部 Mod 无需直接操作引擎。</p>
 *
 * <h3>注册时机</h3>
 * <p>必须在 {@code FMLClientSetupEvent} 中注册，过早注册（如静态块）可能导致引擎未初始化。</p>
 *
 * <h3>注册自定义 Provider 示例</h3>
 * <pre>{@code
 * modEventBus.addListener((FMLClientSetupEvent event) -> {
 *     ShowYourKeysAPI.providers().register(new MyCustomProvider());
 * });
 * }</pre>
 */
public final class ProviderRegistry {

    /** 全局单例，通过 {@link com.gzxx.show_your_keys.api.ShowYourKeysAPI#providers()} 访问 */
    public static final ProviderRegistry INSTANCE = new ProviderRegistry();

    private ProviderRegistry() {}

    /**
     * 注册自定义 Provider。
     *
     * <p>引擎会按 {@link IKeyHintProvider#getPriority()} 自动排序，无需关心注册顺序。</p>
     *
     * @param provider 实现了 {@link IKeyHintProvider} 的提供者实例
     */
    public void register(IKeyHintProvider provider) {
        HintEngine.getInstance().register(provider);
    }

    /**
     * 获取当前已注册的 Provider 总数（含内置 Provider）。
     *
     * @return Provider 数量
     */
    public int getCount() {
        return HintEngine.getInstance().getProviderCount();
    }
}