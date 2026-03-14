package com.ron.match.network;

import com.ron.match.match.MatchSettings;
import com.ron.match.match.PlayerPrepEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PreparationHallSyncPacket {
    private final CompoundTag settingsTag;
    private final List<PlayerPrepEntry> entries;
    private final boolean canEditSettings;
    private final int countdownTicksLeft;

    public PreparationHallSyncPacket(MatchSettings settings, List<PlayerPrepEntry> entries, boolean canEditSettings, int countdownTicksLeft) {
        this.settingsTag = settings.save();
        this.entries = entries;
        this.canEditSettings = canEditSettings;
        this.countdownTicksLeft = countdownTicksLeft;
    }

    public PreparationHallSyncPacket(FriendlyByteBuf buf) {
        this.settingsTag = buf.readNbt();
        int n = buf.readVarInt();
        this.entries = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            CompoundTag t = buf.readNbt();
            if (t != null) {
                PlayerPrepEntry e = new PlayerPrepEntry("");
                e.load(t);
                entries.add(e);
            }
        }
        this.canEditSettings = buf.readBoolean();
        this.countdownTicksLeft = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(settingsTag);
        buf.writeVarInt(entries.size());
        for (PlayerPrepEntry e : entries) {
            buf.writeNbt(e.save());
        }
        buf.writeBoolean(canEditSettings);
        buf.writeVarInt(countdownTicksLeft);
    }

    public static void handle(PreparationHallSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            String myName = mc.player != null ? mc.player.getName().getString() : null;
            boolean iAmInHall = myName != null && pkt.entries.stream().anyMatch(e -> e.getPlayerName().equals(myName));
            if (!iAmInHall) {
                mc.setScreen(null);
                return;
            }
            MatchSettings s = new MatchSettings();
            if (pkt.settingsTag != null) s.load(pkt.settingsTag);
            if (mc.screen instanceof com.ron.match.client.screen.PreparationHallScreen screen) {
                screen.applySync(pkt.settingsTag, pkt.entries, pkt.canEditSettings, pkt.countdownTicksLeft);
            } else {
                mc.setScreen(new com.ron.match.client.screen.PreparationHallScreen(s, pkt.entries, pkt.canEditSettings, pkt.countdownTicksLeft));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
