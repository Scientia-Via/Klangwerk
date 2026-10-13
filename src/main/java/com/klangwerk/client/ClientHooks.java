package com.klangwerk.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public class ClientHooks {
    public static void openPlayerScreen(BlockPos pos) {
        Minecraft.getInstance().setScreen(new PlayerScreen(pos));
    }
}
