package com.gzxx.show_your_keys;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Show Your Keys 模组配置。
 *
 * <p>通过 NeoForge 的 {@link ModConfigSpec} 管理，配置文件位于
 * {@code .minecraft/config/show_your_keys-client.toml}。</p>
 *
 * <p>当前版本暂无用户可配置项。后续计划加入：</p>
 * <ul>
 *   <li>HUD 位置与缩放自定义</li>
 *   <li>面板透明度调节</li>
 *   <li>按槽位开关各类提示</li>
 * </ul>
 */

public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static final ModConfigSpec SPEC = BUILDER.build();
}