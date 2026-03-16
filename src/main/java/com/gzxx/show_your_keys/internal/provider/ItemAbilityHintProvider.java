package com.gzxx.show_your_keys.internal.provider;

import com.gzxx.show_your_keys.api.hint.Hint;
import com.gzxx.show_your_keys.api.hint.HintContext;
import com.gzxx.show_your_keys.api.hint.HintSlot;
import com.gzxx.show_your_keys.api.hint.IKeyHintProvider;
import com.gzxx.show_your_keys.api.hint.SlotContainer;
import com.gzxx.show_your_keys.api.registry.AbilityHintRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 工具能力按键提示 Provider（内部实现，优先级 65）。
 *
 * <p>遍历 {@link AbilityHintRegistry} 中所有已注册的 {@link ItemAbility}，
 * 通过模拟工具变换（{@code Block.getToolModifiedState()}）检测当前工具对准心方块
 * 是否可执行对应操作，并在 USE 槽显示提示。</p>
 *
 * <p>多个工具能力均适用于同一方块时（极少见），按注册顺序依次赋予递增 priority，
 * 由 {@link SlotContainer} 的同键去重机制保证最终只显示 priority 最小（最优先）的一条。</p>
 */
public class ItemAbilityHintProvider implements IKeyHintProvider {

    @Override
    public int getPriority() { return 65; }

    @Override
    public Optional<List<SlotContainer>> getHints(HintContext ctx) {
        if (!ctx.isLookingAtBlock()) return Optional.empty();
        BlockHitResult bhr = ctx.getBlockHitResult();
        if (bhr == null || ctx.heldItem().isEmpty()) return Optional.empty();

        BlockState state = ctx.getTargetBlockState();
        if (state == null) return Optional.empty();

        Minecraft mc = Minecraft.getInstance();
        SlotContainer useSlot = new SlotContainer(HintSlot.USE);

        UseOnContext useCtx = new UseOnContext(
                ctx.level(), ctx.player(), InteractionHand.MAIN_HAND, ctx.heldItem(), bhr);

        // 遍历注册表，按注册顺序（即优先级顺序）检测
        // priority 从 0 开始递增，确保先注册的能力在同键冲突时胜出
        int slotPriority = 0;
        for (Map.Entry<ItemAbility, String> entry :
                AbilityHintRegistry.INSTANCE.getEntries().entrySet()) {

            ItemAbility ability = entry.getKey();
            if (!ctx.heldItem().canPerformAction(ability)) continue;

            BlockState modified = state.getBlock().getToolModifiedState(state, useCtx, ability, true);
            if (modified != null && modified != state) {
                useSlot.add(Hint.fromMapping(slotPriority, mc.options.keyUse, entry.getValue()));
                slotPriority++;
            }
        }

        if (useSlot.isEmpty()) return Optional.empty();
        return Optional.of(List.of(useSlot));
    }
}