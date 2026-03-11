package com.gzxx.show_your_keys.render;

import com.gzxx.show_your_keys.ShowYourKeys;
import com.gzxx.show_your_keys.hint.HintContext;
import com.gzxx.show_your_keys.hint.HintEngine;
import com.gzxx.show_your_keys.hint.HintEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.List;

/**
 * HUD 渲染器：每帧将按键提示绘制到屏幕右上角。
 *
 * <h3>每行布局</h3>
 * <pre>
 *  ← PADDING_H ─→  ← prefixAreaW →  ← keyBoxW →  ← GAP →  ← actionW →  ← PADDING_H →
 *                   工具错误           [左键]        空格       挖掘
 *                                      [右键]                  交互
 * </pre>
 *
 * <p>所有行的按键框 X 坐标统一（取决于最宽的前缀宽度），
 * 保证无论某行是否有前缀，按键框都对齐。</p>
 */
@EventBusSubscriber(modid = ShowYourKeys.MOD_ID, value = Dist.CLIENT)
public class HudRenderer {

    // ── 布局参数（像素） ──────────────────────────────────────────────────

    private static final int MARGIN_RIGHT   = 10;
    private static final int MARGIN_TOP     = 10;
    private static final int LINE_HEIGHT    = 13;
    private static final int PADDING_H      = 6;
    private static final int PADDING_V      = 5;
    private static final int KEY_BOX_PAD    = 3;
    private static final int KEY_ACTION_GAP = 5;
    /** 前缀文字与按键框之间的间距 */
    private static final int PREFIX_KEY_GAP = 5;

    // ── 颜色（ARGB 格式） ─────────────────────────────────────────────────

    private static final int COLOR_PANEL_BG = 0x88000000;
    private static final int COLOR_KEY_BG   = 0xCC3A3A3A;
    private static final int COLOR_KEY_TEXT = 0xFFFFFFFF;
    private static final int COLOR_ACTION   = 0xFFCCCCCC;
    /** 前缀文字：红色警告 */
    private static final int COLOR_PREFIX   = 0xFFFF5555;

    // ── 渲染事件 ──────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;

        HintEngine.getInstance().compute(HintContext.capture());

        List<HintEntry> hints = HintEngine.getInstance().getCurrentHints();
        if (hints.isEmpty()) return;

        renderHintPanel(event.getGuiGraphics(), mc.font, hints,
                mc.getWindow().getGuiScaledWidth());
    }

    // ── 渲染逻辑 ─────────────────────────────────────────────────────────

    private static void renderHintPanel(GuiGraphics graphics, Font font,
                                        List<HintEntry> hints, int screenW) {

        // ── 第一遍：统计各列最大宽度 ──────────────────────────────────────
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

        // 按键框总宽
        int keyBoxW = maxKeyTextW + KEY_BOX_PAD * 2;
        // 前缀预留区域宽度（无前缀行时为 0，不留白）
        int prefixAreaW = (maxPrefixW > 0) ? (maxPrefixW + PREFIX_KEY_GAP) : 0;

        int panelW = PADDING_H + prefixAreaW + keyBoxW + KEY_ACTION_GAP + maxActionW + PADDING_H;
        int panelH = PADDING_V * 2 + hints.size() * LINE_HEIGHT;

        int panelX = screenW - panelW - MARGIN_RIGHT;
        int panelY = MARGIN_TOP;

        // ── 面板背景 ──────────────────────────────────────────────────────
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL_BG);

        // ── 第二遍：逐行绘制 ──────────────────────────────────────────────
        for (int i = 0; i < hints.size(); i++) {
            HintEntry hint = hints.get(i);
            int lineY   = panelY + PADDING_V + i * LINE_HEIGHT;
            // 按键框 X 对所有行统一
            int keyBoxX = panelX + PADDING_H + prefixAreaW;

            // 前缀：右对齐，紧靠按键框左侧
            if (hint.hasPrefix()) {
                int prefixX = keyBoxX - PREFIX_KEY_GAP - font.width(hint.prefix());
                graphics.drawString(font, hint.prefix(), prefixX, lineY, COLOR_PREFIX, false);
            }

            // 按键框背景
            graphics.fill(keyBoxX, lineY - 1,
                    keyBoxX + keyBoxW, lineY + font.lineHeight + 1,
                    COLOR_KEY_BG);

            // 按键标签（居中于框内）
            int keyTextX = keyBoxX + (keyBoxW - font.width(hint.keyLabel())) / 2;
            graphics.drawString(font, hint.keyLabel(), keyTextX, lineY, COLOR_KEY_TEXT, false);

            // 动作说明
            graphics.drawString(font, hint.actionLabel(),
                    keyBoxX + keyBoxW + KEY_ACTION_GAP, lineY, COLOR_ACTION, false);
        }
    }
}