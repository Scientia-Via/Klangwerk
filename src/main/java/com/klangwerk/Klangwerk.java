package com.klangwerk;

import com.klangwerk.client.ClientSetup;
import com.klangwerk.network.NetworkHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Klangwerk.MODID)
public class Klangwerk {
    public static final String MODID = "klangwerk";

    public Klangwerk(IEventBus bus, ModContainer container) {
        ModRegistry.register(bus);
        bus.addListener(NetworkHandler::register);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) ClientSetup.init(bus);
    }
}
