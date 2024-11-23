package org.vibehistorian.vibecomposer.Components;

import javax.swing.*;
import java.awt.*;

public class BigPickerOpener extends JComponent {

    private static final long serialVersionUID = -8294355665689457350L;

    int w = 8;
    int h = 8;

    public BigPickerOpener() {
        setPreferredSize(new Dimension(w, h));
        setOpaque(true);
        setSize(new Dimension(w, h));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.white);
        g.drawRect(0, 0, w, h);
        g.setColor(Color.black);
        g.drawString("?", 0, 0);
    }
}
