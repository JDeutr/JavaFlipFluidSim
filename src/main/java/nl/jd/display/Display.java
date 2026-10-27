package nl.jd.display;

import nl.jd.classes.Particles;
import nl.jd.classes.SimulationLoop;
import nl.jd.classes.SimulationPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Random;

public class Display {
    private static final int TARGET_FPS = 144;
    private static final int PARTICLE_CAPACITY = 10_000;

    public static void main(String[] args) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

        JFrame frame = createFrame(screen);
        SimulationPanel panel = new SimulationPanel(screen.width / 4, screen.height / 4);
        frame.add(panel, BorderLayout.CENTER);

        Particles particles = createParticles(panel.getSimWidth(), panel.getSimHeight());
        Draw draw = new Draw(panel, particles);
        draw.drawCursor();

        SimulationLoop loop = createLoop(panel, particles, draw);

        addControls(panel, loop);
        frame.setVisible(true);
        loop.start();
    }

    private static JFrame createFrame(Dimension screen) {
        JFrame frame = new JFrame("FLIP SIMULATION");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(screen.width / 2, screen.height / 2);
        frame.setLocationRelativeTo(null);
        return frame;
    }

    private static Particles createParticles(int w, int h) {
        Particles particles = new Particles(PARTICLE_CAPACITY);
        Random rng = new Random(69);
        for (int row = 0; row < 60; row++) {
            for (int col = 0; col < 100; col++) {
                particles.add(w / 4f + col * 2f + rng.nextFloat(),
                        h / 4f + row * 2f + rng.nextFloat());
            }
        }
        return particles;
    }

    private static SimulationLoop createLoop(SimulationPanel panel, Particles particles, Draw draw) {
        int w = panel.getSimWidth();
        int h = panel.getSimHeight();
        return new SimulationLoop(TARGET_FPS,
                () -> particles.step(SimulationLoop.DT, w, h),
                draw::redraw);
    }

    private static void addControls(SimulationPanel panel, SimulationLoop loop) {
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        PixelSlider slider = new PixelSlider();
        slider.setPreferredSize(new Dimension(150, 20));
        slider.setOpaque(false);
        panel.add(slider);

        JLabel stats = new JLabel("FPS: -");
        stats.setForeground(Color.WHITE);
        panel.add(stats);

        startStatsUpdates(stats, loop);
    }

    private static void startStatsUpdates(JLabel label, SimulationLoop loop) {
        ActionListener refresh = e -> label.setText("FPS: " + loop.getFramesPerSecond()
                + "  Steps/s: " + loop.getStepsPerSecond());
        new Timer(500, refresh).start();
    }
}