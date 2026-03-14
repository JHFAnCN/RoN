package com.ron.match.client.screen;

import com.ron.match.match.MatchSettings;
import com.ron.match.network.MatchPacketHandler;
import com.ron.match.network.UpdateSettingsPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class MatchSettingsScreen extends Screen {
    private final MatchSettings settings;
    private final boolean canSave;
    /** 从准备页打开时传入，保存后返回该界面；否则为 null。 */
    private final Screen parentScreen;
    private static final int PANEL_W = 360;
    private static final int PANEL_H = 340;
    private static final int PANEL_PAD = 16;
    private static final int ROW_H = 22;
    private static final int ROW_PAD = 8;
    /** 内容区宽度，保证所有控件不超出边界 */
    private static final int CONTENT_W = PANEL_W - PANEL_PAD * 2;
    /** 左侧标签列宽度，右侧控件宽度 = CONTENT_W - LABEL_W - 8 */
    private static final int LABEL_W = 140;
    private static final int CONTROL_W = CONTENT_W - LABEL_W - 8;
    private int panelLeft;
    private int panelTop;
    private EditBox maxPopulationBox;
    private EditBox countdownSecondsBox;
    private AbstractButton countdownEnabledBtn;
    private AbstractButton friendlyFireBtn;
    private AbstractButton allyControlBtn;

    public MatchSettingsScreen(MatchSettings settings) {
        this(settings, null);
    }

    public MatchSettingsScreen(MatchSettings settings, Screen parentScreen) {
        super(Component.literal("匹配设置"));
        this.settings = settings;
        this.canSave = true;
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        panelLeft = (width - PANEL_W) / 2;
        panelTop = (height - PANEL_H) / 2;
        int x = panelLeft + PANEL_PAD;
        int ctrlX = x + LABEL_W + 8;
        int y = panelTop + 32;
        addRenderableWidget(MatchScreenHelper.createButton(x, y - 2, LABEL_W, 22, Component.literal("游戏人口上限"), () -> {}));
        maxPopulationBox = new EditBox(font, ctrlX, y, 88, 18, Component.literal("maxPop"));
        maxPopulationBox.setValue(String.valueOf(settings.getMaxPopulation()));
        maxPopulationBox.setMaxLength(5);
        addRenderableWidget(maxPopulationBox);
        y += ROW_H + ROW_PAD;
        countdownEnabledBtn = MatchScreenHelper.createButton(x, y, CONTENT_W, 22,
            Component.literal("开始前倒计时: " + (settings.isCountdownEnabled() ? "是" : "否")),
            () -> { settings.setCountdownEnabled(!settings.isCountdownEnabled()); countdownEnabledBtn.setMessage(Component.literal("开始前倒计时: " + (settings.isCountdownEnabled() ? "是" : "否"))); });
        addRenderableWidget(countdownEnabledBtn);
        y += ROW_H + ROW_PAD;
        addRenderableWidget(MatchScreenHelper.createButton(x, y - 2, LABEL_W, 22, Component.literal("倒计时秒数 (5-300)"), () -> {}));
        countdownSecondsBox = new EditBox(font, ctrlX, y, 88, 18, Component.literal("countdown"));
        countdownSecondsBox.setValue(String.valueOf(settings.getCountdownSeconds()));
        countdownSecondsBox.setMaxLength(3);
        addRenderableWidget(countdownSecondsBox);
        y += ROW_H + ROW_PAD;
        friendlyFireBtn = MatchScreenHelper.createButton(x, y, CONTENT_W, 22,
            Component.literal("对盟友造成伤害: " + (settings.isFriendlyFire() ? "是" : "否")),
            () -> { settings.setFriendlyFire(!settings.isFriendlyFire()); friendlyFireBtn.setMessage(Component.literal("对盟友造成伤害: " + (settings.isFriendlyFire() ? "是" : "否"))); });
        addRenderableWidget(friendlyFireBtn);
        y += ROW_H + ROW_PAD;
        allyControlBtn = MatchScreenHelper.createButton(x, y, CONTENT_W, 22,
            Component.literal("可控制盟友单位: " + (settings.isAllyControl() ? "是" : "否")),
            () -> { settings.setAllyControl(!settings.isAllyControl()); allyControlBtn.setMessage(Component.literal("可控制盟友单位: " + (settings.isAllyControl() ? "是" : "否"))); });
        addRenderableWidget(allyControlBtn);
        y += ROW_H + ROW_PAD + 10;
        addRenderableWidget(MatchScreenHelper.createButton(panelLeft + PANEL_W / 2 - 100, y, 200, 22, Component.literal("保存并关闭"), () -> saveAndClose()));
    }

    private void saveAndClose() {
        try {
            settings.setMaxPopulation(Integer.parseInt(maxPopulationBox.getValue()));
        } catch (NumberFormatException ignored) {}
        try {
            int secs = Integer.parseInt(countdownSecondsBox.getValue());
            settings.setCountdownSeconds(Math.max(MatchSettings.COUNTDOWN_SECONDS_MIN, secs));
        } catch (NumberFormatException ignored) {}
        if (canSave) {
            MatchPacketHandler.INSTANCE.sendToServer(new UpdateSettingsPacket(settings));
        }
        if (parentScreen != null && minecraft != null) {
            minecraft.setScreen(parentScreen);
        } else {
            onClose();
        }
    }

    public void applyServerSettings(CompoundTag tag) {
        if (tag != null) settings.load(tag);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        MatchScreenHelper.renderWindowPanel(g, panelLeft, panelTop, PANEL_W, PANEL_H);
        g.drawCenteredString(font, title.getString(), panelLeft + PANEL_W / 2, panelTop + 12, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partialTick);
    }

}
