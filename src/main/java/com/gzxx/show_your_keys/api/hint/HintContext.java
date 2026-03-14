package com.gzxx.show_your_keys.api.hint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 当前帧玩家状态快照
 *
 * @param player    当前玩家实例
 * @param heldItem  玩家主手持有的物品
 * @param hitResult 准心射线命中结果（可为 {@code null}）
 * @param level     当前世界
 */
public record HintContext(
        LocalPlayer player,
        ItemStack heldItem,
        @Nullable HitResult hitResult,
        Level level
) {

    // ── 采集 ─────────────────────────────────────────────────────────────────

    /**
     * 采集当前帧快照。
     *
     * @return 快照，若玩家或世界为空则返回 null
     */
    @Nullable
    public static HintContext capture() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;
        return new HintContext(
                mc.player,
                mc.player.getMainHandItem(),
                mc.hitResult,
                mc.level
        );
    }

    // ── 准心查询 ─────────────────────────────────────────────────────────────

    /** 准心是否对准了一个方块 */
    public boolean isLookingAtBlock() {
        return hitResult != null && hitResult.getType() == HitResult.Type.BLOCK;
    }

    /** 准心是否对准了一个实体 */
    public boolean isLookingAtEntity() {
        return hitResult != null && hitResult.getType() == HitResult.Type.ENTITY;
    }

    /**
     * 获取准心指向的方块状态。
     *
     * @return 方块状态，若未对准方块则返回 {@code null}
     */
    @Nullable
    public BlockState getTargetBlockState() {
        if (hitResult instanceof BlockHitResult bhr) {
            return level.getBlockState(bhr.getBlockPos());
        }
        return null;
    }

    /**
     * 获取方块命中结果（包含命中面、命中坐标等信息）。
     *
     * @return 方块命中结果，若未对准方块则返回 {@code null}
     */
    @Nullable
    public BlockHitResult getBlockHitResult() {
        return hitResult instanceof BlockHitResult bhr ? bhr : null;
    }

    /**
     * 获取准心指向的实体。
     *
     * @return 目标实体，若未对准实体则返回 {@code null}
     */
    @Nullable
    public Entity getTargetEntity() {
        if (hitResult instanceof EntityHitResult ehr) {
            return ehr.getEntity();
        }
        return null;
    }
}