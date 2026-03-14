package com.ron.match.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestResetMatchPacket {
    private final boolean confirmed;

    public RequestResetMatchPacket(boolean confirmed) { this.confirmed = confirmed; }
    public RequestResetMatchPacket(FriendlyByteBuf buf) { this.confirmed = buf.readBoolean(); }
    public void encode(FriendlyByteBuf buf) { buf.writeBoolean(confirmed); }

    public static void handle(RequestResetMatchPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> com.ron.match.server.MatchServerEvents.handleResetMatch(ctx.get().getSender(), pkt.confirmed));
        ctx.get().setPacketHandled(true);
    }
}
