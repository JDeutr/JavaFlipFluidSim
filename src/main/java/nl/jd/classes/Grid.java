package nl.jd.classes;

public class Grid {
    public static final int SOLID = 2, FLUID = 1, AIR = 0;
    private final int rows, cols, cellSize;
    private final float[] u, v, uWeight, vWeight;
    private final int[] cellType;
    private int[] snapshotType;

    public Grid(int simWidth, int simHeight) {
        this.cellSize = 6;
        this.cols = simWidth / cellSize + 2;
        this.rows = simHeight / cellSize + 2;
        this.cellType = new int[rows * cols];
        this.u = new float[rows * (cols + 1)];
        this.v = new float[(rows + 1) * cols];
        this.uWeight = new float[rows * (cols + 1)];
        this.vWeight = new float[(rows + 1) * cols];

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

    public void updateSnapshot() {
        snapshotType = java.util.Arrays.copyOf(cellType, cellType.length);
    }

    public int[] getSnapshotType() {
        return snapshotType;
    }

    public int getCellType(int index) {
        return cellType[index];
    }

    public void resetWeights() {
        java.util.Arrays.fill(uWeight, 0f);
        java.util.Arrays.fill(vWeight, 0f);
    }

    public float[] getUWeights() {
        return uWeight;
    }

    public float[] getVWeights() {
        return vWeight;
    }

    public void p2g(float[] px, float[] py, float[] pvx, float[] pvy, int count) {
        resetWeights();
        for (int i = 0; i < count; i++) {
            accumulateParticle(px[i], py[i], pvx[i], pvy[i]);
        }
        normalize();
    }

    private void accumulateParticle(float px, float py, float pvx, float pvy) {
        int cellCol = clamp(getCol(px), cols - 2);
        int cellRow = clamp(getRow(py), rows - 2);

        float fracX = px / cellSize - (int) (px / cellSize);
        float fracY = py / cellSize - (int) (py / cellSize);

        accumulateU(pvx, cellCol, cellRow, fracX, fracY);
        accumulateV(pvy, cellCol, cellRow, fracX, fracY);
    }

    private void accumulateU(float pvx, int cellCol, int cellRow, float wRight, float wBottom) {
        float wLeft = 1f - wRight;
        float wTop = 1f - wBottom;

        int uTL = getUIndex(cellRow, cellCol);
        int uTR = getUIndex(cellRow, cellCol + 1);
        int uBL = getUIndex(cellRow + 1, cellCol);
        int uBR = getUIndex(cellRow + 1, cellCol + 1);

        u[uTL] += pvx * wLeft * wTop;
        uWeight[uTL] += wLeft * wTop;
        u[uTR] += pvx * wRight * wTop;
        uWeight[uTR] += wRight * wTop;
        u[uBL] += pvx * wLeft * wBottom;
        uWeight[uBL] += wLeft * wBottom;
        u[uBR] += pvx * wRight * wBottom;
        uWeight[uBR] += wRight * wBottom;
    }

    private void accumulateV(float pvy, int cellCol, int cellRow, float wRight, float wBottom) {
        float wLeft = 1f - wRight;
        float wTop = 1f - wBottom;

        int vTL = getVIndex(cellRow, cellCol);
        int vTR = getVIndex(cellRow, cellCol + 1);
        int vBL = getVIndex(cellRow + 1, cellCol);
        int vBR = getVIndex(cellRow + 1, cellCol + 1);

        v[vTL] += pvy * wTop * wLeft;
        vWeight[vTL] += wTop * wLeft;
        v[vTR] += pvy * wTop * wRight;
        vWeight[vTR] += wTop * wRight;
        v[vBL] += pvy * wBottom * wLeft;
        vWeight[vBL] += wBottom * wLeft;
        v[vBR] += pvy * wBottom * wRight;
        vWeight[vBR] += wBottom * wRight;
    }

    private void normalize() {
        for (int i = 0; i < u.length; i++) {
            if (uWeight[i] > 0) u[i] /= uWeight[i];
        }
        for (int i = 0; i < v.length; i++) {
            if (vWeight[i] > 0) v[i] /= vWeight[i];
        }
    }

    private int clamp(int val, int max) {
        return Math.clamp(val, 0, max);
    }
}