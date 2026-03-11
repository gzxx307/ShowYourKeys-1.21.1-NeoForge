package com.gzxx.show_your_keys;

import com.gzxx.show_your_keys.hint.HintEngine;
import com.gzxx.show_your_keys.hint.provider.VanillaHintProvider;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

/**
 * 客户端初始化入口。
 *
 * <p>NeoForge 1.21.1 的现代写法：不使用已废弃的
 * {@code @EventBusSubscriber(bus = Bus.MOD)}，
 * 而是通过主类构造函数传入的 {@link IEventBus} 直接用
 * {@code modEventBus.addListener()} 注册 Mod 总线事件。</p>
 *
 * <p>游戏总线事件（如 {@code RenderGuiEvent}）仍通过
 * {@code @EventBusSubscriber} 注解注册，但无需指定 {@code bus} 参数
 * ——NeoForge 会根据事件是否实现 {@code IModBusEvent} 自动判断。</p>
 */
public class ShowYourKeysClient {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 由 {@link ShowYourKeys} 构造函数在客户端调用，
     * 将所有客户端 Mod 总线事件监听器注册到 modEventBus。
     */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ShowYourKeysClient::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        HintEngine engine = HintEngine.getInstance();

        // 优先级 81：原版规则，覆盖骑乘/游泳/方块/实体/持有物品等场景
        engine.register(new VanillaHintProvider());

        // TODO 后续阶段将在此处陆续注册：
        //   优先级 61-80：ItemAbility 通用推断 Provider
        //   优先级 31-60：Create、Mekanism 等大型 Mod 的硬编码 Compat Provider
        //   优先级 11-30：第三方 Mod 通过 API 注册的 Provider
        //   优先级  1-10：用户/整合包 JSON 配置文件 Provider

        LOGGER.info("[ShowYourKeys] Client setup complete. HintEngine initialized with {} provider(s).",
                engine.getProviderCount());
    }
}