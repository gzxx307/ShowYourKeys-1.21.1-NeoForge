package com.gzxx.show_your_keys;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(ShowYourKeys.MOD_ID)
public class ShowYourKeys {

    public static final String MOD_ID = "show_your_keys";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ShowYourKeys(IEventBus modEventBus) {
        LOGGER.info("[ShowYourKeys] Initializing...");

        // 仅在客户端物理端注册客户端事件
        // FMLEnvironment.dist 在物理服务端时为 DEDICATED_SERVER，绝对不会加载客户端类
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ShowYourKeysClient.register(modEventBus);
        }
    }
}