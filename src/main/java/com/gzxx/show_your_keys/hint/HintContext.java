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
 * 当前帧玩家状态快照，作为所有提示提供者的统一输入
 * 
 * <p>该类每帧捕获一次玩家状态，提供准心目标、手持物品等信息。</p>
 */
public record HintContext(
        // 当前玩家实例
        LocalPlayer player,
        // 玩家主手持有的物品
        ItemStack heldItem,
        // 准心射线命中结果（可为 null）
        @Nullable HitResult hitResult,
        // 当前世界
        Level level
) {

    // 获取玩家与世界状态
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

    // 准心是否对准了一个方块
    public boolean isLookingAtBlock() {
        return hitResult != null && hitResult.getType() == HitResult.Type.BLOCK;
    }

    // 检查准心是否对准了一个实体
    public boolean isLookingAtEntity() {
        return hitResult != null && hitResult.getType() == HitResult.Type.ENTITY;
    }

    // 获取准心指向的方块状态
    @Nullable
    public BlockState getTargetBlockState() {
        if (hitResult instanceof BlockHitResult bhr) {
            return level.getBlockState(bhr.getBlockPos());
        }
        return null;
    }

    // 获取方块的基本信息
    @Nullable
    public BlockHitResult getBlockHitResult() {
        return hitResult instanceof BlockHitResult bhr ? bhr : null;
    }

    // 获取准信指向的实体
    @Nullable
    public Entity getTargetEntity() {
        if (hitResult instanceof EntityHitResult ehr) {
            return ehr.getEntity();
        }
        return null;
    }
}
