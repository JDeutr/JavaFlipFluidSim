package nl.jd.classes;

public class Gameloop implements Runnable{
    static final long TIME_STEP = 1_000_000_000L/60; // in Nano Seconds
    static final float DT = TIME_STEP / 1e9f; // 60 Hz
    volatile boolean running;
    final int frameRate;
    private Thread thread;
    private final Runnable update;
    private final Runnable render;

    Gameloop(int frameRate, Runnable update, Runnable render) {
        this.frameRate = frameRate;
        this.update = update;
        this.render = render;
        this.running = true;
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


    public int getFramesPerSecond() {
        return frameRate; }

}
