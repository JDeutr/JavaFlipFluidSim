package nl.jd.display;

import nl.jd.classes.Grid;
import nl.jd.classes.Interaction;
import nl.jd.classes.Particles;
import nl.jd.classes.SimulationPanel;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class Draw {
    static volatile int pixelSize = 1;
    private final SimulationPanel simulationPanel;
    private final Particles particles;
    private final Grid grid;
    private final AtomicBoolean redrawPending = new AtomicBoolean(false);
    private volatile Consumer<Runnable> physicsExecutor = Runnable::run;
    private float lastPaintX = Float.NaN, lastPaintY = Float.NaN;

    public Draw(SimulationPanel simulationPanel, Particles particles, Grid grid) {
        this.simulationPanel = simulationPanel;
        this.particles = particles;
        this.grid = grid;
    }

    public void setPhysicsExecutor(Consumer<Runnable> physicsExecutor) {
        this.physicsExecutor = physicsExecutor;
    }

    public void redraw() {
        if (!redrawPending.compareAndSet(false, true)) return;
        SwingUtilities.invokeLater(() -> {
            try {
                simulationPanel.clear(0x000000);
                drawObstacles();
                drawParticles();
                drawPointer();
                simulationPanel.repaint();
            } finally {
                redrawPending.set(false);
            }
        });
    }

    private void drawObstacles() {
        int[] types = grid.getSnapshotType();
        if (types == null) return;
        int h = grid.getCellSize();

        for (int row = 1; row < grid.getRows() - 1; row++) {
            for (int col = 1; col < grid.getCols() - 1; col++) {
                if (types[grid.getCellIndex(row, col)] != Grid.SOLID) continue;
                int x0 = (col - 1) * h, y0 = (row - 1) * h;
                for (int dy = 0; dy < h; dy++) {
                    for (int dx = 0; dx < h; dx++) {
                        simulationPanel.setPixel(x0 + dx, y0 + dy, 0x888888);
                    }
                }
            }
        }
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

    private void drawPointer() {
        Interaction in = particles.getInteraction();
        if (!in.hovering) return;
        switch (in.mode) {
            case Interaction.DRAG -> drawRing(in.x, in.y, in.radius, 0x00FF00);
            case Interaction.REPEL -> drawRing(in.x, in.y, in.radius, 0xFF4444);
            case Interaction.OBSTACLE_ADD -> drawRing(in.x, in.y, in.brushRadius, 0xFFFFFF);
            case Interaction.OBSTACLE_ERASE -> drawRing(in.x, in.y, in.brushRadius, 0xFF8800);
            default -> {
                if (in.ctrlDown) drawRing(in.x, in.y, in.brushRadius, 0xFFFF00);
                else drawRing(in.x, in.y, in.radius, 0x888888);
            }
        }
    }

    private void drawRing(float cx, float cy, float r, int rgb) {
        int steps = Math.max(16, (int) (2 * Math.PI * r));
        for (int i = 0; i < steps; i++) {
            double a = 2 * Math.PI * i / steps;
            simulationPanel.setPixel((int) (cx + r * Math.cos(a)), (int) (cy + r * Math.sin(a)), rgb);
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
            public void mouseMoved(MouseEvent e) {
                updatePointer(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                updatePointer(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                updatePointer(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                updatePointer(e, false);
                particles.getInteraction().mode = Interaction.NONE;
                resetStroke();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                updatePointer(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                int buttons = InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK;
                if ((e.getModifiersEx() & buttons) == 0) {
                    particles.getInteraction().hovering = false;
                    resetStroke();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                Interaction in = particles.getInteraction();
                if (e.isControlDown()) {
                    in.brushRadius = Math.clamp(in.brushRadius - e.getWheelRotation() * 1f, 1f, 40f);
                } else {
                    in.radius = Math.clamp(in.radius - e.getWheelRotation() * 3f, 5f, 160f);
                }
            }
        };
        simulationPanel.addMouseListener(mouseAdapter);
        simulationPanel.addMouseMotionListener(mouseAdapter);
        simulationPanel.addMouseWheelListener(mouseAdapter);
    }

    private void updatePointer(MouseEvent e) {
        updatePointer(e, true);
    }

    private void updatePointer(MouseEvent e, boolean allowPaint) {
        Interaction in = particles.getInteraction();
        in.x = e.getX() * (float) simulationPanel.getSimWidth() / simulationPanel.getWidth();
        in.y = e.getY() * (float) simulationPanel.getSimHeight() / simulationPanel.getHeight();
        in.hovering = true;

        int m = e.getModifiersEx();
        boolean ctrl = (m & InputEvent.CTRL_DOWN_MASK) != 0;
        boolean left = (m & InputEvent.BUTTON1_DOWN_MASK) != 0;
        boolean right = (m & InputEvent.BUTTON3_DOWN_MASK) != 0;
        in.ctrlDown = ctrl;

        if (ctrl && right) in.mode = Interaction.OBSTACLE_ERASE;
        else if (ctrl && left) in.mode = Interaction.OBSTACLE_ADD;
        else if (right) in.mode = Interaction.REPEL;
        else if (left) in.mode = Interaction.DRAG;
        else in.mode = Interaction.NONE;

        if (allowPaint && in.mode == Interaction.OBSTACLE_ADD) paintStroke(in.x, in.y, true);
        else if (allowPaint && in.mode == Interaction.OBSTACLE_ERASE) paintStroke(in.x, in.y, false);
        else resetStroke();
    }

    private void resetStroke() {
        lastPaintX = Float.NaN;
        lastPaintY = Float.NaN;
    }

    private void paintStroke(float x, float y, boolean solid) {
        float r = particles.getInteraction().brushRadius;
        float fromX = Float.isNaN(lastPaintX) ? x : lastPaintX;
        float fromY = Float.isNaN(lastPaintY) ? y : lastPaintY;
        lastPaintX = x;
        lastPaintY = y;

        float dx = x - fromX, dy = y - fromY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        int stamps = Math.max(1, (int) Math.ceil(dist / Math.max(1f, r * 0.5f)));

        physicsExecutor.accept(() -> {
            boolean changed = false;
            for (int s = 1; s <= stamps; s++) {
                float t = (float) s / stamps;
                changed |= grid.paintObstacle(fromX + dx * t, fromY + dy * t, r, solid);
            }
            if (changed) grid.updateSnapshot();
        });
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