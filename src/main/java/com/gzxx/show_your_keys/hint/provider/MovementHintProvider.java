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
 * 通用移动/状态按键提示 Provider（优先级 80，叠加模式）。
 *
 * <h3>职责</h3>
 * <p>负责与准心目标无关、但与玩家运动状态相关的常驻按键提示：
 * 蹲下/起身、疾跑/疾游、丢出物品。</p>
 *
 * <h3>状态过滤规则</h3>
 * <ul>
 *   <li><b>骑乘中</b>：Shift 键由 VanillaHintProvider 用于「下马」，
 *       本 Provider 跳过 SHIFT 槽；Sprint 在骑乘中无意义，跳过。</li>
 *   <li><b>游泳中</b>：Shift 键用于「下潜」（VanillaHintProvider），跳过；
 *       Sprint 键改为「疾游」。</li>
 *   <li><b>已蹲下</b>：显示「起身」替代「蹲下」。</li>
 *   <li><b>已疾跑</b>：隐藏疾跑提示（无需重复提示）。</li>
 * </ul>
 *
 * <h3>与 Slot 系统的关系</h3>
 * <p>本 Provider 的每条提示都有明确的槽位（SHIFT/SPRINT/DROP），
 * HintEngine 排序后它们会固定出现在 HUD 的对应位置，
 * 与 VanillaHintProvider 的 USE/ATTACK 提示不重叠。</p>
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

        // ── SHIFT 槽：蹲下/起身 ────────────────────────────────────────
        // 骑乘/游泳中 Shift 已被 VanillaHintProvider 占用（下马/下潜），此处跳过
        if (!riding && !swimming) {
            if (crouching) {
                hints.add(HintEntry.fromMapping(HintSlot.SHIFT, mc.options.keyShift,
                        "hint.show_your_keys.stand_up"));
            } else {
                hints.add(HintEntry.fromMapping(HintSlot.SHIFT, mc.options.keyShift,
                        "hint.show_your_keys.sneak"));
            }
        }

        // ── SPRINT 槽：疾跑/疾游 ──────────────────────────────────────
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

        // ── DROP 槽：丢出物品 ────────────────────────────────────────
        // 骑乘时仍可丢出物品，不过滤
        if (!ctx.heldItem().isEmpty()) {
            hints.add(HintEntry.fromMapping(HintSlot.DROP, mc.options.keyDrop,
                    "hint.show_your_keys.drop_item"));
        }

        return hints.isEmpty() ? Optional.empty() : Optional.of(hints);
    }
}
