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
        //
        // 检测顺序（优先级从高到低）：
        //   1. 告示牌  → "编辑告示牌"
        //   2. 打开 GUI（容器、工作台等）→ "打开"
        //   3. 有交互但不开 GUI（门、拉杆等）→ "交互"
        //   4. 持有方块可放置 → "放置"
        //
        // 蹲下时跳过 2/3，Minecraft 原版行为：蹲下右键绕过方块交互直接放置。
        if (!crouching) {
            if (block instanceof SignBlock) {
                useSlot.add(Hint.fromMapping(mc.options.keyUse,
                        "hint.show_your_keys.edit_sign"));
            } else if (isGuiBlock(ctx, state)) {
                // 右键会打开界面（箱子、熔炉、工作台等）
                useSlot.add(Hint.fromMapping(mc.options.keyUse,
                        "hint.show_your_keys.open"));
            } else if (isInteractiveBlock(ctx, state)) {
                // 右键有交互效果但不打开界面（门、拉杆、音符盒等）
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
     * 判断右键该方块是否会打开一个界面（GUI Screen）。
     *
     * <p>满足此条件时显示"打开"提示，优先级高于普通"交互"。</p>
     *
     * <h3>检测层次</h3>
     * <ol>
     *   <li>{@code BlockEntity implements MenuProvider}：覆盖所有容器方块，
     *       包括箱子、熔炉、漏斗、发射器、投掷器、酿造台，以及所有模组容器方块。</li>
     *   <li>无 BE 但仍会打开 GUI 的原版方块：
     *       工作台、铁砧、砂轮、附魔台、锻造台。</li>
     * </ol>
     *
     * <p><b>注意</b>：本方法不感知持有物品，仅判断方块本身是否具备打开界面的能力。
     * 蹲下绕过方块交互的逻辑由调用处（{@code buildBlockHints}）负责。</p>
     */
    private boolean isGuiBlock(HintContext ctx, BlockState state) {
        Block block = state.getBlock();
        BlockHitResult bhr = ctx.getBlockHitResult();

        // 层次 1：BlockEntity implements MenuProvider
        if (bhr != null) {
            BlockEntity be = ctx.level().getBlockEntity(bhr.getBlockPos());
            if (be instanceof MenuProvider) return true;
        }

        // 层次 2：无 BE 但打开 GUI 的原版方块
        return block instanceof CraftingTableBlock
                || block instanceof AnvilBlock
                || block instanceof GrindstoneBlock
                || block instanceof EnchantingTableBlock
                || state.is(Blocks.SMITHING_TABLE);
    }

    /**
     * 判断右键该方块是否会触发交互效果（但不打开界面）。
     *
     * <p>满足此条件时显示"交互"提示。
     * 调用前应先排除 {@link #isGuiBlock(HintContext, BlockState)} 为 {@code true} 的情况，
     * 避免同一方块同时匹配两个分支。</p>
     *
     * <h3>检测层次</h3>
     * <ol>
     *   <li>BlockTag：门 / 活板门 / 栅栏门 / 床 / 按钮（开/关、睡觉、触发）</li>
     *   <li>无 GUI 但有交互效果的原版方块：
     *       拉杆（切换状态）、音符盒（播放音符）、唱片机（放/取唱片）、
     *       钟（敲响）、重生锚（设置重生点）、蛋糕（食用）</li>
     * </ol>
     *
     * <p>TODO: 后续可用自定义 BlockTag {@code #show_your_keys:interactable}
     * 替代第 2 层的硬编码，方便 Mod 注册自己的无 GUI 交互方块。</p>
     */
    private boolean isInteractiveBlock(HintContext ctx, BlockState state) {
        Block block = state.getBlock();

        // 层次 1：BlockTag
        if (state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS)
                || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.BEDS)
                || state.is(BlockTags.BUTTONS)) {
            return true;
        }

        // 层次 2：无 GUI 但有交互效果的原版方块
        return block instanceof LeverBlock
                || block instanceof NoteBlock
                || block instanceof JukeboxBlock
                || state.is(Blocks.BELL)
                || state.is(Blocks.RESPAWN_ANCHOR)
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