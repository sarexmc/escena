package com.sarexstudio.cinematics;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class SarexStudio implements ModInitializer {
    public static final String MODID = "sarexstudio";

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(PlayScenePayload.TYPE, PlayScenePayload.CODEC);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                SarexCommands.register(dispatcher));
    }
}
