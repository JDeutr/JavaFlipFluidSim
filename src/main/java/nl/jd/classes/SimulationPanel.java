package nl.jd.classes;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class SimulationPanel extends JPanel {
    final int[] pixels;
    private final int width, height;
    private final BufferedImage visuals;

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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(visuals, 0, 0, getWidth(), getHeight(), null);
    }
}
