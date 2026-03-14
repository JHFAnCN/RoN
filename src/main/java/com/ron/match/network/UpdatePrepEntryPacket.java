package com.ron.match.network;

import com.ron.match.match.PlayerPrepEntry;
import com.ron.match.server.MatchServerState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdatePrepEntryPacket {
    private final CompoundTag entryTag;

    public UpdatePrepEntryPacket(PlayerPrepEntry entry) {
        this.entryTag = entry.save();
    }

    public UpdatePrepEntryPacket(FriendlyByteBuf buf) {
        this.entryTag = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(entryTag);
    }

    public static void handle(UpdatePrepEntryPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || pkt.entryTag == null) return;
            String name = player.getName().getString();
            if (!pkt.entryTag.getString("playerName").equals(name)) return;
            PlayerPrepEntry entry = MatchServerState.get().getOrCreateEntry(name);
            entry.load(pkt.entryTag);
            int countdown = com.ron.match.server.MatchServerEvents.getCountdownTicksLeft();
            if (countdown > 0 && !entry.isReady()) {
                com.ron.match.server.MatchServerEvents.cancelCountdown();
            } else {
                com.ron.match.server.MatchServerEvents.broadcastPreparationHallSync();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
