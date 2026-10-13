package com.klangwerk.client;

import com.klangwerk.client.audio.ClientAudio;
import com.klangwerk.network.DownloadChunkPacket;
import com.klangwerk.network.DownloadRequestPacket;
import com.klangwerk.network.SongStore;
import com.klangwerk.network.UploadChunkPacket;
import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Client-Seite des Song-Transfers (Hochladen eigener MP3, Herunterladen fremder). */
public final class ClientNet {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Set<String> REQUESTED = ConcurrentHashMap.newKeySet();
    private static final Map<String, ByteArrayOutputStream> DOWNLOADS = new HashMap<>();

    private ClientNet() {}

    /** Der Server will meine MP3 haben. */
    public static void onUploadRequest(String hash) {
        Path f = Songs.pathByHash(hash);
        if (f == null) return;
        Thread t = new Thread(() -> {
            try {
                byte[] all = Files.readAllBytes(f);
                int count = Math.max(1, (all.length + SongStore.CHUNK - 1) / SongStore.CHUNK);
                for (int i = 0; i < count; i++) {
                    int from = i * SongStore.CHUNK, len = Math.min(SongStore.CHUNK, all.length - from);
                    byte[] part = new byte[len];
                    System.arraycopy(all, from, part, 0, len);
                    PacketDistributor.sendToServer(new UploadChunkPacket(hash, i, count, part));
                    if (i % 8 == 7) Thread.sleep(5);
                }
            } catch (Exception e) {
                LOG.error("Klangwerk: Hochladen fehlgeschlagen", e);
            }
        }, "Klangwerk-Upload");
        t.setDaemon(true);
        t.start();
    }

    /** Ein Stueck einer MP3 vom Server. */
    public static void onDownloadChunk(String hash, int index, int count, byte[] data) {
        ByteArrayOutputStream buf = DOWNLOADS.get(hash);
        if (index == 0) {
            buf = new ByteArrayOutputStream();
            DOWNLOADS.put(hash, buf);
        }
        if (buf == null) return;
        buf.writeBytes(data);
        if (index != count - 1) return;
        DOWNLOADS.remove(hash);
        try {
            byte[] all = buf.toByteArray();
            if (!SongStore.sha256(all).equals(hash)) {
                REQUESTED.remove(hash);
                return;
            }
            Files.createDirectories(Songs.cacheDir());
            Path tmp = Songs.cacheDir().resolve(hash + ".part");
            Files.write(tmp, all);
            Files.move(tmp, Songs.cacheDir().resolve(hash + ".mp3"), StandardCopyOption.REPLACE_EXISTING);
            ClientAudio.refresh();
        } catch (Exception e) {
            LOG.error("Klangwerk: Herunterladen fehlgeschlagen", e);
            REQUESTED.remove(hash);
        }
    }

    /** Fragt den Server einmal nach der Datei. */
    public static void request(String hash) {
        if (hash.isEmpty() || !REQUESTED.add(hash)) return;
        PacketDistributor.sendToServer(new DownloadRequestPacket(hash));
    }

    public static void reset() {
        REQUESTED.clear();
        DOWNLOADS.clear();
    }
}
