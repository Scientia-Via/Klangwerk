package com.klangwerk.client.audio;

import com.klangwerk.Channel;
import com.klangwerk.SpeakerType;
import com.mojang.logging.LogUtils;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Eine Box = eine Stimme: eigener MP3-Decoder, eigene OpenAL-Quelle (Mono, 3D-positioniert)
 * und ein Filter passend zum Boxtyp (Hoehen/Mitten/Bass).
 */
final class Voice {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int NBUF = 4, FRAMES = 6;

    final Path file;
    final Vec3 pos;
    final SpeakerType type;
    final Channel channel;
    final long startMs;
    final float weight;
    volatile boolean stop;
    volatile boolean finished;

    private int src = -1;
    private final int[] bufs = new int[NBUF];
    private final ArrayDeque<Integer> free = new ArrayDeque<>();
    private InputStream in;
    private Bitstream bs;
    private Decoder dec;
    private boolean eof;
    private List<Biquad> filters;

    Voice(Path file, Vec3 pos, SpeakerType type, Channel channel, long startMs, float weight) {
        this.file = file;
        this.pos = pos;
        this.type = type;
        this.channel = channel;
        this.startMs = startMs;
        this.weight = weight;
    }

    /** Wird nur vom Audio-Thread aufgerufen. */
    void pump() {
        if (finished) return;
        try {
            if (stop) { cleanup(); return; }
            if (src < 0 && !init()) { cleanup(); return; }

            int processed = AL10.alGetSourcei(src, AL10.AL_BUFFERS_PROCESSED);
            while (processed-- > 0) free.add(AL10.alSourceUnqueueBuffers(src));

            while (!free.isEmpty() && !eof) {
                int b = free.peek();
                if (!fill(b)) break;
                free.poll();
                AL10.alSourceQueueBuffers(src, b);
            }
            if (eof && free.size() == NBUF) { cleanup(); return; }
            if (free.size() < NBUF && AL10.alGetSourcei(src, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) AL10.alSourcePlay(src);

            Vec3 l = ClientAudio.listener;
            float att = (float) Math.max(0.0, 1.0 - l.distanceTo(pos) / ClientAudio.range);
            AL10.alSourcef(src, AL10.AL_GAIN, ClientAudio.volume * att * weight);
            AL10.alSource3f(src, AL10.AL_POSITION, (float) pos.x, (float) pos.y, (float) pos.z);
        } catch (Throwable t) {
            LOG.error("Klangwerk: Wiedergabefehler bei {}", file, t);
            cleanup();
        }
    }

    private boolean init() throws Exception {
        if (ALC10.alcGetCurrentContext() == 0L) return false; // Sound ist aus
        in = new BufferedInputStream(Files.newInputStream(file), 1 << 16);
        bs = new Bitstream(in);
        dec = new Decoder();
        double skipped = 0;
        while (skipped < startMs) {
            Header h = bs.readFrame();
            if (h == null) { eof = true; break; }
            skipped += h.ms_per_frame();
            bs.closeFrame();
        }
        src = AL10.alGenSources();
        for (int i = 0; i < NBUF; i++) {
            bufs[i] = AL10.alGenBuffers();
            free.add(bufs[i]);
        }
        AL10.alSourcei(src, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
        AL10.alSourcef(src, AL10.AL_ROLLOFF_FACTOR, 0f); // Lautstaerke nach Entfernung berechnen wir selbst
        AL10.alSourcef(src, AL10.AL_GAIN, 0f);
        return true;
    }

    private void buildFilters(int rate) {
        filters = new ArrayList<>();
        if (type.highpass > 0 && type.highpass < rate / 2.2) {
            filters.add(Biquad.highpass(type.highpass, rate));
            filters.add(Biquad.highpass(type.highpass, rate));
        }
        if (type.lowpass > 0 && type.lowpass < rate / 2.2) {
            filters.add(Biquad.lowpass(type.lowpass, rate));
            filters.add(Biquad.lowpass(type.lowpass, rate));
        }
    }

    private boolean fill(int buffer) throws Exception {
        short[] out = new short[FRAMES * 1152];
        int n = 0, rate = 44100;
        for (int f = 0; f < FRAMES; f++) {
            Header h = bs.readFrame();
            if (h == null) { eof = true; break; }
            SampleBuffer sb = (SampleBuffer) dec.decodeFrame(h, bs);
            bs.closeFrame();
            rate = sb.getSampleFrequency();
            if (filters == null) buildFilters(rate);
            short[] data = sb.getBuffer();
            int ch = Math.max(1, sb.getChannelCount());
            int frames = sb.getBufferLength() / ch;
            for (int i = 0; i < frames && n < out.length; i++) {
                float l = data[i * ch] / 32768f;
                float r = ch > 1 ? data[i * ch + 1] / 32768f : l;
                float x = channel.source == 0 ? l : channel.source == 1 ? r : (l + r) * 0.5f;
                for (Biquad q : filters) x = q.process(x);
                out[n++] = (short) Math.max(-32768, Math.min(32767, Math.round(x * 32767f)));
            }
        }
        if (n == 0) { eof = true; return false; }
        ShortBuffer sbuf = BufferUtils.createShortBuffer(n);
        sbuf.put(out, 0, n).flip();
        AL10.alBufferData(buffer, AL10.AL_FORMAT_MONO16, sbuf, rate);
        return true;
    }

    private void cleanup() {
        finished = true;
        try {
            if (src >= 0) {
                AL10.alSourceStop(src);
                AL10.alSourcei(src, AL10.AL_BUFFER, 0);
                AL10.alDeleteSources(src);
                for (int b : bufs) if (b != 0) AL10.alDeleteBuffers(b);
                src = -1;
            }
        } catch (Throwable ignored) {
        }
        try {
            if (bs != null) bs.close();
            if (in != null) in.close();
        } catch (Throwable ignored) {
        }
    }
}
