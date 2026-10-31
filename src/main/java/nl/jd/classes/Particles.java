package nl.jd.classes;

import java.util.Arrays;

public class Particles {
    static final float GRAVITY = 200f;
    public final float[] x, y, vx, vy;
    public int count = 0;
    private volatile float[][] snapshot;

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

    public void remove(int i) {
        int last = count - 1;

        x[i] = x[last];
        y[i] = y[last];
        vx[i] = vx[last];
        vy[i] = vy[last];
        count--;
    }

    public void publish() {
        snapshot = new float[][]{Arrays.copyOf(x, count), Arrays.copyOf(y, count)};
    }

    public float[][] getSnapshot() {
        return snapshot;
    }

    public void step(float dt, int width, int height, Grid grid) {
        integrate(dt, width, height);
        markGridCells(grid);
        grid.p2g(x, y, vx, vy, count);
        publish();
    }

    private void integrate(float dt, int width, int height) {
        for (int i = 0; i < count; i++) {
            vy[i] += GRAVITY * dt;
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;

            borderChecks(width, height, i);
        }
    }

    private void borderChecks(int width, int height, int i) {
        // Wall Checks
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
            grid.setCellToType(row, col, Grid.FLUID);
        }

        grid.updateSnapshot();
    }
}