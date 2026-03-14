package com.gzxx.show_your_keys.internal.provider;

import com.gzxx.show_your_keys.api.hint.HintContext;
import com.gzxx.show_your_keys.api.hint.HintEntry;
import com.gzxx.show_your_keys.api.hint.HintSlot;
import com.gzxx.show_your_keys.api.hint.IKeyHintProvider;
import com.gzxx.show_your_keys.api.registry.AbilityHintRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.ArrayList;
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
 * <p>本 Provider 优先级为 65，低于 VanillaHintProvider（81）。
 * 当检测到可用工具能力时，其 USE 槽条目会覆盖 Vanilla 的交互提示；
 * 若无可用能力（返回 empty），则 USE 槽由 VanillaHintProvider 正常处理。</p>
 *
 * <p>无需修改此类即可扩展支持的工具能力，只需向
 * {@link com.gzxx.show_your_keys.api.ShowYourKeysAPI#abilities()} 注册映射。</p>
 */
public class ItemAbilityHintProvider implements IKeyHintProvider {

    @Override
    public int getPriority() { return 65; }

    @Override
    public Optional<List<HintEntry>> getHints(HintContext ctx) {
        // 只在准心对准方块且持有物品时处理
        if (!ctx.isLookingAtBlock()) return Optional.empty();
        BlockHitResult bhr = ctx.getBlockHitResult();
        if (bhr == null || ctx.heldItem().isEmpty()) return Optional.empty();

        BlockState state = ctx.getTargetBlockState();
        if (state == null) return Optional.empty();

        Minecraft mc = Minecraft.getInstance();
        List<HintEntry> hints = new ArrayList<>();

        UseOnContext useCtx = new UseOnContext(
                ctx.level(), ctx.player(), InteractionHand.MAIN_HAND, ctx.heldItem(), bhr);

        // 遍历注册表，按注册顺序（即优先级顺序）检测
        int slotPriority = 0;
        for (Map.Entry<ItemAbility, String> entry :
                AbilityHintRegistry.INSTANCE.getEntries().entrySet()) {

            ItemAbility ability = entry.getKey();
            if (!ctx.heldItem().canPerformAction(ability)) continue;

            BlockState modified = state.getBlock().getToolModifiedState(state, useCtx, ability, true);
            if (modified != null && modified != state) {
                hints.add(HintEntry.fromMapping(HintSlot.USE, slotPriority,
                        mc.options.keyUse, entry.getValue()));
                slotPriority++;
            }
        }

        return hints.isEmpty() ? Optional.empty() : Optional.of(hints);
    }
}