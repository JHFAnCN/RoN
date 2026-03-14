package com.ron.match.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 准备页/设置页共用：窗口描边 + 透明背景、统一按钮风格。
 */
public final class MatchScreenHelper {
    private static final int BORDER = 2;
    private static final int BORDER_COLOR = 0xFF606060;
    private static final int BG_COLOR = 0x20000000;

    /** 统一按钮：背景色 */
    public static final int BTN_BG = 0xFF505050;
    /** 统一按钮：悬停背景色 */
    public static final int BTN_BG_HOVER = 0xFF606060;
    /** 统一按钮：上/左高光边 */
    public static final int BTN_EDGE_LIGHT = 0xFF808080;
    /** 统一按钮：下/右阴影边 */
    public static final int BTN_EDGE_DARK = 0xFF303030;
    /** 统一按钮：文字颜色 */
    public static final int BTN_TEXT_COLOR = 0xFFFFFF;

    /** 绘制窗口面板：透明背景 + 描边，不铺满屏。 */
    public static void renderWindowPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + BORDER, y + BORDER, x + w - BORDER, y + h - BORDER, BG_COLOR);
        g.fill(x, y, x + w, y + BORDER, BORDER_COLOR);
        g.fill(x, y + h - BORDER, x + w, y + h, BORDER_COLOR);
        g.fill(x, y, x + BORDER, y + h, BORDER_COLOR);
        g.fill(x + w - BORDER, y, x + w, y + h, BORDER_COLOR);
    }

    /** 绘制统一风格按钮（仅绘制，不处理点击）。 */
    public static void renderButton(GuiGraphics g, int x, int y, int w, int h, boolean hover) {
        g.fill(x, y, x + w, y + h, hover ? BTN_BG_HOVER : BTN_BG);
        g.fill(x, y, x + w, y + 1, BTN_EDGE_LIGHT);
        g.fill(x, y + h - 1, x + w, y + h, BTN_EDGE_DARK);
        g.fill(x, y, x + 1, y + h, BTN_EDGE_LIGHT);
        g.fill(x + w - 1, y, x + w, y + h, BTN_EDGE_DARK);
    }

    /** 创建统一风格的按钮（与 renderButton 视觉一致）。 */
    public static MatchStyleButton createButton(int x, int y, int w, int h, Component message, Runnable onPress) {
        return new MatchStyleButton(x, y, w, h, message, onPress);
    }
}
