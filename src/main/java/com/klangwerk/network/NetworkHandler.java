package com.klangwerk.network;

import com.klangwerk.client.ClientNet;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {
    private static final String VERSION = "2";

    public static void register(RegisterPayloadHandlersEvent e) {
        PayloadRegistrar reg = e.registrar(VERSION);
        reg.playToServer(ControlPacket.TYPE, ControlPacket.STREAM_CODEC, ControlPacket::handle);
        reg.playToServer(UploadChunkPacket.TYPE, UploadChunkPacket.STREAM_CODEC, SongStore::onUploadChunk);
        reg.playToServer(DownloadRequestPacket.TYPE, DownloadRequestPacket.STREAM_CODEC, SongStore::onDownloadRequest);
        reg.playToClient(RequestUploadPacket.TYPE, RequestUploadPacket.STREAM_CODEC,
                (p, ctx) -> ClientNet.onUploadRequest(p.hash()));
        reg.playToClient(DownloadChunkPacket.TYPE, DownloadChunkPacket.STREAM_CODEC,
                (p, ctx) -> ClientNet.onDownloadChunk(p.hash(), p.index(), p.count(), p.data()));
    }
}
