package com.ron.match.network;

import com.ron.match.match.MatchSettings;
import com.ron.match.server.MatchServerState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateSettingsPacket {
    private final CompoundTag settingsTag;

    public UpdateSettingsPacket(MatchSettings settings) {
        this.settingsTag = settings.save();
    }

    public UpdateSettingsPacket(FriendlyByteBuf buf) {
        this.settingsTag = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(settingsTag);
    }

    public static void handle(UpdateSettingsPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || pkt.settingsTag == null) return;
            if (!com.ron.match.server.MatchServerEvents.canEditSettings(player)) return;
            MatchServerState.get().getSettings().load(pkt.settingsTag);
        });
        ctx.get().setPacketHandled(true);
    }
}
