package com.ron.match.match;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.BitSet;

/**
 * 对局设置（设置页面）
 */
public class MatchSettings {
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 10000;
    public static final int DEFAULT_PLAYERS = 10;
    public static final int MIN_TEAM_MEMBERS = 1;
    public static final int MAX_TEAM_MEMBERS = 10;
    public static final int DEFAULT_TEAM_MEMBERS = 1;
    public static final int DEFAULT_MAX_POPULATION = 150;
    public static final int MIN_POPULATION = 1;
    public static final int MAX_POPULATION = 99999;
    public static final int COUNTDOWN_SECONDS_MIN = 5;
    public static final int COUNTDOWN_SECONDS_MAX = 300;
    public static final int COUNTDOWN_SECONDS_DEFAULT = 30;
    public static final int COLOR_COUNT = 16;

    private int maxPlayers = DEFAULT_PLAYERS;
    private int maxTeamMembers = DEFAULT_TEAM_MEMBERS;
    private int maxPopulation = DEFAULT_MAX_POPULATION;
    private boolean allowJoinAfterStart = true;
    private boolean countdownEnabled = true;
    private int countdownSeconds = COUNTDOWN_SECONDS_DEFAULT;
    /** 开放的队伍颜色：index 0..15 对应 16 种颜色，true 表示可选 */
    private final BitSet openColors = new BitSet(COLOR_COUNT);
    private boolean friendlyFire = false;
    private boolean allyControl = false;

    public MatchSettings() {
        openColors.set(0, COLOR_COUNT);
    }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int v) { this.maxPlayers = Math.max(MIN_PLAYERS, Math.min(MAX_PLAYERS, v)); }

    public int getMaxTeamMembers() { return maxTeamMembers; }
    public void setMaxTeamMembers(int v) { this.maxTeamMembers = Math.max(MIN_TEAM_MEMBERS, Math.min(MAX_TEAM_MEMBERS, v)); }

    public int getMaxPopulation() { return maxPopulation; }
    public void setMaxPopulation(int v) { this.maxPopulation = Math.max(MIN_POPULATION, Math.min(MAX_POPULATION, v)); }

    public boolean isAllowJoinAfterStart() { return allowJoinAfterStart; }
    public void setAllowJoinAfterStart(boolean v) { this.allowJoinAfterStart = v; }

    public boolean isCountdownEnabled() { return countdownEnabled; }
    public void setCountdownEnabled(boolean v) { this.countdownEnabled = v; }

    public int getCountdownSeconds() { return countdownSeconds; }
    public void setCountdownSeconds(int v) { this.countdownSeconds = Math.max(COUNTDOWN_SECONDS_MIN, Math.min(COUNTDOWN_SECONDS_MAX, v)); }

    public boolean isColorOpen(int index) { return index >= 0 && index < COLOR_COUNT && openColors.get(index); }
    public void setColorOpen(int index, boolean open) { if (index >= 0 && index < COLOR_COUNT) openColors.set(index, open); }
    public void toggleColorOpen(int index) { if (index >= 0 && index < COLOR_COUNT) openColors.flip(index); }

    public boolean isFriendlyFire() { return friendlyFire; }
    public void setFriendlyFire(boolean v) { this.friendlyFire = v; }

    public boolean isAllyControl() { return allyControl; }
    public void setAllyControl(boolean v) { this.allyControl = v; }

    public void copyFrom(MatchSettings other) {
        maxPlayers = other.maxPlayers;
        maxTeamMembers = other.maxTeamMembers;
        maxPopulation = other.maxPopulation;
        allowJoinAfterStart = other.allowJoinAfterStart;
        countdownEnabled = other.countdownEnabled;
        countdownSeconds = other.countdownSeconds;
        openColors.clear();
        openColors.or(other.openColors);
        friendlyFire = other.friendlyFire;
        allyControl = other.allyControl;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("maxPlayers", maxPlayers);
        tag.putInt("maxTeamMembers", maxTeamMembers);
        tag.putInt("maxPopulation", maxPopulation);
        tag.putBoolean("allowJoinAfterStart", allowJoinAfterStart);
        tag.putBoolean("countdownEnabled", countdownEnabled);
        tag.putInt("countdownSeconds", countdownSeconds);
        tag.putByteArray("openColors", openColors.toByteArray());
        tag.putBoolean("friendlyFire", friendlyFire);
        tag.putBoolean("allyControl", allyControl);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("maxPlayers")) setMaxPlayers(tag.getInt("maxPlayers"));
        if (tag.contains("maxTeamMembers")) setMaxTeamMembers(tag.getInt("maxTeamMembers"));
        if (tag.contains("maxPopulation")) setMaxPopulation(tag.getInt("maxPopulation"));
        if (tag.contains("allowJoinAfterStart")) allowJoinAfterStart = tag.getBoolean("allowJoinAfterStart");
        if (tag.contains("countdownEnabled")) countdownEnabled = tag.getBoolean("countdownEnabled");
        if (tag.contains("countdownSeconds")) setCountdownSeconds(tag.getInt("countdownSeconds"));
        if (tag.contains("openColors")) {
            byte[] arr = tag.getByteArray("openColors");
            openColors.clear();
            if (arr.length > 0) openColors.or(BitSet.valueOf(arr));
            openColors.set(COLOR_COUNT, false);
        }
        if (tag.contains("friendlyFire")) friendlyFire = tag.getBoolean("friendlyFire");
        if (tag.contains("allyControl")) allyControl = tag.getBoolean("allyControl");
    }
}
