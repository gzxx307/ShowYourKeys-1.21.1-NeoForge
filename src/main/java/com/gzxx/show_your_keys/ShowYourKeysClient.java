package com.gzxx.show_your_keys;

import com.gzxx.show_your_keys.api.ShowYourKeysAPI;
import com.gzxx.show_your_keys.internal.provider.FallbackHintProvider;
import com.gzxx.show_your_keys.internal.provider.ItemAbilityHintProvider;
import com.gzxx.show_your_keys.internal.provider.MovementHintProvider;
import com.gzxx.show_your_keys.internal.provider.VanillaHintProvider;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

/**
 * Show Your Keys 客户端初始化入口。
 *
 * <p>负责在 {@link FMLClientSetupEvent} 中注册所有内置 Provider。
 * 外部 Mod 同样应在 {@code FMLClientSetupEvent} 中通过
 * {@link ShowYourKeysAPI} 完成注册，以确保时序正确。</p>
 */
public class ShowYourKeysClient {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ShowYourKeysClient::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        registerBuiltinProviders();
        LOGGER.info("[ShowYourKeys] Client setup complete. {} provider(s) registered.",
                ShowYourKeysAPI.providers().getCount());
    }

    /**
     * 注册所有内置 Provider。
     *
     * <p>优先级从低到高排列（数字越小优先级越高）：</p>
     * <pre>
     *  65  ItemAbilityHintProvider  叠加  工具能力（去皮、耕地…）
     *  80  MovementHintProvider     叠加  移动状态（蹲下、疾跑…）
     *  81  VanillaHintProvider      终止  原版交互（方块/实体/物品）
     * 100  FallbackHintProvider     终止  兜底（始终有内容）
     * </pre>
     */
    private static void registerBuiltinProviders() {
        ShowYourKeysAPI.providers().register(new ItemAbilityHintProvider());
        ShowYourKeysAPI.providers().register(new MovementHintProvider());
        ShowYourKeysAPI.providers().register(new VanillaHintProvider());
        ShowYourKeysAPI.providers().register(new FallbackHintProvider());
    }
}