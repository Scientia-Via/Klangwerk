package com.klangwerk.client;

import com.klangwerk.network.SongStore;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/** Der Ordner "Music" im Spielverzeichnis (root) mit den MP3-Dateien + Cache fuer Songs von anderen Spielern. */
public class Songs {
    private record Hashed(long size, long modified, String hash) {}
    private static final Map<Path, Hashed> HASHES = new ConcurrentHashMap<>();
    private static final Map<String, Path> BY_HASH = new ConcurrentHashMap<>();

    public static Path dir() {
        return FMLPaths.GAMEDIR.get().resolve("Music");
    }

    public static void ensureDir() {
        try {
            Files.createDirectories(dir());
        } catch (IOException ignored) {
        }
    }

    public static boolean isMp3(String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(".mp3");
    }

    /** Alle MP3-Dateien (relativ zu Music/). */
    public static List<String> list() {
        return listAll().stream().filter(Songs::isMp3).toList();
    }

    /** Alle Dateien im Ordner (auch Nicht-MP3), relativ zu Music/. */
    public static List<String> listAll() {
        Path base = dir();
        try (Stream<Path> s = Files.walk(base, 4)) {
            return s.filter(Files::isRegularFile)
                    .map(p -> base.relativize(p).toString().replace('\\', '/'))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Wandelt einen Songnamen in eine Datei um (nur innerhalb von Music/). null = nicht vorhanden. */
    public static Path resolve(String name) {
        if (name == null || name.isBlank()) return null;
        Path base = dir().toAbsolutePath().normalize();
        Path p = base.resolve(name).normalize();
        if (!p.startsWith(base) || !Files.isRegularFile(p)) return null;
        return p;
    }

    /** SHA-256 einer Datei (zwischengespeichert, bis sich die Datei aendert). */
    public static String hashOf(Path f) {
        try {
            long size = Files.size(f), mod = Files.getLastModifiedTime(f).toMillis();
            Hashed h = HASHES.get(f);
            if (h == null || h.size != size || h.modified != mod) {
                h = new Hashed(size, mod, SongStore.sha256(Files.readAllBytes(f)));
                HASHES.put(f, h);
            }
            BY_HASH.put(h.hash, f);
            return h.hash;
        } catch (Exception e) {
            return "";
        }
    }

    /** Lokale Datei zu einem Hash (aus dem Music-Ordner, falls schon berechnet). */
    public static Path pathByHash(String hash) {
        Path p = BY_HASH.get(hash);
        return p != null && Files.isRegularFile(p) ? p : null;
    }

    public static Path cacheDir() {
        return SongStore.dir();
    }

    /** Findet die Datei fuer einen Song: erst Music/ (wenn identisch), dann Cache. null = muss vom Server geladen werden. */
    public static Path fileFor(String name, String hash) {
        Path local = resolve(name);
        if (hash == null || hash.isEmpty()) return local;
        if (local != null && hashOf(local).equals(hash)) return local;
        Path c = cacheDir().resolve(hash + ".mp3");
        return Files.isRegularFile(c) ? c : null;
    }
}
