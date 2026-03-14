package com.ron.match.client.screen;

import com.ron.match.match.MatchSettings;
import com.ron.match.match.PlayerPrepEntry;
import com.ron.match.network.*;
import com.ron.match.util.StartColorHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PreparationHallScreen extends Screen {
    private final MatchSettings settings;
    private List<PlayerPrepEntry> entries;
    private boolean canEditSettings;
    private List<Integer> availableColorIds = new ArrayList<>();
    private boolean showExitConfirm;
    private static final int PANEL_W = 440;
    private static final int PANEL_H = 340;
    private static final int PAD = 16;
    private static final int ENTRY_H = 28;
    private static final int BTN_H = 22;
    private static final int BTN_GAP = 6;
    private static final String[] FACTION_NAMES = new String[] { "村民", "怪物", "猪灵", "随机" };
    private static final String[] COLOR_NAMES = new String[] {
        "蓝", "黄", "绿", "红", "橙", "青", "品红", "棕", "白", "黑", "浅蓝", "黄绿", "浅灰", "灰", "紫", "粉"
    };
    private int panelLeft;
    private int panelTop;
    private static final int DROP_ITEM_H = 20;
    private static final int DROP_VISIBLE_ROWS = 8;
    private int dropdownType;
    private int dropdownX, dropdownY, dropdownW, dropdownH;
    private int dropdownScrollOffset;
    private int dropdownTotalItems;
    private List<Integer> dropdownColorOptions = new ArrayList<>();
    private AbstractButton readyBtn;
    /** 开始游戏倒计时剩余 tick，-1 表示无倒计时。 */
    private int countdownTicksLeft = -1;
    private static final int CANCEL_BTN_W = 72;
    private static final int CANCEL_BTN_H = 22;

    public PreparationHallScreen(MatchSettings settings, List<PlayerPrepEntry> entries, boolean canEditSettings) {
        this(settings, entries, canEditSettings, -1);
    }

    public PreparationHallScreen(MatchSettings settings, List<PlayerPrepEntry> entries, boolean canEditSettings, int countdownTicksLeft) {
        super(Component.literal("准备大厅"));
        this.settings = settings;
        this.entries = entries != null ? new ArrayList<>(entries) : new ArrayList<>();
        this.canEditSettings = canEditSettings;
        this.countdownTicksLeft = countdownTicksLeft;
        MatchPacketHandler.INSTANCE.sendToServer(new CheckStartPosPacket());
    }

    @Override
    protected void init() {
        panelLeft = (width - PANEL_W) / 2;
        panelTop = (height - PANEL_H) / 2;
        int by = panelTop + PANEL_H - BTN_H - PAD;
        int bx = panelLeft + PAD;
        addRenderableWidget(MatchScreenHelper.createButton(bx, by, 82, BTN_H, Component.literal("退出游戏"), () -> showExitConfirm = true));
        bx += 82 + BTN_GAP;
        readyBtn = MatchScreenHelper.createButton(bx, by, 72, BTN_H, Component.literal(isMyEntryReady() ? "取消准备" : "准备"), () -> {
            toggleReady();
            if (readyBtn != null) readyBtn.setMessage(Component.literal(isMyEntryReady() ? "取消准备" : "准备"));
        });
        addRenderableWidget(readyBtn);
        bx += 72 + BTN_GAP;
        addRenderableWidget(MatchScreenHelper.createButton(bx, by, 92, BTN_H, Component.literal("开始游戏"), () -> MatchPacketHandler.INSTANCE.sendToServer(new RequestStartGamePacket())));
        bx += 92 + BTN_GAP;
        if (canEditSettings) {
            addRenderableWidget(MatchScreenHelper.createButton(bx, by, 56, BTN_H, Component.literal("设置"), () -> minecraft.setScreen(new MatchSettingsScreen(settings, this))));
        }
        addMyEntryDropdowns();
    }

    private void addMyEntryDropdowns() {
        String myName = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getName().getString() : null;
        if (myName == null) return;
        PlayerPrepEntry myEntry = null;
        int myIndex = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getPlayerName().equals(myName)) {
                myEntry = entries.get(i);
                myIndex = i;
                break;
            }
        }
        if (myEntry == null) return;
        final PlayerPrepEntry entryForLambdas = myEntry;
        int rowY = panelTop + 36 + myIndex * ENTRY_H;
        int teamX = panelLeft + PAD;
        int colorX = panelLeft + PAD + 56;
        int factionX = panelLeft + PAD + 56 + 88;
        int teamW = 52;
        int colorW = 84;
        int factionW = 56;
        addRenderableWidget(MatchScreenHelper.createButton(teamX, rowY, teamW, 20, Component.literal(entryForLambdas.getTeamLetter() + " ▼"), () -> {
            dropdownType = 1;
            dropdownX = teamX;
            dropdownY = rowY + 20;
            dropdownW = 60;
            dropdownTotalItems = 27;
            dropdownH = DROP_VISIBLE_ROWS * DROP_ITEM_H;
            dropdownScrollOffset = 0;
        }));
        Set<Integer> usedColors = entries.stream().map(PlayerPrepEntry::getColorId).filter(id -> id >= 0).collect(Collectors.toSet());
        List<Integer> selectable = new ArrayList<>();
        for (int id : availableColorIds) {
            if (!usedColors.contains(id) || id == entryForLambdas.getColorId()) selectable.add(id);
        }
        if (selectable.isEmpty() && entryForLambdas.getColorId() >= 0) selectable.add(entryForLambdas.getColorId());
        if (selectable.isEmpty()) selectable.add(-1);
        addRenderableWidget(MatchScreenHelper.createButton(colorX, rowY, colorW, 20, Component.literal((entryForLambdas.getColorId() >= 0 ? colorIdToName(entryForLambdas.getColorId()) : "未选") + " ▼"), () -> {
            Set<Integer> used = entries.stream().map(PlayerPrepEntry::getColorId).filter(id -> id >= 0).collect(Collectors.toSet());
            List<Integer> sel = new ArrayList<>();
            for (int id : availableColorIds) {
                if (!used.contains(id) || id == entryForLambdas.getColorId()) sel.add(id);
            }
            if (sel.isEmpty() && entryForLambdas.getColorId() >= 0) sel.add(entryForLambdas.getColorId());
            if (sel.isEmpty()) sel.add(-1);
            dropdownColorOptions = sel;
            dropdownType = 2;
            dropdownX = colorX;
            dropdownY = rowY + 20;
            dropdownW = 84;
            dropdownTotalItems = dropdownColorOptions.size();
            dropdownH = Math.min(DROP_VISIBLE_ROWS, dropdownTotalItems) * DROP_ITEM_H;
            dropdownScrollOffset = 0;
        }));
        addRenderableWidget(MatchScreenHelper.createButton(factionX, rowY, factionW, 20, Component.literal(FACTION_NAMES[entryForLambdas.getFaction()] + " ▼"), () -> {
            dropdownType = 3;
            dropdownX = factionX;
            dropdownY = rowY + 20;
            dropdownW = 56;
            dropdownTotalItems = 4;
            dropdownH = Math.min(DROP_VISIBLE_ROWS, 4) * DROP_ITEM_H;
            dropdownScrollOffset = 0;
        }));
    }

    private boolean isMyEntryReady() {
        String myName = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getName().getString() : null;
        if (myName == null) return false;
        for (PlayerPrepEntry e : entries) {
            if (e.getPlayerName().equals(myName)) return e.isReady();
        }
        return false;
    }

    private void toggleReady() {
        String myName = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getName().getString() : null;
        if (myName == null) return;
        for (PlayerPrepEntry e : entries) {
            if (e.getPlayerName().equals(myName)) {
                e.setReady(!e.isReady());
                MatchPacketHandler.INSTANCE.sendToServer(new UpdatePrepEntryPacket(e));
                break;
            }
        }
    }

    public void applySync(CompoundTag settingsTag, List<PlayerPrepEntry> newEntries, boolean canEdit, int countdownTicksLeft) {
        if (settingsTag != null) settings.load(settingsTag);
        entries = newEntries != null ? new ArrayList<>(newEntries) : new ArrayList<>();
        canEditSettings = canEdit;
        this.countdownTicksLeft = countdownTicksLeft;
        if (minecraft != null) init(minecraft, width, height);
    }

    public void setAvailableStartPosColorIds(List<Integer> colorIds) {
        this.availableColorIds = colorIds != null ? new ArrayList<>(colorIds) : new ArrayList<>();
        if (minecraft != null) init(minecraft, width, height);
    }

    public void closeConfirmExit() {
        showExitConfirm = false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        MatchScreenHelper.renderWindowPanel(g, panelLeft, panelTop, PANEL_W, PANEL_H);
        g.drawCenteredString(font, "准备大厅", panelLeft + PANEL_W / 2, panelTop + 10, 0xFFFFFF);
        g.drawCenteredString(font, "———————", panelLeft + PANEL_W / 2, panelTop + 22, 0xAAAAAA);
        int y = panelTop + 36;
        Set<Integer> usedColors = entries.stream().map(PlayerPrepEntry::getColorId).filter(id -> id >= 0).collect(Collectors.toSet());
        String myName = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getName().getString() : null;
        for (PlayerPrepEntry e : entries) {
            renderEntry(g, e, panelLeft + PAD, y, myName != null && e.getPlayerName().equals(myName), usedColors);
            y += ENTRY_H;
        }
        renderDropdown(g, mouseX, mouseY);
        if (countdownTicksLeft > 0) {
            int cx = panelLeft + PANEL_W - PAD - CANCEL_BTN_W;
            int cy = panelTop + PANEL_H - PAD - CANCEL_BTN_H;
            boolean hover = mouseX >= cx && mouseX < cx + CANCEL_BTN_W && mouseY >= cy && mouseY < cy + CANCEL_BTN_H;
            MatchScreenHelper.renderButton(g, cx, cy, CANCEL_BTN_W, CANCEL_BTN_H, hover);
            g.drawCenteredString(font, "终止游戏", cx + CANCEL_BTN_W / 2, cy + (CANCEL_BTN_H - font.lineHeight) / 2, MatchScreenHelper.BTN_TEXT_COLOR);
        }
        super.render(g, mouseX, mouseY, partialTick);
        if (showExitConfirm) {
            int cw = 200, ch = 80;
            int cx = panelLeft + PANEL_W / 2, cy = panelTop + PANEL_H / 2;
            int ex = cx - cw / 2, ey = cy - ch / 2;
            MatchScreenHelper.renderWindowPanel(g, ex, ey, cw, ch);
            g.drawCenteredString(font, "确认退出准备大厅？", cx, ey + 14, 0xFFFFFF);
            int by = ey + 40, bh = 24, bw = cw - 32;
            int bx = ex + 16;
            boolean hoverExit = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
            MatchScreenHelper.renderButton(g, bx, by, bw, bh, hoverExit);
            g.drawCenteredString(font, "确认退出", bx + bw / 2, by + (bh - font.lineHeight) / 2, MatchScreenHelper.BTN_TEXT_COLOR);
        }
    }

    private void renderEntry(GuiGraphics g, PlayerPrepEntry e, int x, int y, boolean isMe, Set<Integer> usedColors) {
        int teamW = 56, colorW = 88, factionW = 56, nameW = 100, headBoxW = 20, headBoxH = 20;
        int gapAfterHead = 10;
        int cx = x;
        if (!isMe) {
            g.drawString(font, "队伍:" + e.getTeamLetter(), cx, y + 6, 0xFFFFFF);
        }
        cx += teamW;
        if (!isMe) {
            g.drawString(font, "颜色:" + (e.getColorId() >= 0 ? colorIdToName(e.getColorId()) : "-"), cx, y + 6, 0xAAAAAA);
        }
        cx += colorW;
        if (!isMe) {
            g.drawString(font, FACTION_NAMES[e.getFaction()], cx, y + 6, 0xFFFFFF);
        }
        cx += factionW;
        g.drawString(font, e.getPlayerName(), cx, y + 6, isMe ? 0xFFFF00 : 0xFFFFFF);
        cx += nameW;
        g.fill(cx, y, cx + headBoxW, y + headBoxH, 0xFF404040);
        if (minecraft != null && minecraft.getConnection() != null) {
            var list = minecraft.getConnection().getOnlinePlayers();
            for (var info : list) {
                if (info.getProfile().getName().equals(e.getPlayerName())) {
                    if (info.isSkinLoaded()) {
                        var skinLocation = info.getSkinLocation();
                        if (skinLocation != null) {
                            g.blit(skinLocation, cx, y, headBoxW, headBoxH, 8f, 8f, 8, 8, 64, 64);
                        }
                    }
                    break;
                }
            }
        }
        cx += headBoxW + gapAfterHead;
        g.drawString(font, e.isReady() ? "已准备" : "未准备", cx, y + 6, e.isReady() ? 0x00FF00 : 0xFF6666);
    }

    private String colorIdToName(int colorId) {
        int idx = StartColorHelper.mapColorIdToIndex(colorId);
        return idx >= 0 && idx < COLOR_NAMES.length ? COLOR_NAMES[idx] : String.valueOf(colorId);
    }

    private void renderDropdown(GuiGraphics g, int mouseX, int mouseY) {
        if (dropdownType == 0) return;
        g.fill(dropdownX, dropdownY, dropdownX + dropdownW, dropdownY + dropdownH, 0xE0202020);
        g.fill(dropdownX, dropdownY, dropdownX + dropdownW, dropdownY + 1, 0xFF606060);
        g.fill(dropdownX, dropdownY + dropdownH - 1, dropdownX + dropdownW, dropdownY + dropdownH, 0xFF606060);
        g.fill(dropdownX, dropdownY, dropdownX + 1, dropdownY + dropdownH, 0xFF606060);
        g.fill(dropdownX + dropdownW - 1, dropdownY, dropdownX + dropdownW, dropdownY + dropdownH, 0xFF606060);
        if (dropdownType == 1) {
            int colW = Math.max(28, dropdownW / 2);
            int totalRows = 14;
            int visibleRows = Math.min(DROP_VISIBLE_ROWS, totalRows - dropdownScrollOffset);
            for (int r = 0; r < visibleRows; r++) {
                int actualRow = dropdownScrollOffset + r;
                for (int col = 0; col < 2; col++) {
                    int idx = actualRow * 2 + col;
                    if (idx >= dropdownTotalItems) break;
                    int ix = dropdownX + col * colW;
                    int iy = dropdownY + r * DROP_ITEM_H;
                    boolean hover = mouseX >= ix && mouseX < ix + colW && mouseY >= iy && mouseY < iy + DROP_ITEM_H;
                    if (hover) g.fill(ix, iy, ix + colW, iy + DROP_ITEM_H, 0x40808080);
                    g.drawString(font, idx < 26 ? String.valueOf((char) ('A' + idx)) : "随机", ix + 4, iy + 6, 0xFFFFFF);
                }
            }
        } else if (dropdownType == 2) {
            int visibleCount = Math.min(DROP_VISIBLE_ROWS, dropdownTotalItems - dropdownScrollOffset);
            for (int i = 0; i < visibleCount; i++) {
                int idx = dropdownScrollOffset + i;
                if (idx >= dropdownColorOptions.size()) break;
                int iy = dropdownY + i * DROP_ITEM_H;
                boolean hover = mouseX >= dropdownX && mouseX < dropdownX + dropdownW && mouseY >= iy && mouseY < iy + DROP_ITEM_H;
                if (hover) g.fill(dropdownX, iy, dropdownX + dropdownW, iy + DROP_ITEM_H, 0x40808080);
                int id = dropdownColorOptions.get(idx);
                g.drawString(font, id >= 0 ? colorIdToName(id) : "未选", dropdownX + 4, iy + 6, 0xFFFFFF);
            }
        } else if (dropdownType == 3) {
            int visibleCount = Math.min(DROP_VISIBLE_ROWS, dropdownTotalItems - dropdownScrollOffset);
            for (int i = 0; i < visibleCount; i++) {
                int idx = dropdownScrollOffset + i;
                if (idx >= 4) break;
                int iy = dropdownY + i * DROP_ITEM_H;
                boolean hover = mouseX >= dropdownX && mouseX < dropdownX + dropdownW && mouseY >= iy && mouseY < iy + DROP_ITEM_H;
                if (hover) g.fill(dropdownX, iy, dropdownX + dropdownW, iy + DROP_ITEM_H, 0x40808080);
                g.drawString(font, FACTION_NAMES[idx], dropdownX + 4, iy + 6, 0xFFFFFF);
            }
        }
    }

    private void applyToMyEntry(java.util.function.Consumer<PlayerPrepEntry> action) {
        String myName = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getName().getString() : null;
        if (myName == null) return;
        for (PlayerPrepEntry e : entries) {
            if (e.getPlayerName().equals(myName)) {
                action.accept(e);
                MatchPacketHandler.INSTANCE.sendToServer(new UpdatePrepEntryPacket(e));
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && countdownTicksLeft > 0) {
            int cx = panelLeft + PANEL_W - PAD - CANCEL_BTN_W;
            int cy = panelTop + PANEL_H - PAD - CANCEL_BTN_H;
            if (mouseX >= cx && mouseX < cx + CANCEL_BTN_W && mouseY >= cy && mouseY < cy + CANCEL_BTN_H) {
                MatchPacketHandler.INSTANCE.sendToServer(new com.ron.match.network.CancelCountdownPacket());
                return true;
            }
        }
        if (dropdownType != 0 && button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            boolean inDropdown = mx >= dropdownX && mx < dropdownX + dropdownW && my >= dropdownY && my < dropdownY + dropdownH;
            if (inDropdown) {
                boolean selected = false;
                if (dropdownType == 1) {
                    int colW = Math.max(28, dropdownW / 2);
                    int col = (mx - dropdownX) / colW;
                    int row = (my - dropdownY) / DROP_ITEM_H;
                    int idx = (dropdownScrollOffset + row) * 2 + col;
                    if (idx >= 0 && idx < 27 && row >= 0 && row < DROP_VISIBLE_ROWS && col >= 0 && col < 2) {
                        if (idx == 26) {
                            applyToMyEntry(e -> e.setTeamIndex(new java.util.Random().nextInt(26)));
                        } else {
                            applyToMyEntry(e -> e.setTeamIndex(idx));
                        }
                        selected = true;
                    }
                } else if (dropdownType == 2) {
                    int row = (my - dropdownY) / DROP_ITEM_H;
                    int idx = dropdownScrollOffset + row;
                    if (idx >= 0 && idx < dropdownColorOptions.size() && row >= 0 && row < DROP_VISIBLE_ROWS) {
                        int id = dropdownColorOptions.get(idx);
                        applyToMyEntry(e -> e.setColorId(id >= 0 ? id : -1));
                        selected = true;
                    }
                } else if (dropdownType == 3) {
                    int row = (my - dropdownY) / DROP_ITEM_H;
                    int idx = dropdownScrollOffset + row;
                    if (idx >= 0 && idx < 4 && row >= 0 && row < DROP_VISIBLE_ROWS) {
                        applyToMyEntry(e -> e.setFaction(idx));
                        selected = true;
                    }
                }
                if (selected && minecraft != null) init(minecraft, width, height);
            }
            dropdownType = 0;
            return inDropdown;
        }
        if (showExitConfirm) {
            int cw = 200, ch = 80;
            int cx = panelLeft + PANEL_W / 2, cy = panelTop + PANEL_H / 2;
            int ex = cx - cw / 2, ey = cy - ch / 2;
            int by = ey + 40, bh = 24, bw = cw - 32, bx = ex + 16;
            if (mouseY >= by && mouseY < by + bh && mouseX >= bx && mouseX < bx + bw) {
                MatchPacketHandler.INSTANCE.sendToServer(new ExitPreparationPacket());
                if (minecraft != null) minecraft.setScreen(null);
                return true;
            }
            showExitConfirm = false;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        if (dropdownType != 0) {
            int maxScroll = 0;
            if (dropdownType == 1) {
                maxScroll = Math.max(0, 14 - DROP_VISIBLE_ROWS);
            } else if (dropdownType == 2) {
                maxScroll = Math.max(0, dropdownTotalItems - DROP_VISIBLE_ROWS);
            } else if (dropdownType == 3) {
                maxScroll = Math.max(0, dropdownTotalItems - DROP_VISIBLE_ROWS);
            }
            int delta = (int) Math.signum(-scrollAmount);
            dropdownScrollOffset = Math.max(0, Math.min(maxScroll, dropdownScrollOffset + delta));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (dropdownType != 0) {
                dropdownType = 0;
                return true;
            }
            if (showExitConfirm) showExitConfirm = false;
            else showExitConfirm = true;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
