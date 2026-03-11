package com.gzxx.show_your_keys.hint;

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
 * 当前帧玩家状态的不可变快照，作为所有 HintProvider 的统一输入。
 *
 * <p>每帧由渲染事件驱动调用 {@link #capture()} 捕获一次，
 * 然后传入 {@link HintEngine#compute(HintContext)} 进行提示计算。</p>
 */
public record HintContext(
        LocalPlayer player,
        ItemStack heldItem,
        @Nullable HitResult hitResult,
        Level level
) {

    /**
     * 从当前 Minecraft 客户端捕获状态快照。
     * 若玩家不在游戏内（如在主界面），返回 null。
     */
    @Nullable
    public static HintContext capture() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;
        return new HintContext(
                mc.player,
                mc.player.getMainHandItem(),
                mc.hitResult,           // 原版每帧已计算好的准心射线结果，直接复用
                mc.level
        );
    }

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
     * 若准心未指向方块，返回 null。
     */
    @Nullable
    public BlockState getTargetBlockState() {
        if (hitResult instanceof BlockHitResult bhr) {
            return level.getBlockState(bhr.getBlockPos());
        }
        return null;
    }

    /**
     * 获取原始的 BlockHitResult（包含方块坐标、方向等信息）。
     * 若准心未指向方块，返回 null。
     */
    @Nullable
    public BlockHitResult getBlockHitResult() {
        return hitResult instanceof BlockHitResult bhr ? bhr : null;
    }

    /**
     * 获取准心指向的实体。
     * 若准心未指向实体，返回 null。
     */
    @Nullable
    public Entity getTargetEntity() {
        if (hitResult instanceof EntityHitResult ehr) {
            return ehr.getEntity();
        }
        return null;
    }
}
