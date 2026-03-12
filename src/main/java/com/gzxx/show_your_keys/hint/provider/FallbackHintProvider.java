package com.gzxx.show_your_keys.hint.provider;

import com.gzxx.show_your_keys.hint.HintContext;
import com.gzxx.show_your_keys.hint.HintEntry;
import com.gzxx.show_your_keys.hint.HintSlot;
import com.gzxx.show_your_keys.hint.IKeyHintProvider;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 退化到最低级的 Provider
 *
 * <p>当所有上游 Provider 均返回 {@link Optional#empty()} 时，
 * 本 Provider 确保屏幕上始终有内容显示，避免完全空白 </p>
 */
public class FallbackHintProvider implements IKeyHintProvider {

    // 获取到最低级的 Provider 优先级
    @Override
    public int getPriority() { return 100; }

    /**
     * 生成兜底按键提示
     * 
     * @param ctx 当前帧上下文
     * @return 包含基础交互和攻击提示的 Optional
     */
    @Override
    public Optional<List<HintEntry>> getHints(HintContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        List<HintEntry> hints = new ArrayList<>();

        // 如果准心对准方块或实体，显示交互提示
        if (ctx.isLookingAtBlock() || ctx.isLookingAtEntity()) {
            hints.add(HintEntry.fromMapping(HintSlot.USE, mc.options.keyUse,
                    "hint.show_your_keys.interact"));
        }
        
        // 总是显示攻击提示
        hints.add(HintEntry.fromMapping(HintSlot.ATTACK, mc.options.keyAttack,
                "hint.show_your_keys.attack"));

        return Optional.of(hints);
    }
}