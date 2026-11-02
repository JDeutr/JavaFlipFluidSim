package nl.jd.display;

import nl.jd.classes.Grid;
import nl.jd.classes.Particles;
import nl.jd.classes.SimulationLoop;
import nl.jd.classes.SimulationPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

public class ControlPanel extends JPanel {
    private final SimulationLoop loop;
    private final Runnable reset;
    private final Runnable clearObstacles;
    private final JButton pauseButton = new JButton("Pause");

    public ControlPanel(SimulationPanel panel, Grid grid, Particles particles,
                        SimulationLoop loop, Runnable reset) {
        this.loop = loop;
        this.reset = reset;
        this.clearObstacles = () -> loop.runOnPhysicsThread(() -> {
            grid.clearObstacles();
            grid.updateSnapshot();
        });

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.BLACK);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setPreferredSize(new Dimension(200, 0));

        addSlider("FLIP ratio", 0, 100, Math.round(grid.getFlipRatio() * 100),
                v -> grid.setFlipRatio(v / 100f), v -> String.format("%.2f", v / 100f));
        addSlider("Drift stiffness", 0, 100, Math.round(grid.getDriftStiffness() * 10),
                v -> grid.setDriftStiffness(v / 10f), v -> String.format("%.1f", v / 10f));
        addSlider("Pressure iterations", 5, 100, grid.getPressureIterations(),
                grid::setPressureIterations, String::valueOf);
        addSlider("Gravity", 0, 600, Math.round(particles.getGravity()),
                v -> particles.setGravity(v), String::valueOf);

        addPixelSlider();
        addGridToggle(panel);

        add(Box.createVerticalStrut(12));
        addButton(pauseButton, this::togglePause);
        addButton(new JButton("Step (N)"), this::step);
        addButton(new JButton("Reset (R)"), reset);
        addButton(new JButton("Clear obstacles (C)"), clearObstacles);

        add(Box.createVerticalStrut(12));
        JLabel help = new JLabel("<html>Space: pause<br>N: step<br>R: reset<br>"
                + "C: clear obstacles<br>LMB: drag fluid<br>RMB: push away<br>Wheel: radius<br>"
                + "Ctrl+LMB: add obstacle<br>Ctrl+RMB: erase<br>Ctrl+Wheel: brush size</html>");
        help.setForeground(Color.LIGHT_GRAY);
        help.setAlignmentX(LEFT_ALIGNMENT);
        add(help);
    }

    public void installKeys(JRootPane root) {
        bind(root, KeyEvent.VK_SPACE, "pause", this::togglePause);
        bind(root, KeyEvent.VK_N, "step", this::step);
        bind(root, KeyEvent.VK_R, "reset", reset);
        bind(root, KeyEvent.VK_C, "clear", clearObstacles);
    }

    private void bind(JRootPane root, int key, String id, Runnable action) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0), id);
        root.getActionMap().put(id, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                action.run();
            }
        });
    }

    private void togglePause() {
        loop.togglePause();
        syncPauseButton();
    }

    private void step() {
        loop.stepOnce();
        syncPauseButton();
    }

    private void syncPauseButton() {
        pauseButton.setText(loop.isPaused() ? "Resume" : "Pause");
    }

    private void addButton(JButton button, Runnable action) {
        button.setFocusable(false);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
        button.addActionListener(e -> action.run());
        add(button);
        add(Box.createVerticalStrut(4));
    }

    private void addPixelSlider() {
        JLabel label = new JLabel("Pixel size: " + Draw.pixelSize);
        label.setForeground(Color.WHITE);
        label.setAlignmentX(LEFT_ALIGNMENT);

        PixelSlider slider = new PixelSlider();
        slider.setFocusable(false);
        slider.setOpaque(false);
        slider.setAlignmentX(LEFT_ALIGNMENT);
        slider.addChangeListener(e -> label.setText("Pixel size: " + slider.getValue()));

        add(label);
        add(slider);
        add(Box.createVerticalStrut(6));
    }

    private void addGridToggle(SimulationPanel panel) {
        JCheckBox gridToggle = new JCheckBox("Show Grid", panel.isShowGrid());
        gridToggle.setForeground(Color.WHITE);
        gridToggle.setOpaque(false);
        gridToggle.setFocusable(false);
        gridToggle.setAlignmentX(LEFT_ALIGNMENT);
        gridToggle.addActionListener(e -> {
            panel.setShowGrid(gridToggle.isSelected());
            panel.repaint();
        });

        add(gridToggle);
        add(Box.createVerticalStrut(6));
    }

    private void addSlider(String name, int min, int max, int value,
                           IntConsumer onChange, IntFunction<String> format) {
        JLabel label = new JLabel(name + ": " + format.apply(value));
        label.setForeground(Color.WHITE);
        label.setAlignmentX(LEFT_ALIGNMENT);

        JSlider slider = new JSlider(min, max, Math.clamp(value, min, max));
        slider.setFocusable(false);
        slider.setOpaque(false);
        slider.setAlignmentX(LEFT_ALIGNMENT);
        slider.addChangeListener(e -> {
            int v = slider.getValue();
            label.setText(name + ": " + format.apply(v));
            onChange.accept(v);
        });

        add(label);
        add(slider);
        add(Box.createVerticalStrut(6));
    }
}