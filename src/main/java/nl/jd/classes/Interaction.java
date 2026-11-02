package nl.jd.classes;

public class Interaction {
    public static final int NONE = 0, DRAG = 1, REPEL = 2, OBSTACLE_ADD = 3, OBSTACLE_ERASE = 4;

    public volatile float x, y;
    public volatile float radius = 20f;
    public volatile float brushRadius = 6f;
    public volatile boolean ctrlDown;
    public volatile int mode = NONE;
    public volatile boolean hovering;
}