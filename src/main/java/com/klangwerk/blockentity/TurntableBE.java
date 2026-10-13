package com.klangwerk.blockentity;

import com.klangwerk.Channel;
import com.klangwerk.Config;
import com.klangwerk.ModRegistry;
import com.klangwerk.block.SpeakerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class TurntableBE extends AudioPlayerBE {

    private final Map<Channel, List<BlockPos>> speakers = new EnumMap<>(Channel.class);

    public TurntableBE(BlockPos pos, BlockState state) {
        super(ModRegistry.TURNTABLE_BE.get(), pos, state);
    }

    public Map<Channel, List<BlockPos>> speakers() { return speakers; }

    // ---- Kabel ----
    public boolean link(Channel ch, BlockPos sp) {
        List<BlockPos> l = speakers.computeIfAbsent(ch, k -> new ArrayList<>());
        if (l.contains(sp) || l.size() >= Config.MAX_SPEAKERS.get()) return false;
        l.add(sp.immutable());
        sync();
        return true;
    }

    public boolean unlinkAll(BlockPos sp) {
        boolean r = false;
        for (List<BlockPos> l : speakers.values()) r |= l.remove(sp);
        if (r) sync();
        return r;
    }

    public void clear(Channel ch) {
        speakers.remove(ch);
        sync();
    }

    @Override
    protected void tickExtra(long now) {
        if (now % 40 == 0) {
            boolean changed = false;
            for (List<BlockPos> l : speakers.values())
                changed |= l.removeIf(p -> level.isLoaded(p) && !(level.getBlockState(p).getBlock() instanceof SpeakerBlock));
            speakers.values().removeIf(List::isEmpty);
            if (changed) sync();
        }
        if (now % 100 == 0) setChanged();
    }

    // ---- Speichern ----
    @Override
    protected void saveAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.saveAdditional(t, reg);
        ListTag list = new ListTag();
        for (Map.Entry<Channel, List<BlockPos>> e : speakers.entrySet())
            for (BlockPos p : e.getValue()) {
                CompoundTag c = new CompoundTag();
                c.putInt("Ch", e.getKey().ordinal());
                c.putLong("Pos", p.asLong());
                list.add(c);
            }
        t.put("Speakers", list);
    }

    @Override
    protected void loadAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.loadAdditional(t, reg);
        speakers.clear();
        ListTag list = t.getList("Speakers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            int idx = c.getInt("Ch");
            if (idx < 0 || idx >= Channel.values().length) continue;
            speakers.computeIfAbsent(Channel.values()[idx], k -> new ArrayList<>()).add(BlockPos.of(c.getLong("Pos")));
        }
    }
}
