package nl.jd.display;

import nl.jd.classes.SimulationPanel;

import javax.swing.*;
import java.awt.*;

public class Display {
    public static void main(String[] args) throws InterruptedException {
        JFrame frame = new JFrame();

        frame.setTitle("FLIP SIMULATION");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        var x = Toolkit.getDefaultToolkit().getScreenSize();
        PixelSlider slider = new PixelSlider();

        frame.setSize(x.width, x.height);
        frame.setLocationRelativeTo(null);

        SimulationPanel panel = new SimulationPanel(frame.getWidth() / 2, frame.getHeight() / 2);

        frame.add(panel, BorderLayout.CENTER);

        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        slider.setPreferredSize(new Dimension(150, 20));
        slider.setOpaque(false);   // so the drawing shows through its background
        panel.add(slider);

        frame.setVisible(true);

        Draw draw = new Draw(panel, frame);
        draw.drawCursor();


//        draw.drawLine();
    }
}
