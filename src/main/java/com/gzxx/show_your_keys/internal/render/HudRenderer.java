package com.gzxx.show_your_keys.internal.render;

import com.gzxx.show_your_keys.ShowYourKeys;
import com.gzxx.show_your_keys.api.hint.HintContext;
import com.gzxx.show_your_keys.api.hint.HintEntry;
import com.gzxx.show_your_keys.internal.engine.HintEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.List;

/**
 * HUD 渲染器（内部实现，请勿直接引用）。
 *
 * <p>每帧在 {@link RenderGuiEvent.Post} 时触发，调用 {@link HintEngine} 计算当前帧提示，
 * 并在屏幕右上角绘制半透明面板。</p>
 */
@EventBusSubscriber(modid = ShowYourKeys.MOD_ID, value = Dist.CLIENT)
public class HudRenderer {

    // ── 布局常量 ───────────────────────────────────────────────────────────────

    /** 面板右侧与屏幕边缘的间距（px） */
    private static final int MARGIN_RIGHT   = 10;
    /** 面板顶部与屏幕边缘的间距（px） */
    private static final int MARGIN_TOP     = 10;
    /** 每行高度（px） */
    private static final int LINE_HEIGHT    = 13;
    /** 面板水平内边距（px） */
    private static final int PADDING_H      = 6;
    /** 面板垂直内边距（px） */
    private static final int PADDING_V      = 5;
    /** 按键框内文字左右内边距（px） */
    private static final int KEY_BOX_PAD    = 3;
    /** 按键框与动作文字之间的间距（px） */
    private static final int KEY_ACTION_GAP = 5;
    /** 前缀文字与按键框之间的间距（px） */
    private static final int PREFIX_KEY_GAP = 5;

    // ── 颜色常量（ARGB） ───────────────────────────────────────────────────────

    private static final int COLOR_PANEL_BG  = 0x88000000;
    private static final int COLOR_KEY_BG    = 0xCC3A3A3A;
    private static final int COLOR_KEY_TEXT  = 0xFFFFFFFF;
    private static final int COLOR_ACTION    = 0xFFCCCCCC;
    private static final int COLOR_PREFIX    = 0xFFFF5555;

    // ── 事件处理 ───────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        // 玩家存在、GUI 未隐藏、无打开界面时才渲染
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;

        HintEngine.getInstance().compute(HintContext.capture());

        List<HintEntry> hints = HintEngine.getInstance().getCurrentHints();
        if (hints.isEmpty()) return;

        renderPanel(event.getGuiGraphics(), mc.font, hints,
                mc.getWindow().getGuiScaledWidth());
    }

    // ── 渲染 ───────────────────────────────────────────────────────────────────

    private static void renderPanel(GuiGraphics g, Font font,
                                    List<HintEntry> hints, int screenW) {

        // 统计各列最大宽度
        int maxPrefixW  = 0;
        int maxKeyTextW = 0;
        int maxActionW  = 0;

        for (HintEntry hint : hints) {
            if (hint.hasPrefix()) {
                maxPrefixW = Math.max(maxPrefixW, font.width(hint.prefix()));
            }
            maxKeyTextW = Math.max(maxKeyTextW, font.width(hint.keyLabel()));
            maxActionW  = Math.max(maxActionW,  font.width(hint.actionLabel()));
        }

        int keyBoxW    = maxKeyTextW + KEY_BOX_PAD * 2;
        int prefixAreaW = (maxPrefixW > 0) ? (maxPrefixW + PREFIX_KEY_GAP) : 0;

        int panelW = PADDING_H + prefixAreaW + keyBoxW + KEY_ACTION_GAP + maxActionW + PADDING_H;
        int panelH = PADDING_V * 2 + hints.size() * LINE_HEIGHT;

        int panelX = screenW - panelW - MARGIN_RIGHT;
        int panelY = MARGIN_TOP;

        // 面板背景
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL_BG);

        // 逐行绘制
        for (int i = 0; i < hints.size(); i++) {
            HintEntry hint = hints.get(i);
            int lineY  = panelY + PADDING_V + i * LINE_HEIGHT;
            int keyBoxX = panelX + PADDING_H + prefixAreaW;

            // 前缀（右对齐至按键框左侧）
            if (hint.hasPrefix()) {
                int prefixX = keyBoxX - PREFIX_KEY_GAP - font.width(hint.prefix());
                g.drawString(font, hint.prefix(), prefixX, lineY, COLOR_PREFIX, false);
            }

            // 按键框背景
            g.fill(keyBoxX, lineY - 1, keyBoxX + keyBoxW, lineY + font.lineHeight + 1, COLOR_KEY_BG);

            // 按键名（居中）
            int keyTextX = keyBoxX + (keyBoxW - font.width(hint.keyLabel())) / 2;
            g.drawString(font, hint.keyLabel(), keyTextX, lineY, COLOR_KEY_TEXT, false);

            // 动作说明
            g.drawString(font, hint.actionLabel(),
                    keyBoxX + keyBoxW + KEY_ACTION_GAP, lineY, COLOR_ACTION, false);
        }
    }
}