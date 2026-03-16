package com.gzxx.show_your_keys.internal.render;

import com.gzxx.show_your_keys.ShowYourKeys;
import com.gzxx.show_your_keys.api.hint.Hint;
import com.gzxx.show_your_keys.api.hint.HintContext;
import com.gzxx.show_your_keys.api.hint.SlotContainer;
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
 *
 * <h3>渲染结构</h3>
 * <p>引擎输出 {@link SlotContainer} 列表（已按槽位 order 排序）。
 * 渲染器对每个容器依次渲染其中的 {@link Hint} 列表（已按 priority 排序），
 * 每条提示占一行。</p>
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
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;

        HintEngine.getInstance().compute(HintContext.capture());

        List<SlotContainer> slots = HintEngine.getInstance().getCurrentSlots();
        if (slots.isEmpty()) return;

        renderPanel(event.getGuiGraphics(), mc.font, slots,
                mc.getWindow().getGuiScaledWidth());
    }

    // ── 渲染 ───────────────────────────────────────────────────────────────────

    private static void renderPanel(GuiGraphics g, Font font,
                                    List<SlotContainer> slots, int screenW) {

        // ── 预计算列宽 ──────────────────────────────────────────────────────────
        // 先展平所有 Hint 以计算面板整体尺寸，再逐行绘制

        int maxPrefixW  = 0;
        int maxKeyTextW = 0;
        int maxActionW  = 0;
        int totalLines  = 0;

        for (SlotContainer container : slots) {
            for (Hint hint : container.getHints()) {
                if (hint.hasPrefix()) {
                    maxPrefixW = Math.max(maxPrefixW, font.width(hint.prefix()));
                }
                maxKeyTextW = Math.max(maxKeyTextW, font.width(hint.keyLabel()));
                maxActionW  = Math.max(maxActionW,  font.width(hint.actionLabel()));
                totalLines++;
            }
        }

        if (totalLines == 0) return;

        int keyBoxW     = maxKeyTextW + KEY_BOX_PAD * 2;
        int prefixAreaW = (maxPrefixW > 0) ? (maxPrefixW + PREFIX_KEY_GAP) : 0;

        int panelW = PADDING_H + prefixAreaW + keyBoxW + KEY_ACTION_GAP + maxActionW + PADDING_H;
        int panelH = PADDING_V * 2 + totalLines * LINE_HEIGHT;

        int panelX = screenW - panelW - MARGIN_RIGHT;
        int panelY = MARGIN_TOP;

        // ── 绘制面板背景 ────────────────────────────────────────────────────────
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL_BG);

        // ── 逐槽逐行绘制 ────────────────────────────────────────────────────────
        int lineIndex = 0;
        for (SlotContainer container : slots) {
            for (Hint hint : container.getHints()) {
                int lineY   = panelY + PADDING_V + lineIndex * LINE_HEIGHT;
                int keyBoxX = panelX + PADDING_H + prefixAreaW;

                // 前缀（右对齐至按键框左侧）
                if (hint.hasPrefix()) {
                    int prefixX = keyBoxX - PREFIX_KEY_GAP - font.width(hint.prefix());
                    g.drawString(font, hint.prefix(), prefixX, lineY, COLOR_PREFIX, false);
                }

                // 按键框背景
                g.fill(keyBoxX, lineY - 1,
                        keyBoxX + keyBoxW, lineY + font.lineHeight + 1,
                        COLOR_KEY_BG);

                // 按键名（水平居中）
                int keyTextX = keyBoxX + (keyBoxW - font.width(hint.keyLabel())) / 2;
                g.drawString(font, hint.keyLabel(), keyTextX, lineY, COLOR_KEY_TEXT, false);

                // 动作说明
                g.drawString(font, hint.actionLabel(),
                        keyBoxX + keyBoxW + KEY_ACTION_GAP, lineY, COLOR_ACTION, false);

                lineIndex++;
            }
        }
    }
}