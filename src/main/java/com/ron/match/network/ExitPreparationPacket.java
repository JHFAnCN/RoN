package com.ron.match.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ExitPreparationPacket {
    public ExitPreparationPacket() {}
    public ExitPreparationPacket(FriendlyByteBuf buf) {}
    public void encode(FriendlyByteBuf buf) {}

    public static void handle(ExitPreparationPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> com.ron.match.server.MatchServerEvents.handleExitPreparation(ctx.get().getSender()));
        ctx.get().setPacketHandled(true);
    }
}
