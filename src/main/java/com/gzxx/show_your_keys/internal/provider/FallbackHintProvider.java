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
 * 兜底 Provider（内部实现，优先级 100）。
 *
 * <p>当所有上游 Provider 均返回 {@link Optional#empty()} 时，
 * 此 Provider 确保 HUD 上始终有内容显示，避免完全空白。</p>
 */
public class FallbackHintProvider implements IKeyHintProvider {

    @Override
    public int getPriority() { return 100; }

    @Override
    public Optional<List<SlotContainer>> getHints(HintContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        List<SlotContainer> result = new ArrayList<>();

        // 准心对准方块或实体时，显示通用交互提示
        if (ctx.isLookingAtBlock() || ctx.isLookingAtEntity()) {
            result.add(SlotContainer.of(HintSlot.USE,
                    Hint.fromMapping(mc.options.keyUse, "hint.show_your_keys.interact")));
        }

        // 始终显示攻击提示
        result.add(SlotContainer.of(HintSlot.ATTACK,
                Hint.fromMapping(mc.options.keyAttack, "hint.show_your_keys.attack")));

        return Optional.of(result);
    }
}