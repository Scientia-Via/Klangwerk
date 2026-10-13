package com.klangwerk.network;

import com.klangwerk.Config;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Server-Seite: nimmt die MP3 vom Spieler an, der den Titel startet, speichert sie (klangwerk-cache/)
 * und schickt sie an alle Mitspieler, die sie nicht haben. Nur der Starter braucht die Datei im Music-Ordner.
 */
public final class SongStore {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
    public static final int CHUNK = 30000;

    private static final Map<String, UUID> PENDING = new HashMap<>();          // hash -> Spieler, der hochladen soll
    private static final Map<String, ByteArrayOutputStream> UPLOADS = new HashMap<>();
    private static final Map<String, Set<UUID>> WAITING = new HashMap<>();     // hash -> Spieler, die warten

    private SongStore() {}

    public static boolean validHash(String h) { return h != null && HASH.matcher(h).matches(); }

    public static Path dir() { return FMLPaths.GAMEDIR.get().resolve("klangwerk-cache"); }

    private static Path file(String hash) { return dir().resolve(hash + ".mp3"); }

    private static boolean has(String hash) { return Files.isRegularFile(file(hash)); }

    /** Ein Spieler hat einen Titel gewaehlt: wenn der Server die Datei noch nicht hat, soll er sie hochladen. */
    public static void onSelect(ServerPlayer player, String hash) {
        if (has(hash)) return;
        PENDING.put(hash, player.getUUID());
        UPLOADS.remove(hash);
        PacketDistributor.sendToPlayer(player, new RequestUploadPacket(hash));
    }

    public static void onUploadChunk(UploadChunkPacket p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        String hash = p.hash();
        if (!validHash(hash) || !player.getUUID().equals(PENDING.get(hash))) return;
        long max = Config.MAX_SONG_MB.get() * 1024L * 1024L;
        if (p.count() <= 0 || p.count() > max / CHUNK + 2 || p.data().length > CHUNK) { abort(hash); return; }
        ByteArrayOutputStream buf = UPLOADS.get(hash);
        if (p.index() == 0) {
            buf = new ByteArrayOutputStream();
            UPLOADS.put(hash, buf);
        }
        if (buf == null) return;
        buf.writeBytes(p.data());
        if (buf.size() > max) { abort(hash); return; }
        if (p.index() != p.count() - 1) return;

        byte[] all = buf.toByteArray();
        abort(hash);
        try {
            if (!sha256(all).equals(hash)) { LOG.warn("Klangwerk: Upload mit falschem Hash verworfen"); return; }
            Files.createDirectories(dir());
            Path tmp = dir().resolve(hash + ".part");
            Files.write(tmp, all);
            Files.move(tmp, file(hash), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOG.error("Klangwerk: Song konnte nicht gespeichert werden", e);
            return;
        }
        Set<UUID> waiting = WAITING.remove(hash);
        if (waiting != null && player.getServer() != null)
            for (UUID id : waiting) {
                ServerPlayer sp = player.getServer().getPlayerList().getPlayer(id);
                if (sp != null) send(sp, hash, all);
            }
    }

    public static void onDownloadRequest(DownloadRequestPacket p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player) || !validHash(p.hash())) return;
        if (has(p.hash())) {
            try {
                send(player, p.hash(), Files.readAllBytes(file(p.hash())));
            } catch (IOException e) {
                LOG.error("Klangwerk: Song konnte nicht gelesen werden", e);
            }
        } else {
            WAITING.computeIfAbsent(p.hash(), k -> new HashSet<>()).add(player.getUUID());
        }
    }

    private static void abort(String hash) {
        UPLOADS.remove(hash);
        PENDING.remove(hash);
    }

    private static void send(ServerPlayer to, String hash, byte[] data) {
        int count = Math.max(1, (data.length + CHUNK - 1) / CHUNK);
        for (int i = 0; i < count; i++) {
            int from = i * CHUNK, len = Math.min(CHUNK, data.length - from);
            byte[] part = new byte[len];
            System.arraycopy(data, from, part, 0, len);
            PacketDistributor.sendToPlayer(to, new DownloadChunkPacket(hash, i, count, part));
        }
    }

    public static String sha256(byte[] data) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : d) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
