package com.klangwerk.blockentity;

import com.klangwerk.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class GramophoneBE extends AudioPlayerBE {
    public GramophoneBE(BlockPos pos, BlockState state) {
        super(ModRegistry.GRAMOPHONE_BE.get(), pos, state);
    }
}
