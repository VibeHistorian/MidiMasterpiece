package org.vibehistorian.vibecomposer.Popups;

import org.vibehistorian.vibecomposer.Components.MidiEditArea;
import org.vibehistorian.vibecomposer.Components.VeloRect;
import org.vibehistorian.vibecomposer.Helpers.PhraseNotes;
import org.vibehistorian.vibecomposer.MidiGenerator;
import org.vibehistorian.vibecomposer.Panels.MelodyPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CustomDurationsEditPopup extends CloseablePopup {

    public MidiEditArea cdMvea;

    public CustomDurationsEditPopup(PhraseNotes values, List<Integer> chances, Component parentComponent) {
        super("Custom Durations Editor", 15, new Point(0,0), parentComponent);
        JPanel cdMveaPanel = new JPanel();
        cdMveaPanel.setLayout(new BoxLayout(cdMveaPanel, BoxLayout.X_AXIS));
        cdMvea = new MidiEditArea(0, MelodyPanel.CUSTOM_DURATIONS_LIMIT-1, values);
        cdMvea.splitNotesByGrid = true;
        cdMvea.drawNoteStrings = false;
        cdMvea.sectionLength = MidiGenerator.Durations.WHOLE_NOTE;
        cdMvea.timeGridChoice = 0;
        cdMvea.forceMarginTime = true;
        //cdMvea.setPop(null);
        cdMvea.setPreferredSize(new Dimension(920, 500));
        JPanel patternChancePanel = new JPanel();
        patternChancePanel.setPreferredSize(new Dimension(30,415));
        patternChancePanel.setLayout(new GridLayout(0,1,0,10));
        JPanel patternChancePanelWrapper = new JPanel();
        patternChancePanelWrapper.setPreferredSize(new Dimension(80,500));
        //patternChancePanelWrapper.add(new JLabel());
        for (int i = MelodyPanel.CUSTOM_DURATIONS_LIMIT - 1; i >= 0; i--) {
            VeloRect vr = VeloRect.percent(chances.get(i));
            vr.setMargin(new Insets(10,0,10,0));
            int finalI = i;
            vr.updatedValueListener = newVal -> {
                chances.set(finalI, newVal);
            };
            patternChancePanel.add(vr);
        }
        JLabel chanceLabel = new JLabel("CHANCE%");
        chanceLabel.setPreferredSize(new Dimension(60, 10));
        patternChancePanelWrapper.add(chanceLabel);
        patternChancePanelWrapper.add(patternChancePanel);
        patternChancePanel.setLocation(30, 0);
        cdMveaPanel.setPreferredSize(new Dimension(1000, 500));
        cdMveaPanel.add(patternChancePanelWrapper);
        cdMveaPanel.add(cdMvea);
        cdMvea.addKeyboardControls(cdMveaPanel);
        cdMvea.setAndRepaint();
        frame.add(cdMveaPanel);
        frame.pack();
        frame.setVisible(true);
    }

    @Override
    protected void addFrameWindowOperation() {
        frame.addWindowListener(CloseablePopup.EMPTY_WINDOW_LISTENER);
    }
}
