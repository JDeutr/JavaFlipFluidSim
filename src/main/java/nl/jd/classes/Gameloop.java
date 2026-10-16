package nl.jd.classes;

public class Gameloop implements Runnable {
    static final long TIME_STEP = 1_000_000_000L / 60; // in Nano Seconds
    static final float DT = TIME_STEP / 1e9f; // 60 Hz
    final int frameRate;
    private final Runnable update;
    private final Runnable render;
    volatile boolean running;
    volatile int measuredFps;
    volatile int measuredSteps;
    private Thread thread;

    Gameloop(int frameRate, Runnable update, Runnable render) {
        this.frameRate = frameRate;
        this.update = update;
        this.render = render;
    }

    @Override
    public void run() {
        final long maxFrameNanos = 250_000_000L;
        final long minFrameNanos = frameRate > 0 ? 1_000_000_000L / frameRate : 0;

        long previous = System.nanoTime();
        long accumulator = 0;

        int steps = 0, frames = 0;
        long counterStart = previous;

        while (running) {
            long frameStart = System.nanoTime();
            accumulator += Math.min(frameStart - previous, maxFrameNanos);
            previous = frameStart;

            while (accumulator >= TIME_STEP) {
                update.run();
                accumulator -= TIME_STEP;
                steps++;
            }

            render.run();
            frames++;

            if (frameStart - counterStart >= 1_000_000_000L) {
                measuredSteps = steps;
                measuredFps = frames;
                steps = 0;
                frames = 0;
                counterStart = frameStart;
            }

            long remaining = minFrameNanos - (System.nanoTime() - frameStart);
            if (remaining > 0) {
                try {
                    Thread.sleep(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }


    public void start() {
        if (running) return;
        running = true;
        thread = new Thread(this, "simulation loop");
        thread.start();
    }

    public void stop() {
        running = false;
    }

    public int getFramesPerSecond() {
        return frameRate;
    }

}
