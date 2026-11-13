package com.sarexstudio.cinematics.client;

import com.mojang.blaze3d.platform.NativeImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.LongSupplier;

/** Lee JPEGs concatenados en un hilo aparte; si el juego va lento descarta fotogramas (el audio no se atrasa). */
public class MjpegDecoder implements AutoCloseable {
    public record Frame(long index, NativeImage image) {}

    private final InputStream in;
    private final int w, h;
    private final double fps;
    private final LongSupplier clockNanos;
    private final BlockingQueue<Frame> ready = new ArrayBlockingQueue<>(3);
    private final BlockingQueue<NativeImage> pool = new ArrayBlockingQueue<>(4);
    private volatile boolean running = true;
    private volatile boolean ended = false;
    private final Thread thread;
    private int[] rgb;

    public MjpegDecoder(InputStream in, int w, int h, double fps, LongSupplier clockNanos) {
        this.in = new BufferedInputStream(in, 1 << 20);
        this.w = w; this.h = h; this.fps = fps; this.clockNanos = clockNanos;
        for (int i = 0; i < 4; i++) pool.add(new NativeImage(NativeImage.Format.RGBA, w, h, false));
        thread = new Thread(this::run, "SarexStudio-Decoder");
        thread.setDaemon(true);
        thread.start();
    }

    public boolean isEnded() { return ended && ready.isEmpty(); }

    public Frame latest(long expectedIndex) {
        Frame best = null, f;
        while ((f = ready.peek()) != null && f.index() <= expectedIndex) {
            ready.poll();
            if (best != null) pool.offer(best.image());
            best = f;
        }
        return best;
    }

    public void recycle(Frame f) { if (f != null) pool.offer(f.image()); }

    private long expected() { return (long) (clockNanos.getAsLong() / 1_000_000_000.0 * fps); }

    private void run() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(1 << 18);
        long index = 0;
        int prev = -1;
        try {
            int b;
            while (running && (b = in.read()) != -1) {
                buf.write(b);
                if (prev == 0xFF && b == 0xD9) {                  // fin de un JPEG
                    byte[] jpg = buf.toByteArray();
                    buf.reset();
                    prev = -1;
                    if (index + 1 < expected()) { index++; continue; }   // tarde: saltar
                    while (running && index > expected() + 2) Thread.sleep(4); // adelantado: esperar
                    BufferedImage bi = ImageIO.read(new ByteArrayInputStream(jpg));
                    if (bi != null) {
                        NativeImage img = pool.take();
                        fill(img, bi);
                        ready.put(new Frame(index, img));
                    }
                    index++;
                    continue;
                }
                prev = b;
            }
        } catch (InterruptedException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ended = true;
        }
    }

    private void fill(NativeImage img, BufferedImage bi) {
        int bw = Math.min(w, bi.getWidth()), bh = Math.min(h, bi.getHeight());
        if (rgb == null || rgb.length < bw * bh) rgb = new int[bw * bh];
        bi.getRGB(0, 0, bw, bh, rgb, 0, bw);
        for (int y = 0; y < bh; y++) {
            int row = y * bw;
            for (int x = 0; x < bw; x++) {
                int argb = rgb[row + x];
                img.setPixelRGBA(x, y, 0xFF000000 | ((argb & 0xFF) << 16) | (argb & 0xFF00) | ((argb >> 16) & 0xFF));
            }
        }
    }

    @Override
    public void close() {
        running = false;
        thread.interrupt();
        try { in.close(); } catch (IOException ignored) {}
        Frame f;
        while ((f = ready.poll()) != null) f.image().close();
        NativeImage n;
        while ((n = pool.poll()) != null) n.close();
    }
}
