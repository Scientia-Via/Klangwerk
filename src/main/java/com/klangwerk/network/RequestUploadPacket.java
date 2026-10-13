package com.klangwerk.network;

import com.klangwerk.Klangwerk;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> Client: "Schick mir bitte die MP3 mit diesem Hash". */
public record RequestUploadPacket(String hash) implements CustomPacketPayload {
    public static final Type<RequestUploadPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Klangwerk.MODID, "requestuploadpacket"));
    public static final StreamCodec<FriendlyByteBuf, RequestUploadPacket> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeUtf(p.hash, 64); },
            b -> new RequestUploadPacket(b.readUtf(64)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
