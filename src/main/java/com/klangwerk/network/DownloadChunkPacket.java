package com.klangwerk.network;

import com.klangwerk.Klangwerk;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> Client: ein Stueck der MP3. */
public record DownloadChunkPacket(String hash, int index, int count, byte[] data) implements CustomPacketPayload {
    public static final Type<DownloadChunkPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Klangwerk.MODID, "downloadchunkpacket"));
    public static final StreamCodec<FriendlyByteBuf, DownloadChunkPacket> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeUtf(p.hash, 64); b.writeVarInt(p.index); b.writeVarInt(p.count); b.writeByteArray(p.data); },
            b -> new DownloadChunkPacket(b.readUtf(64), b.readVarInt(), b.readVarInt(), b.readByteArray(SongStore.CHUNK + 64)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
