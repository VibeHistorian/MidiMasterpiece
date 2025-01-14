package org.vibehistorian.vibecomposer.Panels;

import org.apache.commons.lang3.tuple.Pair;
import org.vibehistorian.vibecomposer.Components.CustomCheckBox;
import org.vibehistorian.vibecomposer.Components.ScrollComboBox;
import org.vibehistorian.vibecomposer.InstUtils;
import org.vibehistorian.vibecomposer.Parts.Defaults.DrumDefaults;
import org.vibehistorian.vibecomposer.Parts.DrumPart;
import org.vibehistorian.vibecomposer.Parts.InstPart;
import org.vibehistorian.vibecomposer.VibeComposerGUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DrumPanel extends InstPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6219184197272490684L;

	private JCheckBox isVelocityPattern = new CustomCheckBox("Ghosts", true);

	public void initComponents(ActionListener l) {

		instrument.initInstPool(InstUtils.POOL.DRUM);
		instrument.setInstrument(36);
		instrument.setScrollEnabled(false);
		ScrollComboBox.addAll(new Integer[] { 10 }, midiChannel);

		initDefaults(l);
		//this.add(new JLabel("#"));
		this.add(panelOrder);
		addDefaultInstrumentControls();
		addDefaultPanelButtons();

		this.add(chordSpanFillPanel);
		chordSpanFill.setScrollEnabled(false);

		// pattern business
		this.add(hitsPerPattern);
		hitsPerPattern.setScrollEnabled(false);

		hitsPerPattern.getKnob().setTickThresholds(Arrays.asList(
				new Integer[] { 4, 6, 8, 10, 12, 16, 24, 32, VisualPatternPanel.MAX_HITS }));
		hitsPerPattern.getKnob().setTickSpacing(50);

		pattern.setScrollEnabled(false);

		this.add(pattern);
		JButton doublerButt = new JButton("Dd");
		doublerButt.setPreferredSize(new Dimension(25, 30));
		doublerButt.setMargin(new Insets(0, 0, 0, 0));
		JButton expanderButt = new JButton("<>");
		expanderButt.setPreferredSize(new Dimension(25, 30));
		expanderButt.setMargin(new Insets(0, 0, 0, 0));
		JButton veloTogglerButt = new JButton("V");
		veloTogglerButt.setPreferredSize(new Dimension(25, 30));
		veloTogglerButt.setMargin(new Insets(0, 0, 0, 0));
		comboPanel = makeVisualPatternPanel();
		comboPanel.linkDoubler(doublerButt);
		comboPanel.linkExpander(expanderButt);
		comboPanel.linkVelocityToggle(veloTogglerButt);
		comboPanel.setBigModeAllowed(true);
		comboPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		JPanel comboPanelWrapper = new JPanel();

		comboPanelWrapper.add(doublerButt);
		comboPanelWrapper.add(expanderButt);
		comboPanelWrapper.add(comboPanel);
		comboPanelWrapper.add(veloTogglerButt);

		this.add(comboPanelWrapper);
		this.add(patternFlip);
		this.add(patternShift);
		patternShift.setScrollEnabled(false);
		this.add(isVelocityPattern);
		comboPanel.linkGhostNoteSwitch(isVelocityPattern);

		this.add(chordSpan);
		chordSpan.setScrollEnabled(false);
		this.add(pauseChance);
		pauseChance.setScrollEnabled(false);

		this.add(swingPercent);
		swingPercent.setScrollEnabled(false);

		this.add(exceptionChance);
		exceptionChance.setScrollEnabled(false);

		this.add(minMaxVelSlider);


		addOffsetAndDelayControls(false);


		this.add(patternSeedLabel);
		this.add(patternSeed);

		this.add(new JLabel("Midi ch. 10"));

		getInstrumentBox().box().setToolTipText("test");
		//toggleableComponents.add(useMelodyNotePattern);
		toggleableComponents.remove(patternShift);
		initDefaultsPost();
	}

	public DrumPanel(ActionListener l) {
		initComponents(l);
	}

	public DrumPart toDrumPart(int lastRandomSeed) {
		DrumPart part = new DrumPart();
		part.setFromPanel(this, lastRandomSeed);

		part.setVelocityPattern(getIsVelocityPattern());
		return part;
	}

	public void setFromInstPart(InstPart p) {
		DrumPart part = (DrumPart) p;

		setDefaultsFromInstPart(part);

		setIsVelocityPattern(part.isVelocityPattern());

	}

	public boolean getIsVelocityPattern() {
		return isVelocityPattern.isSelected();
	}

	public void setIsVelocityPattern(boolean val) {
		this.isVelocityPattern.setSelected(val);
	}


	public void transitionToPool(String[] pool) {
		instrument.changeInstPoolMapping(pool);
	}

	@Override
	public InstPart toInstPart(int lastRandomSeed) {
		return toDrumPart(lastRandomSeed);
	}

	@Override
	public int getPartNum() {
		return 4;
	}

	@Override
	protected Pair<List<Integer>, Map<Integer, Integer>> makeMappedRhythmGrid() {
		int weightMultiplier = VibeComposerGUI.PUNCHY_DRUMS.contains(getInstrument()) ? 3
				: (DrumDefaults.getOrder(getInstrument()) != 2 ? 2 : 1);
		Pair<List<Integer>, Map<Integer, Integer>> mapped = super.makeMappedRhythmGrid();
		List<Integer> baseGrid = mapped.getLeft();
		for (int i = 0; i < baseGrid.size(); i++) {
			if (baseGrid.get(i) != null && baseGrid.get(i) > 0) {
				baseGrid.set(i, weightMultiplier);
			}
		}

		return mapped;
	}

	@Override
	public Class<? extends InstPart> getPartClass() {
		return DrumPart.class;
	}
}
