package com.klangwerk.network;

import com.klangwerk.Klangwerk;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> Server: ein Stueck der MP3. */
public record UploadChunkPacket(String hash, int index, int count, byte[] data) implements CustomPacketPayload {
    public static final Type<UploadChunkPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Klangwerk.MODID, "uploadchunkpacket"));
    public static final StreamCodec<FriendlyByteBuf, UploadChunkPacket> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeUtf(p.hash, 64); b.writeVarInt(p.index); b.writeVarInt(p.count); b.writeByteArray(p.data); },
            b -> new UploadChunkPacket(b.readUtf(64), b.readVarInt(), b.readVarInt(), b.readByteArray(SongStore.CHUNK + 64)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
