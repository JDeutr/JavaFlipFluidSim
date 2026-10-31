package nl.jd.display;

import nl.jd.classes.Grid;
import nl.jd.classes.Particles;
import nl.jd.classes.SimulationPanel;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Draw {
    static volatile int pixelSize = 2;
    private final SimulationPanel simulationPanel;
    private final Particles particles;
    private Grid grid;

    public Draw(SimulationPanel simulationPanel, Particles particles, Grid grid) {
        this.simulationPanel = simulationPanel;
        this.particles = particles;
        this.grid = grid;
    }

    // The render callback: called from the render thread, returns immediately
    public void redraw() {
        SwingUtilities.invokeLater(() -> {
            simulationPanel.clear(0x000000);
            drawParticles();
            simulationPanel.repaint();
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
            simulationPanel.drawGrid(grid, 0xFF0000);
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