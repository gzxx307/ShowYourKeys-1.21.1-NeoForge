package com.gzxx.show_your_keys.internal.provider;

import com.gzxx.show_your_keys.api.hint.HintContext;
import com.gzxx.show_your_keys.api.hint.HintEntry;
import com.gzxx.show_your_keys.api.hint.HintSlot;
import com.gzxx.show_your_keys.api.hint.IKeyHintProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 原版交互按键提示 Provider（内部实现，优先级 81）。
 *
 * <p>覆盖原版 Minecraft 的全部交互场景：</p>
 * <ul>
 *   <li>骑乘载具（马/船/矿车）</li>
 *   <li>游泳状态</li>
 *   <li>准心对准方块（交互、挖掘、放置）</li>
 *   <li>准心对准实体（攻击、骑乘、驯服）</li>
 *   <li>手持物品对准空气（食物、弓弩、盔甲等）</li>
 *   <li>空手对准空气（兜底攻击提示）</li>
 * </ul>
 */
public class VanillaHintProvider implements IKeyHintProvider {

    @Override
    public int getPriority() { return 81; }

    @Override
    public Optional<List<HintEntry>> getHints(HintContext ctx) {

        // ── 骑乘状态 ────────────────────────────────────────────────────────────
        if (ctx.player().isPassenger()) {
            return Optional.of(buildVehicleHints(ctx));
        }

        // ── 游泳状态 ────────────────────────────────────────────────────────────
        if (ctx.player().isSwimming()) {
            return Optional.of(buildSwimHints(ctx));
        }

        // ── 准心对准方块 ─────────────────────────────────────────────────────────
        if (ctx.isLookingAtBlock()) {
            BlockState state = ctx.getTargetBlockState();
            if (state != null) {
                return Optional.of(buildBlockHints(ctx, state));
            }
        }

        // ── 准心对准实体 ─────────────────────────────────────────────────────────
        if (ctx.isLookingAtEntity()) {
            return Optional.of(buildEntityHints(ctx));
        }

        // ── 准心对准空气且手持物品 ───────────────────────────────────────────────
        if (!ctx.heldItem().isEmpty()) {
            return buildItemAirHints(ctx);
        }

        // ── 空手对准空气 ─────────────────────────────────────────────────────────
        return Optional.of(buildBareHandHints(ctx));
    }

    // ── 载具 ──────────────────────────────────────────────────────────────────

    private List<HintEntry> buildVehicleHints(HintContext ctx) {
        List<HintEntry> hints = new ArrayList<>();
        var vehicle = ctx.player().getVehicle();

        hints.add(HintEntry.of(HintSlot.SHIFT, "Shift", "hint.show_your_keys.dismount"));

        if (vehicle instanceof AbstractHorse horse) {
            if (horse.isTamed()) {
                hints.add(HintEntry.of(HintSlot.MOVE, "W / A / S / D", "hint.show_your_keys.steer"));
                hints.add(HintEntry.of(HintSlot.JUMP, "Space", "hint.show_your_keys.horse_jump"));
            }
        } else if (vehicle instanceof Boat) {
            hints.add(HintEntry.of(HintSlot.MOVE, "W / A / S / D", "hint.show_your_keys.steer_boat"));
        } else if (vehicle instanceof AbstractMinecart) {
            hints.add(HintEntry.of(HintSlot.MOVE, "W", "hint.show_your_keys.accelerate_minecart"));
        }

        return hints;
    }

    // ── 游泳 ──────────────────────────────────────────────────────────────────

    private List<HintEntry> buildSwimHints(HintContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        return List.of(
                HintEntry.fromMapping(HintSlot.SHIFT, mc.options.keyShift, "hint.show_your_keys.swim_down"),
                HintEntry.fromMapping(HintSlot.JUMP,  mc.options.keyJump,  "hint.show_your_keys.swim_up")
        );
    }

    // ── 方块交互 ──────────────────────────────────────────────────────────────

    private List<HintEntry> buildBlockHints(HintContext ctx, BlockState state) {
        List<HintEntry> hints = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        Item heldItem = ctx.heldItem().getItem();
        Block block = state.getBlock();
        boolean crouching = ctx.player().isCrouching();
        boolean holdingBlock = heldItem instanceof BlockItem;

        if (!crouching) {
            // 未蹲下：方块交互优先于放置
            if (block instanceof SignBlock) {
                hints.add(HintEntry.fromMapping(HintSlot.USE, mc.options.keyUse,
                        "hint.show_your_keys.edit_sign"));
            } else if (isInteractiveBlock(ctx, state)) {
                hints.add(HintEntry.fromMapping(HintSlot.USE, mc.options.keyUse,
                        "hint.show_your_keys.interact"));
            } else if (holdingBlock && canPlaceAtTarget(ctx)) {
                hints.add(HintEntry.fromMapping(HintSlot.USE, 10, mc.options.keyUse,
                        "hint.show_your_keys.place_block"));
            }
        } else {
            // 蹲下：跳过方块交互，仅显示放置
            if (holdingBlock && canPlaceAtTarget(ctx)) {
                hints.add(HintEntry.fromMapping(HintSlot.USE, 10, mc.options.keyUse,
                        "hint.show_your_keys.place_block"));
            }
        }

        // 刷怪蛋
        if (heldItem instanceof SpawnEggItem) {
            hints.add(HintEntry.fromMapping(HintSlot.USE, 5, mc.options.keyUse,
                    "hint.show_your_keys.spawn_entity"));
        }

        // 挖掘（工具不对时显示警告前缀）
        if (hasCorrectTool(ctx, state)) {
            hints.add(HintEntry.fromMapping(HintSlot.ATTACK, mc.options.keyAttack,
                    "hint.show_your_keys.mine"));
        } else {
            hints.add(HintEntry.fromMapping(HintSlot.ATTACK,
                    "hint.show_your_keys.prefix.wrong_tool",
                    mc.options.keyAttack, "hint.show_your_keys.mine"));
        }

        return hints;
    }

    /**
     * 判断方块是否支持右键交互（打开 GUI 或触发交互效果）。
     *
     * <h3>检测层次</h3>
     * <ol>
     *   <li>BlockTag：门 / 活板门 / 栅栏门 / 床 / 按钮</li>
     *   <li>BlockEntity instanceof MenuProvider：覆盖所有容器（含模组容器）</li>
     *   <li>无 BE 但开 GUI 的原版方块：工作台、铁砧、砂轮、附魔台、锻造台</li>
     *   <li>无 GUI 但有交互效果的方块：拉杆、音符盒、唱片机、钟、重生锚、蛋糕</li>
     * </ol>
     */
    private boolean isInteractiveBlock(HintContext ctx, BlockState state) {
        Block block = state.getBlock();
        BlockHitResult bhr = ctx.getBlockHitResult();

        // 层次 1：BlockTag
        if (state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS)
                || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.BEDS)
                || state.is(BlockTags.BUTTONS)) {
            return true;
        }

        // 层次 2：BE instanceof MenuProvider（覆盖所有模组容器）
        if (bhr != null) {
            BlockEntity be = ctx.level().getBlockEntity(bhr.getBlockPos());
            if (be instanceof MenuProvider) return true;
        }

        // 层次 3：无 BE 但开 GUI 的原版方块
        if (block instanceof CraftingTableBlock || block instanceof AnvilBlock
                || block instanceof GrindstoneBlock || block instanceof EnchantingTableBlock
                || state.is(Blocks.SMITHING_TABLE)) {
            return true;
        }

        // 层次 4：无 GUI 但有交互效果的原版方块
        // TODO: 后续可用自定义 BlockTag #show_your_keys:interactable 替代此硬编码
        return block instanceof LeverBlock || block instanceof NoteBlock
                || block instanceof JukeboxBlock
                || state.is(Blocks.BELL) || state.is(Blocks.RESPAWN_ANCHOR)
                || state.is(Blocks.CAKE);
    }

    // ── 实体交互 ──────────────────────────────────────────────────────────────

    private List<HintEntry> buildEntityHints(HintContext ctx) {
        List<HintEntry> hints = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        var entity = ctx.getTargetEntity();

        hints.add(makeAttackHint(ctx, mc));

        if (entity instanceof AbstractHorse horse) {
            hints.add(HintEntry.fromMapping(HintSlot.USE, mc.options.keyUse,
                    horse.isTamed() ? "hint.show_your_keys.mount" : "hint.show_your_keys.tame"));
        } else if (entity instanceof Boat || entity instanceof AbstractMinecart) {
            hints.add(HintEntry.fromMapping(HintSlot.USE, mc.options.keyUse,
                    "hint.show_your_keys.board_vehicle"));
        } else {
            hints.add(HintEntry.fromMapping(HintSlot.USE, mc.options.keyUse,
                    "hint.show_your_keys.interact_entity"));
        }

        return hints;
    }

    // ── 手持物品对空气 ────────────────────────────────────────────────────────

    /**
     * 准心对准空气时，根据持有物品类型给出提示。
     *
     * <h3>识别策略</h3>
     * <ul>
     *   <li><b>可装备物品</b>（盔甲、鞘翅）：{@code Equipable} 接口</li>
     *   <li><b>食物</b>：{@code DataComponents.FOOD}（覆盖所有模组食物）</li>
     *   <li><b>持续使用物品</b>（弓、弩、三叉戟等）：{@link UseAnim} 检测</li>
     *   <li><b>投掷物</b>：暂无通用接口，保留显式实例检测</li>
     * </ul>
     */
    private Optional<List<HintEntry>> buildItemAirHints(HintContext ctx) {
        List<HintEntry> hints = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        Item item = ctx.heldItem().getItem();

        // 方块物品 / 刷怪蛋：面对空气无法操作
        if (item instanceof BlockItem || item instanceof SpawnEggItem) {
            hints.add(makeAttackHint(ctx, mc));
            return Optional.of(hints);
        }

        // 可装备物品（盔甲、鞘翅等实现 Equipable 的物品）
        if (item instanceof Equipable equipable) {
            EquipmentSlot slot = equipable.getEquipmentSlot();
            boolean isArmorSlot = slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                    || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
            if (isArmorSlot && ctx.player().getItemBySlot(slot).isEmpty()) {
                hints.add(HintEntry.fromMapping(HintSlot.USE, mc.options.keyUse,
                        "hint.show_your_keys.equip"));
            }
            hints.add(makeAttackHint(ctx, mc));
            return Optional.of(hints);
        }

        // 食物（DataComponents.FOOD 覆盖原版及所有模组食物）
        if (ctx.heldItem().has(DataComponents.FOOD)) {
            hints.add(HintEntry.fromMapping(HintSlot.USE, mc.options.keyUse, "hint.show_your_keys.eat"));
            hints.add(makeAttackHint(ctx, mc));
            return Optional.of(hints);
        }

        // 持续使用物品：通过 UseAnim 识别（模组物品设置正确 UseAnim 即可自动适配）
        UseAnim useAnim = ctx.heldItem().getUseAnimation();
        switch (useAnim) {
            case BOW -> {
                hints.add(HintEntry.fromMapping(HintSlot.USE, mc.options.keyUse,
                        "hint.show_your_keys.draw_bow"));
                return Optional.of(hints);
            }
            case CROSSBOW -> {
                hints.add(HintEntry.fromMapping(HintSlot.USE, mc.options.keyUse,
                        "hint.show_your_keys.load_crossbow"));
                return Optional.of(hints);
            }
            case SPEAR -> {
                hints.add(HintEntry.fromMapping(HintSlot.USE, mc.options.keyUse,
                        "hint.show_your_keys.charge_trident"));
                return Optional.of(hints);
            }
            case DRINK -> {
                hints.add(HintEntry.fromMapping(HintSlot.USE, mc.options.keyUse,
                        "hint.show_your_keys.drink_potion"));
                return Optional.of(hints);
            }
            case BLOCK -> {
                // 盾牌：举盾同时仍可攻击
                hints.add(HintEntry.fromMapping(HintSlot.USE, mc.options.keyUse,
                        "hint.show_your_keys.block_shield"));
                hints.add(makeAttackHint(ctx, mc));
                return Optional.of(hints);
            }
            case SPYGLASS -> {
                hints.add(HintEntry.fromMapping(HintSlot.USE, mc.options.keyUse,
                        "hint.show_your_keys.use_spyglass"));
                return Optional.of(hints);
            }
            case TOOT_HORN -> {
                hints.add(HintEntry.fromMapping(HintSlot.USE, mc.options.keyUse,
                        "hint.show_your_keys.toot_horn"));
                return Optional.of(hints);
            }
            default -> { /* UseAnim.NONE：继续向下判断 */ }
        }

        // 投掷物：1.21.1 无通用接口，保留显式实例检测
        // TODO: 等待 NeoForge ProjectileItem 接口
        if (item == Items.EGG || item == Items.SNOWBALL || item == Items.ENDER_PEARL
                || item == Items.EXPERIENCE_BOTTLE || item == Items.SPLASH_POTION
                || item == Items.LINGERING_POTION) {
            hints.add(HintEntry.fromMapping(HintSlot.USE, mc.options.keyUse,
                    "hint.show_your_keys.throw_item"));
            return Optional.of(hints);
        }

        // 通用工具 / 武器：仅显示攻击
        hints.add(makeAttackHint(ctx, mc));
        return Optional.of(hints);
    }

    // ── 空手对空气 ────────────────────────────────────────────────────────────

    private List<HintEntry> buildBareHandHints(HintContext ctx) {
        return List.of(makeAttackHint(ctx, Minecraft.getInstance()));
    }

    // ── 工具方法 ──────────────────────────────────────────────────────────────

    /**
     * 生成攻击提示，若蓄力未完成则附加警告前缀。
     */
    private HintEntry makeAttackHint(HintContext ctx, Minecraft mc) {
        if (ctx.player().getAttackStrengthScale(0f) < 0.95f) {
            return HintEntry.fromMapping(HintSlot.ATTACK,
                    "hint.show_your_keys.prefix.not_charged",
                    mc.options.keyAttack, "hint.show_your_keys.attack");
        }
        return HintEntry.fromMapping(HintSlot.ATTACK, mc.options.keyAttack,
                "hint.show_your_keys.attack");
    }

    /** 检查手持物品是否为目标方块的正确挖掘工具 */
    private boolean hasCorrectTool(HintContext ctx, BlockState state) {
        Tool tool = ctx.heldItem().get(DataComponents.TOOL);
        return tool != null && tool.isCorrectForDrops(state);
    }

    /** 检查是否可以在准心方块的相邻位置放置手持方块物品 */
    private boolean canPlaceAtTarget(HintContext ctx) {
        if (!(ctx.heldItem().getItem() instanceof BlockItem blockItem)) return false;
        BlockHitResult bhr = ctx.getBlockHitResult();
        if (bhr == null) return false;

        Level level = ctx.player().level();
        BlockPos placePos = bhr.getBlockPos().relative(bhr.getDirection());
        BlockState atPlace = level.getBlockState(placePos);

        if (!atPlace.canBeReplaced()) return false;
        return blockItem.getBlock().defaultBlockState().canSurvive(level, placePos);
    }
}