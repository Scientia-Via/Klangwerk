package com.klangwerk;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_SONG_MB = B
            .comment("Maximale Groesse einer MP3, die ein Spieler zum Server hochladen darf (MB)")
            .defineInRange("maxSongMb", 32, 1, 256);
    public static final ModConfigSpec.IntValue CABLE_RANGE = B
            .comment("Maximale Kabellaenge in Bloecken")
            .defineInRange("maxCableLength", 32, 4, 128);
    public static final ModConfigSpec.IntValue HEAR_RANGE = B
            .comment("Hoerweite einer Box in Bloecken (Client)")
            .defineInRange("speakerRange", 32, 4, 128);
    public static final ModConfigSpec.IntValue MAX_SPEAKERS = B
            .comment("Maximale Anzahl Boxen pro Ausgang")
            .defineInRange("maxSpeakersPerOutput", 8, 1, 32);

    public static final ModConfigSpec SPEC = B.build();
}
