package com.gzxx.show_your_keys.hint.provider;

import com.gzxx.show_your_keys.hint.HintContext;
import com.gzxx.show_your_keys.hint.HintEntry;
import com.gzxx.show_your_keys.hint.HintSlot;
import com.gzxx.show_your_keys.hint.IKeyHintProvider;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 移动状态按键提示提供者，优先级为 80（叠加模式）
 * 
 * <p>该提供者负责处理与玩家运动状态相关的常驻按键提示，如蹲下、疾跑、丢出物品等。</p>
 */
public class MovementHintProvider implements IKeyHintProvider {

    // 返回优先级 80
    @Override
    public int getPriority() { return 80; }

    // 是否为叠加模式
    @Override
    public boolean isAdditive() { return true; }

    /**
     * 根据当前上下文生成移动状态相关的按键提示
     * 
     * @param ctx 当前帧上下文
     * @return 包含提示列表的 Optional
     */
    @Override
    public Optional<List<HintEntry>> getHints(HintContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        List<HintEntry> hints = new ArrayList<>();

        boolean riding    = ctx.player().isPassenger();
        boolean swimming  = ctx.player().isSwimming();
        boolean crouching = ctx.player().isCrouching();
        boolean sprinting = ctx.player().isSprinting();

        // SHIFT 槽：蹲下/起身（骑乘/游泳时跳过，由 VanillaHintProvider 处理）
        if (!riding && !swimming) {
            if (crouching) {
                hints.add(HintEntry.fromMapping(HintSlot.SHIFT, mc.options.keyShift,
                        "hint.show_your_keys.stand_up"));
            } else {
                hints.add(HintEntry.fromMapping(HintSlot.SHIFT, mc.options.keyShift,
                        "hint.show_your_keys.sneak"));
            }
        }

        // SPRINT 槽：疾跑/疾游
        if (!riding) {
            if (swimming && !sprinting) {
                hints.add(HintEntry.fromMapping(HintSlot.SPRINT, mc.options.keySprint,
                        "hint.show_your_keys.sprint_swim"));
            } else if (!swimming && !sprinting && !crouching) {
                // 陆地：蹲下时无法疾跑，不显示
                hints.add(HintEntry.fromMapping(HintSlot.SPRINT, mc.options.keySprint,
                        "hint.show_your_keys.sprint"));
            }
        }

        // DROP 槽：丢出物品
        if (!ctx.heldItem().isEmpty()) {
            hints.add(HintEntry.fromMapping(HintSlot.DROP, mc.options.keyDrop,
                    "hint.show_your_keys.drop_item"));
        }

        return hints.isEmpty() ? Optional.empty() : Optional.of(hints);
    }
}
