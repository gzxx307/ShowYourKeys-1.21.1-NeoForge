package com.gzxx.show_your_keys.internal.provider;

import com.gzxx.show_your_keys.api.hint.HintContext;
import com.gzxx.show_your_keys.api.hint.HintEntry;
import com.gzxx.show_your_keys.api.hint.HintSlot;
import com.gzxx.show_your_keys.api.hint.IKeyHintProvider;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 移动状态按键提示 Provider（内部实现，优先级 80，叠加模式）。
 *
 * <p>负责处理与玩家运动状态相关的常驻提示：蹲下/起身、疾跑/疾游、丢物品。
 * 使用叠加模式，其结果会与主 Provider（{@link VanillaHintProvider}）的结果合并显示。</p>
 */
public class MovementHintProvider implements IKeyHintProvider {

    @Override
    public int getPriority() { return 80; }

    @Override
    public boolean isAdditive() { return true; }

    @Override
    public Optional<List<HintEntry>> getHints(HintContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        List<HintEntry> hints = new ArrayList<>();

        boolean riding    = ctx.player().isPassenger();
        boolean swimming  = ctx.player().isSwimming();
        boolean crouching = ctx.player().isCrouching();
        boolean sprinting = ctx.player().isSprinting();

        // ── SHIFT 槽：蹲下 / 起身（骑乘/游泳由 VanillaHintProvider 处理）──────
        if (!riding && !swimming) {
            hints.add(HintEntry.fromMapping(HintSlot.SHIFT, mc.options.keyShift,
                    crouching
                            ? "hint.show_your_keys.stand_up"
                            : "hint.show_your_keys.sneak"));
        }

        // ── SPRINT 槽：疾跑 / 疾游（骑乘时无效）────────────────────────────────
        if (!riding) {
            if (swimming && !sprinting) {
                hints.add(HintEntry.fromMapping(HintSlot.SPRINT, mc.options.keySprint,
                        "hint.show_your_keys.sprint_swim"));
            } else if (!swimming && !sprinting && !crouching) {
                // 蹲下时无法疾跑，不显示
                hints.add(HintEntry.fromMapping(HintSlot.SPRINT, mc.options.keySprint,
                        "hint.show_your_keys.sprint"));
            }
        }

        // ── DROP 槽：丢出物品────────────────────────────────────────────────────
        if (!ctx.heldItem().isEmpty()) {
            hints.add(HintEntry.fromMapping(HintSlot.DROP, mc.options.keyDrop,
                    "hint.show_your_keys.drop_item"));
        }

        return hints.isEmpty() ? Optional.empty() : Optional.of(hints);
    }
}