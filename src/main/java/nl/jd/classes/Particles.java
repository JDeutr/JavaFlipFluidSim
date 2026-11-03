package nl.jd.classes;

import java.util.Arrays;

public class Particles {
    static final float DEFAULT_GRAVITY = 200f;
    static final float MIN_DIST = 1f;

    static final float DRAG_STRENGTH = 0.3f;
    static final float REPEL_ACCEL = 3000f;
    static final float MAX_POINTER_SPEED = 600f;

    public final float[] x, y, vx, vy;
    private final Interaction interaction = new Interaction();
    public int count = 0;
    private volatile float[][] snapshot;
    private volatile float gravity = DEFAULT_GRAVITY;
    private float prevPointerX, prevPointerY, pointerVx, pointerVy;
    private boolean pointerInitialised;

    public Particles(int capacity) {
        x = new float[capacity];
        y = new float[capacity];
        vx = new float[capacity];
        vy = new float[capacity];
    }

    public void add(float px, float py) {
        if (count == x.length) {
            System.out.println("CANNOT ADD: Particle max reached");
            return;
        }

        x[count] = px;
        y[count] = py;
        vx[count] = 0;
        vy[count] = 0;
        count++;
    }

    public void clear() {
        count = 0;
    }

    public void publish() {
        snapshot = new float[][]{Arrays.copyOf(x, count), Arrays.copyOf(y, count)};
    }

    public float[][] getSnapshot() {
        return snapshot;
    }

    public Interaction getInteraction() {
        return interaction;
    }

    public float getGravity() {
        return gravity;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public void step(float dt, int width, int height, Grid grid) {
        applyPointer(dt);
        integrate(dt, width, height);
        grid.guardParticles(x, y, vx, vy, count, width, height);

        grid.separateParticles(x, y, count, MIN_DIST, width, height);
        enforceBounds(width, height);
        grid.resolveSolidCollisions(x, y, vx, vy, count);

        markGridCells(grid);
        grid.p2g(x, y, vx, vy, count);
        grid.zeroSolidFaces();
        grid.saveBefore();

        grid.solvePressure();

        grid.applyFlip(x, y, vx, vy, count);

        grid.updateSnapshot();
        publish();
    }

    private void applyPointer(float dt) {
        float mx = interaction.x, my = interaction.y;
        int mode = interaction.mode;
        float radius = interaction.radius;

        if (!pointerInitialised) {
            prevPointerX = mx;
            prevPointerY = my;
            pointerInitialised = true;
        }

        float rawVx = (mx - prevPointerX) / dt;
        float rawVy = (my - prevPointerY) / dt;
        prevPointerX = mx;
        prevPointerY = my;

        float speed = (float) Math.sqrt(rawVx * rawVx + rawVy * rawVy);
        if (speed > MAX_POINTER_SPEED) {
            rawVx *= MAX_POINTER_SPEED / speed;
            rawVy *= MAX_POINTER_SPEED / speed;
        }
        pointerVx += (rawVx - pointerVx) * 0.3f;
        pointerVy += (rawVy - pointerVy) * 0.3f;

        if (mode != Interaction.DRAG && mode != Interaction.REPEL) return;

        float r2 = radius * radius;
        for (int i = 0; i < count; i++) {
            float dx = x[i] - mx;
            float dy = y[i] - my;
            float d2 = dx * dx + dy * dy;
            if (d2 >= r2) continue;
            float w = 1f - d2 / r2;

            if (mode == Interaction.DRAG) {
                float k = DRAG_STRENGTH * w;
                vx[i] += (pointerVx - vx[i]) * k;
                vy[i] += (pointerVy - vy[i]) * k;
            } else {
                float d = (float) Math.sqrt(d2);
                if (d < 1e-3f) continue;
                vx[i] += dx / d * REPEL_ACCEL * w * dt;
                vy[i] += dy / d * REPEL_ACCEL * w * dt;
            }
        }
    }

    private void integrate(float dt, int width, int height) {
        float g = gravity;
        for (int i = 0; i < count; i++) {
            vy[i] += g * dt;
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;

            borderChecks(width, height, i);
        }
    }

    private void enforceBounds(int width, int height) {
        for (int i = 0; i < count; i++) {
            borderChecks(width, height, i);
        }
    }

    private void borderChecks(int width, int height, int i) {
        if (x[i] < 0) {
            x[i] = 0;
            vx[i] = 0;
        }
        if (x[i] > width - 1) {
            x[i] = width - 1;
            vx[i] = 0;
        }
        if (y[i] < 0) {
            y[i] = 0;
            vy[i] = 0;
        }
        if (y[i] > height - 1) {
            y[i] = height - 1;
            vy[i] = 0;
        }
    }

    private void markGridCells(Grid grid) {
        grid.cellTypesToAir();

        for (int i = 0; i < count; i++) {
            int row = grid.getRow(y[i]);
            int col = grid.getCol(x[i]);
            grid.markFluid(row, col);
        }
    }
}