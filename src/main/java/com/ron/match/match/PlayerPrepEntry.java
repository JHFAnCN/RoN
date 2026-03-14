package com.ron.match.match;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.MapColor;

/**
 * 准备大厅中单个玩家的条目
 */
public class PlayerPrepEntry {
    public static final int FACTION_VILLAGERS = 0;
    public static final int FACTION_MONSTERS = 1;
    public static final int FACTION_PIGLINS = 2;
    public static final int FACTION_RANDOM = 3;

    private String playerName;
    private int teamIndex;      // 0=A, 1=B, ... 25=Z
    private int colorId;        // MapColor.id，未选可为 -1
    private int faction;        // FACTION_*
    private boolean ready;

    public PlayerPrepEntry(String playerName) {
        this.playerName = playerName;
        this.teamIndex = 0;
        this.colorId = -1;
        this.faction = FACTION_RANDOM;
        this.ready = false;
    }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String v) { this.playerName = v; }

    public int getTeamIndex() { return teamIndex; }
    public void setTeamIndex(int v) { this.teamIndex = Math.max(0, Math.min(25, v)); }
    public char getTeamLetter() { return (char) ('A' + teamIndex); }

    public int getColorId() { return colorId; }
    public void setColorId(int v) { this.colorId = v; }

    public int getFaction() { return faction; }
    public void setFaction(int v) { this.faction = Math.max(0, Math.min(3, v)); }

    public boolean isReady() { return ready; }
    public void setReady(boolean v) { this.ready = v; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("playerName", playerName);
        tag.putInt("teamIndex", teamIndex);
        tag.putInt("colorId", colorId);
        tag.putInt("faction", faction);
        tag.putBoolean("ready", ready);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("playerName")) playerName = tag.getString("playerName");
        if (tag.contains("teamIndex")) setTeamIndex(tag.getInt("teamIndex"));
        if (tag.contains("colorId")) colorId = tag.getInt("colorId");
        if (tag.contains("faction")) setFaction(tag.getInt("faction"));
        if (tag.contains("ready")) ready = tag.getBoolean("ready");
    }
}
