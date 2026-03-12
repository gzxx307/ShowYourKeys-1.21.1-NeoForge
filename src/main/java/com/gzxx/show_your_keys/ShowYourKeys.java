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
    
    // 获取到日志系统
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 构造函数，在 Mod 加载时调用
     * 
     * @param modEventBus Mod 事件总线，用于注册事件监听器
     */
    public ShowYourKeys(IEventBus modEventBus) {
        LOGGER.info("[ShowYourKeys] Initializing...");

        // 仅在客户端环境下注册客户端相关功能
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ShowYourKeysClient.register(modEventBus);
        }
    }
}