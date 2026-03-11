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
 * HUD 渲染器：每帧将当前按键提示绘制到屏幕右上角。
 *
 * <h3>NeoForge 1.21.1 注册方式说明</h3>
 * <p>{@code RenderGuiEvent.Post} 是游戏总线（NeoForge.EVENT_BUS）事件，
 * 不实现 {@code IModBusEvent}，因此 {@code @EventBusSubscriber} 无需指定
 * {@code bus} 参数（默认即为游戏总线）。
 * 指定 {@code bus = Bus.GAME} 在 1.21.1 中已废弃，省略即可。</p>
 *
 * <h3>第一阶段：静态渲染（无动画）</h3>
 * <p>提示直接显示在右上角固定位置。
 * 戴森球风格滑入/滑出动画将在第二阶段实现。</p>
 *
 * <h3>视觉效果</h3>
 * <pre>
 *  ┌─────────────────────────────────┐
 *  │  [左键]  挖掘                   │
 *  │  [右键]  交互                   │
 *  └─────────────────────────────────┘
 * </pre>
 */
@EventBusSubscriber(modid = ShowYourKeys.MOD_ID, value = Dist.CLIENT)
public class HudRenderer {

    // ── 布局参数（像素） ──────────────────────────────────────────────────

    /** 面板距屏幕右边缘的距离 */
    private static final int MARGIN_RIGHT   = 10;
    /** 面板距屏幕上边缘的距离 */
    private static final int MARGIN_TOP     = 10;
    /** 每行提示的行高 */
    private static final int LINE_HEIGHT    = 13;
    /** 面板内容区水平内边距 */
    private static final int PADDING_H      = 6;
    /** 面板内容区垂直内边距 */
    private static final int PADDING_V      = 5;
    /** 按键标签背景框的内边距 */
    private static final int KEY_BOX_PAD    = 3;
    /** 按键标签和动作文字之间的间距 */
    private static final int KEY_ACTION_GAP = 5;

    // ── 颜色（ARGB 格式） ─────────────────────────────────────────────────

    /** 整体面板的半透明黑色背景 */
    private static final int COLOR_PANEL_BG = 0x88000000;
    /** 按键标签的深灰色背景 */
    private static final int COLOR_KEY_BG   = 0xCC3A3A3A;
    /** 按键标签文字：亮白色 */
    private static final int COLOR_KEY_TEXT = 0xFFFFFFFF;
    /** 动作说明文字：浅灰色 */
    private static final int COLOR_ACTION   = 0xFFCCCCCC;

    // ── 渲染事件 ──────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        // 玩家不存在、F1 隐藏 HUD、或正在开着 GUI 界面时跳过
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;

        // ① 捕获当前帧上下文并触发计算（复用原版已有的 hitResult，零额外开销）
        HintEngine.getInstance().compute(HintContext.capture());

        // ② 读取计算结果
        List<HintEntry> hints = HintEngine.getInstance().getCurrentHints();
        if (hints.isEmpty()) return;

        // ③ 渲染
        renderHintPanel(
                event.getGuiGraphics(),
                mc.font,
                hints,
                mc.getWindow().getGuiScaledWidth()
        );
    }

    // ── 渲染逻辑 ─────────────────────────────────────────────────────────

    private static void renderHintPanel(GuiGraphics graphics, Font font,
                                        List<HintEntry> hints, int screenW) {

        // 统计各列最大宽度以实现对齐
        int maxKeyTextW = 0;
        int maxActionW  = 0;
        for (HintEntry hint : hints) {
            maxKeyTextW = Math.max(maxKeyTextW, font.width(hint.keyLabel()));
            maxActionW  = Math.max(maxActionW,  font.width(hint.actionLabel()));
        }

        // 按键框总宽 = 文字宽 + 左右内边距
        int keyBoxW = maxKeyTextW + KEY_BOX_PAD * 2;

        // 面板尺寸
        int panelW = PADDING_H + keyBoxW + KEY_ACTION_GAP + maxActionW + PADDING_H;
        int panelH = PADDING_V * 2 + hints.size() * LINE_HEIGHT;

        // 面板左上角坐标（右上角锚定）
        int panelX = screenW - panelW - MARGIN_RIGHT;
        int panelY = MARGIN_TOP;

        // ── 面板背景 ──
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL_BG);

        // ── 逐行渲染 ──
        for (int i = 0; i < hints.size(); i++) {
            HintEntry hint = hints.get(i);
            int lineY   = panelY + PADDING_V + i * LINE_HEIGHT;
            int keyBoxX = panelX + PADDING_H;

            // 按键标签背景框
            graphics.fill(
                    keyBoxX,
                    lineY - 1,
                    keyBoxX + keyBoxW,
                    lineY + font.lineHeight + 1,
                    COLOR_KEY_BG
            );

            // 按键标签文字（水平居中于框内）
            int keyTextX = keyBoxX + (keyBoxW - font.width(hint.keyLabel())) / 2;
            graphics.drawString(font, hint.keyLabel(),    keyTextX,                    lineY, COLOR_KEY_TEXT, false);

            // 动作说明文字
            graphics.drawString(font, hint.actionLabel(), keyBoxX + keyBoxW + KEY_ACTION_GAP, lineY, COLOR_ACTION,   false);
        }
    }
}