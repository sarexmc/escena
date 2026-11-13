package com.sarexstudio.cinematics.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.sarexstudio.cinematics.SarexStudio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Pantalla a pantalla completa: al cubrir todo el cliente, chat, hotbar y HUD no se ven. */
public class CinematicScreen extends Screen {
    /** true = ESC salta la escena. false = obligatoria (usa /sarexstudio detener). */
    private static final boolean SKIPPABLE = true;

    private final String scene;
    private MjpegDecoder decoder;
    private DynamicTexture texture;
    private ResourceLocation textureLoc;
    private SimpleSoundInstance sound;
    private int vw = 1280, vh = 720;
    private double fps = 24, duration = 0;
    private long startNanos;
    private boolean started, closing, hasFrame;

    public CinematicScreen(String scene) {
        super(Component.empty());
        this.scene = scene;
    }

    private long elapsed() { return System.nanoTime() - startNanos; }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(SarexStudio.MODID, path);
    }

    @Override
    protected void init() {
        if (started) return;
        started = true;
        Minecraft mc = Minecraft.getInstance();
        try {
            Optional<Resource> idx = mc.getResourceManager().getResource(rl("cinematics/index.json"));
            if (idx.isPresent()) {
                try (InputStream in = idx.get().open()) {
                    JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                            .getAsJsonObject().getAsJsonObject(scene);
                    if (o != null) {
                        fps = o.get("fps").getAsDouble();
                        vw = o.get("width").getAsInt();
                        vh = o.get("height").getAsInt();
                        duration = o.get("duration").getAsDouble();
                    }
                }
            }
            Optional<Resource> res = mc.getResourceManager().getResource(rl("cinematics/" + scene + ".mjpg"));
            if (res.isEmpty()) {
                if (mc.player != null) mc.player.displayClientMessage(
                        Component.literal("[SarexStudio] No encuentro la escena: " + scene), false);
                finish();
                return;
            }
            texture = new DynamicTexture(vw, vh, false);
            textureLoc = rl("dyn/" + scene);
            mc.getTextureManager().register(textureLoc, texture);

            startNanos = System.nanoTime();
            decoder = new MjpegDecoder(res.get().open(), vw, vh, fps, this::elapsed);

            mc.getMusicManager().stopPlaying();
            sound = SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(rl(scene)), 1.0f);
            mc.getSoundManager().play(sound);
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF000000);
        if (decoder == null || texture == null) return;

        long expected = (long) (elapsed() / 1_000_000_000.0 * fps);
        MjpegDecoder.Frame f = decoder.latest(expected);
        if (f != null) {
            NativeImage dst = texture.getPixels();
            if (dst != null) {
                dst.copyFrom(f.image());
                texture.upload();
                hasFrame = true;
            }
            decoder.recycle(f);
        }
        if (hasFrame) {
            double s = Math.min((double) width / vw, (double) height / vh);   // conserva proporcion
            int dw = (int) (vw * s), dh = (int) (vh * s);
            g.blit(textureLoc, (width - dw) / 2, (height - dh) / 2, dw, dh, 0, 0, vw, vh, vw, vh);
        }
        boolean timeUp = duration > 0 && elapsed() / 1e9 > duration + 0.3;
        if (decoder.isEnded() && (timeUp || duration <= 0)) finish();
    }

    @Override public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return SKIPPABLE; }
    @Override public void onClose() { finish(); }
    @Override public void removed() { if (!closing) finish(); }

    public void finish() {
        if (closing) return;
        closing = true;
        Minecraft mc = Minecraft.getInstance();
        if (decoder != null) decoder.close();
        if (sound != null) mc.getSoundManager().stop(sound);
        if (textureLoc != null) mc.getTextureManager().release(textureLoc);
        if (mc.screen == this) mc.setScreen(null);
    }
}
