package com.ron.match.network;

import com.ron.match.RoN_Match;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class MatchPacketHandler {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(RoN_Match.MOD_ID, "main"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );
    private static int id;

    public static void register() {
        INSTANCE.registerMessage(id++, SettingsSyncPacket.class, SettingsSyncPacket::encode, SettingsSyncPacket::new, SettingsSyncPacket::handle);
        INSTANCE.registerMessage(id++, UpdateSettingsPacket.class, UpdateSettingsPacket::encode, UpdateSettingsPacket::new, UpdateSettingsPacket::handle);
        INSTANCE.registerMessage(id++, OpenSettingsScreenPacket.class, OpenSettingsScreenPacket::encode, OpenSettingsScreenPacket::new, OpenSettingsScreenPacket::handle);
        INSTANCE.registerMessage(id++, PreparationHallSyncPacket.class, PreparationHallSyncPacket::encode, PreparationHallSyncPacket::new, PreparationHallSyncPacket::handle);
        INSTANCE.registerMessage(id++, OpenPreparationScreenPacket.class, OpenPreparationScreenPacket::encode, OpenPreparationScreenPacket::new, OpenPreparationScreenPacket::handle);
        INSTANCE.registerMessage(id++, UpdatePrepEntryPacket.class, UpdatePrepEntryPacket::encode, UpdatePrepEntryPacket::new, UpdatePrepEntryPacket::handle);
        INSTANCE.registerMessage(id++, RequestStartGamePacket.class, RequestStartGamePacket::encode, RequestStartGamePacket::new, RequestStartGamePacket::handle);
        INSTANCE.registerMessage(id++, CancelCountdownPacket.class, CancelCountdownPacket::encode, CancelCountdownPacket::new, CancelCountdownPacket::handle);
        INSTANCE.registerMessage(id++, ExitPreparationPacket.class, ExitPreparationPacket::encode, ExitPreparationPacket::new, ExitPreparationPacket::handle);
        INSTANCE.registerMessage(id++, GameStartedClientPacket.class, GameStartedClientPacket::encode, GameStartedClientPacket::new, GameStartedClientPacket::handle);
        INSTANCE.registerMessage(id++, RequestResetMatchPacket.class, RequestResetMatchPacket::encode, RequestResetMatchPacket::new, RequestResetMatchPacket::handle);
        INSTANCE.registerMessage(id++, CheckStartPosPacket.class, CheckStartPosPacket::encode, CheckStartPosPacket::new, CheckStartPosPacket::handle);
        INSTANCE.registerMessage(id++, AvailableStartPosPacket.class, AvailableStartPosPacket::encode, AvailableStartPosPacket::new, AvailableStartPosPacket::handle);
    }
}
