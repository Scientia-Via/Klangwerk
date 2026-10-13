package com.klangwerk.client.audio;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Header;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Mp3 {
    /** Laenge der MP3 in Millisekunden (liest nur die Frame-Header, dekodiert nichts). */
    public static long durationMs(Path file) {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file), 1 << 16)) {
            Bitstream bs = new Bitstream(in);
            double ms = 0;
            Header h;
            while ((h = bs.readFrame()) != null) {
                ms += h.ms_per_frame();
                bs.closeFrame();
            }
            return (long) ms;
        } catch (Exception e) {
            return 0;
        }
    }
}
