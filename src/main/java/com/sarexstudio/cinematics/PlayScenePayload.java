package com.sarexstudio.cinematics;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Servidor -> cliente: reproducir una escena o detener la actual. */
public record PlayScenePayload(String scene, boolean stop) implements CustomPacketPayload {
    public static final Type<PlayScenePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SarexStudio.MODID, "play_scene"));

    public static final StreamCodec<ByteBuf, PlayScenePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PlayScenePayload::scene,
            ByteBufCodecs.BOOL, PlayScenePayload::stop,
            PlayScenePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
