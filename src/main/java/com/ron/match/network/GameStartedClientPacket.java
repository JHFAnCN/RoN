package com.ron.match.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GameStartedClientPacket {
    private final BlockPos spawnPos;

    public GameStartedClientPacket(BlockPos spawnPos) {
        this.spawnPos = spawnPos;
    }

    public GameStartedClientPacket(FriendlyByteBuf buf) {
        this.spawnPos = buf.readBoolean() ? buf.readBlockPos() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(spawnPos != null);
        if (spawnPos != null) buf.writeBlockPos(spawnPos);
    }

    public static void handle(GameStartedClientPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.ron.match.client.MatchClientEvents.onGameStarted();
            if (pkt.spawnPos != null) {
                com.solegendary.reignofnether.orthoview.OrthoviewClientEvents.centreCameraOnPos(pkt.spawnPos);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
