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

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

public class RedstoneHintProvider implements IKeyHintProvider {

    @Override
    public int getPriority() { return 79; }

    @Override
    public boolean isAdditive() { return true; }

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