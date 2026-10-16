package nl.jd.display;

import nl.jd.classes.SimulationPanel;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.TimeUnit;

public class Draw {
    SimulationPanel simulationPanel;
    JFrame frame;

    public Draw (SimulationPanel simulationPanel, JFrame frame) {
        this.simulationPanel = simulationPanel;
        this.frame = frame;
    }

    public void drawLine() throws InterruptedException {
        for (int i = 0; i < 2000; i++) {
            simulationPanel.setPixel(i,i, 0xFF0000);
            repaint();
            TimeUnit.MILLISECONDS.sleep(50);
        }
    }

    public void drawCursor() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { draw(e); }

            @Override
            public void mouseDragged(MouseEvent e) { draw(e); }
        };
        simulationPanel.addMouseListener(mouseAdapter);
        simulationPanel.addMouseMotionListener(mouseAdapter);
    }

    private void draw(MouseEvent e) {
        int x = e.getX() * simulationPanel.getSimWidth()  / simulationPanel.getWidth();
        int y = e.getY() * simulationPanel.getSimHeight() / simulationPanel.getHeight();
        simulationPanel.setPixel(x, y, 0xFFFFFF);
        simulationPanel.repaint();
    }

    private void repaint() {
        frame.repaint();
    }
}
