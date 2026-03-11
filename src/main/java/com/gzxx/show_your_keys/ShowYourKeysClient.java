package com.gzxx.show_your_keys;

import com.gzxx.show_your_keys.hint.HintEngine;
import com.gzxx.show_your_keys.hint.provider.FallbackHintProvider;
import com.gzxx.show_your_keys.hint.provider.ItemAbilityHintProvider;
import com.gzxx.show_your_keys.hint.provider.MovementHintProvider;
import com.gzxx.show_your_keys.hint.provider.VanillaHintProvider;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

/**
 * 客户端初始化入口。
 *
 * <h3>Provider 注册顺序（按优先级）</h3>
 * <pre>
 *   65  ItemAbilityHintProvider  [叠加] USE 槽：工具变换（去皮/耕地/铲平等）
 *   80  MovementHintProvider     [叠加] SHIFT/SPRINT/DROP 槽：蹲下/疾跑/丢出物品
 *   81  VanillaHintProvider      [终止] USE/ATTACK/MOVE/JUMP/SHIFT 槽：全场景
 *  100  FallbackHintProvider     [终止] 兜底（通常不触发）
 * </pre>
 *
 * <h3>手持斧头面对原木时的完整执行流程</h3>
 * <pre>
 *  ItemAbilityHintProvider (65, 叠加)  → 追加 [USE: 去皮]            → 链继续
 *  MovementHintProvider    (80, 叠加)  → 追加 [SHIFT: 蹲下][SPRINT: 疾跑][DROP: 丢出]
 *                                                                       → 链继续
 *  VanillaHintProvider     (81, 终止)  → 追加 [ATTACK: 挖掘]          → 链停止
 *
 *  HintEngine 按 sortKey 排序后渲染：
 *    [右键]  去皮       (USE,    priority 0)
 *    [左键]  挖掘       (ATTACK, priority 0)
 *    [Shift] 蹲下       (SHIFT,  priority 0)
 *    [Sprint]疾跑       (SPRINT, priority 0)
 *    [Q]     丢出物品   (DROP,   priority 0)
 * </pre>
 */
public class ShowYourKeysClient {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ShowYourKeysClient::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        HintEngine engine = HintEngine.getInstance();

        engine.register(new ItemAbilityHintProvider());
        engine.register(new MovementHintProvider());
        engine.register(new VanillaHintProvider());
        engine.register(new FallbackHintProvider());

        LOGGER.info("[ShowYourKeys] {} provider(s) registered.", engine.getProviderCount());
    }
}