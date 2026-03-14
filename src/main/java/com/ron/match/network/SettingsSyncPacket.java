package com.ron.match.network;

import com.ron.match.match.MatchSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SettingsSyncPacket {
    private final CompoundTag settingsTag;

    public SettingsSyncPacket(MatchSettings settings) {
        this.settingsTag = settings.save();
    }

    public SettingsSyncPacket(FriendlyByteBuf buf) {
        this.settingsTag = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(settingsTag);
    }

    public static void handle(SettingsSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof com.ron.match.client.screen.MatchSettingsScreen screen) {
                screen.applyServerSettings(pkt.settingsTag);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
