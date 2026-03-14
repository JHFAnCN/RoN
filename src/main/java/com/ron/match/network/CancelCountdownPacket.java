package com.ron.match.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端请求终止开始游戏的倒计时。 */
public class CancelCountdownPacket {

    public CancelCountdownPacket() {}

    public CancelCountdownPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(CancelCountdownPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.ron.match.server.MatchServerEvents.cancelCountdown();
        });
        ctx.get().setPacketHandled(true);
    }
}
