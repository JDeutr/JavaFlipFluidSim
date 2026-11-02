package nl.jd.classes;

import java.util.Arrays;

public class Grid {
    public static final int SOLID = 2, FLUID = 1, AIR = 0;
    private final int rows, cols, cellSize;
    private final float[] u, v, uWeight, vWeight;
    private final float[] uBefore, vBefore;
    private final float[] pressure, density;
    private final int[] cellType;
    private volatile int[] snapshotType;
    private volatile float[] snapshotU, snapshotV;

    private int pressureIterations = 50;
    private float divergenceThreshold = 1e-3f;
    private float flipRatio = 0.95f;
    private float overRelaxation = 1.9f;
    private float driftStiffness = 1f;   // tune: higher = stronger push-apart of over-dense cells
    private float restDensity = 0f;      // measured on the first frame

    // spatial hash scratch buffers for separateParticles
    private int[] cellStart, cellEntries;

    public Grid(int simWidth, int simHeight) {
        this.cellSize = 6;
        this.cols = simWidth / cellSize + 2;
        this.rows = simHeight / cellSize + 2;
        this.cellType = new int[rows * cols];
        this.u = new float[rows * (cols + 1)];
        this.v = new float[(rows + 1) * cols];
        this.uWeight = new float[rows * (cols + 1)];
        this.vWeight = new float[(rows + 1) * cols];
        this.uBefore = new float[rows * (cols + 1)];
        this.vBefore = new float[(rows + 1) * cols];
        this.pressure = new float[rows * cols];
        this.density = new float[rows * cols];

        initSolidBorder();
        cellTypesToAir();
    }

    private void initSolidBorder() {
        for (int col = 0; col < cols; col++) {
            cellType[getCellIndex(0, col)] = SOLID;
            cellType[getCellIndex(rows - 1, col)] = SOLID;
        }
        for (int row = 0; row < rows; row++) {
            cellType[getCellIndex(row, 0)] = SOLID;
            cellType[getCellIndex(row, cols - 1)] = SOLID;
        }
    }

    public void cellTypesToAir() {
        for (int i = 0; i < cellType.length; i++) {
            if (cellType[i] != SOLID) {
                cellType[i] = AIR;
            }
        }
    }

    // ------------------------------------------------------------------
    // Indexing / accessors
    // ------------------------------------------------------------------

    public int getCellIndex(int row, int col) {
        return row * cols + col;
    }

    public void setCellToType(int row, int col, int type) {
        if (row > 0 && row < rows - 1 && col > 0 && col < cols - 1) {
            cellType[getCellIndex(row, col)] = type;
        }
    }

    public int getUIndex(int row, int col) {
        return row * (cols + 1) + col;
    }

    public int getVIndex(int row, int col) {
        return row * cols + col;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getCellSize() {
        return cellSize;
    }

    public int getCol(float x) {
        return (int) (x / cellSize) + 1;
    }

    public int getRow(float y) {
        return (int) (y / cellSize) + 1;
    }

    public int getCellType(int index) {
        return cellType[index];
    }

    public float[] getPressure() {
        return pressure;
    }

    public float[] getDensity() {
        return density;
    }

    public float[] getUBefore() {
        return uBefore;
    }

    public float[] getVBefore() {
        return vBefore;
    }

    public float[] getUWeights() {
        return uWeight;
    }

    public float[] getVWeights() {
        return vWeight;
    }

    // ------------------------------------------------------------------
    // Render snapshot
    // ------------------------------------------------------------------

    public void updateSnapshot() {
        snapshotType = Arrays.copyOf(cellType, cellType.length);
        snapshotU = Arrays.copyOf(u, u.length);
        snapshotV = Arrays.copyOf(v, v.length);
    }

    public int[] getSnapshotType() {
        return snapshotType;
    }

    public float[] getSnapshotU() {
        return snapshotU;
    }

    public float[] getSnapshotV() {
        return snapshotV;
    }

    // ------------------------------------------------------------------
    // Particles -> grid
    // ------------------------------------------------------------------

    public void resetWeights() {
        Arrays.fill(uWeight, 0f);
        Arrays.fill(vWeight, 0f);
    }

    public void p2g(float[] px, float[] py, float[] pvx, float[] pvy, int count) {
        Arrays.fill(u, 0f);
        Arrays.fill(v, 0f);
        resetWeights();
        for (int i = 0; i < count; i++) {
            accumulateParticle(px[i], py[i], pvx[i], pvy[i]);
        }
        normalize();
        updateDensity(px, py, count);
    }

    private void accumulateParticle(float px, float py, float pvx, float pvy) {
        float gx = px / cellSize + 1;
        float gy = py / cellSize + 1;
        accumulateU(pvx, gx, gy);
        accumulateV(pvy, gx, gy);
    }

    private void accumulateU(float pvx, float gx, float gy) {
        int col0 = (int) gx;
        float tx = gx - col0;
        int row0 = (int) (gy - 0.5f);
        float ty = (gy - 0.5f) - row0;

        float wTL = (1f - tx) * (1f - ty);
        float wTR = tx * (1f - ty);
        float wBL = (1f - tx) * ty;
        float wBR = tx * ty;

        int uTL = getUIndex(row0, col0);
        int uTR = getUIndex(row0, col0 + 1);
        int uBL = getUIndex(row0 + 1, col0);
        int uBR = getUIndex(row0 + 1, col0 + 1);

        u[uTL] += pvx * wTL;
        uWeight[uTL] += wTL;
        u[uTR] += pvx * wTR;
        uWeight[uTR] += wTR;
        u[uBL] += pvx * wBL;
        uWeight[uBL] += wBL;
        u[uBR] += pvx * wBR;
        uWeight[uBR] += wBR;
    }

    private void accumulateV(float pvy, float gx, float gy) {
        int col0 = (int) (gx - 0.5f);
        float tx = (gx - 0.5f) - col0;
        int row0 = (int) gy;
        float ty = gy - row0;

        float wTL = (1f - tx) * (1f - ty);
        float wTR = tx * (1f - ty);
        float wBL = (1f - tx) * ty;
        float wBR = tx * ty;

        int vTL = getVIndex(row0, col0);
        int vTR = getVIndex(row0, col0 + 1);
        int vBL = getVIndex(row0 + 1, col0);
        int vBR = getVIndex(row0 + 1, col0 + 1);

        v[vTL] += pvy * wTL;
        vWeight[vTL] += wTL;
        v[vTR] += pvy * wTR;
        vWeight[vTR] += wTR;
        v[vBL] += pvy * wBL;
        vWeight[vBL] += wBL;
        v[vBR] += pvy * wBR;
        vWeight[vBR] += wBR;
    }

    private void normalize() {
        for (int i = 0; i < u.length; i++) {
            if (uWeight[i] > 0) u[i] /= uWeight[i];
        }
        for (int i = 0; i < v.length; i++) {
            if (vWeight[i] > 0) v[i] /= vWeight[i];
        }
    }

    /** Bilinear particle density at cell centres; rest density is measured on the first frame. */
    private void updateDensity(float[] px, float[] py, int count) {
        Arrays.fill(density, 0f);
        for (int i = 0; i < count; i++) {
            // cell centres sit at (col + 0.5, row + 0.5) in grid space
            float x = px[i] / cellSize + 0.5f;
            float y = py[i] / cellSize + 0.5f;
            int x0 = (int) x, y0 = (int) y;
            float tx = x - x0, ty = y - y0;
            addDensity(y0, x0, (1f - tx) * (1f - ty));
            addDensity(y0, x0 + 1, tx * (1f - ty));
            addDensity(y0 + 1, x0, (1f - tx) * ty);
            addDensity(y0 + 1, x0 + 1, tx * ty);
        }

        if (restDensity == 0f && count > 0) {
            float sum = 0f;
            int n = 0;
            for (int i = 0; i < density.length; i++) {
                if (cellType[i] == FLUID) {
                    sum += density[i];
                    n++;
                }
            }
            if (n > 0) restDensity = sum / n;
        }
    }

    private void addDensity(int row, int col, float w) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            density[getCellIndex(row, col)] += w;
        }
    }

    // ------------------------------------------------------------------
    // Grid housekeeping
    // ------------------------------------------------------------------

    /** Zero every face that touches a SOLID cell. */
    public void zeroSolidFaces() {
        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col < cols; col++) {
                int idx = getUIndex(row, col);
                if (cellType[getCellIndex(row, col - 1)] == SOLID
                        || cellType[getCellIndex(row, col)] == SOLID) {
                    u[idx] = 0f;
                }
            }
        }
        for (int row = 1; row < rows; row++) {
            for (int col = 1; col < cols - 1; col++) {
                int idx = getVIndex(row, col);
                if (cellType[getCellIndex(row - 1, col)] == SOLID
                        || cellType[getCellIndex(row, col)] == SOLID) {
                    v[idx] = 0f;
                }
            }
        }
    }

    /** Store the grid velocities before the pressure solve (needed for the FLIP delta). */
    public void saveBefore() {
        System.arraycopy(u, 0, uBefore, 0, u.length);
        System.arraycopy(v, 0, vBefore, 0, v.length);
    }

    // ------------------------------------------------------------------
    // Pressure solve (Gauss-Seidel, FLUID cells only)
    // ------------------------------------------------------------------

    /** @return the largest correction applied in the last iteration (convergence indicator). */
    public float solvePressure() {
        Arrays.fill(pressure, 0f);
        float lastMax = 0f;

        for (int iter = 0; iter < pressureIterations; iter++) {
            float localMax = 0f;

            for (int row = 1; row < rows - 1; row++) {
                for (int col = 1; col < cols - 1; col++) {
                    int ci = getCellIndex(row, col);
                    if (cellType[ci] != FLUID) continue;   // AIR cells are the free surface (p = 0)

                    boolean openL = cellType[ci - 1] != SOLID;
                    boolean openR = cellType[ci + 1] != SOLID;
                    boolean openU = cellType[ci - cols] != SOLID;
                    boolean openD = cellType[ci + cols] != SOLID;
                    int s = (openL ? 1 : 0) + (openR ? 1 : 0) + (openU ? 1 : 0) + (openD ? 1 : 0);
                    if (s == 0) continue;

                    int uLeft = getUIndex(row, col);
                    int uRight = getUIndex(row, col + 1);
                    int vAbove = getVIndex(row, col);
                    int vBelow = getVIndex(row + 1, col);

                    float div = u[uRight] - u[uLeft] + v[vBelow] - v[vAbove];

                    // drift compensation: ask over-dense cells to expand
                    if (restDensity > 0f) {
                        float compression = density[ci] - restDensity;
                        if (compression > 0f) div -= driftStiffness * compression;
                    }

                    float correction = overRelaxation * div / s;
                    pressure[ci] += correction;

                    // never move faces that touch SOLID
                    if (openL) u[uLeft] += correction;
                    if (openR) u[uRight] -= correction;
                    if (openU) v[vAbove] += correction;
                    if (openD) v[vBelow] -= correction;

                    float abs = Math.abs(correction);
                    if (abs > localMax) localMax = abs;
                }
            }

            lastMax = localMax;
            if (localMax < divergenceThreshold) break;
        }
        return lastMax;
    }

    /** Debug helper: worst divergence over FLUID cells. */
    public float computeMaxDivergence() {
        float max = 0f;
        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col < cols - 1; col++) {
                if (cellType[getCellIndex(row, col)] != FLUID) continue;
                float div = u[getUIndex(row, col + 1)] - u[getUIndex(row, col)]
                        + v[getVIndex(row + 1, col)] - v[getVIndex(row, col)];
                float abs = Math.abs(div);
                if (abs > max) max = abs;
            }
        }
        return max;
    }

    // ------------------------------------------------------------------
    // Grid -> particles (FLIP / PIC blend)
    // ------------------------------------------------------------------

    public void applyFlip(float[] px, float[] py, float[] pvx, float[] pvy, int count) {
        for (int i = 0; i < count; i++) {
            float gx = px[i] / cellSize + 1;
            float gy = py[i] / cellSize + 1;

            float newU = sampleU(u, gx, gy);
            float oldU = sampleU(uBefore, gx, gy);
            float newV = sampleV(v, gx, gy);
            float oldV = sampleV(vBefore, gx, gy);

            pvx[i] = flipRatio * (pvx[i] + newU - oldU) + (1f - flipRatio) * newU;
            pvy[i] = flipRatio * (pvy[i] + newV - oldV) + (1f - flipRatio) * newV;

            if (Float.isNaN(pvx[i]) || Float.isInfinite(pvx[i])) pvx[i] = 0f;
            if (Float.isNaN(pvy[i]) || Float.isInfinite(pvy[i])) pvy[i] = 0f;
        }
    }

    private float sampleU(float[] f, float gx, float gy) {
        int c0 = (int) gx, r0 = (int) (gy - 0.5f);
        float tx = gx - c0, ty = (gy - 0.5f) - r0;
        float sum = 0f, w = 0f;
        for (int dr = 0; dr <= 1; dr++) {
            for (int dc = 0; dc <= 1; dc++) {
                int r = r0 + dr, c = c0 + dc;
                if (!validU(r, c)) continue;
                float wt = (dc == 0 ? 1f - tx : tx) * (dr == 0 ? 1f - ty : ty);
                sum += f[getUIndex(r, c)] * wt;
                w += wt;
            }
        }
        return w > 0f ? sum / w : 0f;
    }

    private float sampleV(float[] f, float gx, float gy) {
        int c0 = (int) (gx - 0.5f), r0 = (int) gy;
        float tx = (gx - 0.5f) - c0, ty = gy - r0;
        float sum = 0f, w = 0f;
        for (int dr = 0; dr <= 1; dr++) {
            for (int dc = 0; dc <= 1; dc++) {
                int r = r0 + dr, c = c0 + dc;
                if (!validV(r, c)) continue;
                float wt = (dc == 0 ? 1f - tx : tx) * (dr == 0 ? 1f - ty : ty);
                sum += f[getVIndex(r, c)] * wt;
                w += wt;
            }
        }
        return w > 0f ? sum / w : 0f;
    }

    private boolean validU(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col > cols) return false;
        boolean left = col > 0 && cellType[getCellIndex(row, col - 1)] != AIR;
        boolean right = col < cols && cellType[getCellIndex(row, col)] != AIR;
        return left || right;
    }

    private boolean validV(int row, int col) {
        if (row < 0 || row > rows || col < 0 || col >= cols) return false;
        boolean above = row > 0 && cellType[getCellIndex(row - 1, col)] != AIR;
        boolean below = row < rows && cellType[getCellIndex(row, col)] != AIR;
        return above || below;
    }

    // ------------------------------------------------------------------
    // Particle separation (counting-sort spatial hash, moves positions)
    // ------------------------------------------------------------------

    public void separateParticles(float[] px, float[] py, int count,
                                  float minDist, int simW, int simH) {
        float inv = 1f / minDist;
        int hw = (int) (simW * inv) + 2;
        int hh = (int) (simH * inv) + 2;
        if (cellStart == null || cellStart.length != hw * hh + 1) cellStart = new int[hw * hh + 1];
        if (cellEntries == null || cellEntries.length < count) cellEntries = new int[count];
        Arrays.fill(cellStart, 0);

        for (int i = 0; i < count; i++) cellStart[hashOf(px[i], py[i], inv, hw, hh)]++;
        int sum = 0;
        for (int c = 0; c < cellStart.length; c++) {
            sum += cellStart[c];
            cellStart[c] = sum;
        }
        for (int i = count - 1; i >= 0; i--) {
            cellEntries[--cellStart[hashOf(px[i], py[i], inv, hw, hh)]] = i;
        }

        float min2 = minDist * minDist;
        for (int iter = 0; iter < 2; iter++) {
            for (int i = 0; i < count; i++) {
                int cx = Math.min(Math.max((int) (px[i] * inv), 0), hw - 1);
                int cy = Math.min(Math.max((int) (py[i] * inv), 0), hh - 1);
                for (int ny = Math.max(cy - 1, 0); ny <= Math.min(cy + 1, hh - 1); ny++) {
                    for (int nx = Math.max(cx - 1, 0); nx <= Math.min(cx + 1, hw - 1); nx++) {
                        int c = ny * hw + nx;
                        for (int k = cellStart[c]; k < cellStart[c + 1]; k++) {
                            int j = cellEntries[k];
                            if (j == i) continue;
                            float dx = px[j] - px[i];
                            float dy = py[j] - py[i];
                            float d2 = dx * dx + dy * dy;
                            if (d2 >= min2 || d2 == 0f) continue;
                            float d = (float) Math.sqrt(d2);
                            float s = 0.5f * (minDist - d) / d;
                            px[i] -= dx * s;
                            py[i] -= dy * s;
                            px[j] += dx * s;
                            py[j] += dy * s;
                        }
                    }
                }
            }
        }
    }

    private static int hashOf(float x, float y, float inv, int hw, int hh) {
        int cx = Math.min(Math.max((int) (x * inv), 0), hw - 1);
        int cy = Math.min(Math.max((int) (y * inv), 0), hh - 1);
        return cy * hw + cx;
    }

    // ------------------------------------------------------------------
    // Safety
    // ------------------------------------------------------------------

    public void guardParticles(float[] px, float[] py, float[] pvx, float[] pvy,
                               int count, int width, int height) {
        for (int i = 0; i < count; i++) {
            if (Float.isNaN(px[i]) || Float.isInfinite(px[i])) {
                px[i] = width / 2f;
                pvx[i] = 0f;
            }
            if (Float.isNaN(py[i]) || Float.isInfinite(py[i])) {
                py[i] = height / 2f;
                pvy[i] = 0f;
            }
            if (Float.isNaN(pvx[i]) || Float.isInfinite(pvx[i])) pvx[i] = 0f;
            if (Float.isNaN(pvy[i]) || Float.isInfinite(pvy[i])) pvy[i] = 0f;
        }
    }

    // ------------------------------------------------------------------
    // Tunables
    // ------------------------------------------------------------------

    public float getFlipRatio() {
        return flipRatio;
    }

    public void setFlipRatio(float flipRatio) {
        this.flipRatio = flipRatio;
    }

    public int getPressureIterations() {
        return pressureIterations;
    }

    public void setPressureIterations(int pressureIterations) {
        this.pressureIterations = pressureIterations;
    }

    public float getDivergenceThreshold() {
        return divergenceThreshold;
    }

    public void setDivergenceThreshold(float divergenceThreshold) {
        this.divergenceThreshold = divergenceThreshold;
    }

    public float getOverRelaxation() {
        return overRelaxation;
    }

    public void setOverRelaxation(float overRelaxation) {
        this.overRelaxation = overRelaxation;
    }

    public float getDriftStiffness() {
        return driftStiffness;
    }

    public void setDriftStiffness(float driftStiffness) {
        this.driftStiffness = driftStiffness;
    }
}