package com.klangwerk.client.audio;

import com.klangwerk.Channel;
import com.klangwerk.Config;
import com.klangwerk.SpeakerType;
import com.klangwerk.block.SpeakerBlock;
import com.klangwerk.blockentity.AudioPlayerBE;
import com.klangwerk.blockentity.GramophoneBE;
import com.klangwerk.blockentity.TurntableBE;
import com.klangwerk.client.ClientNet;
import com.klangwerk.client.Songs;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/** Client-Seite: verwaltet die Stimmen (Voices) aller Player in Reichweite und den Audio-Thread. */
public final class ClientAudio {
    private static final Logger LOG = LogUtils.getLogger();
    private static final List<Voice> VOICES = new CopyOnWriteArrayList<>();
    private static final Map<BlockPos, State> STATES = new HashMap<>();
    private static Thread thread;

    static volatile Vec3 listener = Vec3.ZERO;
    static volatile float volume = 1f;
    static volatile float range = 32f;

    private static final class State {
        int rev = -1;
        boolean failed, waiting;
        List<Voice> voices = new ArrayList<>();
    }

    private ClientAudio() {}

    /** Jeden Client-Tick fuer jeden Player-Block aufgerufen. */
    public static void update(AudioPlayerBE be) {
        Minecraft mc = Minecraft.getInstance();
        Level level = be.getLevel();
        if (level == null || mc.level != level || mc.player == null) return;

        listener = mc.gameRenderer.getMainCamera().getPosition();
        volume = mc.options.getSoundSourceVolume(SoundSource.MASTER) * mc.options.getSoundSourceVolume(SoundSource.RECORDS);
        range = Config.HEAR_RANGE.get();

        State st = STATES.computeIfAbsent(be.getBlockPos().immutable(), k -> new State());
        if (st.rev != be.rev()) {
            stop(st);
            st.rev = be.rev();
            st.failed = false;
            st.waiting = false;
        }
        boolean want = be.isPlaying() && !be.song().isEmpty() && !st.failed && !st.waiting;
        if (!want) {
            stop(st);
            return;
        }
        double reach = range + (be instanceof TurntableBE ? Config.CABLE_RANGE.get() : 0);
        double d = listener.distanceTo(Vec3.atCenterOf(be.getBlockPos()));
        if (st.voices.isEmpty() && d < reach + 8) start(be, st);
        else if (!st.voices.isEmpty() && d > reach + 16) stop(st);
    }

    private static void start(AudioPlayerBE be, State st) {
        Path file = Songs.fileFor(be.song(), be.hash());
        if (file == null) {
            if (!be.hash().isEmpty()) {
                ClientNet.request(be.hash()); // Datei kommt vom Server, danach startet refresh() die Wiedergabe
                st.waiting = true;
            } else {
                LOG.warn("Klangwerk: '{}' wurde nicht im Ordner Music gefunden.", be.song());
                st.failed = true;
            }
            return;
        }
        Level level = be.getLevel();
        long pos = be.positionMs(level.getGameTime());
        List<Voice> vs = new ArrayList<>();

        if (be instanceof TurntableBE tb) {
            for (Map.Entry<Channel, List<BlockPos>> e : tb.speakers().entrySet()) {
                Map<SpeakerType, Integer> count = new EnumMap<>(SpeakerType.class);
                List<Map.Entry<BlockPos, SpeakerType>> found = new ArrayList<>();
                for (BlockPos sp : e.getValue()) {
                    BlockState bs = level.getBlockState(sp);
                    if (bs.getBlock() instanceof SpeakerBlock sb) {
                        found.add(Map.entry(sp, sb.type));
                        count.merge(sb.type, 1, Integer::sum);
                    }
                }
                for (Map.Entry<BlockPos, SpeakerType> f : found) {
                    float w = (float) (1.0 / Math.sqrt(count.get(f.getValue())));
                    vs.add(new Voice(file, Vec3.atCenterOf(f.getKey()), f.getValue(), e.getKey(), pos, w));
                }
            }
        } else if (be instanceof GramophoneBE) {
            vs.add(new Voice(file, Vec3.atCenterOf(be.getBlockPos()).add(0, 0.4, 0), SpeakerType.GRAMOPHONE, Channel.CENTER, pos, 1f));
        }
        st.voices = vs;
        VOICES.addAll(vs);
        ensureThread();
    }

    private static void stop(State st) {
        for (Voice v : st.voices) v.stop = true;
        st.voices = new ArrayList<>();
    }

    public static void removed(AudioPlayerBE be) {
        State st = STATES.remove(be.getBlockPos());
        if (st != null) stop(st);
    }

    /** Eine heruntergeladene Datei ist fertig: wartende Player neu starten. */
    public static void refresh() {
        for (State st : STATES.values()) st.waiting = false;
    }

    public static void stopAll() {
        for (State st : STATES.values()) stop(st);
        STATES.clear();
    }

    private static synchronized void ensureThread() {
        if (thread != null && thread.isAlive()) return;
        thread = new Thread(ClientAudio::loop, "Klangwerk-Audio");
        thread.setDaemon(true);
        thread.start();
    }

    private static void loop() {
        while (true) {
            try {
                if (VOICES.isEmpty()) {
                    Thread.sleep(50);
                    continue;
                }
                for (Voice v : VOICES) {
                    v.pump();
                    if (v.finished) VOICES.remove(v);
                }
                Thread.sleep(10);
            } catch (InterruptedException e) {
                return;
            } catch (Throwable t) {
                LOG.error("Klangwerk: Audio-Thread-Fehler", t);
            }
        }
    }
}
