package com.gzxx.show_your_keys.hint.provider;

import com.gzxx.show_your_keys.hint.HintContext;
import com.gzxx.show_your_keys.hint.HintEntry;
import com.gzxx.show_your_keys.hint.IKeyHintProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 原版按键提示 Provider（优先级 81）。
 *
 * <h3>1.21.1 兼容性说明</h3>
 * <ul>
 *   <li>{@code Player#hasCorrectToolForDrops(BlockState)} 已废弃 →
 *       改为从持有 ItemStack 读取 {@code DataComponents.TOOL}，
 *       调用 {@code Tool#isCorrectForDrops(BlockState)}</li>
 *   <li>{@code SmithingTableBlock} 在 1.21.1 中无专属子类 →
 *       改为 {@code state.is(Blocks.SMITHING_TABLE)} 直接比较注册表实例</li>
 * </ul>
 */
public class VanillaHintProvider implements IKeyHintProvider {

    @Override
    public int getPriority() {
        return 81;
    }

    @Override
    public Optional<List<HintEntry>> getHints(HintContext ctx) {

        // ── 1. 骑乘状态 ──────────────────────────────────────────────────
        if (ctx.player().isPassenger()) {
            return Optional.of(buildVehicleHints(ctx));
        }

        // ── 2. 游泳状态 ──────────────────────────────────────────────────
        if (ctx.player().isSwimming()) {
            return Optional.of(buildSwimHints());
        }

        // ── 3. 准心对准方块 ──────────────────────────────────────────────
        if (ctx.isLookingAtBlock()) {
            BlockState state = ctx.getTargetBlockState();
            if (state != null) {
                return Optional.of(buildBlockHints(ctx, state));
            }
        }

        // ── 4. 准心对准实体 ──────────────────────────────────────────────
        if (ctx.isLookingAtEntity()) {
            return Optional.of(buildEntityHints(ctx));
        }

        // ── 5. 准心对准空气（持有物品判断）──────────────────────────────
        if (!ctx.heldItem().isEmpty()) {
            return buildItemAirHints(ctx);
        }

        // ── 6. 空手准心对准空气 ──────────────────────────────────────────
        return Optional.of(buildBareHandAirHints(ctx));
    }

    // ─────────────────────────────────────────────────────────────────────
    // 攻击蓄力辅助
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 根据攻击蓄力状态生成一条「攻击」提示。
     *
     * <p>蓄力未满时，在同一条目的左侧加红色「蓄力未完成」前缀，
     * 而不是再追加一条新条目，避免「攻击」与「蓄力未完成 攻击」同时出现。</p>
     *
     * <p>{@code getAttackStrengthScale(0f)}：0.0（刚攻击完）→ 1.0（蓄满）。
     * 用 0.95 阈值而非 1.0，避免浮点精度导致的每帧闪烁。</p>
     */
    private HintEntry makeAttackHint(HintContext ctx, Minecraft mc) {
        if (ctx.player().getAttackStrengthScale(0f) < 0.95f) {
            return HintEntry.fromMapping(
                    "hint.show_your_keys.prefix.not_charged",
                    mc.options.keyAttack,
                    "hint.show_your_keys.attack");
        }
        return HintEntry.fromMapping(mc.options.keyAttack, "hint.show_your_keys.attack");
    }

    // ─────────────────────────────────────────────────────────────────────
    // 工具检测辅助
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 判断玩家持有物品是否为目标方块的正确工具。
     *
     * <p>替代已废弃的 {@code Player#hasCorrectToolForDrops(BlockState)}。</p>
     */
    private boolean hasCorrectTool(HintContext ctx, BlockState state) {
        Tool tool = ctx.heldItem().get(DataComponents.TOOL);
        return tool != null && tool.isCorrectForDrops(state);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 方块物品放置检测
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 判断玩家是否能在当前准心命中面上放置手持的 BlockItem。
     *
     * <p>检测流程：</p>
     * <ol>
     *   <li>手持物必须是 {@link BlockItem}</li>
     *   <li>放置目标位置（命中方块沿命中面偏移一格）必须可被替换</li>
     *   <li>要放置的方块默认状态必须能在目标位置存活（{@code canSurvive}）</li>
     * </ol>
     *
     * <p>第 3 步能过滤大多数不合理放置，例如：木门面对竖直墙面时，
     * 目标位置旁虽是空气（可替换），但其下方无固体支撑，
     * 木门的 {@code canSurvive} 返回 {@code false}，从而不显示放置提示。</p>
     */
    private boolean canPlaceAtTarget(HintContext ctx) {
        if (!(ctx.heldItem().getItem() instanceof BlockItem blockItem)) return false;
        var bhr = ctx.getBlockHitResult();
        if (bhr == null) return false;

        Level level = ctx.player().level();
        BlockPos placePos = bhr.getBlockPos().relative(bhr.getDirection());
        BlockState atPlace = level.getBlockState(placePos);

        // 目标位置必须可被替换（空气、草丛等）
        if (!atPlace.canBeReplaced()) return false;

        // 方块自身必须能在该位置存活（处理木门、仙人掌等有放置限制的方块）
        BlockState newState = blockItem.getBlock().defaultBlockState();
        return newState.canSurvive(level, placePos);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 各场景构建
    // ─────────────────────────────────────────────────────────────────────

    private List<HintEntry> buildVehicleHints(HintContext ctx) {
        List<HintEntry> hints = new ArrayList<>();
        var vehicle = ctx.player().getVehicle();

        hints.add(HintEntry.of("Shift", "hint.show_your_keys.dismount"));

        if (vehicle instanceof AbstractHorse horse) {
            if (horse.isTamed()) {
                hints.add(HintEntry.of("W / A / S / D", "hint.show_your_keys.steer"));
                hints.add(HintEntry.of("Space",          "hint.show_your_keys.horse_jump"));
            }
        } else if (vehicle instanceof Boat) {
            hints.add(HintEntry.of("W / A / S / D", "hint.show_your_keys.steer_boat"));
        } else if (vehicle instanceof AbstractMinecart) {
            hints.add(HintEntry.of("W", "hint.show_your_keys.accelerate_minecart"));
        }

        return hints;
    }

    private List<HintEntry> buildSwimHints() {
        List<HintEntry> hints = new ArrayList<>();
        hints.add(HintEntry.of("Space", "hint.show_your_keys.swim_up"));
        hints.add(HintEntry.of("Shift", "hint.show_your_keys.swim_down"));
        return hints;
    }

    private List<HintEntry> buildBlockHints(HintContext ctx, BlockState state) {
        List<HintEntry> hints = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        Item heldItem = ctx.heldItem().getItem();

        // ── 右键：交互性方块 ──
        if (isInteractiveBlock(state)) {
            hints.add(HintEntry.fromMapping(mc.options.keyUse, "hint.show_your_keys.interact"));
        }

        // ── 右键：刷怪蛋（面对方块才能召唤，面对空气不显示）──
        if (heldItem instanceof SpawnEggItem) {
            hints.add(HintEntry.fromMapping(mc.options.keyUse, "hint.show_your_keys.spawn_entity"));
        }

        // ── 右键：放置方块（仅当能合法放置时才显示）──
        // canPlaceAtTarget 同时检查 canBeReplaced + canSurvive，
        // 防止面对竖直墙面时木门等有放置限制的方块误显示"放置"。
        if (heldItem instanceof BlockItem && canPlaceAtTarget(ctx)) {
            hints.add(HintEntry.fromMapping(mc.options.keyUse, "hint.show_your_keys.place_block"));
        }

        // ── 左键：挖掘（工具不对时显示红色前缀）──
        // 挖掘速度不受攻击蓄力影响，此处不追加蓄力提示。
        if (hasCorrectTool(ctx, state)) {
            hints.add(HintEntry.fromMapping(
                    mc.options.keyAttack, "hint.show_your_keys.mine"));
        } else {
            hints.add(HintEntry.fromMapping(
                    "hint.show_your_keys.prefix.wrong_tool",
                    mc.options.keyAttack, "hint.show_your_keys.mine"));
        }

        return hints;
    }

    /**
     * 判断方块是否支持右键交互。
     *
     * <p>尽量使用 BlockTag（自动兼容模组新增的同类方块）。</p>
     *
     * <p>{@code SmithingTableBlock} 在 1.21.1 中无专属 public 子类，
     * 改用 {@code state.is(Blocks.SMITHING_TABLE)} 直接与注册表实例比较。</p>
     */
    private boolean isInteractiveBlock(BlockState state) {
        Block block = state.getBlock();
        return state.is(BlockTags.DOORS)
                || state.is(BlockTags.TRAPDOORS)
                || state.is(BlockTags.FENCE_GATES)
                || state.is(BlockTags.BEDS)
                || state.is(BlockTags.BUTTONS)
                || block instanceof ChestBlock
                || block instanceof EnderChestBlock
                || block instanceof BarrelBlock
                || block instanceof ShulkerBoxBlock
                || block instanceof CraftingTableBlock
                || block instanceof AbstractFurnaceBlock
                || block instanceof AnvilBlock
                || block instanceof EnchantingTableBlock      // 含 'ing'，非 EnchantmentTable
                || state.is(Blocks.SMITHING_TABLE)            // 无专属子类，直接比较注册表实例
                || block instanceof LoomBlock
                || block instanceof CartographyTableBlock
                || block instanceof GrindstoneBlock
                || block instanceof StonecutterBlock
                || block instanceof LecternBlock
                || block instanceof NoteBlock
                || block instanceof LeverBlock
                || block instanceof JukeboxBlock;
    }

    private List<HintEntry> buildEntityHints(HintContext ctx) {
        List<HintEntry> hints = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        var entity = ctx.getTargetEntity();

        // 攻击（含蓄力前缀，合并为单条目）
        hints.add(makeAttackHint(ctx, mc));

        if (entity instanceof AbstractHorse horse) {
            hints.add(HintEntry.fromMapping(mc.options.keyUse,
                    horse.isTamed() ? "hint.show_your_keys.mount"
                            : "hint.show_your_keys.tame"));
        } else if (entity instanceof Boat || entity instanceof AbstractMinecart) {
            hints.add(HintEntry.fromMapping(mc.options.keyUse, "hint.show_your_keys.board_vehicle"));
        } else {
            hints.add(HintEntry.fromMapping(mc.options.keyUse, "hint.show_your_keys.interact_entity"));
        }

        return hints;
    }

    /**
     * 准心对准空气时，根据持有物品类型给出提示。
     *
     * <h3>BlockItem / SpawnEggItem 说明</h3>
     * <ul>
     *   <li>{@link BlockItem}：面对空气时右键无法放置，仅显示攻击。</li>
     *   <li>{@link SpawnEggItem}：面对空气时右键无法召唤，仅显示攻击。</li>
     * </ul>
     * 放置/召唤提示统一放在 {@link #buildBlockHints} 中处理，
     * 确保只在准心命中合法方块面时才出现。
     */
    private Optional<List<HintEntry>> buildItemAirHints(HintContext ctx) {
        List<HintEntry> hints = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        Item item = ctx.heldItem().getItem();

        // 方块物品：面对空气无法放置，仅显示攻击
        if (item instanceof BlockItem) {
            hints.add(makeAttackHint(ctx, mc));
            return Optional.of(hints);
        }

        // 刷怪蛋：面对空气无法召唤，仅显示攻击
        if (item instanceof SpawnEggItem) {
            hints.add(makeAttackHint(ctx, mc));
            return Optional.of(hints);
        }

        // 食物
        if (ctx.heldItem().has(DataComponents.FOOD)) {
            hints.add(HintEntry.fromMapping(mc.options.keyUse, "hint.show_your_keys.eat"));
            hints.add(makeAttackHint(ctx, mc));
            return Optional.of(hints);
        }

        // 弓 / 弩
        if (item instanceof BowItem || item instanceof CrossbowItem) {
            hints.add(HintEntry.fromMapping(mc.options.keyUse, "hint.show_your_keys.draw_bow"));
            return Optional.of(hints);
        }

        // 三叉戟
        if (item instanceof TridentItem) {
            hints.add(HintEntry.fromMapping(mc.options.keyUse, "hint.show_your_keys.charge_trident"));
            return Optional.of(hints);
        }

        // 投掷物（1.21 无专属 public 子类，直接与注册表实例比较）
        if (item == Items.EGG || item == Items.SNOWBALL || item == Items.ENDER_PEARL) {
            hints.add(HintEntry.fromMapping(mc.options.keyUse, "hint.show_your_keys.throw_item"));
            return Optional.of(hints);
        }

        // 可饮用药水
        if (item instanceof PotionItem) {
            hints.add(HintEntry.fromMapping(mc.options.keyUse, "hint.show_your_keys.drink_potion"));
            return Optional.of(hints);
        }

        // 通用工具 / 武器（剑、镐、斧等）
        hints.add(makeAttackHint(ctx, mc));
        return Optional.of(hints);
    }

    /** 空手准心对准空气：仅显示攻击（拳头），含蓄力前缀。 */
    private List<HintEntry> buildBareHandAirHints(HintContext ctx) {
        List<HintEntry> hints = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        hints.add(makeAttackHint(ctx, mc));
        return hints;
    }
}