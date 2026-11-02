package nl.jd.display;

import nl.jd.classes.Grid;
import nl.jd.classes.Particles;
import nl.jd.classes.SimulationLoop;
import nl.jd.classes.SimulationPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class Display {
    private static final int TARGET_FPS = 144;
    private static final int PARTICLE_COUNT = 25_000;
    private static final float PARTICLE_SPACING = 1f;
    public static Dimension screen;

    public static void main(String[] args) {
        screen = Toolkit.getDefaultToolkit().getScreenSize();

        JFrame frame = createFrame(screen);
        SimulationPanel panel = new SimulationPanel(screen.width / 4, screen.height / 4);
        frame.add(panel, BorderLayout.CENTER);

        int w = panel.getSimWidth();
        int h = panel.getSimHeight();

        Particles particles = new Particles(PARTICLE_COUNT);
        fillParticles(particles, w, h);
        Grid grid = new Grid(w, h);
        grid.setParticleSpacing(PARTICLE_SPACING);
        Draw draw = new Draw(panel, particles, grid);
        draw.drawCursor();

        SimulationLoop loop = createLoop(panel, particles, draw, grid);
        draw.setPhysicsExecutor(loop::runOnPhysicsThread);
        Runnable reset = () -> loop.runOnPhysicsThread(() -> {
            fillParticles(particles, w, h);
            grid.reset();
            particles.publish();
            grid.updateSnapshot();
        });

        addControls(panel, loop);
        ControlPanel controls = new ControlPanel(panel, grid, particles, loop, reset);
        frame.add(controls, BorderLayout.EAST);
        controls.installKeys(frame.getRootPane());

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

    private static void fillParticles(Particles particles, int w, int h) {
        int cols = (int) (w / PARTICLE_SPACING);
        float startY = h * 0.15f;

        particles.clear();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            int currCol = i % cols;
            int currRow = i / cols;
            particles.add(currCol * PARTICLE_SPACING,
                    startY + currRow * PARTICLE_SPACING);
        }
    }

    private static SimulationLoop createLoop(SimulationPanel panel, Particles particles, Draw draw, Grid grid) {
        int w = panel.getSimWidth();
        int h = panel.getSimHeight();
        return new SimulationLoop(TARGET_FPS,
                () -> particles.step(SimulationLoop.DT, w, h, grid),
                draw::redraw);
    }

    private static void addControls(SimulationPanel panel, SimulationLoop loop) {
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JLabel stats = new JLabel("FPS: -");
        stats.setForeground(Color.WHITE);
        panel.add(stats);

        startStatsUpdates(stats, loop);
    }

    private static void startStatsUpdates(JLabel label, SimulationLoop loop) {
        ActionListener refresh = e -> label.setText("FPS: " + loop.getFramesPerSecond()
                + "  Steps/s: " + loop.getStepsPerSecond()
                + (loop.isPaused() ? "  [PAUSED]" : ""));
        new Timer(500, refresh).start();
    }
}