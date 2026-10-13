package com.klangwerk.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/** Gemeinsame Logik von Grammophon und Plattenspieler: Titel, Position, Play/Pause, Spulen. */
public abstract class AudioPlayerBE extends BlockEntity {
    /** Werden vom Client-Setup gesetzt, damit dieser Code keine Client-Klassen laedt. */
    public static Consumer<AudioPlayerBE> CLIENT_TICK = be -> {};
    public static Consumer<AudioPlayerBE> CLIENT_REMOVED = be -> {};

    public enum Action { SELECT, PLAY_PAUSE, STOP, SEEK_REL, SEEK_ABS, LOOP }

    protected String song = "";
    protected boolean playing, loop;
    protected String hash = "";
    protected long basePos, baseTick, duration;
    protected int rev;

    protected AudioPlayerBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ---- Zustand ----
    public String song() { return song; }
    public boolean isPlaying() { return playing; }
    public boolean isLoop() { return loop; }
    public String hash() { return hash; }
    public long duration() { return duration; }
    public int rev() { return rev; }

    public long positionMs(long gameTime) {
        if (!playing) return basePos;
        long p = Math.max(basePos, basePos + (gameTime - baseTick) * 50L);
        return duration > 0 ? Math.min(p, duration) : p;
    }

    protected void tickExtra(long now) {}

    // ---- Steuerung (nur Server) ----
    public void control(Action a, long v, String s, String h) {
        if (level == null || level.isClientSide) return;
        long now = level.getGameTime();
        switch (a) {
            case SELECT -> {
                if (s.isBlank() || s.length() > 200 || s.contains("..")) return;
                song = s;
                hash = h;
                duration = Math.max(0, Math.min(v, 24L * 3600 * 1000));
                basePos = 0;
                start(now);
            }
            case PLAY_PAUSE -> {
                if (playing) {
                    basePos = positionMs(now);
                    playing = false;
                } else if (!song.isEmpty()) {
                    if (duration > 0 && basePos >= duration) basePos = 0;
                    start(now);
                }
            }
            case STOP -> {
                playing = false;
                basePos = 0;
                baseTick = now;
            }
            case SEEK_REL -> seekTo(positionMs(now) + v, now);
            case SEEK_ABS -> seekTo(v, now);
            case LOOP -> loop = !loop;
        }
        sync();
    }

    private void start(long now) {
        baseTick = now;
        playing = true;
    }

    private void seekTo(long p, long now) {
        if (duration > 0) p = Math.min(p, duration - 1);
        basePos = Math.max(0, p);
        baseTick = now;
    }

    public void serverTick() {
        if (level == null) return;
        long now = level.getGameTime();
        if (playing) {
            if (duration > 0 && basePos + (now - baseTick) * 50L >= duration) {
                if (loop) {
                    basePos = 0;
                    baseTick = now;
                } else {
                    playing = false;
                    basePos = 0;
                }
                sync();
            }
        }
        tickExtra(now);
    }

    public void clientTick() {
        CLIENT_TICK.accept(this);
    }

    protected void sync() {
        rev++;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // ---- Speichern / Netzwerk ----
    @Override
    protected void saveAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.saveAdditional(t, reg);
        t.putString("Song", song);
        t.putBoolean("Playing", playing);
        t.putBoolean("Loop", loop);
        t.putString("Hash", hash);
        t.putLong("BasePos", basePos);
        t.putLong("BaseTick", baseTick);
        t.putLong("Duration", duration);
        t.putInt("Rev", rev);
    }

    @Override
    protected void loadAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.loadAdditional(t, reg);
        song = t.getString("Song");
        playing = t.getBoolean("Playing");
        loop = t.getBoolean("Loop");
        hash = t.getString("Hash");
        basePos = t.getLong("BasePos");
        baseTick = t.getLong("BaseTick");
        duration = t.getLong("Duration");
        rev = t.getInt("Rev");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
        return saveWithoutMetadata(reg);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) CLIENT_REMOVED.accept(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && level.isClientSide) CLIENT_REMOVED.accept(this);
    }
}
