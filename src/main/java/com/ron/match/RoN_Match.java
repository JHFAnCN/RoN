package com.ron.match;

import com.ron.match.network.MatchPacketHandler;
import com.ron.match.registration.CommandRegistration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(RoN_Match.MOD_ID)
public class RoN_Match {
    public static final String MOD_ID = "ron_match";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public RoN_Match(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        MatchPacketHandler.register();
        if (FMLEnvironment.dist.isClient()) {
            modBus.register(com.ron.match.client.ClientSetup.class);
        }
        MinecraftForge.EVENT_BUS.register(CommandRegistration.class);
        MinecraftForge.EVENT_BUS.register(com.ron.match.server.MatchServerEvents.class);
    }
}
