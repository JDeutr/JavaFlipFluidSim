package nl.jd.classes;

import java.util.concurrent.locks.LockSupport;

public class SimulationLoop {
    static final long TIME_STEP = 1_000_000_000L / 60;
    public static final float DT = TIME_STEP / 1e9f;

    private final int frameRate;
    private final Runnable update, render;
    private volatile boolean running;
    private volatile int measuredFps, measuredSteps;

    public SimulationLoop(int frameRate, Runnable update, Runnable render) {
        this.frameRate = frameRate;
        this.update = update;
        this.render = render;
    }

    public void start() {
        if (running) return;
        running = true;
        new Thread(this::physicsLoop, "physics").start();
        new Thread(this::renderLoop, "render").start();
    }

    public void stop() {
        running = false;
    }

    private void physicsLoop() {
        final long maxFrameNanos = 250_000_000L;
        long previous = System.nanoTime();
        long counterStart = previous;
        long accumulator = 0;
        int steps = 0;

        while (running) {
            long now = System.nanoTime();
            accumulator += Math.min(now - previous, maxFrameNanos);
            previous = now;

            while (accumulator >= TIME_STEP) {
                try {
                    update.run();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
                accumulator -= TIME_STEP;
                steps++;
            }

            if (now - counterStart >= 1_000_000_000L) {
                measuredSteps = steps;
                steps = 0;
                counterStart = now;
            }
            LockSupport.parkNanos(TIME_STEP - accumulator);
        }
    }

    private void renderLoop() {
        final long frameNanos = 1_000_000_000L / frameRate;
        long counterStart = System.nanoTime();
        int frames = 0;

        while (running) {
            long start = System.nanoTime();
            render.run();
            frames++;

            if (start - counterStart >= 1_000_000_000L) {
                measuredFps = frames;
                frames = 0;
                counterStart = start;
            }
            long remaining = frameNanos - (System.nanoTime() - start);
            if (remaining > 0) LockSupport.parkNanos(remaining);
        }
    }

    public int getFramesPerSecond() {
        return measuredFps;
    }

    public int getStepsPerSecond() {
        return measuredSteps;
    }
}
