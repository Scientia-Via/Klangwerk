package com.klangwerk.network;

import com.klangwerk.Klangwerk;
import com.klangwerk.blockentity.AudioPlayerBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> Server: Bedienung des Players (Titel waehlen, Play/Pause, Spulen ...). hash = SHA-256 der MP3 (nur bei SELECT). */
public record ControlPacket(BlockPos pos, AudioPlayerBE.Action action, long value, String text, String hash) implements CustomPacketPayload {
    public static final Type<ControlPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Klangwerk.MODID, "control"));

    public static final StreamCodec<FriendlyByteBuf, ControlPacket> STREAM_CODEC = StreamCodec.of(ControlPacket::write, ControlPacket::read);

    private static void write(FriendlyByteBuf b, ControlPacket p) {
        b.writeBlockPos(p.pos);
        b.writeEnum(p.action);
        b.writeLong(p.value);
        b.writeUtf(p.text, 256);
        b.writeUtf(p.hash, 64);
    }

    private static ControlPacket read(FriendlyByteBuf b) {
        return new ControlPacket(b.readBlockPos(), b.readEnum(AudioPlayerBE.Action.class), b.readLong(), b.readUtf(256), b.readUtf(64));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Laeuft auf dem Server-Haupt-Thread. */
    public static void handle(ControlPacket p, IPayloadContext ctx) {
        Player pl = ctx.player();
        if (!(pl instanceof ServerPlayer player)) return;
        Level level = player.level();
        if (!level.isLoaded(p.pos) || player.distanceToSqr(Vec3.atCenterOf(p.pos)) > 100) return;
        if (level.getBlockEntity(p.pos) instanceof AudioPlayerBE be) {
            String h = SongStore.validHash(p.hash) ? p.hash : "";
            be.control(p.action, p.value, p.text, h);
            if (p.action == AudioPlayerBE.Action.SELECT && !h.isEmpty()) SongStore.onSelect(player, h);
        }
    }
}
