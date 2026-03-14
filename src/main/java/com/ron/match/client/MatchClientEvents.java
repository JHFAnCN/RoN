package com.ron.match.client;

import com.solegendary.reignofnether.guiscreen.TopdownGuiServerboundPacket;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import net.minecraft.client.Minecraft;

/**
 * 游戏开始后客户端：进入指挥官视角并打开 RTS 界面。
 */
public class MatchClientEvents {

    public static void onGameStarted() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen instanceof com.ron.match.client.screen.PreparationHallScreen) {
            mc.setScreen(null);
        }
        try {
            if (!OrthoviewClientEvents.isEnabled()) {
                java.lang.reflect.Field f = OrthoviewClientEvents.class.getDeclaredField("enabled");
                f.setAccessible(true);
                f.setBoolean(null, true);
                PlayerServerboundPacket.enableOrthoview();
                MinimapClientEvents.setMapCentre(mc.player.getX(), mc.player.getZ());
                java.lang.reflect.Field by = OrthoviewClientEvents.class.getDeclaredField("orthoviewPlayerBaseY");
                by.setAccessible(true);
                double baseY = by.getDouble(null);
                PlayerServerboundPacket.teleportPlayer(mc.player.getX(), baseY, mc.player.getZ());
                TopdownGuiServerboundPacket.openTopdownGui(mc.player.getId());
            }
        } catch (Throwable t) {
            com.ron.match.RoN_Match.LOGGER.warn("RoN_Match: Could not enable orthoview on client", t);
        }
    }
}
