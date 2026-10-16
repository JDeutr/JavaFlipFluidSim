package nl.jd.display;

import nl.jd.classes.SimulationPanel;

import javax.swing.*;
import java.util.concurrent.TimeUnit;

public class Display {
    public static void main(String[] args) throws InterruptedException {
        JFrame frame = new JFrame();

        frame.setTitle("FLIP SIMULATION");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setSize(1280, 720);
        frame.setLocationRelativeTo(null);

        SimulationPanel panel = new SimulationPanel(frame.getWidth() / 2, frame.getHeight() / 2);
        frame.add(panel);
        frame.setVisible(true);

        Draw draw = new Draw(panel, frame);
        draw.drawCursor();
        draw.drawLine();
    }
}
