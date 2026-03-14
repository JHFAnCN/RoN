package com.ron.match.util;

import net.minecraft.ChatFormatting;
import net.minecraft.world.level.material.MapColor;

/**
 * 与 RoN RTS 出生点 16 色一致的顺序：与 BlockRegistrar 中 RTS_START_BLOCK_* 顺序一致。
 */
public final class StartColorHelper {
    public static final int COLOR_COUNT = 16;
    private static final int[] INDEX_TO_MAP_COLOR_ID = new int[] {
        MapColor.COLOR_BLUE.id,
        MapColor.COLOR_YELLOW.id,
        MapColor.COLOR_GREEN.id,
        MapColor.COLOR_RED.id,
        MapColor.COLOR_ORANGE.id,
        MapColor.COLOR_CYAN.id,
        MapColor.COLOR_MAGENTA.id,
        MapColor.COLOR_BROWN.id,
        MapColor.SNOW.id,
        MapColor.COLOR_BLACK.id,
        MapColor.COLOR_LIGHT_BLUE.id,
        MapColor.COLOR_LIGHT_GREEN.id,
        MapColor.COLOR_LIGHT_GRAY.id,
        MapColor.COLOR_GRAY.id,
        MapColor.COLOR_PURPLE.id,
        MapColor.COLOR_PINK.id
    };

    public static int colorIndexToMapColorId(int index) {
        if (index < 0 || index >= COLOR_COUNT) return -1;
        return INDEX_TO_MAP_COLOR_ID[index];
    }

    public static int mapColorIdToIndex(int mapColorId) {
        for (int i = 0; i < COLOR_COUNT; i++) {
            if (INDEX_TO_MAP_COLOR_ID[i] == mapColorId) return i;
        }
        return -1;
    }

    /** 16 色索引对应 MC 队伍发光颜色（ChatFormatting），与 INDEX_TO_MAP_COLOR_ID 顺序一致。 */
    private static final ChatFormatting[] INDEX_TO_CHAT_COLOR = new ChatFormatting[] {
        ChatFormatting.BLUE,      // 蓝
        ChatFormatting.YELLOW,    // 黄
        ChatFormatting.GREEN,     // 绿
        ChatFormatting.RED,       // 红
        ChatFormatting.GOLD,      // 橙
        ChatFormatting.AQUA,      // 青
        ChatFormatting.LIGHT_PURPLE, // 品红
        ChatFormatting.GOLD,      // 棕
        ChatFormatting.WHITE,     // 白
        ChatFormatting.BLACK,     // 黑
        ChatFormatting.AQUA,      // 浅蓝
        ChatFormatting.GREEN,     // 黄绿
        ChatFormatting.GRAY,      // 浅灰
        ChatFormatting.DARK_GRAY, // 灰
        ChatFormatting.DARK_PURPLE, // 紫
        ChatFormatting.LIGHT_PURPLE  // 粉
    };

    public static ChatFormatting colorIndexToChatFormatting(int colorIndex) {
        if (colorIndex < 0 || colorIndex >= COLOR_COUNT) return ChatFormatting.WHITE;
        return INDEX_TO_CHAT_COLOR[colorIndex];
    }
}
