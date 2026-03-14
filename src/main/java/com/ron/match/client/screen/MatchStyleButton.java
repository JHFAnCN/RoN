package com.ron.match.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 与 MatchScreenHelper 统一风格的按钮：扁平背景 + 描边 + 居中文字。
 */
public class MatchStyleButton extends AbstractButton {
    private final Runnable onPress;

    public MatchStyleButton(int x, int y, int width, int height, Component message, Runnable onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        if (onPress != null) onPress.run();
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        MatchScreenHelper.renderButton(g, getX(), getY(), getWidth(), getHeight(), isHovered());
        int textColor = MatchScreenHelper.BTN_TEXT_COLOR;
        if (!active) textColor = 0xA0A0A0;
        g.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, textColor);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
