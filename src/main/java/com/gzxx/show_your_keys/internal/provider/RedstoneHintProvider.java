package com.gzxx.show_your_keys.internal.provider;

import com.gzxx.show_your_keys.api.hint.HintContext;
import com.gzxx.show_your_keys.api.hint.HintEntry;
import com.gzxx.show_your_keys.api.hint.HintSlot;
import com.gzxx.show_your_keys.api.hint.IKeyHintProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

/**
 * 红石元件按键提示 Provider（内部实现，优先级 79）。
 *
 * <p>针对中继器和比较器提供专属的 USE / ATTACK 提示，优先级（79）高于
 * {@code VanillaHintProvider}（81），因此会完全覆盖这两个槽上的原版提示。</p>
 *
 * <p>本 Provider 只返回 USE 和 ATTACK 槽的条目，
 * {@code MovementHintProvider}（80）负责的 SHIFT / SPRINT / DROP 槽不受影响。</p>
 */
public class RedstoneHintProvider implements IKeyHintProvider {

    @Override
    public int getPriority() { return 79; }

    @Override
    public Optional<List<HintEntry>> getHints(HintContext ctx) {
        if (!ctx.isLookingAtBlock()) return Optional.empty();

        BlockState state = ctx.getTargetBlockState();
        if (state == null) return Optional.empty();

        Block block = state.getBlock();
        Minecraft mc = Minecraft.getInstance();

        // 中继器
        if (block instanceof RepeaterBlock) {
            return Optional.of(List.of(
                    HintEntry.fromMapping(
                            HintSlot.USE,
                            mc.options.keyUse,
                            "hint.show_your_keys.repeater_delay"
                    ),
                    HintEntry.fromMapping(
                            HintSlot.ATTACK,
                            mc.options.keyAttack,
                            "hint.show_your_keys.mine"
                    )
            ));
        }

        // 比较器
        if (block instanceof ComparatorBlock) {
            return Optional.of(List.of(
                    HintEntry.fromMapping(
                            HintSlot.USE,
                            mc.options.keyUse,
                            "hint.show_your_keys.comparator_mode"
                    ),
                    HintEntry.fromMapping(
                            HintSlot.ATTACK,
                            mc.options.keyAttack,
                            "hint.show_your_keys.mine"
                    )
            ));
        }

        return Optional.empty();
    }
}