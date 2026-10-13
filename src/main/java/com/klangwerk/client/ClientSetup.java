package com.klangwerk.client;

import com.klangwerk.ModRegistry;
import com.klangwerk.blockentity.AudioPlayerBE;
import com.klangwerk.client.audio.ClientAudio;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public class ClientSetup {
    public static void init(IEventBus bus) {
        bus.addListener(ClientSetup::onClientSetup);
        bus.addListener(ClientSetup::registerRenderers);
        NeoForge.EVENT_BUS.addListener(ClientSetup::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(ClientSetup::onScreenInit);
    }

    private static void onClientSetup(FMLClientSetupEvent e) {
        Songs.ensureDir();
        AudioPlayerBE.CLIENT_TICK = ClientAudio::update;
        AudioPlayerBE.CLIENT_REMOVED = ClientAudio::removed;
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        // Turntable: Platte mittig bei (7|8), Gramophone: bei (6|8)
        e.registerBlockEntityRenderer(ModRegistry.TURNTABLE_BE.get(), ctx -> new PlayerRenderer<>(7f, 9.05f, 8f, 5.5f));
        e.registerBlockEntityRenderer(ModRegistry.GRAMOPHONE_BE.get(), ctx -> new PlayerRenderer<>(6f, 8.05f, 8f, 4f));
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut e) {
        ClientAudio.stopAll();
        ClientNet.reset();
    }

    /** Button "Klangwerk-Musik..." in Optionen > Musik & Geraeusche. */
    private static void onScreenInit(ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof SoundOptionsScreen screen)) return;
        e.addListener(Button.builder(Component.translatable("gui.klangwerk.music_button"),
                b -> net.minecraft.client.Minecraft.getInstance().setScreen(new MusicFolderScreen(screen)))
                .bounds(screen.width - 136, 6, 130, 20).build());
    }
}
