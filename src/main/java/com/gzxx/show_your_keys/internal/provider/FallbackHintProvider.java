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
 * 兜底 Provider（内部实现，优先级 100，终止模式）。
 *
 * <p>当所有上游 Provider 均返回 {@link Optional#empty()} 时，
 * 此 Provider 确保 HUD 上始终有内容显示，避免完全空白。</p>
 *
 * <p>若你的自定义 Provider 覆盖了大量场景并导致此 Provider 显示的提示不恰当，
 * 可通过注册一个优先级为 99 的终止模式 Provider 来替代它。</p>
 */
public class FallbackHintProvider implements IKeyHintProvider {

    @Override
    public int getPriority() { return 100; }

    @Override
    public Optional<List<HintEntry>> getHints(HintContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        List<HintEntry> hints = new ArrayList<>();

        // 准心对准方块或实体时，显示通用交互提示
        if (ctx.isLookingAtBlock() || ctx.isLookingAtEntity()) {
            hints.add(HintEntry.fromMapping(HintSlot.USE, mc.options.keyUse,
                    "hint.show_your_keys.interact"));
        }

        // 始终显示攻击提示
        hints.add(HintEntry.fromMapping(HintSlot.ATTACK, mc.options.keyAttack,
                "hint.show_your_keys.attack"));

        return Optional.of(hints);
    }
}