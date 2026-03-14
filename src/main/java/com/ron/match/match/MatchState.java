package com.ron.match.match;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端匹配状态：设置 + 准备大厅玩家列表。
 * 游戏开始后由 MatchGameStart 使用并可在对局结束时重置。
 */
public class MatchState {
    private final MatchSettings settings = new MatchSettings();
    private final Map<String, PlayerPrepEntry> preparationPlayers = new ConcurrentHashMap<>();
    private boolean gameInProgress;

    public MatchSettings getSettings() { return settings; }
    public Map<String, PlayerPrepEntry> getPreparationPlayers() { return preparationPlayers; }
    public boolean isGameInProgress() { return gameInProgress; }
    public void setGameInProgress(boolean v) { this.gameInProgress = v; }

    public PlayerPrepEntry getOrCreateEntry(String playerName) {
        return preparationPlayers.computeIfAbsent(playerName, PlayerPrepEntry::new);
    }

    public void removePlayer(String playerName) {
        preparationPlayers.remove(playerName);
    }

    public List<PlayerPrepEntry> getOrderedEntries() {
        List<PlayerPrepEntry> list = new ArrayList<>(preparationPlayers.values());
        list.sort(Comparator.comparing(PlayerPrepEntry::getPlayerName));
        return list;
    }

    public Set<Integer> getUsedColorIds() {
        Set<Integer> set = new HashSet<>();
        for (PlayerPrepEntry e : preparationPlayers.values()) {
            if (e.getColorId() >= 0) set.add(e.getColorId());
        }
        return set;
    }

    /** 重置匹配信息（对局结束或 /rts-restmatch） */
    public void reset() {
        preparationPlayers.clear();
        gameInProgress = false;
    }

    public void load(CompoundTag tag) {
        settings.load(tag.getCompound("settings"));
        preparationPlayers.clear();
        ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            PlayerPrepEntry e = new PlayerPrepEntry("");
            e.load(list.getCompound(i));
            if (!e.getPlayerName().isEmpty())
                preparationPlayers.put(e.getPlayerName(), e);
        }
        gameInProgress = tag.getBoolean("gameInProgress");
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("settings", settings.save());
        ListTag list = new ListTag();
        for (PlayerPrepEntry e : preparationPlayers.values()) {
            list.add(e.save());
        }
        tag.put("players", list);
        tag.putBoolean("gameInProgress", gameInProgress);
        return tag;
    }
}
