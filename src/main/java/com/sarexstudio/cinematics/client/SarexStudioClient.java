package com.sarexstudio.cinematics.client;

import com.sarexstudio.cinematics.PlayScenePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class SarexStudioClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PlayScenePayload.TYPE,
                (payload, context) -> ClientHandler.handle(payload));
    }
}
