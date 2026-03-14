package com.gzxx.show_your_keys;

import com.gzxx.show_your_keys.api.ShowYourKeysAPI;
import com.gzxx.show_your_keys.internal.provider.*;
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
     * <p>引擎采用 <b>Slot 级别优先级竞争</b>：对每个 Slot，
     * 优先级数值最小的 Provider 的条目获胜，其余被忽略。</p>
     *
     * <pre>
     *  priority  Provider                 覆盖的 Slot
     *  ────────────────────────────────────────────────────────
     *    65      ItemAbilityHintProvider  USE（工具能力：去皮、耕地…）
     *    79      RedstoneHintProvider     USE, ATTACK（中继器/比较器专属）
     *    80      MovementHintProvider     SHIFT, SPRINT, DROP（移动状态）
     *    81      VanillaHintProvider      USE, ATTACK, MOVE, JUMP…（原版通用）
     *   100      FallbackHintProvider     USE, ATTACK（兜底，无人竞争时生效）
     * </pre>
     */
    private static void registerBuiltinProviders() {
        ShowYourKeysAPI.providers().register(new ItemAbilityHintProvider());
        ShowYourKeysAPI.providers().register(new MovementHintProvider());
        ShowYourKeysAPI.providers().register(new VanillaHintProvider());
        ShowYourKeysAPI.providers().register(new FallbackHintProvider());
        ShowYourKeysAPI.providers().register(new RedstoneHintProvider());
    }
}