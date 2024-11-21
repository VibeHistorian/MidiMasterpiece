package org.vibehistorian.vibecomposer.Panels;

import org.vibehistorian.vibecomposer.Components.CustomCheckBox;
import org.vibehistorian.vibecomposer.Components.ScrollComboBox;
import org.vibehistorian.vibecomposer.LG;
import org.vibehistorian.vibecomposer.OMNI;
import org.vibehistorian.vibecomposer.Popups.TemporaryInfoPopup;
import org.vibehistorian.vibecomposer.VibeComposerGUI;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.xml.bind.JAXBException;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

public class PartManagerPanel extends TransparentablePanel {

    private int part = -1;

    JLabel partName = new JLabel("");
    JTextField newPresetName = new JTextField("");
    ScrollComboBox<String> partPresetBox = new ScrollComboBox<>(false);
    JCheckBox overwriteExistingCheckbox = new CustomCheckBox("Overwrite", true);

    public PartManagerPanel(int partNum) {
        part = partNum;

        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.setBorder(new BevelBorder(BevelBorder.LOWERED));

        String folderName = VibeComposerGUI.instNames[partNum];
        partName.setText("Presets:");

        initPresetField(folderName);
        initPresetBox(folderName);
        add(partName);
        add(newPresetName);
        add(partPresetBox);
        add(overwriteExistingCheckbox);
    }

    private void initPresetField(String folderName) {
        newPresetName.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String partsDirectory = "PartPresets/" + folderName + "/";
                    File makeSavedDir = new File(partsDirectory);
                    makeSavedDir.mkdir();

                    try {
                        String dirPath = makeSavedDir.getPath().toString();
                        String fileName = newPresetName.getText().replaceAll(".xml", "");
                        VibeComposerGUI.marshalParts(dirPath + "/" + fileName + ".xml", part);
                        partPresetBox.addItem(fileName);
                        newPresetName.setText("");
                    } catch (Exception ex) {
                        new TemporaryInfoPopup("Saving failed!", 1500);
                        LG.e(ex);
                    }
                }
            }
        });
    }

    private void initPresetBox(String folderName) {
        ScrollComboBox.addAll(new String[] { OMNI.EMPTYCOMBO }, partPresetBox);
        File folder = new File("PartPresets/" + folderName);
        if (folder.exists()) {
            File[] listOfFiles = folder.listFiles();
            for (File f : listOfFiles) {
                if (f.isFile()) {
                    String fileName = f.getName();
                    int pos = fileName.lastIndexOf(".");
                    if (pos > 0 && pos < (fileName.length() - 1)) {
                        fileName = fileName.substring(0, pos);
                    }

                    partPresetBox.addItem(fileName);
                }
            }
        }


        partPresetBox.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                String item = (String) event.getItem();
                if (OMNI.EMPTYCOMBO.equals(item)) {
                    return;
                }

                LG.i("Trying to load part preset: " + folderName + "/" + item);

                // check if file exists
                File loadedFile = new File("PartPresets/" + folderName + "/" + item + ".xml");
                if (loadedFile.exists()) {
                    try {
                        VibeComposerGUI.vibeComposerGUI.unmarshallParts(loadedFile, part, overwriteExistingCheckbox.isSelected());
                        partPresetBox.setVal(OMNI.EMPTYCOMBO);
                    } catch (JAXBException | IOException e) {
                        LG.e(e);
                        return;
                    }
                }

                VibeComposerGUI.vibeComposerGUI.recalculateTabPaneCounts();
                VibeComposerGUI.vibeComposerGUI.recalculateGenerationCounts();
                VibeComposerGUI.vibeComposerGUI.recalculateSoloMuters();

                LG.i("Loaded preset: " + item);
            }
        });
    }

}
