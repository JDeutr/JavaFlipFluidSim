package nl.jd.classes;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class SimulationPanel extends JPanel {
    final int[] pixels;
    private final int width, height;
    private final BufferedImage visuals;
    private boolean showGrid = false;

    public SimulationPanel(int width, int height) {
        this.width = width;
        this.height = height;
        visuals = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) visuals.getRaster().getDataBuffer()).getData();
        setPreferredSize(new Dimension(width * 3, height * 3));
    }

    public int getSimHeight() {
        return height;
    }

    public int getSimWidth() {
        return width;
    }

    public void setPixel(int x, int y, int rgb) {
        if (x < 0 || y < 0 || x >= width || y >= height) return;
        pixels[y * width + x] = rgb;
    }

    public void clear(int rgb) {
        java.util.Arrays.fill(pixels, rgb);
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public void drawGrid(Grid grid, int rgb) {
        int cellSize = grid.getCellSize();
        int cols = grid.getCols();
        int rows = grid.getRows();

        for (int row = 0; row <= rows; row++) {
            int y = row * cellSize;
            for (int x = 0; x < width; x++) {
                setPixel(x, y, rgb);
            }
        }
        for (int col = 0; col <= cols; col++) {
            int x = col * cellSize;
            for (int y = 0; y < height; y++) {
                setPixel(x, y, rgb);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(visuals, 0, 0, getWidth(), getHeight(), null);
    }
}
