package com.gzxx.show_your_keys;

import com.gzxx.show_your_keys.hint.HintEngine;
import com.gzxx.show_your_keys.hint.provider.FallbackHintProvider;
import com.gzxx.show_your_keys.hint.provider.Native.ItemAbilityHintProvider;
import com.gzxx.show_your_keys.hint.provider.Native.MovementHintProvider;
import com.gzxx.show_your_keys.hint.provider.Native.VanillaHintProvider;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;


public class ShowYourKeysClient {

    // 获取到日志系统
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 注册客户端事件监听器
     * 
     * @param modEventBus Mod 事件总线
     */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ShowYourKeysClient::onClientSetup);
    }

    /**
     * 客户端设置事件处理，注册所有按键提示 Provider
     * 
     * @param event 客户端设置事件
     */
    private static void onClientSetup(FMLClientSetupEvent event) {
        HintEngine engine = HintEngine.getInstance();

        // 按优先级顺序获取 Provider
        engine.register(new ItemAbilityHintProvider());
        engine.register(new MovementHintProvider());
        engine.register(new VanillaHintProvider());
        engine.register(new FallbackHintProvider());

        LOGGER.info("[ShowYourKeys] {} provider(s) registered.", engine.getProviderCount());
    }
}