package com.gzxx.show_your_keys.internal.provider;

import com.gzxx.show_your_keys.api.hint.Hint;
import com.gzxx.show_your_keys.api.hint.HintContext;
import com.gzxx.show_your_keys.api.hint.HintSlot;
import com.gzxx.show_your_keys.api.hint.IKeyHintProvider;
import com.gzxx.show_your_keys.api.hint.SlotContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
 *   <li>准心对准方块（交互、挖掘、放置、流体）</li>
 *   <li>准心对准实体（攻击、骑乘、驯服、剪羊毛）</li>
 *   <li>手持物品对准空气（食物、弓弩、盔甲等）</li>
 *   <li>空手对准空气（兜底攻击提示）</li>
 * </ul>
 */
public class VanillaHintProvider implements IKeyHintProvider {

    @Override
    public int getPriority() { return 81; }

    @Override
    public Optional<List<SlotContainer>> getHints(HintContext ctx) {

        if (ctx.player().isPassenger()) {
            return Optional.of(buildVehicleHints(ctx));
        }

        if (ctx.player().isSwimming()) {
            return Optional.of(buildSwimHints(ctx));
        }

        if (ctx.isLookingAtBlock()) {
            BlockState state = ctx.getTargetBlockState();
            if (state != null) {
                return Optional.of(buildBlockHints(ctx, state));
            }
        }

        if (ctx.isLookingAtEntity()) {
            return Optional.of(buildEntityHints(ctx));
        }

        if (!ctx.heldItem().isEmpty()) {
            return buildItemAirHints(ctx);
        }

        return Optional.of(buildBareHandHints(ctx));
    }

    // ── 载具 ──────────────────────────────────────────────────────────────────

    private List<SlotContainer> buildVehicleHints(HintContext ctx) {
        List<SlotContainer> result = new ArrayList<>();
        var vehicle = ctx.player().getVehicle();

        result.add(SlotContainer.of(HintSlot.SHIFT,
                Hint.of("Shift", "hint.show_your_keys.dismount")));

        if (vehicle instanceof AbstractHorse horse) {
            if (horse.isTamed()) {
                result.add(SlotContainer.of(HintSlot.MOVE,
                        Hint.of("W / A / S / D", "hint.show_your_keys.steer")));
                result.add(SlotContainer.of(HintSlot.JUMP,
                        Hint.of("Space", "hint.show_your_keys.horse_jump")));
            }
        } else if (vehicle instanceof Boat) {
            result.add(SlotContainer.of(HintSlot.MOVE,
                    Hint.of("W / A / S / D", "hint.show_your_keys.steer_boat")));
        } else if (vehicle instanceof AbstractMinecart) {
            result.add(SlotContainer.of(HintSlot.MOVE,
                    Hint.of("W", "hint.show_your_keys.accelerate_minecart")));
        }

        return result;
    }

    // ── 游泳 ──────────────────────────────────────────────────────────────────

    private List<SlotContainer> buildSwimHints(HintContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        return List.of(
                SlotContainer.of(HintSlot.SHIFT,
                        Hint.fromMapping(mc.options.keyShift, "hint.show_your_keys.swim_down")),
                SlotContainer.of(HintSlot.JUMP,
                        Hint.fromMapping(mc.options.keyJump, "hint.show_your_keys.swim_up"))
        );
    }

    // ── 方块交互 ──────────────────────────────────────────────────────────────

    /**
     * 构建准心对准方块时的提示。
     *
     * <h3>USE 槽优先级说明</h3>
     * <p>多个操作可能共用同一右键，通过 {@link SlotContainer} 同键去重，
     * 只保留最高优先级（priority 最小）的提示：</p>
     * <ul>
     *   <li>告示牌编辑 / 方块交互：priority=0（最高，先检测，先插入）</li>
     *   <li>刷怪蛋：priority=5</li>
     *   <li>方块放置：priority=10（最低，面对可交互方块时被覆盖）</li>
     *   <li>流体桶：priority=0（仅在无更高优先级右键操作时生效）</li>
     * </ul>
     *
     * <h3>流体检测修复</h3>
     * <p>{@code mc.hitResult} 使用 {@code ClipContext.Fluid.NONE}，射线会穿透水/熔岩。
     * 手持空桶面对流体源时，{@code state} 是流体后的固体方块，需通过
     * {@link #hasFluidSourceInSight(HintContext)} 补发流体感知射线修正。</p>
     */
    private List<SlotContainer> buildBlockHints(HintContext ctx, BlockState state) {
        Minecraft mc = Minecraft.getInstance();
        Item heldItem    = ctx.heldItem().getItem();
        Block block      = state.getBlock();
        boolean crouching    = ctx.player().isCrouching();
        boolean holdingBlock = heldItem instanceof BlockItem;

        SlotContainer useSlot    = new SlotContainer(HintSlot.USE);
        SlotContainer attackSlot = new SlotContainer(HintSlot.ATTACK);

        // ── USE 槽：方块交互 / 放置 ──────────────────────────────────────────────
        if (!crouching) {
            if (block instanceof SignBlock) {
                useSlot.add(Hint.fromMapping(mc.options.keyUse,
                        "hint.show_your_keys.edit_sign"));
            } else if (isInteractiveBlock(ctx, state)) {
                useSlot.add(Hint.fromMapping(mc.options.keyUse,
                        "hint.show_your_keys.interact"));
            } else if (holdingBlock && canPlaceAtTarget(ctx)) {
                useSlot.add(Hint.fromMapping(10, mc.options.keyUse,
                        "hint.show_your_keys.place_block"));
            }
        } else {
            if (holdingBlock && canPlaceAtTarget(ctx)) {
                useSlot.add(Hint.fromMapping(10, mc.options.keyUse,
                        "hint.show_your_keys.place_block"));
            }
        }

        // ── USE 槽：刷怪蛋（priority=5）──────────────────────────────────────────
        if (heldItem instanceof SpawnEggItem) {
            useSlot.add(Hint.fromMapping(5, mc.options.keyUse,
                    "hint.show_your_keys.spawn_entity"));
        }

        // ── USE 槽：流体桶 ────────────────────────────────────────────────────────
        // 修复 1：SolidBucketItem（细雪桶）不继承 BucketItem，须先独立判断。
        // 修复 2：空桶面对流体源时射线穿透，用 hasFluidSourceInSight 补发流体感知射线。
        if (heldItem instanceof SolidBucketItem) {
            useSlot.add(Hint.fromMapping(mc.options.keyUse,
                    "hint.show_your_keys.place_liquid"));

        } else if (heldItem instanceof BucketItem bucket) {
            if (bucket.content != Fluids.EMPTY) {
                // 非空桶（水桶、熔岩桶）
                useSlot.add(Hint.fromMapping(mc.options.keyUse,
                        "hint.show_your_keys.place_liquid"));
            } else {
                // 空桶：多途径检测可盛装目标
                FluidState fluidState = state.getFluidState();
                boolean canScoop =
                        (fluidState.isSource() &&
                                (fluidState.getType() == Fluids.WATER
                                        || fluidState.getType() == Fluids.LAVA))
                                || hasFluidSourceInSight(ctx)
                                || state.is(Blocks.POWDER_SNOW)
                                || (state.getBlock() instanceof LayeredCauldronBlock
                                && state.getValue(LayeredCauldronBlock.LEVEL) == 3);

                if (canScoop) {
                    useSlot.add(Hint.fromMapping(mc.options.keyUse,
                            "hint.show_your_keys.scoop_liquid"));
                }
            }
        }

        // ── ATTACK 槽：挖掘（工具不对时显示警告前缀）────────────────────────────
        if (hasCorrectTool(ctx, state)) {
            attackSlot.add(Hint.fromMapping(mc.options.keyAttack,
                    "hint.show_your_keys.mine"));
        } else {
            attackSlot.add(Hint.fromMapping(
                    "hint.show_your_keys.prefix.wrong_tool",
                    mc.options.keyAttack,
                    "hint.show_your_keys.mine"));
        }

        List<SlotContainer> result = new ArrayList<>();
        if (!useSlot.isEmpty()) result.add(useSlot);
        result.add(attackSlot);
        return result;
    }

    /**
     * 通过流体感知射线检测准心前方是否存在可舀取的流体源方块。
     *
     * <p>{@code mc.hitResult} 使用 {@code ClipContext.Fluid.NONE}，会穿透水/熔岩源。
     * 本方法重新发射 {@code ClipContext.Fluid.SOURCE_ONLY} 射线来补充检测。</p>
     */
    private boolean hasFluidSourceInSight(HintContext ctx) {
        Vec3 eye   = ctx.player().getEyePosition();
        Vec3 look  = ctx.player().getViewVector(1.0f);
        double reach = ctx.player().blockInteractionRange() + 1.0;

        BlockHitResult fluidHit = ctx.level().clip(new ClipContext(
                eye,
                eye.add(look.scale(reach)),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.SOURCE_ONLY,
                ctx.player()
        ));

        if (fluidHit.getType() != HitResult.Type.BLOCK) return false;

        FluidState fs = ctx.level().getBlockState(fluidHit.getBlockPos()).getFluidState();
        return fs.isSource() &&
                (fs.getType() == Fluids.WATER || fs.getType() == Fluids.LAVA);
    }

    /**
     * 判断方块是否支持右键交互（打开 GUI 或触发交互效果）。
     *
     * <ol>
     *   <li>BlockTag：门 / 活板门 / 栅栏门 / 床 / 按钮</li>
     *   <li>BlockEntity implements MenuProvider：覆盖所有容器（含模组容器）</li>
     *   <li>无 BE 但开 GUI 的原版方块：工作台、铁砧、砂轮、附魔台、锻造台</li>
     *   <li>无 GUI 但有交互效果：拉杆、音符盒、唱片机、钟、重生锚、蛋糕</li>
     * </ol>
     */
    private boolean isInteractiveBlock(HintContext ctx, BlockState state) {
        Block block = state.getBlock();
        BlockHitResult bhr = ctx.getBlockHitResult();

        if (state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS)
                || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.BEDS)
                || state.is(BlockTags.BUTTONS)) {
            return true;
        }

        if (bhr != null) {
            BlockEntity be = ctx.level().getBlockEntity(bhr.getBlockPos());
            if (be instanceof MenuProvider) return true;
        }

        if (block instanceof CraftingTableBlock || block instanceof AnvilBlock
                || block instanceof GrindstoneBlock || block instanceof EnchantingTableBlock
                || state.is(Blocks.SMITHING_TABLE)) {
            return true;
        }

        // TODO: 后续可用 BlockTag #show_your_keys:interactable 替代此硬编码
        return block instanceof LeverBlock || block instanceof NoteBlock
                || block instanceof JukeboxBlock
                || state.is(Blocks.BELL) || state.is(Blocks.RESPAWN_ANCHOR)
                || state.is(Blocks.CAKE);
    }

    // ── 实体交互 ──────────────────────────────────────────────────────────────

    /**
     * 构建准心对准实体时的提示。
     *
     * <h3>剪羊毛修复</h3>
     * <p>所有实体交互类型使用 {@code else-if} 链互斥，保证同一 USE 槽不会
     * 同时存在"interact_entity"和"shear"。{@link SlotContainer} 的同键去重
     * 作为额外的防御层。</p>
     */
    private List<SlotContainer> buildEntityHints(HintContext ctx) {
        Minecraft mc  = Minecraft.getInstance();
        var entity    = ctx.getTargetEntity();

        SlotContainer attackSlot = new SlotContainer(HintSlot.ATTACK);
        attackSlot.add(makeAttackHint(ctx, mc));

        SlotContainer useSlot = new SlotContainer(HintSlot.USE);

        if (entity instanceof AbstractHorse horse) {
            useSlot.add(Hint.fromMapping(mc.options.keyUse,
                    horse.isTamed()
                            ? "hint.show_your_keys.mount"
                            : "hint.show_your_keys.tame"));

        } else if (entity instanceof Boat || entity instanceof AbstractMinecart) {
            useSlot.add(Hint.fromMapping(mc.options.keyUse,
                    "hint.show_your_keys.board_vehicle"));

        } else if (entity instanceof Sheep && ctx.heldItem().getItem() instanceof ShearsItem) {
            // 持剪刀面对羊：只显示"剪羊毛"，不叠加通用"交互"
            useSlot.add(Hint.fromMapping(mc.options.keyUse,
                    "hint.show_your_keys.shear"));

        } else {
            useSlot.add(Hint.fromMapping(mc.options.keyUse,
                    "hint.show_your_keys.interact_entity"));
        }

        List<SlotContainer> result = new ArrayList<>();
        result.add(attackSlot);
        if (!useSlot.isEmpty()) result.add(useSlot);
        return result;
    }

    // ── 手持物品对空气 ────────────────────────────────────────────────────────

    private Optional<List<SlotContainer>> buildItemAirHints(HintContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        Item item    = ctx.heldItem().getItem();

        SlotContainer attackSlot = new SlotContainer(HintSlot.ATTACK);
        attackSlot.add(makeAttackHint(ctx, mc));

        SlotContainer useSlot = new SlotContainer(HintSlot.USE);

        if (item instanceof BlockItem || item instanceof SpawnEggItem) {
            return Optional.of(List.of(attackSlot));
        }

        if (item instanceof Equipable equipable) {
            EquipmentSlot slot = equipable.getEquipmentSlot();
            boolean isArmorSlot = slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                    || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
            if (isArmorSlot && ctx.player().getItemBySlot(slot).isEmpty()) {
                useSlot.add(Hint.fromMapping(mc.options.keyUse, "hint.show_your_keys.equip"));
            }
            List<SlotContainer> result = new ArrayList<>();
            result.add(attackSlot);
            if (!useSlot.isEmpty()) result.add(useSlot);
            return Optional.of(result);
        }

        if (ctx.heldItem().has(DataComponents.FOOD)) {
            useSlot.add(Hint.fromMapping(mc.options.keyUse, "hint.show_your_keys.eat"));
            return Optional.of(List.of(attackSlot, useSlot));
        }

        UseAnim useAnim = ctx.heldItem().getUseAnimation();
        switch (useAnim) {
            case BOW -> {
                useSlot.add(Hint.fromMapping(mc.options.keyUse, "hint.show_your_keys.draw_bow"));
                return Optional.of(List.of(useSlot));
            }
            case CROSSBOW -> {
                useSlot.add(Hint.fromMapping(mc.options.keyUse, "hint.show_your_keys.load_crossbow"));
                return Optional.of(List.of(useSlot));
            }
            case SPEAR -> {
                useSlot.add(Hint.fromMapping(mc.options.keyUse, "hint.show_your_keys.charge_trident"));
                return Optional.of(List.of(useSlot));
            }
            case DRINK -> {
                useSlot.add(Hint.fromMapping(mc.options.keyUse, "hint.show_your_keys.drink_potion"));
                return Optional.of(List.of(useSlot));
            }
            case BLOCK -> {
                useSlot.add(Hint.fromMapping(mc.options.keyUse, "hint.show_your_keys.block_shield"));
                return Optional.of(List.of(attackSlot, useSlot));
            }
            case SPYGLASS -> {
                useSlot.add(Hint.fromMapping(mc.options.keyUse, "hint.show_your_keys.use_spyglass"));
                return Optional.of(List.of(useSlot));
            }
            case TOOT_HORN -> {
                useSlot.add(Hint.fromMapping(mc.options.keyUse, "hint.show_your_keys.toot_horn"));
                return Optional.of(List.of(useSlot));
            }
            default -> { /* UseAnim.NONE：继续向下判断 */ }
        }

        // 投掷物：1.21.1 无通用接口
        // TODO: 等待 NeoForge ProjectileItem 接口
        if (item == Items.EGG || item == Items.SNOWBALL || item == Items.ENDER_PEARL
                || item == Items.EXPERIENCE_BOTTLE || item == Items.SPLASH_POTION
                || item == Items.LINGERING_POTION) {
            useSlot.add(Hint.fromMapping(mc.options.keyUse, "hint.show_your_keys.throw_item"));
            return Optional.of(List.of(useSlot));
        }

        return Optional.of(List.of(attackSlot));
    }

    // ── 空手对空气 ────────────────────────────────────────────────────────────

    private List<SlotContainer> buildBareHandHints(HintContext ctx) {
        SlotContainer attackSlot = new SlotContainer(HintSlot.ATTACK);
        attackSlot.add(makeAttackHint(ctx, Minecraft.getInstance()));
        return List.of(attackSlot);
    }

    // ── 工具方法 ──────────────────────────────────────────────────────────────

    private Hint makeAttackHint(HintContext ctx, Minecraft mc) {
        if (ctx.player().getAttackStrengthScale(0f) < 0.95f) {
            return Hint.fromMapping(
                    "hint.show_your_keys.prefix.not_charged",
                    mc.options.keyAttack,
                    "hint.show_your_keys.attack");
        }
        return Hint.fromMapping(mc.options.keyAttack, "hint.show_your_keys.attack");
    }

    private boolean hasCorrectTool(HintContext ctx, BlockState state) {
        Tool tool = ctx.heldItem().get(DataComponents.TOOL);
        return tool != null && tool.isCorrectForDrops(state);
    }

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