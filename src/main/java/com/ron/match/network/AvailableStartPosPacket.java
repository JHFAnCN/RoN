package com.ron.match.network;

import com.ron.match.client.screen.PreparationHallScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AvailableStartPosPacket {
    private final int[] colorIds;

    public AvailableStartPosPacket(List<Integer> colorIds) {
        this.colorIds = colorIds.stream().mapToInt(i -> i).toArray();
    }

    public AvailableStartPosPacket(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        this.colorIds = new int[n];
        for (int i = 0; i < n; i++) colorIds[i] = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(colorIds.length);
        for (int id : colorIds) buf.writeVarInt(id);
    }

    public static void handle(AvailableStartPosPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            List<Integer> list = new ArrayList<>();
            for (int id : pkt.colorIds) list.add(id);
            if (Minecraft.getInstance().screen instanceof PreparationHallScreen screen) {
                screen.setAvailableStartPosColorIds(list);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
