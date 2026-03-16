package com.gzxx.show_your_keys.internal.provider;

import com.gzxx.show_your_keys.api.hint.Hint;
import com.gzxx.show_your_keys.api.hint.HintContext;
import com.gzxx.show_your_keys.api.hint.HintSlot;
import com.gzxx.show_your_keys.api.hint.IKeyHintProvider;
import com.gzxx.show_your_keys.api.hint.SlotContainer;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 移动状态按键提示 Provider（内部实现，优先级 80）。
 *
 * <p>负责处理与玩家运动状态相关的常驻提示：蹲下/起身、疾跑/疾游、丢物品。</p>
 *
 * <p>本 Provider 仅覆盖 {@code SHIFT}、{@code SPRINT}、{@code DROP} 三个槽，
 * 与其他 Provider 负责的 {@code USE}、{@code ATTACK} 等槽不冲突。</p>
 */
public class MovementHintProvider implements IKeyHintProvider {

    @Override
    public int getPriority() { return 80; }

    @Override
    public Optional<List<SlotContainer>> getHints(HintContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        List<SlotContainer> result = new ArrayList<>();

        boolean riding    = ctx.player().isPassenger();
        boolean swimming  = ctx.player().isSwimming();
        boolean crouching = ctx.player().isCrouching();
        boolean sprinting = ctx.player().isSprinting();

        // ── SHIFT 槽：蹲下 / 起身（骑乘/游泳由 VanillaHintProvider 处理）──────
        if (!riding && !swimming) {
            result.add(SlotContainer.of(HintSlot.SHIFT,
                    Hint.fromMapping(mc.options.keyShift,
                            crouching
                                    ? "hint.show_your_keys.stand_up"
                                    : "hint.show_your_keys.sneak")));
        }

        // ── SPRINT 槽：疾跑 / 疾游（骑乘时无效）────────────────────────────────
        if (!riding) {
            if (swimming && !sprinting) {
                result.add(SlotContainer.of(HintSlot.SPRINT,
                        Hint.fromMapping(mc.options.keySprint,
                                "hint.show_your_keys.sprint_swim")));
            } else if (!swimming && !sprinting && !crouching) {
                // 蹲下时无法疾跑，不显示
                result.add(SlotContainer.of(HintSlot.SPRINT,
                        Hint.fromMapping(mc.options.keySprint,
                                "hint.show_your_keys.sprint")));
            }
        }

        // ── DROP 槽：丢出物品────────────────────────────────────────────────────
        if (!ctx.heldItem().isEmpty()) {
            result.add(SlotContainer.of(HintSlot.DROP,
                    Hint.fromMapping(mc.options.keyDrop,
                            "hint.show_your_keys.drop_item")));
        }

        return result.isEmpty() ? Optional.empty() : Optional.of(result);
    }
}