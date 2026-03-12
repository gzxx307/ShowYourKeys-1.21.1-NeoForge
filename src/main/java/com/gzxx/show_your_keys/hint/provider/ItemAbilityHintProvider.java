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
 * 工具能力按键提示提供者，优先级为 65（叠加模式）
 * 
 * <p>该提供者负责检测工具的特殊能力，如斧头去皮、铲子铲平等。</p>
 */
public class ItemAbilityHintProvider implements IKeyHintProvider {

    /**
     * <p>将物品行为 ItemAbility 映射为json文件中配置的键</p>
     * <p>使用 LinkedHashMap，根据配置的优先级顺序进行显示</p>
     */
    private static final Map<ItemAbility, String> ABILITY_HINT_KEYS = new LinkedHashMap<>();

    // 注册内置物品能力提示
    static {
        ABILITY_HINT_KEYS.put(ItemAbilities.AXE_STRIP,"hint.show_your_keys.strip");
        ABILITY_HINT_KEYS.put(ItemAbilities.AXE_SCRAPE,"hint.show_your_keys.scrape");
        ABILITY_HINT_KEYS.put(ItemAbilities.AXE_WAX_OFF,"hint.show_your_keys.wax_off");
        ABILITY_HINT_KEYS.put(ItemAbilities.SHOVEL_FLATTEN,"hint.show_your_keys.flatten");
        ABILITY_HINT_KEYS.put(ItemAbilities.HOE_TILL,"hint.show_your_keys.till");
    }

    /**
     * 注册自定义物品能力函数
     * 
     * @param ability 物品能力
     * @param translationKey 翻译键
     */
    public static void registerAbilityHint(ItemAbility ability, String translationKey) {
        ABILITY_HINT_KEYS.put(ability, translationKey);
    }

    // 获取优先级 65
    @Override
    public int getPriority() { return 65; }

    //是否为叠加模式
    @Override
    public boolean isAdditive() { return true; }

    /**
     * 根据当前上下文生成工具能力相关的按键提示
     * 
     * @param ctx 当前帧上下文
     * @return 包含提示列表的 Optional
     */
    @Override
    public Optional<List<HintEntry>> getHints(HintContext ctx) {
        // 只在准心对准方块时处理
        if (!ctx.isLookingAtBlock()) return Optional.empty();
        // 对象异常时不处理
        BlockHitResult bhr = ctx.getBlockHitResult();
        if (bhr == null || ctx.heldItem().isEmpty()) return Optional.empty();

        // 获取方块状态，如果无法获取则不处理
        BlockState state = ctx.getTargetBlockState();
        if (state == null) return Optional.empty();

        Minecraft mc = Minecraft.getInstance();
        List<HintEntry> hints = new ArrayList<>();

        // 创建使用上下文，用于模拟工具能力
        UseOnContext useCtx = new UseOnContext(
                ctx.level(), ctx.player(), InteractionHand.MAIN_HAND, ctx.heldItem(), bhr);

        int priority = 0;
        // 遍历所有已注册的物品能力
        for (Map.Entry<ItemAbility, String> entry : ABILITY_HINT_KEYS.entrySet()) {
            ItemAbility ability = entry.getKey();
            // 检查物品是否支持该能力
            if (!ctx.heldItem().canPerformAction(ability)) continue;

            // 模拟工具变换，检查是否会产生不同的方块状态
            BlockState modified = state.getBlock().getToolModifiedState(state, useCtx, ability, true);
            if (modified != null && modified != state) {
                // 添加工具变换提示到 USE 槽
                hints.add(HintEntry.fromMapping(HintSlot.USE, priority, mc.options.keyUse, entry.getValue()));
                priority++;
            }
        }

        return hints.isEmpty() ? Optional.empty() : Optional.of(hints);
    }
}