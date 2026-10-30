package nl.jd.classes;

public class Grid {
    public static final int SOLID = 2, FLUID = 1, AIR = 0;

    private final int rows, cols, cellSize;
    private final float[] u, v;
    private final int[] cellType;

    public Grid(int simWidth, int simHeight) {
        this.cellSize = 6;
        this.cols = simWidth / cellSize + 2;
        this.rows = simHeight / cellSize + 2;
        this.cellType = new int[rows * cols];
        this.u = new float[rows * (cols + 1)];
        this.v = new float[(rows + 1) * cols];

        initSolidBorder();
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

    public int getCellIndex(int row, int col) {
        return row * cols + col;
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
}