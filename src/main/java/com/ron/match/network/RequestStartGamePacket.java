package com.ron.match.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestStartGamePacket {
    public RequestStartGamePacket() {}
    public RequestStartGamePacket(FriendlyByteBuf buf) {}
    public void encode(FriendlyByteBuf buf) {}

    public static void handle(RequestStartGamePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> com.ron.match.server.MatchServerEvents.handleRequestStartGame(ctx.get().getSender()));
        ctx.get().setPacketHandled(true);
    }
}
