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
}