package com.klangwerk.network;

import com.klangwerk.Klangwerk;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> Server: "Ich brauche die MP3 mit diesem Hash". */
public record DownloadRequestPacket(String hash) implements CustomPacketPayload {
    public static final Type<DownloadRequestPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Klangwerk.MODID, "downloadrequestpacket"));
    public static final StreamCodec<FriendlyByteBuf, DownloadRequestPacket> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeUtf(p.hash, 64); },
            b -> new DownloadRequestPacket(b.readUtf(64)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
