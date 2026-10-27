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

    public Gameloop(int frameRate, Runnable update, Runnable render) {
        this.frameRate = frameRate;
        this.update = update;
        this.render = render;
    }

    @Override
    public void run() {

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
