package org.vibehistorian.vibecomposer.Popups;

import org.vibehistorian.vibecomposer.Components.MidiEditArea;
import org.vibehistorian.vibecomposer.Helpers.PhraseNotes;
import org.vibehistorian.vibecomposer.MidiGenerator;

import javax.swing.*;
import java.awt.*;

public class CustomDurationsEditPopup extends CloseablePopup {

    public MidiEditArea cdMvea;

    public CustomDurationsEditPopup(PhraseNotes values, Component parentComponent) {
        super("Custom Durations Editor", 15, new Point(0,0), parentComponent);
        JPanel cdMveaPanel = new JPanel();
        cdMvea = new MidiEditArea(0, 10, values);
        cdMvea.setRange(0, 10);
        cdMvea.splitNotesByGrid = true;
        cdMvea.drawNoteStrings = false;
        cdMvea.sectionLength = MidiGenerator.Durations.WHOLE_NOTE;
        cdMvea.timeGridChoice = 0;
        cdMvea.forceMarginTime = true;
        //cdMvea.setPop(null);
        cdMvea.setPreferredSize(new Dimension(1000, 500));
        cdMveaPanel.setPreferredSize(new Dimension(1000, 500));
        cdMveaPanel.add(cdMvea);
        cdMvea.addKeyboardControls(cdMveaPanel);
        frame.add(cdMveaPanel);
        frame.pack();
        frame.setVisible(true);
    }

    @Override
    protected void addFrameWindowOperation() {
        frame.addWindowListener(CloseablePopup.EMPTY_WINDOW_LISTENER);
    }
}
