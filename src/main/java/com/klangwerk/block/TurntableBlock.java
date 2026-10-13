package com.klangwerk.block;

import com.klangwerk.Channel;
import com.klangwerk.ModRegistry;
import com.klangwerk.blockentity.AudioPlayerBE;
import com.klangwerk.blockentity.TurntableBE;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TurntableBlock extends AudioPlayerBlock {
    public static final MapCodec<TurntableBlock> CODEC = simpleCodec(TurntableBlock::new);

    public TurntableBlock(Properties props) {
        super(props, Block.box(0, 0, 0, 16, 10, 16));
    }

    @Override protected MapCodec<TurntableBlock> codec() { return CODEC; }
    @Override public BlockEntityType<? extends AudioPlayerBE> beType() { return ModRegistry.TURNTABLE_BE.get(); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TurntableBE(pos, state); }

    /** Welcher der 6 Ausgaenge auf der Rueckseite wurde getroffen? null = nicht die Rueckseite. */
    public static Channel portAt(BlockState st, BlockPos pos, Vec3 hit, Direction face) {
        Direction back = st.getValue(FACING).getOpposite();
        if (face != back) return null;
        double hx = hit.x - pos.getX(), hy = hit.y - pos.getY(), hz = hit.z - pos.getZ();
        if (hy > 0.5) return null;
        Direction right = back.getCounterClockWise();
        double u = (hx - 0.5) * right.getStepX() + (hz - 0.5) * right.getStepZ() + 0.5;
        int col = Mth.clamp((int) (u * 3), 0, 2);
        int row = hy > 0.25 ? 0 : 1;
        return Channel.at(col, row);
    }
}
