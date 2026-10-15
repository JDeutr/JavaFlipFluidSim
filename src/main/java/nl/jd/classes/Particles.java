package nl.jd.classes;

class Particles {
    final float[] x, y, vx, vy;
    int count = 0;

    Particles(int capacity) {
        x = new float[capacity];
        y = new float[capacity];
        vx = new float[capacity];
        vy = new float[capacity];
    }

    void add(float px, float py) {
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

    void remove(int i) {
        int last = count - 1;

        x[i] = x[last];
        y[i] = y[last];
        vx[i] = vx[last];
        vy[i] = vy[last];
        count--;
    }
}