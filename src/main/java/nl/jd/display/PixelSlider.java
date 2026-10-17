package nl.jd.display;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class PixelSlider extends JSlider implements ChangeListener {
    static final int MIN_SIZE = 1;
    static final int MAX_SIZE = 100;

    public PixelSlider() {
        super(MIN_SIZE, MAX_SIZE, MIN_SIZE);
        addChangeListener(this);
    }

    public void stateChanged(ChangeEvent e) {
        JSlider source = (JSlider)e.getSource();
        Draw.pixelSize = source.getValue();
    }
}
