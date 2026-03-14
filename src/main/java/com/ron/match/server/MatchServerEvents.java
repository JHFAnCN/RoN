package com.ron.match.server;

import com.ron.match.RoN_Match;
import com.ron.match.match.MatchSettings;
import com.ron.match.match.MatchState;
import com.ron.match.match.PlayerPrepEntry;
import com.ron.match.network.*;
import com.ron.match.util.StartColorHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MatchServerEvents {
    private static int countdownTicksLeft = -1;
    private static final int RESET_CONFIRM_TICKS = 10 * 20; // 10 seconds
    private static String resetRequesterName = null;
    private static int resetConfirmTicksLeft = 0;
    /** 对局开始后：玩家名 -> 队伍名（用于单位发光描边），对局结束或重置时清空。 */
    private static final Map<String, String> playerTeamNameMap = new ConcurrentHashMap<>();

    public static boolean canEditSettings(ServerPlayer player) {
        return player != null && player.hasPermissions(2);
    }

    public static void openSettingsScreen(ServerPlayer player) {
        MatchState state = MatchServerState.get();
        MatchPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
            new OpenSettingsScreenPacket(state.getSettings()));
    }

    public static void joinPreparationHall(ServerPlayer target) {
        if (target == null) return;
        MatchState state = MatchServerState.get();
        if (state.isGameInProgress()) {
            target.sendSystemMessage(net.minecraft.network.chat.Component.literal("对局已开始，无法加入准备大厅。"));
            return;
        }
        state.getOrCreateEntry(target.getName().getString());
        broadcastPreparationHallSync();
        MatchPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> target),
            new OpenPreparationScreenPacket(state.getSettings(), state.getOrderedEntries(), canEditSettings(target), countdownTicksLeft));
    }

    public static void handleExitPreparation(ServerPlayer player) {
        if (player == null) return;
        MatchServerState.get().removePlayer(player.getName().getString());
        broadcastPreparationHallSync();
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("已退出准备大厅。"));
    }

    /**
     * 将指定玩家强行踢出准备大厅（仅当该玩家在准备大厅时生效）。被踢玩家会收到同步包并自动关闭准备页。
     */
    public static void kickFromPreparationHall(ServerPlayer executor, String targetName) {
        MatchState state = MatchServerState.get();
        if (state.isGameInProgress()) {
            if (executor != null) executor.sendSystemMessage(net.minecraft.network.chat.Component.literal("对局已开始，无法踢出。"));
            return;
        }
        if (!state.getPreparationPlayers().containsKey(targetName)) {
            if (executor != null) executor.sendSystemMessage(net.minecraft.network.chat.Component.literal("该玩家不在准备大厅。"));
            return;
        }
        state.removePlayer(targetName);
        broadcastPreparationHallSync();
        ServerPlayer target = executor != null && executor.getServer() != null
            ? executor.getServer().getPlayerList().getPlayerByName(targetName) : null;
        if (target != null) {
            target.sendSystemMessage(net.minecraft.network.chat.Component.literal("你已被踢出准备大厅。"));
        }
        if (executor != null) {
            executor.sendSystemMessage(net.minecraft.network.chat.Component.literal("已将 " + targetName + " 踢出准备大厅。"));
        }
    }

    public static int getCountdownTicksLeft() {
        return countdownTicksLeft;
    }

    /** 终止开始游戏的倒计时，并同步给所有准备页客户端。 */
    public static void cancelCountdown() {
        if (countdownTicksLeft <= 0) return;
        countdownTicksLeft = -1;
        broadcastPreparationHallSync();
    }

    public static void broadcastPreparationHallSync() {
        MatchState state = MatchServerState.get();
        List<PlayerPrepEntry> list = state.getOrderedEntries();
        boolean anyCanEdit = false;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (canEditSettings(p)) { anyCanEdit = true; break; }
            }
        }
        MatchPacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PreparationHallSyncPacket(
            state.getSettings(), list, anyCanEdit, countdownTicksLeft));
    }

    public static void handleRequestStartGame(ServerPlayer requester) {
        if (requester == null) return;
        MatchState state = MatchServerState.get();
        if (state.isGameInProgress()) {
            requester.sendSystemMessage(net.minecraft.network.chat.Component.literal("对局已进行中。"));
            return;
        }
        List<PlayerPrepEntry> entries = state.getOrderedEntries();
        if (entries.isEmpty()) {
            requester.sendSystemMessage(net.minecraft.network.chat.Component.literal("准备大厅为空。"));
            return;
        }
        long notReady = entries.stream().filter(e -> !e.isReady()).count();
        if (notReady > 0) {
            requester.sendSystemMessage(net.minecraft.network.chat.Component.literal("尚有玩家未准备。"));
            return;
        }
        MatchSettings settings = state.getSettings();
        if (entries.size() < settings.getMaxPlayers() && entries.size() < MatchSettings.MIN_PLAYERS) {
            // optional: require at least 2
            if (entries.size() < 2) {
                requester.sendSystemMessage(net.minecraft.network.chat.Component.literal("至少需要 2 名玩家才能开始。"));
                return;
            }
        }
        if (settings.isCountdownEnabled()) {
            int secs = Math.max(MatchSettings.COUNTDOWN_SECONDS_MIN, settings.getCountdownSeconds());
            countdownTicksLeft = secs * 20;
            requester.sendSystemMessage(net.minecraft.network.chat.Component.literal("游戏将在 " + secs + " 秒后开始。"));
        } else {
            executeGameStart();
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) return;
        if (countdownTicksLeft > 0) {
            int secondsLeft = countdownTicksLeft / 20;
            if (secondsLeft >= 1 && secondsLeft <= 5 && countdownTicksLeft % 20 == 0) {
                MinecraftServer srv = ServerLifecycleHooks.getCurrentServer();
                if (srv != null) {
                    net.minecraft.network.chat.Component msg = net.minecraft.network.chat.Component.literal("游戏即将开始: " + secondsLeft + " 秒");
                    for (ServerPlayer p : srv.getPlayerList().getPlayers()) {
                        p.sendSystemMessage(msg);
                    }
                }
            }
            if (countdownTicksLeft % 20 == 0) {
                broadcastPreparationHallSync();
            }
            countdownTicksLeft--;
            if (countdownTicksLeft == 0) {
                executeGameStart();
            }
        }
        if (resetConfirmTicksLeft > 0) {
            resetConfirmTicksLeft--;
            if (resetConfirmTicksLeft == 0) {
                resetRequesterName = null;
            }
        }
    }

    private static void executeGameStart() {
        countdownTicksLeft = -1;
        MatchState state = MatchServerState.get();
        state.setGameInProgress(true);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { state.setGameInProgress(false); return; }
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null) {
            RoN_Match.LOGGER.warn("RoN_Match: OVERWORLD is null, cannot start game.");
            state.setGameInProgress(false);
            return;
        }
        applyRonSettings(level, state.getSettings());
        List<PlayerPrepEntry> entries = state.getOrderedEntries();
        Map<String, ServerPlayer> nameToPlayer = new HashMap<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            nameToPlayer.put(p.getName().getString(), p);
        }
        List<com.solegendary.reignofnether.startpos.StartPos> startPoses = com.solegendary.reignofnether.startpos.StartPosServerEvents.startPoses;
        for (PlayerPrepEntry entry : entries) {
            ServerPlayer serverPlayer = nameToPlayer.get(entry.getPlayerName());
            if (serverPlayer == null) continue;
            com.solegendary.reignofnether.startpos.StartPos startPos = findStartPosByColor(startPoses, entry.getColorId());
            if (startPos == null) {
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("未找到颜色对应的出生点，无法开始。"));
                continue;
            }
            placeMainCityAtSpawn(serverPlayer, startPos, toRonFaction(entry.getFaction()));
        }
        setupTeamAlliances(entries);
        if (state.getSettings().isAllyControl()) {
            for (String name : nameToPlayer.keySet()) {
                com.solegendary.reignofnether.alliance.AlliancesServerEvents.playersWithAlliedControl.add(name);
                com.solegendary.reignofnether.alliance.AllianceClientboundPacket.setAllyControl(name, true);
            }
        }
        for (ServerPlayer p : nameToPlayer.values()) {
            if (!entries.stream().anyMatch(e -> e.getPlayerName().equals(p.getName().getString()))) continue;
            com.solegendary.reignofnether.player.PlayerServerEvents.enableOrthoview(p.getId());
            com.solegendary.reignofnether.player.PlayerServerEvents.openTopdownGui(p.getId());
            net.minecraft.core.BlockPos spawnPos = null;
            PlayerPrepEntry entry = entries.stream().filter(e -> e.getPlayerName().equals(p.getName().getString())).findFirst().orElse(null);
            if (entry != null) {
                com.solegendary.reignofnether.startpos.StartPos sp = findStartPosByColor(startPoses, entry.getColorId());
                if (sp != null) spawnPos = sp.pos;
            }
            MatchPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), new GameStartedClientPacket(spawnPos));
        }
        setupPlayerGlowTeams(server, entries);
        refreshExistingUnitsGlowTeams(level);
        state.reset();
    }

    /**
     * 路线1：绕过源模组「先选起始位置再选阵营再开始游戏」的限制，在开局后直接为玩家放置起始建筑。
     * 通过源模组 PlayerServerEvents.startRTS(..., startPosColorId) 实现：startPosColorId != 0 时
     * 源模组会在该位置放置主城（村民=城镇中心，怪物=陵墓，猪灵=中央传送门）并完成开局所需的其他初始化。
     * 传入非零 colorId 确保始终走「readied start」分支，自动放置主城；若地图出生点 colorId 为 0 则用默认颜色。
     */
    private static void placeMainCityAtSpawn(ServerPlayer player,
            com.solegendary.reignofnether.startpos.StartPos startPos,
            com.solegendary.reignofnether.faction.Faction faction) {
        net.minecraft.world.phys.Vec3 pos = net.minecraft.world.phys.Vec3.atCenterOf(startPos.pos);
        int colorIdForReadiedStart = startPos.colorId != 0
            ? startPos.colorId
            : StartColorHelper.colorIndexToMapColorId(0);
        com.solegendary.reignofnether.player.PlayerServerEvents.startRTS(
            player.getId(), pos, faction, colorIdForReadiedStart);
    }

    private static com.solegendary.reignofnether.startpos.StartPos findStartPosByColor(
            List<com.solegendary.reignofnether.startpos.StartPos> startPoses, int colorId) {
        int id = colorId >= 0 ? colorId : StartColorHelper.colorIndexToMapColorId(0);
        for (com.solegendary.reignofnether.startpos.StartPos sp : startPoses) {
            if (sp.colorId == id) return sp;
        }
        return null;
    }

    /**
     * 同队玩家自动结盟（使用源模组 AlliancesServerEvents.addAlliance）。
     * 源模组将同盟保存在服务端 Map，玩家掉线不会清除；重连时源模组 onPlayerJoin 会 syncAlliances()
     * 向所有客户端同步当前同盟，实现离线保护。同盟仅在匹配重置时清除。
     */
    private static void setupTeamAlliances(List<PlayerPrepEntry> entries) {
        for (int i = 0; i < entries.size(); i++) {
            PlayerPrepEntry a = entries.get(i);
            for (int j = i + 1; j < entries.size(); j++) {
                PlayerPrepEntry b = entries.get(j);
                if (a.getTeamIndex() == b.getTeamIndex()) {
                    com.solegendary.reignofnether.alliance.AlliancesServerEvents.addAlliance(
                        a.getPlayerName(), b.getPlayerName());
                }
            }
        }
    }

    private static void setupPlayerGlowTeams(MinecraftServer server, List<PlayerPrepEntry> entries) {
        Scoreboard sb = server.getScoreboard();
        for (PlayerPrepEntry entry : entries) {
            int colorId = entry.getColorId() >= 0 ? entry.getColorId() : StartColorHelper.colorIndexToMapColorId(0);
            int colorIndex = StartColorHelper.mapColorIdToIndex(colorId);
            if (colorIndex < 0) colorIndex = 0;
            String teamName = "ron_match_" + colorIndex;
            PlayerTeam team = sb.getPlayerTeam(teamName);
            if (team == null) {
                team = sb.addPlayerTeam(teamName);
                team.setColor(StartColorHelper.colorIndexToChatFormatting(colorIndex));
            }
            sb.addPlayerToTeam(entry.getPlayerName(), team);
            playerTeamNameMap.put(entry.getPlayerName(), teamName);
        }
    }

    /**
     * 新一局开始时，将场上已存在的单位按当前玩家颜色重新加入计分板队伍并设发光。
     * 修复：第一局玩家A蓝色、第二局玩家A黄色时，兵种颜色能立即变为黄色。
     */
    private static void refreshExistingUnitsGlowTeams(ServerLevel level) {
        if (level == null || playerTeamNameMap.isEmpty()) return;
        Scoreboard sb = level.getScoreboard();
        AABB wholeWorld = new AABB(-3e7, level.getMinBuildHeight(), -3e7, 3e7, level.getMaxBuildHeight(), 3e7);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, wholeWorld)) {
            if (!(entity instanceof com.solegendary.reignofnether.unit.interfaces.Unit unit)) continue;
            String ownerName = unit.getOwnerName();
            if (ownerName == null || ownerName.isEmpty()) continue;
            String teamName = playerTeamNameMap.get(ownerName);
            if (teamName == null) continue;
            PlayerTeam team = sb.getPlayerTeam(teamName);
            if (team != null) {
                sb.addPlayerToTeam(entity.getScoreboardName(), team);
                entity.setGlowingTag(true);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent evt) {
        Entity entity = evt.getEntity();
        if (!(entity instanceof com.solegendary.reignofnether.unit.interfaces.Unit unit)) return;
        if (entity.level().isClientSide()) return;
        String ownerName = unit.getOwnerName();
        if (ownerName == null || ownerName.isEmpty()) return;
        String teamName = playerTeamNameMap.get(ownerName);
        if (teamName == null) return;
        Scoreboard sb = evt.getLevel().getScoreboard();
        PlayerTeam team = sb.getPlayerTeam(teamName);
        if (team != null) {
            sb.addPlayerToTeam(entity.getScoreboardName(), team);
            entity.setGlowingTag(true);
        }
    }

    /**
     * 清除本模组创建的发光队伍；先移除队伍内所有玩家再删队，使玩家名颜色恢复白色。
     */
    private static void clearGlowTeams(MinecraftServer server) {
        if (server == null) return;
        Scoreboard sb = server.getScoreboard();
        for (int i = 0; i < StartColorHelper.COLOR_COUNT; i++) {
            PlayerTeam t = sb.getPlayerTeam("ron_match_" + i);
            if (t != null) {
                for (String member : new ArrayList<>(t.getPlayers())) {
                    sb.removePlayerFromTeam(member, t);
                }
                sb.removePlayerTeam(t);
            }
        }
        playerTeamNameMap.clear();
    }

    private static void applyRonSettings(ServerLevel level, MatchSettings settings) {
        try {
            var rule = level.getGameRules().getRule(com.solegendary.reignofnether.registrars.GameRuleRegistrar.MAX_POPULATION);
            if (rule != null) rule.set(settings.getMaxPopulation(), level.getServer());
            com.solegendary.reignofnether.unit.UnitServerEvents.maxPopulation = settings.getMaxPopulation();
        } catch (Throwable t) {
            RoN_Match.LOGGER.warn("RoN_Match: Could not set maxPopulation", t);
        }
        com.solegendary.reignofnether.player.PlayerServerEvents.rtsLocked = !settings.isAllowJoinAfterStart();
    }

    private static com.solegendary.reignofnether.faction.Faction toRonFaction(int faction) {
        switch (faction) {
            case PlayerPrepEntry.FACTION_VILLAGERS: return com.solegendary.reignofnether.faction.Faction.VILLAGERS;
            case PlayerPrepEntry.FACTION_MONSTERS: return com.solegendary.reignofnether.faction.Faction.MONSTERS;
            case PlayerPrepEntry.FACTION_PIGLINS: return com.solegendary.reignofnether.faction.Faction.PIGLINS;
            default:
                int r = new Random().nextInt(3);
                if (r == 0) return com.solegendary.reignofnether.faction.Faction.VILLAGERS;
                if (r == 1) return com.solegendary.reignofnether.faction.Faction.MONSTERS;
                return com.solegendary.reignofnether.faction.Faction.PIGLINS;
        }
    }

    public static void handleResetMatch(ServerPlayer player, boolean confirmed) {
        if (player == null) {
            if (confirmed) {
                clearGlowTeams(ServerLifecycleHooks.getCurrentServer());
                com.solegendary.reignofnether.alliance.AlliancesServerEvents.resetAllAlliances();
                com.solegendary.reignofnether.alliance.AlliancesServerEvents.playersWithAlliedControl.clear();
                MatchServerState.get().reset();
                resetRequesterName = null;
                resetConfirmTicksLeft = 0;
            }
            return;
        }
        String name = player.getName().getString();
        if (!confirmed) {
            resetRequesterName = name;
            resetConfirmTicksLeft = RESET_CONFIRM_TICKS;
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("请在 10 秒内再次执行 /rts-restmatch 以确认清空匹配信息。"));
            return;
        }
        if (resetRequesterName != null && !resetRequesterName.equals(name)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("只有发起者可以确认重置。"));
            return;
        }
        clearGlowTeams(ServerLifecycleHooks.getCurrentServer());
        com.solegendary.reignofnether.alliance.AlliancesServerEvents.resetAllAlliances();
        com.solegendary.reignofnether.alliance.AlliancesServerEvents.playersWithAlliedControl.clear();
        MatchServerState.get().reset();
        resetRequesterName = null;
        resetConfirmTicksLeft = 0;
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("已清空匹配信息。"));
    }

    public static void respondStartPosAvailability(ServerPlayer player) {
        if (player == null || player.level() instanceof ServerLevel == false) return;
        ServerLevel level = (ServerLevel) player.level();
        Set<Integer> colorIds = new HashSet<>();
        for (com.solegendary.reignofnether.startpos.StartPos sp : com.solegendary.reignofnether.startpos.StartPosServerEvents.startPoses) {
            if (level.getBlockState(sp.pos).getBlock() instanceof com.solegendary.reignofnether.blocks.RTSStartBlock) {
                colorIds.add(sp.colorId);
            }
        }
        List<Integer> list = colorIds.stream().sorted().collect(Collectors.toList());
        MatchPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new AvailableStartPosPacket(list));
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent evt) {
        if (evt.getEntity() instanceof ServerPlayer player) {
            MatchState state = MatchServerState.get();
            if (!state.isGameInProgress()) {
                state.removePlayer(player.getName().getString());
                broadcastPreparationHallSync();
            }
        }
    }

    public static void onGameEndFromRon() {
        clearGlowTeams(ServerLifecycleHooks.getCurrentServer());
        com.solegendary.reignofnether.alliance.AlliancesServerEvents.resetAllAlliances();
        com.solegendary.reignofnether.alliance.AlliancesServerEvents.playersWithAlliedControl.clear();
        MatchServerState.get().reset();
    }
}
