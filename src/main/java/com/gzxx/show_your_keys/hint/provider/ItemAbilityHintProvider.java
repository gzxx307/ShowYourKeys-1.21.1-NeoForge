package com.gzxx.show_your_keys.hint.provider;

import com.gzxx.show_your_keys.hint.HintContext;
import com.gzxx.show_your_keys.hint.HintEntry;
import com.gzxx.show_your_keys.hint.HintSlot;
import com.gzxx.show_your_keys.hint.IKeyHintProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于 NeoForge {@link ItemAbility} 的工具变换提示 Provider（优先级 65，叠加模式）。
 *
 * <h3>核心机制</h3>
 * <p>利用 {@code Block.getToolModifiedState(state, ctx, ability, simulate=true)}
 * 无副作用地模拟工具变换，自动兼容模组工具和模组方块。</p>
 *
 * <h3>叠加模式</h3>
 * <p>本 Provider 追加 USE 槽的工具变换提示后，责任链继续传递给
 * {@link VanillaHintProvider} 补充交互/挖掘提示。两者的结果在 HintEngine
 * 中按槽位排序后合并显示。</p>
 *
 * <h3>第三方 Mod 扩展</h3>
 * <p>调用 {@link #registerAbilityHint(ItemAbility, String)} 注册自定义能力映射，
 * 无需修改本 Mod 任何代码。</p>
 */
public class ItemAbilityHintProvider implements IKeyHintProvider {

    /**
     * ItemAbility → 动作翻译 key 映射。
     * 使用 LinkedHashMap 保证注册顺序即槽内显示顺序（优先级均为 0，靠先注册的显示在前）。
     */
    private static final Map<ItemAbility, String> ABILITY_HINT_KEYS = new LinkedHashMap<>();

    static {
        ABILITY_HINT_KEYS.put(ItemAbilities.AXE_STRIP,      "hint.show_your_keys.strip");
        ABILITY_HINT_KEYS.put(ItemAbilities.AXE_SCRAPE,     "hint.show_your_keys.scrape");
        ABILITY_HINT_KEYS.put(ItemAbilities.AXE_WAX_OFF,    "hint.show_your_keys.wax_off");
        ABILITY_HINT_KEYS.put(ItemAbilities.SHOVEL_FLATTEN, "hint.show_your_keys.flatten");
        ABILITY_HINT_KEYS.put(ItemAbilities.HOE_TILL,       "hint.show_your_keys.till");
    }

    /**
     * 为自定义 {@link ItemAbility} 注册提示翻译 key。
     * 应在 {@code FMLClientSetupEvent} 中调用。
     */
    public static void registerAbilityHint(ItemAbility ability, String translationKey) {
        ABILITY_HINT_KEYS.put(ability, translationKey);
    }

    @Override
    public int getPriority() { return 65; }

    @Override
    public boolean isAdditive() { return true; }

    @Override
    public Optional<List<HintEntry>> getHints(HintContext ctx) {
        if (!ctx.isLookingAtBlock()) return Optional.empty();

        BlockHitResult bhr = ctx.getBlockHitResult();
        if (bhr == null || ctx.heldItem().isEmpty()) return Optional.empty();

        BlockState state = ctx.getTargetBlockState();
        if (state == null) return Optional.empty();

        Minecraft mc = Minecraft.getInstance();
        List<HintEntry> hints = new ArrayList<>();

        UseOnContext useCtx = new UseOnContext(
                ctx.level(), ctx.player(), InteractionHand.MAIN_HAND, ctx.heldItem(), bhr);

        int priority = 0;
        for (Map.Entry<ItemAbility, String> entry : ABILITY_HINT_KEYS.entrySet()) {
            ItemAbility ability = entry.getKey();
            if (!ctx.heldItem().canPerformAction(ability)) continue;

            BlockState modified = state.getBlock().getToolModifiedState(state, useCtx, ability, true);
            if (modified != null && modified != state) {
                // 工具变换提示放在 USE 槽，priority 按注册顺序递增，保证多条时顺序稳定
                hints.add(HintEntry.fromMapping(HintSlot.USE, priority, mc.options.keyUse, entry.getValue()));
                priority++;
            }
        }

        return hints.isEmpty() ? Optional.empty() : Optional.of(hints);
    }
}