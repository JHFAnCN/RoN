package com.ron.match.network;

import com.ron.match.match.MatchSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenSettingsScreenPacket {
    private final CompoundTag settingsTag;

    public OpenSettingsScreenPacket(MatchSettings settings) {
        this.settingsTag = settings.save();
    }

    public OpenSettingsScreenPacket(FriendlyByteBuf buf) {
        this.settingsTag = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(settingsTag);
    }

    public static void handle(OpenSettingsScreenPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            MatchSettings s = new MatchSettings();
            if (pkt.settingsTag != null) s.load(pkt.settingsTag);
            Minecraft.getInstance().setScreen(new com.ron.match.client.screen.MatchSettingsScreen(s));
        });
        ctx.get().setPacketHandled(true);
    }
}
