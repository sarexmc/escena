package com.sarexstudio.cinematics.client;

import com.sarexstudio.cinematics.PlayScenePayload;
import net.minecraft.client.Minecraft;

public final class ClientHandler {
    private ClientHandler() {}

    public static void handle(PlayScenePayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (p.stop()) {
            if (mc.screen instanceof CinematicScreen cs) cs.finish();
            return;
        }
        mc.setScreen(new CinematicScreen(p.scene()));
    }
}
