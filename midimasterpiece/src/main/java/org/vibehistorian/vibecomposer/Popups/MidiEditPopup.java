package org.vibehistorian.vibecomposer.Popups;

import jm.music.data.Note;
import jm.music.data.Score;
import org.apache.commons.lang3.tuple.Pair;
import org.vibehistorian.vibecomposer.Components.CheckButton;
import org.vibehistorian.vibecomposer.Components.MidiDropPane;
import org.vibehistorian.vibecomposer.Components.MidiEditArea;
import org.vibehistorian.vibecomposer.Components.MidiListCellRenderer;
import org.vibehistorian.vibecomposer.Components.ScrollComboBox;
import org.vibehistorian.vibecomposer.Helpers.FileTransferHandler;
import org.vibehistorian.vibecomposer.Helpers.PartExt;
import org.vibehistorian.vibecomposer.Helpers.PatternMap;
import org.vibehistorian.vibecomposer.Helpers.PhraseExt;
import org.vibehistorian.vibecomposer.Helpers.PhraseNote;
import org.vibehistorian.vibecomposer.Helpers.PhraseNotes;
import org.vibehistorian.vibecomposer.Helpers.UsedPattern;
import org.vibehistorian.vibecomposer.JMusicUtilsCustom;
import org.vibehistorian.vibecomposer.LG;
import org.vibehistorian.vibecomposer.MidiGenerator;
import org.vibehistorian.vibecomposer.MidiUtils;
import org.vibehistorian.vibecomposer.MidiUtils.ScaleMode;
import org.vibehistorian.vibecomposer.OMNI;
import org.vibehistorian.vibecomposer.Panels.InstPanel;
import org.vibehistorian.vibecomposer.Parts.ArpPart;
import org.vibehistorian.vibecomposer.Parts.BassPart;
import org.vibehistorian.vibecomposer.Parts.ChordPart;
import org.vibehistorian.vibecomposer.Parts.DrumPart;
import org.vibehistorian.vibecomposer.Parts.InstPart;
import org.vibehistorian.vibecomposer.Parts.MelodyPart;
import org.vibehistorian.vibecomposer.Section;
import org.vibehistorian.vibecomposer.VibeComposerGUI;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class MidiEditPopup extends CloseablePopup {

	public static int highlightModeChoice = 3;
	public static int snapToTimeGridChoice = 2;
	public static boolean snapToGridChoice = true;
	public static boolean regenerateInPlaceChoice = false;
	public static boolean applyOnLoadChoice = false;
	public static boolean loadOnSelectChoice = false;
	public static boolean displayingPhraseMarginX = false;

	public static final int baseMargin = 5;
	public static int trackScope = 1;
	public static int trackScopeUpDown = 0;

	public ScrollComboBox<String> highlightMode = new ScrollComboBox<>(false);
	public ScrollComboBox<String> snapToTimeGrid = new ScrollComboBox<>(false);
	public CheckButton regenerateInPlaceOnChange = new CheckButton("R~ on Change",
			regenerateInPlaceChoice);
	public CheckButton applyOnLoad = new CheckButton("Apply on Load/Import", applyOnLoadChoice);
	public CheckButton loadOnSelect = new CheckButton("Load on Select", loadOnSelectChoice);
	public CheckButton snapToScaleGrid = new CheckButton("Snap to Scale", snapToGridChoice);
	public CheckButton displayPhraseMargins = new CheckButton("Margins", displayingPhraseMarginX);

	public ScrollComboBox<String> patternPartBox = new ScrollComboBox<>(false);
	public ScrollComboBox<Integer> patternPartOrderBox = new ScrollComboBox<>(false);
	public ScrollComboBox<PatternNameMarker> patternNameBox = new ScrollComboBox<>(false);

	public JLabel historyLabel = new JLabel("Edit History:");
	public ScrollComboBox<String> editHistoryBox = new ScrollComboBox<>(false);
	public CheckButton displayDrumHelper = new CheckButton("Drum Ghosts", false);

	public JList<File> generatedMidi;
	public boolean saveOnClose = true;
	public int notesHistoryIndex = 0;
	public List<PhraseNotes> notesHistory = new ArrayList<>();

	public int part = 0;
	public int partOrder = 0;

	MidiEditArea mvea = null;
	InstPanel ip = null;
	JTextField text = null;
	Section sec = null;

	public MidiEditPopup(Section section, int secPartNum, int secPartOrder) {
		super("Edit MIDI Phrase (Graphical)", 14);
		sec = section;
		saveOnClose = true;
		trackScopeUpDown = 0;
		LG.i("Midi Edit Popup, Part: " + secPartNum + ", Order: " + secPartOrder);


		text = new JTextField("", 25);
		text.setEditable(false);

		JPanel allPanels = new JPanel();
		allPanels.setLayout(new BoxLayout(allPanels, BoxLayout.Y_AXIS));
		allPanels.setMaximumSize(new Dimension(1500, 750));

		JPanel buttonPanel = makeTopButtonPanel();

		JPanel buttonPanel2 = makePatternSavingPanel();

		setupIdentifiers(secPartNum, secPartOrder);

		JPanel mveaPanel = new JPanel();
		mveaPanel.setPreferredSize(new Dimension(1500, 600));
		mveaPanel.setMinimumSize(new Dimension(1500, 600));
		mvea = new MidiEditArea(126, 1, null);
		mvea.setRange(0,127);
		mvea.setPop(this);
		mvea.setPreferredSize(new Dimension(1500, 600));
		mveaPanel.add(mvea);

		PhraseNotes values = loadSecValues(secPartNum, secPartOrder);
		if (values == null || values.isEmpty()) {
			values = recomposePart(false);
			if (values == null) {
				new TemporaryInfoPopup("Recomposing produced no notes, quitting!", 1500);
				return;
			}
		}

		JPanel bottomSettingsPanel = bottomActionsPreferencesPanel(values);

		setCustomValues(values);

		values.remakeNoteStartTimes();

		for (PhraseNote pn : values) {
			if (pn.getStartTime() < 0) {
				displayingPhraseMarginX = true;
			} else if (pn.getStartTime() + pn.getDuration() > mvea.sectionLength) {
				displayingPhraseMarginX = true;
			}
		}
		displayPhraseMargins.setSelected(displayingPhraseMarginX);

		allPanels.add(buttonPanel);
		allPanels.add(buttonPanel2);
		allPanels.add(bottomSettingsPanel);

		allPanels.add(mveaPanel);

		addKeyboardControls(allPanels);

		//SwingUtils.setFrameLocation(frame, VibeComposerGUI.vibeComposerGUI.getLocation());
		frame.setLocation(VibeComposerGUI.vibeComposerGUI.getLocation());
		frame.add(allPanels);
		frame.pack();
		frame.setVisible(true);
	}

	private PhraseNotes loadSecValues(int secPartNum, int secPartOrder) {
		UsedPattern pat = sec.getPattern(secPartNum, secPartOrder);
		if (pat == null) {
			LG.i("Replacing pattern with section type pattern!");
			pat = new UsedPattern(secPartNum, secPartOrder, sec.getPatternType());
			sec.putPattern(secPartNum, secPartOrder, pat);
		}
		LG.i("Loading pattern: " + pat.toString());
		PhraseNotes values = VibeComposerGUI.guiConfig.getPattern(pat);
		if (values == null) {
			LG.e("-----------------------LoadSecValues returns null!--------------");
		} else {
			setSelectedPattern(pat);
		}
		return values;
	}

	private void addKeyboardControls(JPanel allPanels) {
		Action undoAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				undo();
			}
		};
		Action redoAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				redo();
			}
		};

		Action deleteAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				deleteSelected();
			}
		};
		Action selectAllAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				selectAll();
			}
		};
		Action transposeSelectedAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				transposeSelected();
			}
		};
		allPanels.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
		allPanels.getActionMap().put("undo", undoAction);
		allPanels.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");
		allPanels.getActionMap().put("redo", redoAction);
		allPanels.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		allPanels.getActionMap().put("delete", deleteAction);
		allPanels.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "selectAll");
		allPanels.getActionMap().put("selectAll", selectAllAction);
		allPanels.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK),
				"transposeSelected");
		allPanels.getActionMap().put("transposeSelected", transposeSelectedAction);
	}

	private JPanel makeTopButtonPanel() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(0, 5, 0, 0));
		buttonPanel.setPreferredSize(new Dimension(1500, 50));

		buttonPanel.add(VibeComposerGUI.makeButton("Rand. Pitch", e -> {
			int size = mvea.getValues().size();
			boolean successRandGenerator = false;
			/*if (butt != null && butt.getRandGenerator() != null) {
				List<Integer> randValues = null;
				try {
					randValues = butt.getRandGenerator().apply(new Object());
				} catch (Exception exc) {
					LG.d("Random generator is not ready!");
				}
				if (randValues != null && !randValues.isEmpty()) {
					mvea.getValues().clear();
					mvea.getValues().addAll(randValues);
					successRandGenerator = true;
				}
			
			}*/
			if (!successRandGenerator) {
				Random rnd = new Random();
				for (int i = 0; i < size; i++) {
					if (mvea.getValues().get(i).getPitch() >= 0) {
						int pitch = rnd
								.nextInt(mvea.currentMax - mvea.currentMin + 1 - trackScope * baseMargin * 2)
								+ mvea.currentMin + baseMargin * trackScope;
						if (isSnapPitch()) {
							int closestNormalized = MidiUtils
									.getClosestFromList(MidiUtils.MAJ_SCALE, pitch % 12);

							mvea.getValues().get(i)
									.setPitch(MidiUtils.octavePitch(pitch) + closestNormalized);
						} else {
							mvea.getValues().get(i).setPitch(pitch);
						}

					}
				}
			}
			saveToHistory();
			repaintMvea();
		}));

		buttonPanel.add(VibeComposerGUI.makeButton("Rand. Velocity", e -> {
			Random rand = new Random();
			InstPanel ip = VibeComposerGUI.getAffectedPanels(part).get(partOrder);
			int velmin = ip.getVelocityMin();
			int velmax = ip.getVelocityMax();
			mvea.getValues().forEach(n -> n.setDynamic(rand.nextInt(velmax - velmin + 1) + velmin));


			saveToHistory();
			repaintMvea();
		}));

		buttonPanel.add(VibeComposerGUI.makeButton("Undo", e -> undo()));
		buttonPanel.add(VibeComposerGUI.makeButton("Redo", e -> redo()));

		JPanel midiDragDropPanel = makeMidiDragDropPanel();
		buttonPanel.add(midiDragDropPanel);

		return buttonPanel;
	}

	public boolean isSnapPitch() {
		return snapToScaleGrid.isSelected() && part != 4;
	}

	private JPanel makeMidiDragDropPanel() {
		JPanel midiDragDropPanel = new JPanel();
		midiDragDropPanel.setLayout(new GridLayout(0, 1));

		generatedMidi = new JList<File>();
		MidiListCellRenderer dndRenderer = new MidiListCellRenderer();
		dndRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		generatedMidi.setCellRenderer(dndRenderer);
		generatedMidi.setBorder(new BevelBorder(BevelBorder.RAISED));
		generatedMidi.setTransferHandler(new FileTransferHandler(e -> {
			return buildMidiFileFromNotes();
		}));
		generatedMidi.setDragEnabled(true);
		generatedMidi.setListData(new File[] { new File("tempMidi.mid") });

		midiDragDropPanel.add(generatedMidi);
		midiDragDropPanel.add(new MidiDropPane(e -> {
			PhraseNotes pn = new PhraseNotes(e);
			double length = pn.stream().map(f -> f.getRv()).mapToDouble(f -> f).sum();
			LG.i("Dropped MIDI Length: " + length);
			if (length > mvea.sectionLength + MidiGenerator.DBL_ERR) {
				return null;
			} else if (length < mvea.sectionLength - MidiGenerator.DBL_ERR) {
				PhraseNote lastNote = pn.get(pn.size() - 1);
				lastNote.setRv(lastNote.getRv() + mvea.sectionLength - length);
			}
			pn.forEach(f -> f.setPitch(f.getPitch() - VibeComposerGUI.transposeScore.getInt()));
			setCustomValues(pn.copy());

			return pn;
		}));
		return midiDragDropPanel;
	}

	private JPanel bottomActionsPreferencesPanel(PhraseNotes values) {
		JPanel bottomSettingsPanel = new JPanel();
		bottomSettingsPanel.setLayout(new BoxLayout(bottomSettingsPanel, BoxLayout.X_AXIS));
		bottomSettingsPanel.add(text);
		/*bottomSettingsPanel.add(VibeComposerGUI.makeButton("Apply", e -> {
			if (StringUtils.isNotEmpty(text.getText())) {
				try {
					String[] textSplit = text.getText().split(",");
					List<Integer> nums = new ArrayList<>();
					for (String s : textSplit) {
						nums.add(Integer.valueOf(s));
					}
					for (int i = 0; i < nums.size() && i < mvea.getValues().size(); i++) {
						mvea.getValues().get(i).setPitch(nums.get(i));
					}
					repaintMvea();
				} catch (Exception exc) {
					LG.d("Incorrect text format, cannot convert to list of numbers.");
				}
			}
		}));*/


		ScrollComboBox.addAll(
				new String[] { "No Highlight", "Scale/Key", "Chords", "Scale/Key and Chords" },
				highlightMode);
		highlightMode.setSelectedIndex(highlightModeChoice);
		highlightMode.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				highlightModeChoice = highlightMode.getSelectedIndex();
				mvea.setAndRepaint();
			}
		});

		ScrollComboBox.addAll(new String[] { "1/32", "1/24", "1/16", "1/12", "1/8", "1/6", "1/4",
				"1/3", "1/2", "1" }, snapToTimeGrid);
		snapToTimeGrid.setSelectedIndex(snapToTimeGridChoice);
		snapToTimeGrid.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				snapToTimeGridChoice = snapToTimeGrid.getSelectedIndex();
				mvea.setAndRepaint();
			}
		});

		snapToScaleGrid.setFunc(e -> {
			snapToGridChoice = snapToScaleGrid.isSelected();
		});
		regenerateInPlaceOnChange.setFunc(e -> {
			regenerateInPlaceChoice = regenerateInPlaceOnChange.isSelected();
			VibeComposerGUI.manualArrangement.setSelected(true);
			VibeComposerGUI.manualArrangement.repaint();
		});
		applyOnLoad.setFunc(e -> {
			applyOnLoadChoice = applyOnLoad.isSelected();
		});
		loadOnSelect.setFunc(e -> {
			loadOnSelectChoice = loadOnSelect.isSelected();
		});
		displayDrumHelper.setFunc(e -> {
			repaintMvea();
		});

		displayPhraseMargins.setFunc(e -> {
			displayingPhraseMarginX = displayPhraseMargins.isSelected();
			repaintMvea();
		});

		patternNameBox.setFunc(e -> {
			if (loadOnSelectChoice) {
				loadNotes(true);
			}
		});

		bottomSettingsPanel.add(loadOnSelect);
		bottomSettingsPanel.add(applyOnLoad);
		bottomSettingsPanel.add(regenerateInPlaceOnChange);
		bottomSettingsPanel.add(displayDrumHelper);
		bottomSettingsPanel.add(displayPhraseMargins);
		bottomSettingsPanel.add(new JLabel("  Highlight Mode:"));
		bottomSettingsPanel.add(highlightMode);
		bottomSettingsPanel.add(new JLabel("  Snap To Time:"));
		bottomSettingsPanel.add(snapToTimeGrid);
		bottomSettingsPanel.add(snapToScaleGrid);

		JButton recompButt = new JButton("Recompose Part");
		recompButt.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent evt) {
				recomposePart(SwingUtilities.isMiddleMouseButton(evt));
			}
		});
		bottomSettingsPanel.add(recompButt);

		editHistoryBox.setFunc(e -> {
			if (notesHistoryIndex != editHistoryBox.getSelectedIndex()) {
				loadFromHistory(editHistoryBox.getSelectedIndex());
			}
		});
		bottomSettingsPanel.add(historyLabel);
		bottomSettingsPanel.add(editHistoryBox);

		return bottomSettingsPanel;
	}

	private JPanel makePatternSavingPanel() {
		JPanel buttonPanel2 = new JPanel();
		buttonPanel2.setLayout(new GridLayout(0, 10, 0, 0));
		buttonPanel2.setPreferredSize(new Dimension(1500, 50));

		ScrollComboBox.addAll(VibeComposerGUI.instNames, patternPartBox);
		patternPartBox.setFunc(e -> loadPartOrders());
		patternPartOrderBox.setFunc(e -> loadNames());
		//patternNameBox.setFunc(e -> loadNotes());

		buttonPanel2.add(patternPartBox);
		buttonPanel2.add(patternPartOrderBox);
		buttonPanel2.add(patternNameBox);

		loadPartOrders();
		loadNames();

		buttonPanel2.add(VibeComposerGUI.makeButton("Load Pattern", e -> {
			loadNotes(true);
		}));
		buttonPanel2.add(VibeComposerGUI.makeButton("Import Pattern", e -> {
			loadNotes(false);
		}));
		buttonPanel2.add(VibeComposerGUI.makeButton("Save Pattern", e -> {
			saveNotes(false, false);
		}));
		buttonPanel2.add(VibeComposerGUI.makeButton("<html>Save Pattern<br>+ Apply</html>", e -> {
			saveNotes(false);
		}));
		buttonPanel2.add(VibeComposerGUI
				.makeButtonMoused("<html>Save Pattern as New<br>+ Apply</html>", e -> {
					if (SwingUtilities.isLeftMouseButton(e)) {
						saveNotes(true);
					} else {
						new TextProcessingPopup("Pattern - New", patternName -> {
							PatternNameMarker pnm = new PatternNameMarker(patternName, true);
							patternNameBox.addItem(pnm);
							patternNameBox.setVal(pnm);
							// store in current part as new
							VibeComposerGUI.guiConfig.getPatternMaps().get(part).put(partOrder,
									patternName, getValues());
							apply();
							setSelectedPattern(sec.getPattern(part, partOrder));
						});
					}

				}));

		/*buttonPanel2.add(VibeComposerGUI.makeButton("Apply", e -> {
			apply();
		}));*/

		buttonPanel2.add(VibeComposerGUI.makeButton("Apply 'NONE'", e -> {
			applyNone();
		}));

		buttonPanel2.add(VibeComposerGUI.makeButton("<html>Close<br>(w/o Applying)</html>", e -> {
			saveOnClose = false;
			close();
		}));
		return buttonPanel2;
	}

	private void loadPartOrders() {
		loadPartOrders(patternPartBox, patternPartOrderBox, patternNameBox);
	}

	public static void loadPartOrders(ScrollComboBox<String> parts,
			ScrollComboBox<Integer> partOrders, ScrollComboBox<PatternNameMarker> names) {
		names.removeAllItems();
		partOrders.removeAllItems();
		int part = parts.getSelectedIndex();
		if (VibeComposerGUI.guiConfig.getPatternMaps().size() <= part) {
			return;
		}
		ScrollComboBox.addAll(VibeComposerGUI.guiConfig.getPatternMaps().get(part).getKeys(),
				partOrders);
		if (partOrders.getItemCount() > 0) {
			partOrders.setSelectedIndex(0);
		}
	}

	private void loadNames() {
		loadNames(patternPartBox, patternPartOrderBox, patternNameBox);
	}

	public static void loadNames(ScrollComboBox<String> parts, ScrollComboBox<Integer> partOrders,
			ScrollComboBox<PatternNameMarker> names) {
		names.removeAllItems();
		int part = parts.getSelectedIndex();
		if (VibeComposerGUI.guiConfig.getPatternMaps().size() <= part) {
			return;
		}
		Integer partOrder = partOrders.getSelectedItem();
		if (partOrder == null) {
			return;
		}
		Set<String> patternNames = VibeComposerGUI.guiConfig.getPatternMaps().get(part)
				.getPatternNames(partOrder);
		List<PatternNameMarker> namesWithMarkers = patternNames.stream()
				.map(e -> new PatternNameMarker(e,
						VibeComposerGUI.guiConfig.getPatternRaw(part, partOrder, e) != null))
				.collect(Collectors.toList());
		Collections.sort(namesWithMarkers);
		ScrollComboBox.addAll(namesWithMarkers, names);
		if (names.getItemCount() > 0) {
			names.setSelectedIndex(0);
		}
	}

	private void loadNotes(boolean overwrite) {
		PhraseNotes pn = getPatternMap().get(patternPartOrderBox.getSelectedItem(),
				patternNameBox.getSelectedItem().name);
		if (pn == null || pn.isEmpty()) {
			return;
		}

		if (overwrite) {
			setCustomValues(pn);
		} else {
			// import instead
			UsedPattern pat = sec.getPattern(part, partOrder);
			PhraseNotes oldPn = VibeComposerGUI.guiConfig.getPattern(pat);

			if (oldPn != null) {
				pn.remakeNoteStartTimes();
				oldPn.remakeNoteStartTimes();

				for (PhraseNote n : pn) {
					int closestNormalized = MidiUtils.getClosestFromList(MidiUtils.MAJ_SCALE,
							n.getPitch() % 12);
					if (isSnapPitch()) {
						n.setPitch(MidiUtils.octavePitch(n.getPitch()) + closestNormalized);
					}
					n.setRv(0);
					n.setOffset(n.getStartTime());
					oldPn.add(0, n);
				}

				setCustomValues(oldPn);
			} else {
				setCustomValues(pn);
			}
			boolean loadOnSelectChoiceOLD = loadOnSelectChoice;
			loadOnSelectChoice = false;
			setSelectedPattern(pat);
			loadOnSelectChoice = loadOnSelectChoiceOLD;
		}
		if (applyOnLoadChoice) {
			apply();
		} else {
			repaintMvea();
		}
	}

	public void saveNotes(boolean newName) {
		saveNotes(newName, true);
	}

	public void saveNotes(boolean newName, boolean apply) {
		String patternName = (newName) ? UsedPattern.generateName(part, partOrder)
				: patternNameBox.getSelectedItem().name;

		if (newName) {
			PatternNameMarker pnm = new PatternNameMarker(patternName, true);
			patternNameBox.addItem(pnm);
			patternNameBox.setValRaw(pnm);
			// store in current part as new
			VibeComposerGUI.guiConfig.getPatternMaps().get(part).put(partOrder, patternName,
					getValues());
		} else {
			// store in selected part
			getPatternMap().put(patternPartOrderBox.getSelectedItem(), patternName, getValues());
		}
		if (apply) {
			apply();
			if (newName) {
				setSelectedPattern(sec.getPattern(part, partOrder));
			}
		}
	}

	private void setSelectedPattern(UsedPattern pat) {
		LG.i("Setting pattern selection: " + pat.toString());
		patternPartBox.setSelectedIndex(pat.getPart());
		patternPartOrderBox.setVal(pat.getPartOrder());
		patternNameBox.setValRaw(new PatternNameMarker(pat.getName(), true));
	}

	private UsedPattern getSelectedPattern() {
		return new UsedPattern(patternPartBox.getSelectedIndex(),
				patternPartOrderBox.getSelectedItem(), patternNameBox.getSelectedItem().name);
	}

	private UsedPattern getSelectedPatternNone() {
		return new UsedPattern(patternPartBox.getSelectedIndex(),
				patternPartOrderBox.getSelectedItem(), UsedPattern.NONE);
	}

	public PatternMap getPatternMap() {
		return VibeComposerGUI.guiConfig.getPatternMaps().get(patternPartBox.getSelectedIndex());
	}

	public void setCustomValues(PhraseNotes values) {

		if (values == null || values.isEmpty()) {
			new TemporaryInfoPopup("Empty pattern!", 1000);
			return;
		}

		int vmin = -1 * baseMargin * trackScope;
		int vmax = baseMargin * trackScope;
		if (!values.isEmpty()) {
			vmin += values.stream().map(e -> e.getPitch()).filter(e -> e >= 0).mapToInt(e -> e)
					.min().getAsInt();
			vmax += values.stream().map(e -> e.getPitch()).filter(e -> e >= 0).mapToInt(e -> e)
					.max().getAsInt();
		}
		mvea.setCurrentMin(Math.min(mvea.currentMin, vmin));
		mvea.setCurrentMax(Math.max(mvea.currentMax, vmax));


		mvea.part = part;
		mvea.marginX = (mvea.part == 4) ? 160 : 80;
		mvea.setValues(values);
		saveToHistory();

		repaintMvea();
	}

	public void setupIdentifiers(int secPartNum, int secPartOrder) {
		part = secPartNum;
		partOrder = secPartOrder;
		UsedPattern pat = sec.getPattern(part, partOrder);

		if (pat != null && pat.isCustom(part, partOrder)) {
			setSelectedPattern(pat);
		} else {
			patternPartBox.setSelectedIndex(part);
			patternPartOrderBox.setVal(partOrder);
			String patName = sec.getPatternName(part, partOrder);
			patternNameBox.setValRaw(new PatternNameMarker(patName,
					VibeComposerGUI.guiConfig.getPatternRaw(part, partOrder, patName) != null));
		}
		frame.setTitle("Edit MIDI Phrase (Graphical) | Part: " + VibeComposerGUI.instNames[part]
				+ ", Order: " + secPartOrder);
	}

	private File buildMidiFileFromNotes() {
		Pair<ScaleMode, Integer> scaleKey = VibeComposerGUI
				.keyChangeAt(VibeComposerGUI.actualArrangement.getSections().indexOf(sec));

		PhraseExt phr = mvea.getValues().makePhrase();

		List<Note> notes = phr.getNoteList();
		int extraTranspose = 0;
		InstPanel ip = VibeComposerGUI.getInstList(part).get(partOrder);
		if (scaleKey != null) {
			MidiUtils.transposeNotes(notes, ScaleMode.IONIAN.noteAdjustScale,
					scaleKey.getLeft().noteAdjustScale,
					VibeComposerGUI.transposedNotesForceScale.isSelected());
			extraTranspose = scaleKey.getRight();
		}
		final int finalExtraTranspose = extraTranspose;
		notes.forEach(e -> {
			int pitch = e.getPitch() + VibeComposerGUI.transposeScore.getInt() + finalExtraTranspose
					+ ip.getTranspose();
			e.setPitch(pitch);
		});
		Score scr = new Score();
		PartExt prt = PartExt.makeFillerPart();
		prt.add(phr);
		scr.add(prt);

		JMusicUtilsCustom.midi(scr, "tempMidi.mid");
		File f = new File("tempMidi.mid");
		return f;
	}

	public void deleteSelected() {
		if (mvea.selectedNotes.size() > 0) {
			mvea.selectedNotes.forEach(e -> {
				if (e.getRv() < MidiGenerator.DBL_ERR) {
					mvea.getValues().remove(e);
				} else {
					e.setPitch(Note.REST);
				}
			});
			mvea.selectedNotes.clear();
			mvea.selectedNotesCopy.clear();
			mvea.reset();
			saveToHistory();

			if (MidiEditPopup.regenerateInPlaceChoice) {
				VibeComposerGUI.vibeComposerGUI.regenerateInPlace();
			}
		}
	}

	public void transposeSelected() {
		new TextProcessingPopup("Transpose amount", e -> {
			if (mvea == null || mvea.selectedNotes == null || mvea.selectedNotes.isEmpty()) {
				new TemporaryInfoPopup("No notes selected!", 1000);
				return;
			}
			try {
				int parsedInt = Integer.valueOf(e);
				for (PhraseNote n : mvea.selectedNotes) {
					// 0..127 midi value
					n.setPitch(OMNI.clampMidi(n.getPitch() + parsedInt));
				}
				setCustomValues(mvea.getValues());

				if (MidiEditPopup.regenerateInPlaceChoice) {
					mvea.selectedNotes.clear();
					mvea.selectedNotesCopy.clear();
					VibeComposerGUI.vibeComposerGUI.regenerateInPlace();
				}
			} catch (Exception ex) {
				new TemporaryInfoPopup("Invalid number entered!", 1500);
			}
		});
	}

	public void selectAll() {
		mvea.selectedNotes.clear();
		mvea.selectedNotes.addAll(mvea.getValues().stream().filter(e -> e.getPitch() >= 0)
				.collect(Collectors.toList()));
		mvea.makeSelectedNotesCopy();
	}

	public void apply() {
		if (mvea != null && mvea.getValues() != null) {
			// TODO
			UsedPattern pat = getSelectedPattern();
			PhraseNotes pn = VibeComposerGUI.guiConfig.getPatternRaw(pat);
			if (pn != null) {
				pn.setApplied(true);
				sec.putPattern(part, partOrder, pat);
				LG.i("Applied: " + pat.toString());

				repaintMvea();
				VibeComposerGUI.scrollableArrangementActualTable.repaint();
			} else {
				LG.e("Failed to apply pattern, null: " + pat.toString());
			}
		}
	}

	public void applyNone() {
		if (mvea != null && mvea.getValues() != null) {
			UsedPattern pat = getSelectedPattern();
			VibeComposerGUI.guiConfig.getPatternRaw(pat).setApplied(false);
			sec.putPattern(part, partOrder, new UsedPattern(part, partOrder, UsedPattern.NONE));
			repaintMvea();
			VibeComposerGUI.scrollableArrangementActualTable.repaint();
		}
	}

	public void undo() {
		loadFromHistory(notesHistoryIndex - 1);

		if (MidiEditPopup.regenerateInPlaceChoice) {
			mvea.selectedNotes.clear();
			mvea.selectedNotesCopy.clear();
			VibeComposerGUI.vibeComposerGUI.regenerateInPlace();
		}
	}

	public void redo() {
		loadFromHistory(notesHistoryIndex + 1);
	}

	public void setup(Section sec) {
		LG.i("Midi Edit Popup Setup, Part: " + part + ", Order: " + partOrder);

		if (!sec.containsPattern(part, partOrder)) {
			close();
			LG.i("MidiEditPopup cannot be setup - section doesn't contain the part/partOrder!");
			return;
		}

		setSec(sec);
		PhraseNotes values = loadSecValues(part, partOrder);
		/*if (values == null) {
			LG.i("Setup - not applicable, or not custom midi!");
			apply();
			repaintMvea();
			return;
		}*/
		if (values == null || values.isEmpty()) {
			LG.e("-----------------------Part needed to be recomposed on setup!--------------");
			values = recomposePart(false);
		}
		setCustomValues(values);

		LG.i("Custom MIDI setup successful: " + part + ", " + partOrder);
	}

	public void saveToHistory() {
		if (notesHistoryIndex + 1 < notesHistory.size() && notesHistory.size() > 0) {
			notesHistory = notesHistory.subList(0, notesHistoryIndex + 1);
		}

		notesHistory.add(mvea.getValues().copy());
		notesHistoryIndex = notesHistory.size() - 1;
		updateHistoryBox();
	}

	public void loadFromHistory(int index) {
		if (notesHistoryIndex == index) {
			return;
		}
		LG.i("Loading notes with index: " + index);
		if (notesHistory.size() > 0 && index >= 0 && index < notesHistory.size()) {
			mvea.setValues(notesHistory.get(index).copy());
			notesHistoryIndex = index;
			editHistoryBox.setSelectedIndex(index);
			repaintMvea();
		}
	}

	public PhraseNotes recomposePart(boolean isRandom) {
		MidiGenerator mg = VibeComposerGUI.melodyGen;
		UsedPattern oldPattern = sec.getPattern(part, partOrder);
		try {

			mg.storeGlobalParts();
			mg.replaceWithSectionCustomChordDurations(sec);

			mg.progressionDurations = new ArrayList<>(sec.getGeneratedDurations());

			sec.putPattern(part, partOrder, getSelectedPatternNone());

			LG.i("Chord prog: " + mg.chordProgression.size());
			InstPart ip = MidiGenerator.gc.getInstPartList(part).stream()
					.filter(e -> e.getOrder() == partOrder).findFirst().get();

			int seed = ip.getPatternSeed();
			if (isRandom) {
				ip.setPatternSeed(new Random().nextInt());
			}
			List<Integer> variations = sec.getVariation(part, partOrder);
			switch (part) {
			case 0:
				mg.fillMelodyFromPart((MelodyPart) ip, mg.chordProgression, mg.rootProgression,
						sec.getTypeMelodyOffset(), sec, variations, false, VibeComposerGUI.melodyBlockChoicePreference.getValues());
				break;
			case 1:
				mg.fillBassFromPart((BassPart) ip, mg.rootProgression, sec, variations);
				break;
			case 2:
				mg.fillChordsFromPart((ChordPart) ip, mg.chordProgression, sec, variations);
				break;
			case 3:
				mg.fillArpFromPart((ArpPart) ip, mg.chordProgression, sec, variations);
				break;
			case 4:
				mg.fillDrumsFromPart((DrumPart) ip, mg.chordProgression, sec.isClimax(), sec,
						variations);
				break;
			default:
				throw new IllegalArgumentException("Invalid part: " + part);
			}
			ip.setPatternSeed(seed);

			mg.replaceChordsDurationsFromBackup();
			mg.restoreGlobalPartsToGuiConfig();

		} catch (Exception e) {
			// error will be revealed in next popup
		}
		UsedPattern generatedPat = sec.getPattern(part, partOrder);
		LG.i("Recompose, new pattern: " + generatedPat.toString());
		PhraseNotes pn = MidiGenerator.gc.getPattern(generatedPat);
		VibeComposerGUI.guiConfig.putPattern(generatedPat, pn);

		mvea.currentMin = 110;
		mvea.currentMax = 10;

		if (oldPattern != null) {
			sec.putPattern(part, partOrder, oldPattern);
		}

		if (pn == null) {
			new TemporaryInfoPopup("Recomposing produced no notes, quitting!", 1500);
			saveOnClose = false;
			close();
			return new PhraseNotes(Collections.singletonList(new Note("C4")));
		} else {
			setCustomValues(pn);
			//mg.fill
			return getValues();
		}
	}

	public void repaintMvea() {
		mvea.setAndRepaint();
		mvea.sectionLength = mvea.getValues().stream().map(e -> e.getRv())
				.mapToDouble(e -> e).sum();
		if (sec != null) {
			UsedPattern pat = sec.getPattern(part, partOrder);
			String patName = (pat != null) ? pat.toString() : "<No pattern>";
			PhraseNotes pn = VibeComposerGUI.guiConfig.getPatternRaw(pat);
			patName += (pn != null && pn.isApplied()) ? " - Applied" : " - Not Applied";
			text.setText(patName);
		}
	}

	public void updateHistoryBox() {
		editHistoryBox.removeAllItems();
		for (int i = 0; i < notesHistory.size(); i++) {
			editHistoryBox.addItem(i + " ("
					+ notesHistory.get(i).stream().filter(e -> e.getPitch() >= 0).count() + ")");
		}
		editHistoryBox.setSelectedIndex(editHistoryBox.getItemCount() - 1);
	}

	public void setParent(InstPanel parent) {
		this.ip = parent;
	}

	public InstPanel getParent() {
		return ip;
	}

	@Override
	protected void addFrameWindowOperation() {
		frame.addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {
				// Auto-generated method stub

			}

			@Override
			public void windowClosing(WindowEvent e) {
				if (frame.isVisible() && saveOnClose) {
					saveNotes(false);
				}
			}

			@Override
			public void windowClosed(WindowEvent e) {
				// Auto-generated method stub

			}

			@Override
			public void windowIconified(WindowEvent e) {
				// Auto-generated method stub

			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				// Auto-generated method stub

			}

			@Override
			public void windowActivated(WindowEvent e) {
				// Auto-generated method stub

			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				// Auto-generated method stub

			}

		});
	}

	public Section getSec() {
		return sec;
	}

	public void setSec(Section sec) {
		this.sec = sec;
	}

	public boolean isSectionCustom() {
		return sec.containsPattern(part, partOrder)
				&& sec.getPattern(part, partOrder).isCustom(part, partOrder);
	}

	public PhraseNotes getValues() {
		return mvea.getValues();
	}

	public static class PatternNameMarker implements Comparable<PatternNameMarker> {
		public String name = "";
		public boolean loadable = false;

		public PatternNameMarker(String name, boolean loadable) {
			super();
			this.name = name;
			this.loadable = loadable;
		}

		@Override
		public String toString() {
			return name + (!loadable ? " (!)" : "");
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, loadable);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;

			if (obj == null || !(obj instanceof PatternNameMarker)) {
				return false;
			}
			PatternNameMarker other = (PatternNameMarker) obj;
			return (loadable == other.loadable) && name.equals(other.name);
		}


		@Override
		public int compareTo(PatternNameMarker o) {
			if (o == null) {
				return -1;
			}
			if (loadable && !o.loadable) {
				return -1;
			} else if (!loadable) {
				return 1;
			}

			boolean name1Base = UsedPattern.BASE_PATTERNS_SET.contains(name);
			boolean name2Base = UsedPattern.BASE_PATTERNS_SET.contains(o.name);
			if (name1Base && !name2Base) {
				return 1;
			} else if (!name1Base) {
				return -1;
			}
			return 0;
		}
	}

}
