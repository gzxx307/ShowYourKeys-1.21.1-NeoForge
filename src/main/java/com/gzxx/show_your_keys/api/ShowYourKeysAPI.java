package com.gzxx.show_your_keys.api;

import com.gzxx.show_your_keys.api.hint.IKeyHintProvider;
import com.gzxx.show_your_keys.api.registry.AbilityHintRegistry;
import com.gzxx.show_your_keys.api.registry.ProviderRegistry;
import com.gzxx.show_your_keys.api.registry.SlotRegistry;

/**
 * Show Your Keys 公开 API 统一入口。
 *
 * <p>外部 Mod 通过此类完成所有注册操作，无需直接访问内部实现。
 * 三个静态方法分别对应三个注册表：</p>
 *
 * <ul>
 *   <li>{@link #providers()} — 注册自定义 Provider（核心扩展方式）</li>
 *   <li>{@link #slots()} — 注册自定义显示槽位</li>
 *   <li>{@link #abilities()} — 注册 ItemAbility → 提示文字 映射</li>
 * </ul>
 *
 * <h3>完整使用示例</h3>
 * <pre>{@code
 * // 在你的 Mod 主类或客户端初始化类中：
 * modEventBus.addListener((FMLClientSetupEvent event) -> {
 *
 *     // 1. 注册自定义 Provider（最常用）
 *     ShowYourKeysAPI.providers().register(new MyMachineHintProvider());
 *
 *     // 2. 注册自定义槽位（可选，适合与内置槽位同行显示）
 *     ShowYourKeysAPI.slots().register("mymod.fuel_slot", 350);
 *
 *     // 3. 注册自定义 ItemAbility 提示（可选，适合工具扩展 Mod）
 *     ShowYourKeysAPI.abilities().register(
 *             MyItemAbilities.TEMPER,
 *             "mymod.hint.temper"
 *     );
 * });
 * }</pre>
 *
 * <h3>注册时机</h3>
 * <p>所有注册必须在 {@code FMLClientSetupEvent} 中完成，
 * 不要在静态初始化块或过早的生命周期事件中调用。</p>
 *
 * @see IKeyHintProvider
 * @see com.gzxx.show_your_keys.api.hint.HintSlot
 * @see com.gzxx.show_your_keys.api.hint.HintEntry
 * @see com.gzxx.show_your_keys.api.hint.HintContext
 */
public final class ShowYourKeysAPI {

    private ShowYourKeysAPI() {}

    /**
     * 获取 Provider 注册表。
     *
     * <p>通过此注册表向 HUD 注入自定义按键提示逻辑。</p>
     *
     * @return {@link ProviderRegistry} 单例
     */
    public static ProviderRegistry providers() {
        return ProviderRegistry.INSTANCE;
    }

    /**
     * 获取按键槽位注册表。
     *
     * <p>通过此注册表添加自定义显示槽位，或调整现有槽位的显示顺序。</p>
     *
     * @return {@link SlotRegistry} 单例
     */
    public static SlotRegistry slots() {
        return SlotRegistry.INSTANCE;
    }

    /**
     * 获取 ItemAbility 提示映射注册表。
     *
     * <p>通过此注册表让自定义工具能力（如铸造、磨光等）自动显示操作提示，
     * 无需编写专属 Provider。</p>
     *
     * @return {@link AbilityHintRegistry} 单例
     */
    public static AbilityHintRegistry abilities() {
        return AbilityHintRegistry.INSTANCE;
    }
}