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
 * 兜底 Provider（优先级 100，终止模式）。
 *
 * <p>当所有上游 Provider 均返回 {@link Optional#empty()} 时，
 * 本 Provider 确保屏幕上始终有内容显示，避免完全空白让玩家困惑。</p>
 */
public class FallbackHintProvider implements IKeyHintProvider {

    @Override
    public int getPriority() { return 100; }

    @Override
    public Optional<List<HintEntry>> getHints(HintContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        List<HintEntry> hints = new ArrayList<>();

        if (ctx.isLookingAtBlock() || ctx.isLookingAtEntity()) {
            hints.add(HintEntry.fromMapping(HintSlot.USE, mc.options.keyUse,
                    "hint.show_your_keys.interact"));
        }
        hints.add(HintEntry.fromMapping(HintSlot.ATTACK, mc.options.keyAttack,
                "hint.show_your_keys.attack"));

        return Optional.of(hints);
    }
}