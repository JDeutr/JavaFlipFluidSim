package nl.jd.display;

import nl.jd.classes.Grid;
import nl.jd.classes.Particles;
import nl.jd.classes.SimulationPanel;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;

public class Draw {
    static volatile int pixelSize = 2;
    private final SimulationPanel simulationPanel;
    private final Particles particles;
    private final Grid grid;
    private final AtomicBoolean redrawPending = new AtomicBoolean(false);

    public Draw(SimulationPanel simulationPanel, Particles particles, Grid grid) {
        this.simulationPanel = simulationPanel;
        this.particles = particles;
        this.grid = grid;
    }

    // The render callback: called from the render thread, returns immediately.
    // If the EDT hasn't finished the previous frame yet, this frame is skipped
    // instead of piling up another invokeLater.
    public void redraw() {
        if (!redrawPending.compareAndSet(false, true)) return;
        SwingUtilities.invokeLater(() -> {
            try {
                simulationPanel.clear(0x000000);
                drawParticles();
                simulationPanel.repaint();
            } finally {
                redrawPending.set(false);
            }
        });
    }

    private void drawParticles() {
        float[][] s = particles.getSnapshot();
        if (s == null) return;
        float[] xs = s[0], ys = s[1];
        for (int i = 0; i < xs.length; i++) {
            drawDot((int) xs[i], (int) ys[i], 0x4488FF);
        }

        if (grid != null && simulationPanel.isShowGrid()) {
            drawVelocityArrows(grid);
            simulationPanel.drawGrid(grid, 0xFF0000);
        }
    }

    private void drawVelocityArrows(Grid grid) {
        float[] su = grid.getSnapshotU();
        float[] sv = grid.getSnapshotV();
        int[] snapshotType = grid.getSnapshotType();
        if (su == null || sv == null || snapshotType == null) return;

        int cellSize = grid.getCellSize();
        float scale = 0.02f;

        for (int row = 1; row < grid.getRows() - 1; row++) {
            for (int col = 1; col < grid.getCols() - 1; col++) {
                if (snapshotType[grid.getCellIndex(row, col)] != Grid.FLUID) continue;

                float avgU = (su[grid.getUIndex(row, col)] + su[grid.getUIndex(row, col + 1)]) * 0.5f;
                float avgV = (sv[grid.getVIndex(row, col)] + sv[grid.getVIndex(row + 1, col)]) * 0.5f;

                float len = (float) Math.sqrt(avgU * avgU + avgV * avgV);
                if (len < 0.01f) continue;

                float drawLen = Math.min(len * scale, (float) cellSize);
                float cx = (col - 0.5f) * cellSize;
                float cy = (row - 0.5f) * cellSize;
                int x1 = (int) (cx + avgU / len * drawLen);
                int y1 = (int) (cy + avgV / len * drawLen);

                drawLine((int) cx, (int) cy, x1, y1, 0x00FF00);
                simulationPanel.setPixel(x1, y1, 0xFF0000);
            }
        }
    }

    private void drawLine(int x0, int y0, int x1, int y1, int rgb) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        if (steps == 0) {
            simulationPanel.setPixel(x0, y0, rgb);
            return;
        }
        for (int s = 0; s <= steps; s++) {
            simulationPanel.setPixel(x0 + (x1 - x0) * s / steps,
                    y0 + (y1 - y0) * s / steps, rgb);
        }
    }

    public void drawCursor() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                draw(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                draw(e);
            }
        };
        simulationPanel.addMouseListener(mouseAdapter);
        simulationPanel.addMouseMotionListener(mouseAdapter);
    }

    private void draw(MouseEvent e) {
        int x = e.getX() * simulationPanel.getSimWidth() / simulationPanel.getWidth();
        int y = e.getY() * simulationPanel.getSimHeight() / simulationPanel.getHeight();
        int color = (e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0 ? 0xFFFFFF : 0x000000;

        drawDot(x, y, color);
        simulationPanel.repaint();
    }

    private void drawDot(int x, int y, int rgb) {
        int radius = pixelSize / 2;
        if (radius == 0) {
            simulationPanel.setPixel(x, y, rgb);
            return;
        }
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dy * dy > radius * radius) continue;
                simulationPanel.setPixel(x + dx, y + dy, rgb);
            }
        }
    }
}