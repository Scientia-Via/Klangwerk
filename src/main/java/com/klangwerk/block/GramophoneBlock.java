package com.klangwerk.block;

import com.klangwerk.ModRegistry;
import com.klangwerk.blockentity.AudioPlayerBE;
import com.klangwerk.blockentity.GramophoneBE;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class GramophoneBlock extends AudioPlayerBlock {
    public static final MapCodec<GramophoneBlock> CODEC = simpleCodec(GramophoneBlock::new);

    public GramophoneBlock(Properties props) {
        super(props, Block.box(1, 0, 1, 15, 16, 15));
    }

    @Override protected MapCodec<GramophoneBlock> codec() { return CODEC; }
    @Override public BlockEntityType<? extends AudioPlayerBE> beType() { return ModRegistry.GRAMOPHONE_BE.get(); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GramophoneBE(pos, state); }
}
