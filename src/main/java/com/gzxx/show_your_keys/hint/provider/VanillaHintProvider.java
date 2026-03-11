package com.gzxx.show_your_keys.hint.provider;

import com.gzxx.show_your_keys.hint.HintContext;
import com.gzxx.show_your_keys.hint.HintEntry;
import com.gzxx.show_your_keys.hint.IKeyHintProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 原版按键提示 Provider（优先级 81）。
 *
 * <h3>已修复的 1.21.1 兼容性问题</h3>
 * <ul>
 *   <li>{@code EnchantmentTableBlock} → {@code EnchantingTableBlock}（正确的 Parchment 映射名）</li>
 *   <li>{@code EnderPearlItem} / {@code EggItem} / {@code SnowballItem} →
 *       1.21 中这些投掷物移除了专用 Item 子类，改用 {@code Items.XXX} 直接比较</li>
 * </ul>
 *
 * <h3>覆盖范围</h3>
 * <ul>
 *   <li>骑乘状态（马、船、矿车）</li>
 *   <li>游泳状态</li>
 *   <li>看向可交互方块（门、箱子、工作台等）</li>
 *   <li>看向可挖掘方块（区分正确工具 / 错误工具）</li>
 *   <li>看向实体（攻击 / 交互 / 骑乘 / 驯服）</li>
 *   <li>持有可用物品（食物、弓、弩、三叉戟、投掷物、药水）</li>
 * </ul>
 */
public class VanillaHintProvider implements IKeyHintProvider {

    @Override
    public int getPriority() {
        return 81;
    }

    @Override
    public Optional<List<HintEntry>> getHints(HintContext ctx) {

        // ── 1. 骑乘状态 ──────────────────────────────────────────────────────
        if (ctx.player().isPassenger()) {
            return Optional.of(buildVehicleHints(ctx));
        }

        // ── 2. 游泳状态 ──────────────────────────────────────────────────────
        if (ctx.player().isSwimming()) {
            return Optional.of(buildSwimHints());
        }

        // ── 3. 准心对准方块 ──────────────────────────────────────────────────
        if (ctx.isLookingAtBlock()) {
            BlockState state = ctx.getTargetBlockState();
            if (state != null) {
                return Optional.of(buildBlockHints(ctx, state));
            }
        }

        // ── 4. 准心对准实体 ──────────────────────────────────────────────────
        if (ctx.isLookingAtEntity()) {
            return Optional.of(buildEntityHints(ctx));
        }

        // ── 5. 持有可用物品（准心对准空气）──────────────────────────────────
        if (!ctx.heldItem().isEmpty()) {
            return buildItemAirHints(ctx);
        }

        return Optional.empty();
    }

    // ── 骑乘 ─────────────────────────────────────────────────────────────────

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

    // ── 游泳 ─────────────────────────────────────────────────────────────────

    private List<HintEntry> buildSwimHints() {
        List<HintEntry> hints = new ArrayList<>();
        hints.add(HintEntry.of("Space", "hint.show_your_keys.swim_up"));
        hints.add(HintEntry.of("Shift", "hint.show_your_keys.swim_down"));
        return hints;
    }

    // ── 方块 ─────────────────────────────────────────────────────────────────

    private List<HintEntry> buildBlockHints(HintContext ctx, BlockState state) {
        List<HintEntry> hints = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();

        // 右键可交互的方块（优先提示）
        if (isInteractiveBlock(state)) {
            hints.add(HintEntry.fromMapping(mc.options.keyUse, "hint.show_your_keys.interact"));
        }

        // 挖掘提示（区分是否持有正确工具）
        // 注意：Player#hasCorrectToolForDrops 在 1.21.1 中已废弃，
        // 正确替代是直接调用 ItemStack#isCorrectToolForDrops(BlockState)
        if (ctx.heldItem().isCorrectToolForDrops(state)) {
            hints.add(HintEntry.fromMapping(mc.options.keyAttack, "hint.show_your_keys.mine"));
        } else {
            hints.add(HintEntry.fromMapping(mc.options.keyAttack, "hint.show_your_keys.mine_wrong_tool"));
        }

        return hints;
    }

    /**
     * 判断方块是否支持右键交互。
     *
     * <p>尽量使用 BlockTag 以自动兼容 Mod 新增的同类方块（如模组门、模组床等）。
     * 对于没有对应 Tag 的方块（箱子、工作台等），使用 {@code instanceof} 判断。</p>
     *
     * <p><b>关于 SmithingTableBlock 的说明（警告修复）：</b><br>
     * 在 1.21 中，大장장이 작업대 的实际注册类型并不继承自 {@code SmithingTableBlock}，
     * 导致 {@code instanceof SmithingTableBlock} 静态分析结果永远为 false。<br>
     * 由于它是原版固定方块，改用 {@code block == Blocks.SMITHING_TABLE} 直接比较，
     * 语义更明确，且不产生任何警告。</p>
     *
     * <p><b>关于 EnchantingTableBlock：</b>
     * 正确类名含 'ing'（{@code EnchantingTableBlock}），
     * 错误写法 {@code EnchantmentTableBlock} 会编译报错。</p>
     */
    private boolean isInteractiveBlock(BlockState state) {
        Block block = state.getBlock();
        return state.is(BlockTags.DOORS)          // 含模组门
                || state.is(BlockTags.TRAPDOORS)      // 含模组活板门
                || state.is(BlockTags.FENCE_GATES)    // 含模组栅栏门
                || state.is(BlockTags.BEDS)           // 含模组床
                || state.is(BlockTags.BUTTONS)        // 含模组按钮
                || block instanceof ChestBlock
                || block instanceof EnderChestBlock
                || block instanceof BarrelBlock
                || block instanceof ShulkerBoxBlock
                || block instanceof CraftingTableBlock
                || block instanceof AbstractFurnaceBlock   // 覆盖熔炉、高炉、烟熏炉
                || block instanceof AnvilBlock
                || block instanceof EnchantingTableBlock   // 注意：含 'ing'
                || block instanceof LoomBlock
                || block instanceof CartographyTableBlock
                // SmithingTableBlock 在 1.21 中实际注册类型不是 SmithingTableBlock 子类，
                // 直接用 Blocks.SMITHING_TABLE 比较，安全且无警告
                || block == Blocks.SMITHING_TABLE
                || block instanceof GrindstoneBlock
                || block instanceof StonecutterBlock
                || block instanceof LecternBlock
                || block instanceof NoteBlock
                || block instanceof LeverBlock
                || block instanceof JukeboxBlock;
    }

    // ── 实体 ─────────────────────────────────────────────────────────────────

    private List<HintEntry> buildEntityHints(HintContext ctx) {
        List<HintEntry> hints = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        var entity = ctx.getTargetEntity();

        hints.add(HintEntry.fromMapping(mc.options.keyAttack, "hint.show_your_keys.attack"));

        if (entity instanceof AbstractHorse horse) {
            hints.add(HintEntry.fromMapping(mc.options.keyUse,
                    horse.isTamed() ? "hint.show_your_keys.mount" : "hint.show_your_keys.tame"));
        } else if (entity instanceof Boat || entity instanceof AbstractMinecart) {
            hints.add(HintEntry.fromMapping(mc.options.keyUse, "hint.show_your_keys.board_vehicle"));
        } else {
            hints.add(HintEntry.fromMapping(mc.options.keyUse, "hint.show_your_keys.interact_entity"));
        }

        return hints;
    }

    // ── 持有物品（准心对准空气）─────────────────────────────────────────────

    /**
     * 根据持有物品推断可用操作。
     *
     * <p><b>关于 1.21 投掷物的兼容性修复：</b><br>
     * 1.21 中 {@code EggItem}、{@code SnowballItem}、{@code EnderPearlItem} 等投掷物
     * 移除了专用的 Item 子类（或不再 {@code public}），
     * 改用 {@code item == Items.XXX} 直接注册表比较，安全且零反射开销。</p>
     */
    private Optional<List<HintEntry>> buildItemAirHints(HintContext ctx) {
        List<HintEntry> hints = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        Item item = ctx.heldItem().getItem();

        // 食物（1.21 DataComponent 方式检测，兼容模组食物）
        if (ctx.heldItem().has(DataComponents.FOOD)) {
            hints.add(HintEntry.fromMapping(mc.options.keyUse, "hint.show_your_keys.eat"));
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

        // 投掷物：鸡蛋、雪球、末影珍珠
        // 1.21 中这些物品没有专用 public 子类，直接与注册表实例比较
        if (item == Items.EGG || item == Items.SNOWBALL || item == Items.ENDER_PEARL) {
            hints.add(HintEntry.fromMapping(mc.options.keyUse, "hint.show_your_keys.throw_item"));
            return Optional.of(hints);
        }

        // 可饮用药水
        if (item instanceof PotionItem) {
            hints.add(HintEntry.fromMapping(mc.options.keyUse, "hint.show_your_keys.drink_potion"));
            return Optional.of(hints);
        }

        return Optional.empty();
    }
}