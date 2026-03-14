package com.ron.match.network;

import com.ron.match.server.MatchServerEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端收到后检测当前维度所有 RTS 出生点方块，并回发可用 colorId 列表 */
public class CheckStartPosPacket {
    public CheckStartPosPacket() {}
    public CheckStartPosPacket(FriendlyByteBuf buf) {}
    public void encode(FriendlyByteBuf buf) {}

    public static void handle(CheckStartPosPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> MatchServerEvents.respondStartPosAvailability(ctx.get().getSender()));
        ctx.get().setPacketHandled(true);
    }
}
