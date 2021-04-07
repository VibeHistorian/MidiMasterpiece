/* --------------------
* @author VibeHistorian
* ---------------------
*/
package org.vibehistorian.midimasterpiece.midigenerator;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.BevelBorder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.vibehistorian.midimasterpiece.midigenerator.MidiUtils.PARTS;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.sun.media.sound.AudioSynthesizer;

// main class

public class MidiGeneratorGUI extends JFrame
		implements ActionListener, ItemListener, WindowListener {
	
	private static final long serialVersionUID = -677536546851756969L;
	
	private static final String SOUNDBANK_DEFAULT = "MuseScore_General.sf2";
	private static final String MIDIS_FOLDER = "midis";
	
	private static final double[] MILISECOND_ARRAY = { 0, 500, 333, 250, 200, 166, 125, 83, 62,
			31 };
	private static final double[] MILISECOND_ARRAY_SMALL = { 0, 63, 125, 250, 333 };
	private static final double[] MILISECOND_ARRAY_LARGE = { 500, 625, 750 };
	private static final double[] MILISECOND_MULTIPLIER_ARRAY = { 1, 1.5, 2, 3, 4 };
	
	private static boolean isDarkMode = false;
	
	private static List<JSeparator> separators = new ArrayList<>();
	
	private static int chordGenPanelStart = 50;
	private static int drumGenPanelStart = 200;
	private static int arpGenPanelStart = 100;
	
	JLabel mainTitle;
	JLabel subTitle;
	JButton switchDarkMode;
	Color messageColorDarkMode = new Color(200, 200, 200);
	Color messageColorLightMode = new Color(120, 120, 200);
	
	private Synthesizer synth = null;
	
	private GUIConfig guiConfig = new GUIConfig();
	
	private List<DrumPanel> drumPanels = new ArrayList<>();
	private List<ChordPanel> chordPanels = new ArrayList<>();
	
	JTextField soundbankFilename;
	
	JTextField pieceLength;
	JTextField userChords;
	JTextField userChordsDurations;
	
	JLabel messageLabel;
	
	JList<File> generatedMidi;
	
	JScrollPane drumScrollPane;
	JScrollPane chordScrollPane;
	
	JTextField maxJump;
	JTextField maxExceptions;
	JTextField pauseChance;
	JTextField melodyPauseChance;
	JTextField secondArpPauseChance;
	
	JTextField randomDrumsCount;
	JTextField randomChordsCount;
	
	JTextField spiceChance;
	JTextField chordSlashChance;
	JCheckBox randomizeChordStrumsOnCompose;
	JTextField transposeScore;
	
	JComboBox<String> arpCount;
	JComboBox<String> secondArpMultiplier;
	JComboBox<String> secondArpOctaveAdjust;
	JCheckBox secondArpMultiplierRandom;
	
	JCheckBox spiceAllowDimAug;
	JCheckBox melodyFirstNoteFromChord;
	JCheckBox randomArpPattern;
	JCheckBox randomArpCount;
	JCheckBox addMelody;
	JCheckBox addChords;
	JCheckBox addArp1;
	JCheckBox addArp2;
	JCheckBox addBassRoots;
	JCheckBox addDrums;
	
	JCheckBox melodyLock;
	JCheckBox arp1Lock;
	JCheckBox arp2Lock;
	JCheckBox bassRootsLock;
	
	JComboBox<String> melodyInst;
	JComboBox<String> chordsInst;
	JComboBox<String> arp1Inst;
	JComboBox<String> arp2Inst;
	JComboBox<String> bassRootsInst;
	JComboBox<String> drumInst;
	
	JTextField userMelodySeed;
	JCheckBox randomMelodyOnRegenerate;
	
	JCheckBox randomDrumsOnCompose;
	JCheckBox randomDrumSlide;
	JCheckBox randomDrumPattern;
	
	JCheckBox randomChordsOnCompose;
	JCheckBox randomChordDelay;
	JCheckBox randomChordStrum;
	JCheckBox randomChordSplit;
	JCheckBox randomChordTranspose;
	JCheckBox randomChordPattern;
	
	JTextField chordRotationChance;
	
	JTextField velocityPatternChance;
	JTextField rotationChance;
	
	JCheckBox randomChordNote;
	JCheckBox minorScale;
	JCheckBox fixedLengthChords;
	JCheckBox userChordsEnabled;
	
	JCheckBox randomizeInstOnCompose;
	JCheckBox randomizeBmpTransOnCompose;
	
	JCheckBox arpMelodyLockInst;
	JCheckBox arp2LockInst;
	JCheckBox arpPatternRepeat;
	JCheckBox arpAllowPauses;
	
	JCheckBox arpAffectsBpm;
	
	JComboBox<String> firstChordSelection;
	JComboBox<String> lastChordSelection;
	
	JTextField mainBpm;
	JTextField randomSeed;
	int firstChord = 0;
	int lastChord = 0;
	Sequencer sequencer = null;
	int lastRandomSeed = 0;
	File currentMidi = null;
	
	private static GridBagConstraints constraints = new GridBagConstraints();
	
	public static void main(String args[]) {
		FlatDarculaLaf.install();
		isDarkMode = true;
		MidiGeneratorGUI midiGeneratorGUI = new MidiGeneratorGUI("General MIDI Generator (BETA)");
	}
	
	public MidiGeneratorGUI(String title) {
		super(title);
		
		// register the closebox event
		this.addWindowListener(this);
		
		setLayout(new GridBagLayout());
		//setPreferredSize(new Dimension(1400, 1000));
		
		//constraints.fill = GridBagConstraints.BOTH;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		initTitles(0, GridBagConstraints.CENTER);
		createHorizontalSeparator(15, this);
		// ---- INSTRUMENTS ----
		{
			// melody
			initMelodySettings(20, GridBagConstraints.WEST);
			initMelody(25, GridBagConstraints.WEST);
			createHorizontalSeparator(30, this);
			
			// chords
			initChordSettings(40, GridBagConstraints.WEST);
			initChords(50, GridBagConstraints.CENTER);
			createHorizontalSeparator(100, this);
			
			// arps
			initArps(110, GridBagConstraints.WEST);
			createHorizontalSeparator(150, this);
			
			// bass
			initBassRoots(170, GridBagConstraints.WEST);
			createHorizontalSeparator(175, this);
			
			// drums
			initDrumSettings(190, GridBagConstraints.WEST);
			initDrums(200, GridBagConstraints.CENTER);
			createHorizontalSeparator(290, this);
		}
		
		// ---- OTHER SETTINGS ----
		{
			// randomization buttons
			initRandomButtons(300, GridBagConstraints.CENTER);
			
			// bpm/transpose
			initBpmTrans(320, GridBagConstraints.CENTER);
			
			// chord settings - variety/spice
			initChordSettingsVariety(340, GridBagConstraints.CENTER);
			
			// chord settings - progressions
			initChordSettingsProgression(360, GridBagConstraints.CENTER);
			createHorizontalSeparator(370, this);
		}
		
		// ---- CONTROL PANEL -----
		initControlPanel(400, GridBagConstraints.CENTER);
		
		
		// ---- PLAY PANEL ----
		initPlayPanel(420, GridBagConstraints.CENTER);
		
		
		// --- GENERATED MIDI DRAG n DROP ---
		
		constraints.anchor = GridBagConstraints.CENTER;
		
		JPanel midiDragAndDropPanel = new JPanel();
		
		generatedMidi = new JList<File>();
		generatedMidi.setTransferHandler(new FileTransferHandler());
		generatedMidi.setDragEnabled(true);
		midiDragAndDropPanel.add(new JLabel("Midi Drag'N'Drop:"));
		midiDragAndDropPanel.add(generatedMidi);
		constraints.gridy = 960;
		add(midiDragAndDropPanel, constraints);
		
		// ---- MESSAGE PANEL ----
		
		JPanel messagePanel = new JPanel();
		messageLabel = new JLabel("Click something!");
		messageLabel.setForeground((isDarkMode) ? Color.CYAN : Color.BLUE);
		messageLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		messagePanel.add(messageLabel);
		constraints.gridy = 999;
		add(messagePanel, constraints);
		
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		int screenHeight = d.height;
		int screenWidth = d.width;
		setSize(screenWidth / 2, screenHeight / 2);
		setLocation(screenWidth / 4, screenHeight / 4);
		pack();
		setVisible(true);
		repaint();
	}
	
	private void initTitles(int startY, int anchorSide) {
		mainTitle = new JLabel("General MIDI Generator (Beta)");
		mainTitle.setForeground((isDarkMode) ? Color.CYAN : Color.BLUE);
		mainTitle.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		subTitle = new JLabel("by Vibe Historian");
		subTitle.setForeground((isDarkMode) ? Color.CYAN : Color.BLUE);
		subTitle.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		constraints.weightx = 100;
		constraints.weighty = 100;
		constraints.gridx = 0;
		constraints.gridy = startY;
		constraints.gridwidth = 3;
		constraints.gridheight = 1;
		constraints.anchor = anchorSide;
		add(mainTitle, constraints);
		constraints.gridy = 1;
		add(subTitle, constraints);
		
		JPanel p5 = new JPanel();
		
		constraints.gridy = startY + 5;
		JButton switchDarkModeButton = new JButton("Switch Dark Mode");
		switchDarkModeButton.addActionListener(this);
		switchDarkModeButton.setActionCommand("SwitchDarkMode");
		p5.add(switchDarkModeButton);
		add(p5, constraints);
		
		
		JPanel macroParams10 = new JPanel();
		soundbankFilename = new JTextField(SOUNDBANK_DEFAULT, 18);
		macroParams10.add(new JLabel("Soundbank name:"));
		macroParams10.add(soundbankFilename);
		
		pieceLength = new JTextField("4", 2);
		macroParams10.add(new JLabel("Piece Length:"));
		macroParams10.add(pieceLength);
		
		fixedLengthChords = new JCheckBox();
		fixedLengthChords.setSelected(true);
		macroParams10.add(new JLabel("Chord duration fixed: "));
		macroParams10.add(fixedLengthChords);
		
		minorScale = new JCheckBox();
		minorScale.setSelected(false);
		
		macroParams10.add(new JLabel("Minor Key:"));
		macroParams10.add(minorScale);
		
		constraints.gridy = startY + 10;
		add(macroParams10, constraints);
		
		constraints.anchor = GridBagConstraints.CENTER;
	}
	
	
	private void initMelodySettings(int startY, int anchorSide) {
		JPanel melodySettingsPanel = new JPanel();
		addMelody = new JCheckBox("Add Melody", false);
		melodySettingsPanel.add(addMelody);
		
		maxJump = new JTextField("4", 2);
		maxExceptions = new JTextField("1", 2);
		melodySettingsPanel.add(new JLabel("Max Note Jump:"));
		melodySettingsPanel.add(maxJump);
		melodySettingsPanel.add(new JLabel("Max Exceptions:"));
		melodySettingsPanel.add(maxExceptions);
		randomChordNote = new JCheckBox();
		randomChordNote.setSelected(true);
		melodyFirstNoteFromChord = new JCheckBox();
		melodyFirstNoteFromChord.setSelected(true);
		
		
		melodySettingsPanel.add(new JLabel("Note#1 From Chord:"));
		melodySettingsPanel.add(melodyFirstNoteFromChord);
		melodySettingsPanel.add(new JLabel("But Randomized:"));
		melodySettingsPanel.add(randomChordNote);
		
		constraints.gridy = startY;
		constraints.anchor = anchorSide;
		add(melodySettingsPanel, constraints);
	}
	
	private void initMelody(int startY, int anchorSide) {
		JPanel melodyPanel = new JPanel();
		
		melodyInst = new JComboBox<String>();
		MidiUtils.addAllToJComboBox(MidiUtils.PART_INST_NAMES.get(PARTS.MELODY), melodyInst);
		arpMelodyLockInst = new JCheckBox("Inst. copy ARP1", true);
		
		MidiUtils.selectJComboBoxByInst(melodyInst, MidiUtils.PART_INST_NAMES.get(PARTS.MELODY),
				12);
		
		melodyInst.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				arpMelodyLockInst.setSelected(false);
			}
			
			public void mouseEntered(java.awt.event.MouseEvent evt) {
				// do nothing
			}
		});
		
		for (Component c : melodyInst.getComponents()) {
			c.addMouseListener(new java.awt.event.MouseAdapter() {
				public void mouseClicked(java.awt.event.MouseEvent evt) {
					arpMelodyLockInst.setSelected(false);
				}
				
				public void mouseEntered(java.awt.event.MouseEvent evt) {
					// do nothing
				}
			});
		}
		
		melodyLock = new JCheckBox("Lock Inst.", false);
		
		userMelodySeed = new JTextField("0", 10);
		JButton generateUserMelodySeed = new JButton("Random");
		generateUserMelodySeed.addActionListener(this);
		generateUserMelodySeed.setActionCommand("GenMelody");
		JButton clearUserMelodySeed = new JButton("Clear");
		clearUserMelodySeed.addActionListener(this);
		clearUserMelodySeed.setActionCommand("ClearMelody");
		melodyPauseChance = new JTextField("20", 3);
		randomMelodyOnRegenerate = new JCheckBox("On regen", false);
		
		
		melodyPanel.add(melodyLock);
		melodyPanel.add(melodyInst);
		melodyPanel.add(arpMelodyLockInst);
		melodyPanel.add(new JLabel("Pause%"));
		melodyPanel.add(melodyPauseChance);
		melodyPanel.add(generateUserMelodySeed);
		melodyPanel.add(userMelodySeed);
		melodyPanel.add(randomMelodyOnRegenerate);
		melodyPanel.add(clearUserMelodySeed);
		
		constraints.gridy = startY;
		constraints.anchor = anchorSide;
		add(melodyPanel, constraints);
		
		
	}
	
	
	private void initChordSettings(int startY, int anchorSide) {
		JPanel chordSettingsPanel = new JPanel();
		
		addChords = new JCheckBox("Add Chords", true);
		chordsInst = new JComboBox<String>();
		MidiUtils.addAllToJComboBox(MidiUtils.PART_INST_NAMES.get(PARTS.CHORDS), chordsInst);
		MidiUtils.selectJComboBoxByInst(chordsInst, MidiUtils.PART_INST_NAMES.get(PARTS.CHORDS), 4);
		chordSettingsPanel.add(addChords);
		chordSettingsPanel.add(chordsInst);
		
		JButton chordAddJButton = new JButton("+Chord");
		chordAddJButton.addActionListener(this);
		chordAddJButton.setActionCommand("AddChord");
		chordSettingsPanel.add(chordAddJButton);
		
		randomChordsCount = new JTextField("4", 2);
		JButton randomizeChords = new JButton("Randomize Chords:");
		randomizeChords.addActionListener(this);
		randomizeChords.setActionCommand("RandChords");
		randomChordsOnCompose = new JCheckBox("on compose", false);
		chordSettingsPanel.add(randomizeChords);
		chordSettingsPanel.add(randomChordsCount);
		chordSettingsPanel.add(randomChordsOnCompose);
		
		
		JButton clearChordPatternSeeds = new JButton("Clear patterns");
		clearChordPatternSeeds.addActionListener(this);
		clearChordPatternSeeds.setActionCommand("ClearChordPatterns");
		
		chordSettingsPanel.add(clearChordPatternSeeds);
		
		randomChordDelay = new JCheckBox("Use delay", false);
		randomChordStrum = new JCheckBox("Use strum", true);
		randomChordSplit = new JCheckBox("Use split", false);
		randomChordTranspose = new JCheckBox("Use transpose", false);
		randomChordPattern = new JCheckBox("Include presets", true);
		chordRotationChance = new JTextField("25", 3);
		
		chordSettingsPanel.add(randomChordDelay);
		chordSettingsPanel.add(randomChordStrum);
		chordSettingsPanel.add(randomChordSplit);
		chordSettingsPanel.add(randomChordTranspose);
		chordSettingsPanel.add(randomChordPattern);
		chordSettingsPanel.add(new JLabel("Rotation chance%"));
		chordSettingsPanel.add(chordRotationChance);
		constraints.gridy = startY;
		constraints.anchor = anchorSide;
		add(chordSettingsPanel, constraints);
	}
	
	private void initChords(int startY, int anchorSide) {
		// ---- CHORDS ----
		// gridy 50 - 99 range
		
		
		JPanel scrollableChordPanels = new JPanel();
		scrollableChordPanels.setLayout(new BoxLayout(scrollableChordPanels, BoxLayout.Y_AXIS));
		scrollableChordPanels.setAutoscrolls(true);
		
		chordScrollPane = new JScrollPane() {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(1300, 200);
			}
		};
		chordScrollPane.setViewportView(scrollableChordPanels);
		chordScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		chordScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		
		if (addChords.isSelected()) {
			createRandomChordPanels(2);
		}
		constraints.gridy = startY;
		constraints.anchor = anchorSide;
		add(chordScrollPane, constraints);
	}
	
	private void initArps(int startY, int anchorSide) {
		// --- ARPS -----------
		JPanel arp1Panel = new JPanel();
		JPanel arp2Panel = new JPanel();
		addArp1 = new JCheckBox("Add ARP1", true);
		arp1Inst = new JComboBox<String>();
		MidiUtils.addAllToJComboBox(MidiUtils.PART_INST_NAMES.get(PARTS.ARP1), arp1Inst);
		
		addArp2 = new JCheckBox("Add ARP2", true);
		arp2Inst = new JComboBox<String>();
		MidiUtils.addAllToJComboBox(MidiUtils.PART_INST_NAMES.get(PARTS.ARP2), arp2Inst);
		arp2LockInst = new JCheckBox("Inst. copy ARP1", false);
		MidiUtils.selectJComboBoxByInst(arp1Inst, MidiUtils.PART_INST_NAMES.get(PARTS.ARP1), 11);
		
		MidiUtils.selectJComboBoxByInst(arp2Inst, MidiUtils.PART_INST_NAMES.get(PARTS.ARP2), 4);
		arp1Lock = new JCheckBox("Lock Inst.", false);
		arp2Lock = new JCheckBox("Lock Inst.", false);
		arpCount = new JComboBox<String>();
		MidiUtils.addAllToJComboBox(new String[] { "1", "2", "3", "4", "5", "6", "7", "8" },
				arpCount);
		arpCount.setSelectedIndex(2);
		randomArpPattern = new JCheckBox("Random pattern", true);
		randomArpCount = new JCheckBox("Random#", true);
		arpPatternRepeat = new JCheckBox("Repeatable", true);
		arpAllowPauses = new JCheckBox("Pauses", true);
		pauseChance = new JTextField("25", 3);
		
		
		arp1Panel.add(addArp1);
		arp1Panel.add(arp1Lock);
		arp1Panel.add(arp1Inst);
		arp1Panel.add(new JLabel("Arps#"));
		arp1Panel.add(arpCount);
		arp1Panel.add(randomArpCount);
		arp1Panel.add(randomArpPattern);
		arp1Panel.add(arpPatternRepeat);
		arp1Panel.add(arpAllowPauses);
		arp1Panel.add(new JLabel("%"));
		arp1Panel.add(pauseChance);
		
		secondArpMultiplier = new JComboBox<String>();
		MidiUtils.addAllToJComboBox(new String[] { "1", "2", "3", "4" }, secondArpMultiplier);
		secondArpMultiplier.setSelectedItem("2");
		
		secondArpOctaveAdjust = new JComboBox<String>();
		MidiUtils.addAllToJComboBox(new String[] { "-24", "-12", "0", "12", "24" },
				secondArpOctaveAdjust);
		secondArpOctaveAdjust.setSelectedItem("0");
		
		secondArpMultiplierRandom = new JCheckBox("random", true);
		
		secondArpPauseChance = new JTextField("50", 3);
		
		arp2Panel.add(addArp2);
		arp2Panel.add(arp2Lock);
		arp2Panel.add(arp2Inst);
		arp2Panel.add(arp2LockInst);
		arp2Panel.add(new JLabel("Repeats#"));
		arp2Panel.add(secondArpMultiplier);
		arp2Panel.add(secondArpMultiplierRandom);
		
		arp2Panel.add(new JLabel("Transpose+-"));
		arp2Panel.add(secondArpOctaveAdjust);
		arp2Panel.add(new JLabel("Pause%"));
		arp2Panel.add(secondArpPauseChance);
		
		constraints.gridy = startY;
		constraints.anchor = anchorSide;
		add(arp1Panel, constraints);
		constraints.gridy = startY + 10;
		add(arp2Panel, constraints);
	}
	
	private void initBassRoots(int startY, int anchorSide) {
		JPanel bassRootsPanel = new JPanel();
		addBassRoots = new JCheckBox("Add BassRoots", true);
		bassRootsInst = new JComboBox<String>();
		MidiUtils.addAllToJComboBox(MidiUtils.PART_INST_NAMES.get(PARTS.BASSROOTS), bassRootsInst);
		MidiUtils.selectJComboBoxByInst(bassRootsInst,
				MidiUtils.PART_INST_NAMES.get(PARTS.BASSROOTS), 74);
		
		
		bassRootsLock = new JCheckBox("Lock Inst.", false);
		
		
		bassRootsPanel.add(addBassRoots);
		bassRootsPanel.add(bassRootsLock);
		bassRootsPanel.add(bassRootsInst);
		constraints.gridy = startY;
		constraints.anchor = anchorSide;
		add(bassRootsPanel, constraints);
	}
	
	private void initDrumSettings(int startY, int anchorSide) {
		JPanel drumsPanel = new JPanel();
		addDrums = new JCheckBox("Add Drums", false);
		
		
		drumInst = new JComboBox<String>();
		MidiUtils.addAllToJComboBox(MidiUtils.PART_INST_NAMES.get(PARTS.DRUMS), drumInst);
		MidiUtils.selectJComboBoxByInst(drumInst, MidiUtils.PART_INST_NAMES.get(PARTS.DRUMS), 42);
		drumsPanel.add(addDrums);
		drumsPanel.add(drumInst);
		
		
		JButton drumAddJButton = new JButton("+Drum");
		drumAddJButton.addActionListener(this);
		drumAddJButton.setActionCommand("AddDrum");
		drumsPanel.add(drumAddJButton);
		
		randomDrumsCount = new JTextField("4", 2);
		JButton randomizeDrums = new JButton("Randomize Drums:");
		randomizeDrums.addActionListener(this);
		randomizeDrums.setActionCommand("RandDrums");
		randomDrumsOnCompose = new JCheckBox("on compose", true);
		drumsPanel.add(randomizeDrums);
		drumsPanel.add(randomDrumsCount);
		drumsPanel.add(randomDrumsOnCompose);
		
		JButton clearPatternSeeds = new JButton("Clear patterns");
		clearPatternSeeds.addActionListener(this);
		clearPatternSeeds.setActionCommand("ClearPatterns");
		drumsPanel.add(clearPatternSeeds);
		
		randomDrumSlide = new JCheckBox("Random slide", true);
		randomDrumPattern = new JCheckBox("Include presets", true);
		velocityPatternChance = new JTextField("100", 3);
		rotationChance = new JTextField("25", 3);
		
		drumsPanel.add(randomDrumSlide);
		drumsPanel.add(randomDrumPattern);
		drumsPanel.add(new JLabel("Velocity pattern%"));
		drumsPanel.add(velocityPatternChance);
		drumsPanel.add(new JLabel("Rotation chance%"));
		drumsPanel.add(rotationChance);
		
		constraints.gridy = startY;
		constraints.anchor = anchorSide;
		add(drumsPanel, constraints);
	}
	
	private void initDrums(int startY, int anchorSide) {
		JPanel scrollableDrumPanels = new JPanel();
		scrollableDrumPanels.setLayout(new BoxLayout(scrollableDrumPanels, BoxLayout.Y_AXIS));
		scrollableDrumPanels.setAutoscrolls(true);
		
		drumScrollPane = new JScrollPane() {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(1300, 200);
			}
		};
		drumScrollPane.setViewportView(scrollableDrumPanels);
		
		drumScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		drumScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		
		if (addDrums.isSelected()) {
			createRandomDrumPanels(4);
		}
		
		constraints.gridy = startY;
		constraints.anchor = anchorSide;
		add(drumScrollPane, constraints);
		
	}
	
	private void initRandomButtons(int startY, int anchorSide) {
		JPanel randomButtonsPanel = new JPanel();
		
		JButton randomizeInstruments = new JButton("Randomize Inst.");
		randomizeInstruments.addActionListener(this);
		randomizeInstruments.setActionCommand("RandomizeInst");
		
		JButton randomizeBpmTransp = new JButton("Randomize BPM+Transpose");
		randomizeBpmTransp.addActionListener(this);
		randomizeBpmTransp.setActionCommand("RandomizeBpmTrans");
		
		randomizeInstOnCompose = new JCheckBox("on Compose");
		randomizeBmpTransOnCompose = new JCheckBox("on Compose");
		randomizeInstOnCompose.setSelected(true);
		randomizeBmpTransOnCompose.setSelected(true);
		
		
		constraints.anchor = GridBagConstraints.CENTER;
		
		
		randomButtonsPanel.add(randomizeInstruments);
		randomButtonsPanel.add(randomizeInstOnCompose);
		randomButtonsPanel.add(randomizeBpmTransp);
		randomButtonsPanel.add(randomizeBmpTransOnCompose);
		
		
		JButton randomizeStrums = new JButton("Randomize strums");
		randomizeStrums.addActionListener(this);
		randomizeStrums.setActionCommand("RandStrums");
		randomButtonsPanel.add(randomizeStrums);
		
		randomizeChordStrumsOnCompose = new JCheckBox("On compose");
		randomizeChordStrumsOnCompose.setSelected(true);
		randomButtonsPanel.add(randomizeChordStrumsOnCompose);
		
		constraints.gridy = startY;
		constraints.anchor = anchorSide;
		add(randomButtonsPanel, constraints);
	}
	
	
	private void initBpmTrans(int startY, int anchorSide) {
		// bpm/transpose
		JPanel bpmTransPanel = new JPanel();
		
		transposeScore = new JTextField("0", 3);
		bpmTransPanel.add(new JLabel("Transpose:"));
		bpmTransPanel.add(transposeScore);
		
		Random bpmRand = new Random();
		mainBpm = new JTextField(String.valueOf(bpmRand.nextInt(30) + 70), 3);
		
		arpAffectsBpm = new JCheckBox("Slowed by ARP2", true);
		
		bpmTransPanel.add(new JLabel("BPM:"));
		bpmTransPanel.add(mainBpm);
		bpmTransPanel.add(arpAffectsBpm);
		
		constraints.gridy = startY;
		constraints.anchor = anchorSide;
		add(bpmTransPanel, constraints);
	}
	
	private void initChordSettingsVariety(int startY, int anchorSide) {
		// ---- CHORD SETTINGS 1 ----
		
		JPanel chordSettingsVarietyPanel = new JPanel();
		
		chordSlashChance = new JTextField("25", 3);
		chordSettingsVarietyPanel.add(new JLabel("Ch1 slash chord%"));
		chordSettingsVarietyPanel.add(chordSlashChance);
		
		spiceChance = new JTextField("8", 3);
		chordSettingsVarietyPanel.add(new JLabel("Spice%"));
		chordSettingsVarietyPanel.add(spiceChance);
		
		spiceAllowDimAug = new JCheckBox("Dim/Aug");
		spiceAllowDimAug.setSelected(false);
		chordSettingsVarietyPanel.add(spiceAllowDimAug);
		
		constraints.gridy = startY;
		constraints.anchor = anchorSide;
		add(chordSettingsVarietyPanel, constraints);
	}
	
	private void initChordSettingsProgression(int startY, int anchorSide) {
		// CHORD SETTINGS 2
		
		JPanel chordSettingsProgressionPanel = new JPanel();
		
		
		firstChordSelection = new JComboBox<String>();
		firstChordSelection.addItem("R");
		firstChordSelection.addItem("I");
		firstChordSelection.addItem("V");
		firstChordSelection.addItem("vi");
		firstChordSelection.addItemListener(this);
		chordSettingsProgressionPanel.add(new JLabel("First Chord:"));
		chordSettingsProgressionPanel.add(firstChordSelection);
		lastChordSelection = new JComboBox<String>();
		lastChordSelection.addItem("R");
		lastChordSelection.addItem("I");
		lastChordSelection.addItem("V");
		lastChordSelection.addItem("vi");
		lastChordSelection.addItemListener(this);
		lastChordSelection.setSelectedIndex(0);
		chordSettingsProgressionPanel.add(new JLabel("Last Chord:"));
		chordSettingsProgressionPanel.add(lastChordSelection);
		
		userChordsEnabled = new JCheckBox();
		userChordsEnabled.setSelected(false);
		
		
		chordSettingsProgressionPanel.add(new JLabel("Custom chords:"));
		chordSettingsProgressionPanel.add(userChordsEnabled);
		
		userChords = new JTextField("R", 8);
		chordSettingsProgressionPanel.add(new JLabel("Chords:"));
		chordSettingsProgressionPanel.add(userChords);
		userChordsDurations = new JTextField("2,2,2,2", 6);
		chordSettingsProgressionPanel.add(new JLabel("Chord durations (max. 8):"));
		chordSettingsProgressionPanel.add(userChordsDurations);
		constraints.gridy = startY;
		constraints.anchor = anchorSide;
		add(chordSettingsProgressionPanel, constraints);
	}
	
	private void initControlPanel(int startY, int anchorSide) {
		JPanel controlPanel = new JPanel();
		randomSeed = new JTextField("0", 8);
		JButton compose = new JButton("Compose");
		compose.addActionListener(this);
		compose.setActionCommand("Compose");
		JButton regenerate = new JButton("Regenerate");
		regenerate.addActionListener(this);
		regenerate.setActionCommand("Regenerate");
		JButton copySeed = new JButton("Copy seed");
		copySeed.addActionListener(this);
		copySeed.setActionCommand("CopySeed");
		JButton copyChords = new JButton("Copy chords");
		copyChords.addActionListener(this);
		copyChords.setActionCommand("CopyChords");
		JButton clearSeed = new JButton("Clear");
		clearSeed.addActionListener(this);
		clearSeed.setActionCommand("ClearSeed");
		
		JButton loadConfig = new JButton("Load Config");
		loadConfig.addActionListener(this);
		loadConfig.setActionCommand("LoadGUIConfig");
		
		controlPanel.add(new JLabel("Random Seed:"));
		controlPanel.add(randomSeed);
		controlPanel.add(compose);
		controlPanel.add(regenerate);
		controlPanel.add(copySeed);
		controlPanel.add(copyChords);
		controlPanel.add(clearSeed);
		controlPanel.add(loadConfig);
		
		constraints.gridy = startY;
		constraints.anchor = anchorSide;
		add(controlPanel, constraints);
	}
	
	private void initPlayPanel(int startY, int anchorSide) {
		
		JPanel playSavePanel = new JPanel();
		
		JButton stopMidi = new JButton("STOP");
		stopMidi.addActionListener(this);
		stopMidi.setActionCommand("StopMidi");
		JButton startMidi = new JButton("PLAY");
		startMidi.addActionListener(this);
		startMidi.setActionCommand("StartMidi");
		
		JButton save3Star = new JButton("Save 3*");
		save3Star.addActionListener(this);
		save3Star.setActionCommand("Save 3*");
		JButton save4Star = new JButton("Save 4*");
		save4Star.addActionListener(this);
		save4Star.setActionCommand("Save 4*");
		JButton save5Star = new JButton("Save 5*");
		save5Star.addActionListener(this);
		save5Star.setActionCommand("Save 5*");
		
		
		playSavePanel.add(startMidi);
		playSavePanel.add(stopMidi);
		playSavePanel.add(save3Star);
		playSavePanel.add(save4Star);
		playSavePanel.add(save5Star);
		
		constraints.gridy = startY;
		constraints.anchor = anchorSide;
		add(playSavePanel, constraints);
	}
	
	// Deal with the window closebox
	public void windowClosing(WindowEvent we) {
		if (sequencer != null) {
			sequencer.close();
		}
		System.exit(0);
	}
	
	// other WindowListener interface methods
	// they do nothing but are required to be present
	public void windowActivated(WindowEvent we) {
	};
	
	public void windowClosed(WindowEvent we) {
	};
	
	public void windowDeactivated(WindowEvent we) {
	};
	
	public void windowIconified(WindowEvent we) {
	};
	
	public void windowDeiconified(WindowEvent we) {
	};
	
	public void windowOpened(WindowEvent we) {
	};
	
	// Deal with item events (generated by the JComboBox<String>boxs)
	public void itemStateChanged(ItemEvent ie) {
	}
	
	// Deal with Action events (button pushes)
	public void actionPerformed(ActionEvent ae) {
		if (melodyLock.isSelected()) {
			arpMelodyLockInst.setSelected(false);
		}
		if (arp2Lock.isSelected()) {
			arp2LockInst.setSelected(false);
		}
		
		
		if (ae.getActionCommand() == "Compose" && secondArpMultiplierRandom.isSelected()) {
			Random instGen = new Random();
			secondArpMultiplier.setSelectedIndex(instGen.nextInt(4));
		}
		
		if (ae.getActionCommand() == "RandStrums" || (ae.getActionCommand() == "Compose"
				& randomizeChordStrumsOnCompose.isSelected())) {
			Random strumsGen = new Random();
			double strum1 = getRandomFromArray(strumsGen, MILISECOND_ARRAY);
			double strum2 = strum1 * getRandomFromArray(strumsGen, MILISECOND_MULTIPLIER_ARRAY);
		}
		
		
		if (ae.getActionCommand() == "RandDrums" || (ae.getActionCommand() == "Compose"
				&& addDrums.isSelected() && randomDrumsOnCompose.isSelected())) {
			createRandomDrumPanels(Integer.valueOf(randomDrumsCount.getText()));
		}
		
		if (ae.getActionCommand() == "RandChords" || (ae.getActionCommand() == "Compose"
				&& addChords.isSelected() && randomChordsOnCompose.isSelected())) {
			createRandomChordPanels(Integer.valueOf(randomChordsCount.getText()));
		}
		
		if (ae.getActionCommand() == "RandomizeInst"
				|| (ae.getActionCommand() == "Compose" && randomizeInstOnCompose.isSelected())) {
			Random instGen = new Random();
			if (!melodyLock.isSelected()) {
				melodyInst.setSelectedIndex(
						instGen.nextInt(MidiUtils.PART_INST_NAMES.get(PARTS.MELODY).length));
			}
			
			for (ChordPanel cp : chordPanels) {
				if (!cp.getLockInst()) {
					cp.setInstrument(MidiUtils.getInstByIndex(
							instGen.nextInt(MidiUtils.PART_INST_NAMES.get(PARTS.CHORDS).length),
							MidiUtils.PART_INST_NAMES.get(PARTS.CHORDS)));
				}
			}
			if (!arp1Lock.isSelected()) {
				
				arp1Inst.setSelectedIndex(
						instGen.nextInt(MidiUtils.PART_INST_NAMES.get(PARTS.ARP1).length));
			}
			if (!arp2Lock.isSelected()) {
				
				arp2Inst.setSelectedIndex(
						instGen.nextInt(MidiUtils.PART_INST_NAMES.get(PARTS.ARP2).length));
			}
			if (!bassRootsLock.isSelected()) {
				
				bassRootsInst.setSelectedIndex(
						instGen.nextInt(MidiUtils.PART_INST_NAMES.get(PARTS.BASSROOTS).length));
			}
			System.out.println("RANDOMIZED INSTS!");
		}
		
		if (ae.getActionCommand() == "RandomizeBpmTrans" || (ae.getActionCommand() == "Compose"
				&& randomizeBmpTransOnCompose.isSelected())) {
			Random instGen = new Random();
			
			int bpm = instGen.nextInt(30) + 50;
			if (arpAffectsBpm.isSelected() && secondArpMultiplier.getSelectedIndex() > 1) {
				bpm *= (1 / ((secondArpMultiplier.getSelectedIndex() + 1) / 2.0));
			}
			mainBpm.setText("" + bpm);
			transposeScore.setText(String.valueOf(instGen.nextInt(12) - 6));
			
			System.out.println("RANDOMIZED BPM/Transpose!");
		}
		
		
		// midi generation
		if (ae.getActionCommand() == "Compose" || ae.getActionCommand() == "Regenerate") {
			Integer masterpieceSeed = 0;
			
			Integer parsedSeed = (NumberUtils.isCreatable(randomSeed.getText()))
					? Integer.valueOf(randomSeed.getText())
					: 0;
			
			if (ae.getActionCommand() == "Regenerate") {
				masterpieceSeed = lastRandomSeed;
				if (parsedSeed != 0) {
					masterpieceSeed = parsedSeed;
				}
				if (randomMelodyOnRegenerate.isSelected()) {
					Random rand = new Random();
					int melodySeed = rand.nextInt();
					userMelodySeed.setText(String.valueOf(melodySeed));
				}
			}
			
			Random seedGenerator = new Random();
			int randomVal = seedGenerator.nextInt();
			if (masterpieceSeed != 0) {
				System.out.println("Skipping, regenerated seed: " + masterpieceSeed);
			} else if ((!StringUtils.isEmpty(randomSeed.getText())
					&& !"0".equals(randomSeed.getText())
					&& (StringUtils.isNumeric(randomSeed.getText())
							|| StringUtils.isNumeric(randomSeed.getText().substring(1))))) {
				masterpieceSeed = Integer.valueOf(randomSeed.getText());
			} else {
				masterpieceSeed = randomVal;
			}
			
			System.out.println("Melody seed: " + masterpieceSeed);
			lastRandomSeed = masterpieceSeed;
			
			MelodyGenerator melodyGen = new MelodyGenerator();
			fillUserParameters();
			
			File makeDir = new File(MIDIS_FOLDER);
			makeDir.mkdir();
			
			String seedData = "" + masterpieceSeed;
			if (MelodyGenerator.USER_MELODY_SEED != 0 && addMelody.isSelected()) {
				seedData += "_" + userMelodySeed.getText();
			}
			
			String fileName = "seed" + seedData;
			String relPath = MIDIS_FOLDER + "/" + fileName + ".mid";
			int melodyInstrument = jm.constants.ProgramChanges.KALIMBA;
			melodyGen.generateMasterpiece(masterpieceSeed, relPath, melodyInstrument);
			currentMidi = null;
			
			arpCount.setSelectedIndex(MelodyGenerator.ARPS_PER_CHORD - 1);
			
			
			try (FileWriter fw = new FileWriter("randomSeedHistory.txt", true);
					BufferedWriter bw = new BufferedWriter(fw);
					PrintWriter out = new PrintWriter(bw)) {
				out.println(new Date().toString() + ", Seed: " + seedData);
			} catch (IOException e) {
				//exception handling left as an exercise for the reader
				System.out.println(
						"Yikers! An exception while writing a single line at the end of a .txt file!");
			}
			
			try {
				if (sequencer != null) {
					sequencer.stop();
				}
				boolean fileFound = false;
				Synthesizer synthesizer = null;
				try {
					
					
					File soundbankFile = new File(soundbankFilename.getText());
					if (soundbankFile.isFile()) {
						if (synth == null) {
							
							Soundbank soundfont = MidiSystem.getSoundbank(
									new BufferedInputStream(new FileInputStream(soundbankFile)));
							synthesizer = MidiSystem.getSynthesizer();
							
							synthesizer.isSoundbankSupported(soundfont);
							synthesizer.open();
							synthesizer.loadAllInstruments(soundfont);
							fileFound = true;
						}
						System.out
								.println("Playing using soundbank: " + soundbankFilename.getText());
					} else {
						fileFound = false;
						synth = null;
						System.out.println("NO SOUNDBANK WITH THAT NAME FOUND!");
					}
					
					
				} catch (FileNotFoundException ex) {
					fileFound = false;
					synth = null;
					System.out.println("NO SOUNDBANK WITH THAT NAME FOUND!");
				}
				
				if (sequencer != null) {
					// do nothing
				} else {
					sequencer = MidiSystem.getSequencer(!fileFound); // Get the default Sequencer
					if (sequencer == null) {
						System.err.println("Sequencer device not supported");
						return;
					}
					sequencer.open(); // Open device
				}
				
				
				// Create sequence, the File must contain MIDI file data.
				currentMidi = new File(relPath);
				generatedMidi.setListData(new File[] { currentMidi });
				pack();
				Sequence sequence = MidiSystem.getSequence(currentMidi);
				sequencer.setSequence(sequence); // load it into sequencer
				
				if (synth != null) {
					// already opened correctly, do nothing
					//saveWavFile("wavtest2.wav", synth);
				} else if (fileFound && (synthesizer != null)) {
					for (Transmitter tm : sequencer.getTransmitters()) {
						tm.close();
					}
					sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
					//saveWavFile("wavtest2.wav", synthesizer);
					//synthesizer.open();
					synth = synthesizer;
				} else {
					for (Transmitter tm : sequencer.getTransmitters()) {
						tm.close();
					}
					Synthesizer defSynth = MidiSystem.getSynthesizer();
					defSynth.open();
					sequencer.getTransmitter().setReceiver(defSynth.getReceiver());
					//saveWavFile("wavtest2.wav", defSynth);
				}
				
				
				/*if (synth != null) {
					double vol = 0.9D;
					ShortMessage volumeMessage = new ShortMessage();
					for (int i = 0; i < 16; i++) {
						volumeMessage.setMessage(ShortMessage.CONTROL_CHANGE, i, 7,
								(int) (vol * 127));
						synth.getReceiver().send(volumeMessage, -1);
					}
				}*/
				
				sequencer.setTickPosition(0);
				sequencer.start();  // start the playback
				
			} catch (MidiUnavailableException | InvalidMidiDataException | IOException ex) {
				ex.printStackTrace();
			}
		}
		if (ae.getActionCommand() == "StopMidi") {
			if (sequencer != null) {
				System.out.println("Stopping Midi..");
				sequencer.stop();
				System.out.println("Stopped Midi!");
			} else {
				System.out.println("Sequencer is NULL!");
			}
		}
		
		if (ae.getActionCommand() == "StartMidi") {
			if (sequencer != null) {
				System.out.println("Starting Midi..");
				sequencer.setTickPosition(0);
				sequencer.start();
				System.out.println("Started Midi!");
			} else {
				System.out.println("Sequencer is NULL!");
			}
		}
		
		if (ae.getActionCommand().startsWith("Save ")) {
			if (currentMidi != null) {
				System.out.println("Saving file: " + currentMidi.getName());
				
				Date date = new Date();
				String[] starSplit = ae.getActionCommand().split(" ");
				if (starSplit.length == 1) {
					System.out.println("WRONG SAVE COMMAND: " + ae.getActionCommand());
					return;
				}
				String rating = starSplit[1].substring(0, 1);
				SimpleDateFormat f = (SimpleDateFormat) SimpleDateFormat.getInstance();
				f.applyPattern("yyMMdd-HH-mm-ss");
				
				String ratingDirectory = "/saved_" + rating + "star/";
				
				File makeSavedDir = new File(MIDIS_FOLDER + ratingDirectory);
				makeSavedDir.mkdir();
				
				String soundbankLoadedString = (synth != null) ? "SB_" : "";
				
				String finalFilePath = currentMidi.getParent() + ratingDirectory + f.format(date)
						+ "_" + soundbankLoadedString + currentMidi.getName();
				
				File savedMidi = new File(finalFilePath);
				try {
					FileUtils.copyFile(currentMidi, savedMidi);
					copyGUItoConfig();
					marshal(finalFilePath);
				} catch (IOException | JAXBException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				System.out.println("currentMidi is NULL!");
			}
		}
		
		if (ae.getActionCommand() == "GenMelody") {
			Random rand = new Random();
			int melodySeed = rand.nextInt();
			userMelodySeed.setText(String.valueOf(melodySeed));
		}
		
		if (ae.getActionCommand() == "ClearMelody") {
			userMelodySeed.setText(String.valueOf(0));
		}
		
		if (ae.getActionCommand() == "CopySeed") {
			String str = String.valueOf(lastRandomSeed);
			/*Toolkit toolkit = Toolkit.getDefaultToolkit();
			Clipboard clipboard = toolkit.getSystemClipboard();
			StringSelection strSel = new StringSelection(str);
			clipboard.setContents(strSel, null);*/
			randomSeed.setText(str);
			System.out.println("Copied to random seed: " + str);
		}
		
		if (ae.getActionCommand() == "CopyChords") {
			String str = StringUtils.join(MelodyGenerator.chordInts, ",");
			userChords.setText(str);
			System.out.println("Copied chords: " + str);
		}
		
		if (ae.getActionCommand() == "ClearSeed") {
			randomSeed.setText("0");
		}
		
		
		if (ae.getActionCommand() == "LoadGUIConfig") {
			FileDialog fd = new FileDialog(this, "Choose a file", FileDialog.LOAD);
			fd.setDirectory(null);
			fd.setFile("*.xml");
			fd.setVisible(true);
			String filename = fd.getFile();
			File[] files = fd.getFiles();
			if (filename == null)
				System.out.println("You cancelled the choice");
			else {
				System.out.println("You chose " + filename);
				try {
					guiConfig = unmarshall(files[0]);
					copyConfigToGUI();
				} catch (JAXBException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
		if (ae.getActionCommand() == "ClearPatterns") {
			for (DrumPanel dp : drumPanels) {
				dp.setPatternSeed(0);
				if (dp.getPattern() != RhythmPattern.RANDOM) {
					dp.setPattern(RhythmPattern.RANDOM);
					dp.setPauseChance(3 * dp.getPauseChance());
				}
				
			}
		}
		
		if (ae.getActionCommand() == "AddDrum") {
			addDrumPanelToLayout();
			pack();
			repaint();
		}
		
		if (ae.getActionCommand().startsWith("RemoveDrum,")) {
			String drumNumber = ae.getActionCommand().split(",")[1];
			removeDrumPanel(Integer.valueOf(drumNumber), true);
		}
		
		if (ae.getActionCommand() == "ClearChordPatterns") {
			for (ChordPanel cp : chordPanels) {
				cp.setPatternSeed(0);
				if (cp.getPattern() != RhythmPattern.RANDOM) {
					cp.setPattern(RhythmPattern.RANDOM);
				}
				
			}
		}
		
		if (ae.getActionCommand() == "AddChord") {
			addChordPanelToLayout();
			pack();
			repaint();
		}
		
		if (ae.getActionCommand().startsWith("RemoveChord,")) {
			String chordNumber = ae.getActionCommand().split(",")[1];
			removeChordPanel(Integer.valueOf(chordNumber), true);
		}
		
		if (ae.getActionCommand() == "SwitchDarkMode") {
			System.out.println("Switching dark mode!");
			if (isDarkMode) {
				FlatIntelliJLaf.install();
			} else {
				FlatDarculaLaf.install();
			}
			isDarkMode = !isDarkMode;
			SwingUtilities.updateComponentTreeUI(this);
			mainTitle.setForeground((isDarkMode) ? Color.CYAN : Color.BLUE);
			subTitle.setForeground((isDarkMode) ? Color.CYAN : Color.BLUE);
			messageLabel.setForeground((isDarkMode) ? Color.CYAN : Color.BLUE);
			for (JSeparator x : separators) {
				x.setForeground((isDarkMode) ? Color.CYAN : Color.BLUE);
			}
			pack();
			setVisible(true);
			repaint();
		}
		
		System.out.println("::" + ae.getActionCommand() + "::");
		messageLabel.setText("::" + ae.getActionCommand() + "::");
	}
	
	public void fillUserParameters() {
		try {
			MelodyGenerator.MAX_JUMP = Integer.valueOf(maxJump.getText());
			MelodyGenerator.MAX_EXCEPTIONS = Integer.valueOf(maxExceptions.getText());
			MelodyGenerator.MELODY_PAUSE_CHANCE = Integer.valueOf(melodyPauseChance.getText());
			MelodyGenerator.SPICE_CHANCE = Integer.valueOf(spiceChance.getText());
			MelodyGenerator.SPICE_ALLOW_DIM_AUG = spiceAllowDimAug.isSelected();
			MelodyGenerator.PIECE_LENGTH = Integer.valueOf(pieceLength.getText());
			MelodyGenerator.RANDOM_CHORD_NOTE = randomChordNote.isSelected();
			MelodyGenerator.FIXED_LENGTH = fixedLengthChords.isSelected();
			MelodyGenerator.TRANSPOSE_SCORE = Integer.valueOf(transposeScore.getText());
			MelodyGenerator.MINOR_SONG = minorScale.isSelected();
			MelodyGenerator.MAIN_BPM = Double.valueOf(mainBpm.getText());
			MelodyGenerator.FIRST_CHORD = chordSelect(
					(String) firstChordSelection.getSelectedItem());
			MelodyGenerator.LAST_CHORD = chordSelect((String) lastChordSelection.getSelectedItem());
			MelodyGenerator.USER_MELODY_SEED = Integer
					.valueOf((StringUtils.isEmpty(userMelodySeed.getText())) ? "0"
							: userMelodySeed.getText());
			
			MelodyGenerator.CHORD_SLASH_CHANCE = Integer.valueOf(chordSlashChance.getText());
			
			if (userChordsEnabled.isSelected()) {
				String[] userChordsSplit = userChords.getText().split(",");
				System.out.println(StringUtils.join(userChordsSplit, ";"));
				String[] userChordsDurationsSplit = userChordsDurations.getText().split(",");
				try {
					boolean userChordsRandom = false;
					if (userChords.getText().contains("R")) {
						userChordsRandom = true;
					}
					if (userChordsRandom
							|| (userChordsSplit.length == userChordsDurationsSplit.length)) {
						System.out.println("Trying to solve user chords!");
						List<Integer> userChordsParsed = new ArrayList<>();
						List<Double> userChordsDurationsParsed = new ArrayList<>();
						for (int i = 0; i < userChordsDurationsSplit.length; i++) {
							if (!userChordsRandom) {
								userChordsParsed.add(Integer.valueOf(userChordsSplit[i]));
							}
							userChordsDurationsParsed
									.add(Double.valueOf(userChordsDurationsSplit[i]));
						}
						MelodyGenerator.userChords = userChordsParsed;
						MelodyGenerator.userChordsDurations = userChordsDurationsParsed;
						System.out.println(userChordsDurationsParsed.toString());
						//MelodyGenerator.FIXED_LENGTH = false;
					} else {
						MelodyGenerator.userChords.clear();
						MelodyGenerator.userChordsDurations.clear();
					}
				} catch (Exception e) {
					System.out.println("Bad user input in custom chords/durations!\n");
					e.printStackTrace();
				}
			} else {
				MelodyGenerator.userChords.clear();
				MelodyGenerator.userChordsDurations.clear();
			}
			
			MelodyGenerator.SECOND_ARP_COUNT_MULTIPLIER = secondArpMultiplier.getSelectedIndex()
					+ 1;
			MelodyGenerator.SECOND_ARP_OCTAVE_ADJUST = Integer
					.valueOf((String) secondArpOctaveAdjust.getSelectedItem());
			
			
			MelodyGenerator.ARPS_PER_CHORD = arpCount.getSelectedIndex() + 1;
			MelodyGenerator.ARP_RANDOM_SHUFFLE = randomArpPattern.isSelected();
			MelodyGenerator.RANDOM_ARPS_PER_CHORD = randomArpCount.isSelected();
			MelodyGenerator.ARP_PATTERN_REPEAT = arpPatternRepeat.isSelected();
			MelodyGenerator.ARP_ALLOW_PAUSES = arpAllowPauses.isSelected();
			MelodyGenerator.FIRST_NOTE_FROM_CHORD = melodyFirstNoteFromChord.isSelected();
			MelodyGenerator.ARP_PAUSE_CHANCE = Integer.valueOf(pauseChance.getText());
			MelodyGenerator.SECOND_ARP_PAUSE_CHANCE = Integer
					.valueOf(secondArpPauseChance.getText());
			
			MelodyGenerator.PARTS_INSTRUMENT_MAP.clear();
			
			if (arpMelodyLockInst.isSelected()) {
				melodyInst.setSelectedIndex(arp1Inst.getSelectedIndex());
			}
			
			if (arp2LockInst.isSelected()) {
				arp2Inst.setSelectedIndex(arp1Inst.getSelectedIndex());
			}
			
			if (addMelody.isSelected())
				MelodyGenerator.PARTS_INSTRUMENT_MAP.put(PARTS.MELODY,
						MidiUtils.getInstByIndex(melodyInst.getSelectedIndex(),
								MidiUtils.PART_INST_NAMES.get(PARTS.MELODY)));
			if (addChords.isSelected()) {
				MelodyGenerator.PARTS_INSTRUMENT_MAP.put(PARTS.CHORDS,
						MidiUtils.getInstByIndex(chordsInst.getSelectedIndex(),
								MidiUtils.PART_INST_NAMES.get(PARTS.CHORDS)));
				MelodyGenerator.CHORD_PARTS = getChordPartsFromChordPanels();
				
				
				MelodyGenerator.CHORD_SETTINGS = getChordSettingsFromUI();
			}
			if (addArp1.isSelected())
				MelodyGenerator.PARTS_INSTRUMENT_MAP.put(PARTS.ARP1, MidiUtils.getInstByIndex(
						arp1Inst.getSelectedIndex(), MidiUtils.PART_INST_NAMES.get(PARTS.ARP1)));
			if (addArp2.isSelected())
				MelodyGenerator.PARTS_INSTRUMENT_MAP.put(PARTS.ARP2, MidiUtils.getInstByIndex(
						arp2Inst.getSelectedIndex(), MidiUtils.PART_INST_NAMES.get(PARTS.ARP2)));
			if (addBassRoots.isSelected())
				MelodyGenerator.PARTS_INSTRUMENT_MAP.put(PARTS.BASSROOTS,
						MidiUtils.getInstByIndex(bassRootsInst.getSelectedIndex(),
								MidiUtils.PART_INST_NAMES.get(PARTS.BASSROOTS)));
			if (addDrums.isSelected()) {
				MelodyGenerator.PARTS_INSTRUMENT_MAP.put(PARTS.DRUMS, 0);
				MelodyGenerator.DRUM_PARTS = getDrumPartsFromDrumPanels();
			}
			
		} catch (Exception e) {
			System.out.println("User screwed up his inputs!" + e);
		}
		
	}
	
	private ChordGenSettings getChordSettingsFromUI() {
		ChordGenSettings chordSettings = new ChordGenSettings();
		
		chordSettings.setIncludePresets(randomChordPattern.isSelected());
		chordSettings.setUseDelay(randomChordDelay.isSelected());
		chordSettings.setUseStrum(randomChordStrum.isSelected());
		chordSettings.setUseSplit(randomChordSplit.isSelected());
		chordSettings.setUseTranspose(randomChordTranspose.isSelected());
		chordSettings.setRotationChance(Integer.valueOf(chordRotationChance.getText()));
		return chordSettings;
	}
	
	private void setChordSettingsFromUI(ChordGenSettings settings) {
		randomChordPattern.setSelected(settings.isIncludePresets());
		randomChordDelay.setSelected(settings.isUseDelay());
		randomChordStrum.setSelected(settings.isUseStrum());
		randomChordSplit.setSelected(settings.isUseSplit());
		randomChordTranspose.setSelected(settings.isUseTranspose());
		chordRotationChance.setText("" + settings.getRotationChance());
	}
	
	//returns a string[] given a upper or lower case roman numeral
	//this method is used in the selection of a first and last bar 
	public int chordSelect(String s) {
		int chord = 0;
		if (s == "R") {
			chord = 0;
		} else if (s == "I") {
			chord = 1;
		} else if (s == "V") {
			chord = 5;
		} else if (s == "vi") {
			chord = 60;
		} else { //if (fChord = "i")
			chord = 0;
		}
		return chord;
	}
	
	protected void saveWavFile(final String wavFileName, Synthesizer normalSynth)
			throws MidiUnavailableException, IOException {
		AudioSynthesizer synth = null;
		AudioInputStream stream1 = null;
		AudioInputStream stream2 = null;
		try {
			synth = (AudioSynthesizer) normalSynth;
			synth.close();
			// Open AudioStream from AudioSynthesizer with default values
			stream1 = synth.openStream(null, null);
			
			// Play Sequence into AudioSynthesizer Receiver.
			double totalLength = this.sendOutputSequenceMidiEvents(synth.getReceiver());
			
			// give it an extra 2 seconds, to the reverb to fade out--otherwise it sounds unnatural
			totalLength += 2;
			// Calculate how long the WAVE file needs to be.
			long len = (long) (stream1.getFormat().getFrameRate() * totalLength);
			stream2 = new AudioInputStream(stream1, stream1.getFormat(), len);
			
			
			// Write the wave file to disk
			AudioSystem.write(stream2, AudioFileFormat.Type.WAVE, new File("wavtest1.wav"));
		} finally {
			if (stream1 != null)
				stream1.close();
			if (stream2 != null)
				stream2.close();
			if (synth != null)
				synth.close();
		}
	}
	
	private double sendOutputSequenceMidiEvents(Receiver receiver) {
		Sequence sequence = sequencer.getSequence();
		// this method is only designed to handle the PPQ division type.
		assert sequence.getDivisionType() == Sequence.PPQ : sequence.getDivisionType();
		
		int microsecondsPerQtrNote = (int) (500000 * 120 / MelodyGenerator.MAIN_BPM);
		int seqRes = sequence.getResolution();
		long totalTime = 0;
		
		for (Track track : sequence.getTracks()) {
			long lastTick = 0;
			long curTime = 0;
			
			for (int i = 0; i < track.size(); i++) {
				MidiEvent event = track.get(i);
				long tick = event.getTick();
				curTime += ((tick - lastTick) * microsecondsPerQtrNote) / seqRes;
				lastTick = tick;
				MidiMessage msg = event.getMessage();
				if (!(msg instanceof MetaMessage)) {
					receiver.send(msg, curTime);
				}
			}
			
			// make the total time be the time of the langest track
			totalTime = Math.max(curTime, totalTime);
		}
		
		return totalTime / 1000000.0;
	}
	
	public void marshal(String path) throws JAXBException, IOException {
		SimpleDateFormat f = (SimpleDateFormat) SimpleDateFormat.getInstance();
		f.applyPattern("yyMMdd-hh-mm-ss");
		JAXBContext context = JAXBContext.newInstance(GUIConfig.class);
		Marshaller mar = context.createMarshaller();
		mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		mar.marshal(guiConfig, new File(path.substring(0, path.length() - 4) + "-guiConfig.xml"));
	}
	
	public GUIConfig unmarshall(File f) throws JAXBException, IOException {
		JAXBContext context = JAXBContext.newInstance(GUIConfig.class);
		return (GUIConfig) context.createUnmarshaller().unmarshal(new FileReader(f));
	}
	
	public void copyGUItoConfig() {
		guiConfig.setRandomSeed(lastRandomSeed);
		
		guiConfig.setSoundbankName(soundbankFilename.getText());
		guiConfig.setMinor(minorScale.isSelected());
		guiConfig.setPieceLength(Integer.valueOf(pieceLength.getText()));
		guiConfig.setFixedDuration(fixedLengthChords.isSelected());
		
		guiConfig.setTranspose(Integer.valueOf(transposeScore.getText()));
		guiConfig.setBpm(Double.valueOf(mainBpm.getText()));
		guiConfig.setArpAffectsBpm(arpAffectsBpm.isSelected());
		
		guiConfig.setSecondArpMultiplier(secondArpMultiplier.getSelectedIndex() + 1);
		guiConfig.setSecondArpOctaveAdjust(
				Integer.valueOf((String) secondArpOctaveAdjust.getSelectedItem()));
		guiConfig.setSecondArpPauseChance(Integer.valueOf(secondArpPauseChance.getText()));
		
		guiConfig.setArpCustomCount(arpCount.getSelectedIndex() + 1);
		guiConfig.setArpRandomPattern(randomArpPattern.isSelected());
		guiConfig.setArpRandomCount(randomArpCount.isSelected());
		guiConfig.setArpRandomRepeats(arpPatternRepeat.isSelected());
		guiConfig.setArpRandomPauses(arpAllowPauses.isSelected());
		
		guiConfig.setMelodyEnable(addMelody.isSelected());
		guiConfig.setChordsEnable(addChords.isSelected());
		guiConfig.setArp1ArpEnable(addArp1.isSelected());
		guiConfig.setArp2ArpEnable(addArp2.isSelected());
		guiConfig.setBassRootsEnable(addBassRoots.isSelected());
		
		guiConfig.setDrumsEnable(addDrums.isSelected());
		guiConfig.setDrumParts(getDrumPartsFromDrumPanels());
		guiConfig.setChordParts(getChordPartsFromChordPanels());
		guiConfig.setChordGenSettings(getChordSettingsFromUI());
		
		guiConfig.setMelodyInst(MidiUtils.getInstByIndex(melodyInst.getSelectedIndex(),
				MidiUtils.PART_INST_NAMES.get(PARTS.MELODY)));
		guiConfig.setArp1ArpInst(MidiUtils.getInstByIndex(arp1Inst.getSelectedIndex(),
				MidiUtils.PART_INST_NAMES.get(PARTS.ARP1)));
		guiConfig.setArp2ArpInst(MidiUtils.getInstByIndex(arp2Inst.getSelectedIndex(),
				MidiUtils.PART_INST_NAMES.get(PARTS.ARP2)));
		guiConfig.setBassRootsInst(MidiUtils.getInstByIndex(bassRootsInst.getSelectedIndex(),
				MidiUtils.PART_INST_NAMES.get(PARTS.BASSROOTS)));
		
		guiConfig.setUserMelodySeed(!StringUtils.isEmpty(userMelodySeed.getText())
				? Long.valueOf(userMelodySeed.getText())
				: 0);
		
		guiConfig.setMaxNoteJump(Integer.valueOf(maxJump.getText()));
		guiConfig.setMaxExceptions(Integer.valueOf(maxExceptions.getText()));
		guiConfig.setPauseChance(Integer.valueOf(pauseChance.getText()));
		guiConfig.setSecondArpPauseChance(Integer.valueOf(secondArpPauseChance.getText()));
		guiConfig.setMelodyPauseChance(Integer.valueOf(melodyPauseChance.getText()));
		guiConfig.setSpiceChance(Integer.valueOf(spiceChance.getText()));
		guiConfig.setDimAugEnabled(spiceAllowDimAug.isSelected());
		guiConfig.setChordSlashChance(Integer.valueOf(chordSlashChance.getText()));
		
		guiConfig.setFirstChord((String) firstChordSelection.getSelectedItem());
		guiConfig.setLastChord((String) lastChordSelection.getSelectedItem());
		guiConfig.setCustomChordsEnabled(userChordsEnabled.isSelected());
		guiConfig.setCustomChords(StringUtils.join(MelodyGenerator.chordInts, ","));
		guiConfig.setCustomChordDurations(userChordsDurations.getText());
		
		guiConfig.setFirstNoteFromChord(melodyFirstNoteFromChord.isSelected());
		guiConfig.setFirstNoteRandomized(randomChordNote.isSelected());
	}
	
	public void copyConfigToGUI() {
		randomSeed.setText(String.valueOf(guiConfig.getRandomSeed()));
		lastRandomSeed = (int) guiConfig.getRandomSeed();
		
		soundbankFilename.setText(guiConfig.getSoundbankName());
		minorScale.setSelected(guiConfig.isMinor());
		pieceLength.setText(String.valueOf(guiConfig.getPieceLength()));
		fixedLengthChords.setSelected(guiConfig.isFixedDuration());
		
		transposeScore.setText(String.valueOf(guiConfig.getTranspose()));
		mainBpm.setText(String.valueOf(guiConfig.getBpm()));
		arpAffectsBpm.setSelected(guiConfig.isArpAffectsBpm());
		
		secondArpMultiplier.setSelectedIndex(guiConfig.getSecondArpMultiplier() - 1);
		secondArpOctaveAdjust.setSelectedItem(String.valueOf(guiConfig.getSecondArpOctaveAdjust()));
		secondArpPauseChance.setText(String.valueOf(guiConfig.getSecondArpPauseChance()));
		
		arpCount.setSelectedIndex(guiConfig.getArpCustomCount() - 1);
		randomArpPattern.setSelected(guiConfig.isArpRandomPattern());
		randomArpCount.setSelected(guiConfig.isArpRandomCount());
		arpPatternRepeat.setSelected(guiConfig.isArpRandomRepeats());
		arpAllowPauses.setSelected(guiConfig.isArpRandomPauses());
		
		addMelody.setSelected(guiConfig.isMelodyEnable());
		addChords.setSelected(guiConfig.isChordsEnable());
		addArp1.setSelected(guiConfig.isArp1ArpEnable());
		addArp2.setSelected(guiConfig.isArp2ArpEnable());
		addBassRoots.setSelected(guiConfig.isBassRootsEnable());
		
		addDrums.setSelected(guiConfig.isDrumsEnable());
		recreateDrumPanelsFromDrumParts(guiConfig.getDrumParts());
		recreateChordPanelsFromChordParts(guiConfig.getChordParts());
		
		setChordSettingsFromUI(guiConfig.getChordGenSettings());
		
		MidiUtils.selectJComboBoxByInst(melodyInst, MidiUtils.PART_INST_NAMES.get(PARTS.MELODY),
				guiConfig.getMelodyInst());
		
		MidiUtils.selectJComboBoxByInst(arp1Inst, MidiUtils.PART_INST_NAMES.get(PARTS.ARP1),
				guiConfig.getArp1ArpInst());
		
		MidiUtils.selectJComboBoxByInst(arp2Inst, MidiUtils.PART_INST_NAMES.get(PARTS.ARP2),
				guiConfig.getArp2ArpInst());
		
		MidiUtils.selectJComboBoxByInst(bassRootsInst,
				MidiUtils.PART_INST_NAMES.get(PARTS.BASSROOTS), guiConfig.getBassRootsInst());
		
		userMelodySeed.setText(String.valueOf(guiConfig.getUserMelodySeed()));
		
		maxJump.setText(String.valueOf(guiConfig.getMaxNoteJump()));
		maxExceptions.setText(String.valueOf(guiConfig.getMaxExceptions()));
		pauseChance.setText(String.valueOf(guiConfig.getPauseChance()));
		secondArpPauseChance.setText(String.valueOf(guiConfig.getSecondArpPauseChance()));
		melodyPauseChance.setText(String.valueOf(guiConfig.getMelodyPauseChance()));
		spiceChance.setText(String.valueOf(guiConfig.getSpiceChance()));
		spiceAllowDimAug.setSelected(guiConfig.isDimAugEnabled());
		chordSlashChance.setText(String.valueOf(guiConfig.getChordSlashChance()));
		
		firstChordSelection.setSelectedItem(guiConfig.getFirstChord());
		lastChordSelection.setSelectedItem(guiConfig.getLastChord());
		userChordsEnabled.setSelected(guiConfig.isCustomChordsEnabled());
		userChords.setText(guiConfig.getCustomChords());
		userChordsDurations.setText(guiConfig.getCustomChordDurations());
		
		melodyFirstNoteFromChord.setSelected(guiConfig.isFirstNoteFromChord());
		randomChordNote.setSelected(guiConfig.isFirstNoteRandomized());
	}
	
	private class FileTransferHandler extends TransferHandler {
		@Override
		protected Transferable createTransferable(JComponent c) {
			List<File> files = new ArrayList<>();
			files.add(currentMidi);
			return new FileTransferable(files);
		}
		
		@Override
		public int getSourceActions(JComponent c) {
			return COPY_OR_MOVE;
		}
	}
	
	
	private static void createHorizontalSeparator(int y, JFrame f) {
		int anchorTemp = constraints.anchor;
		JSeparator x = new JSeparator(SwingConstants.HORIZONTAL);
		x.setPreferredSize(new Dimension(1200, 3));
		x.setForeground(isDarkMode ? Color.CYAN : Color.BLUE);
		JPanel sepPanel = new JPanel();
		sepPanel.add(x);
		constraints.gridy = y;
		constraints.anchor = GridBagConstraints.CENTER;
		f.add(sepPanel, constraints);
		constraints.anchor = anchorTemp;
		separators.add(x);
	}
	
	public DrumPanel addDrumPanelToLayout() {
		int panelOrder = (drumPanels.size() > 0) ? getHighestDrumPanelNumber(drumPanels) + 1 : 1;
		
		constraints.gridy = drumGenPanelStart + panelOrder;
		constraints.anchor = GridBagConstraints.CENTER;
		
		DrumPanel drumJPanel = new DrumPanel(this);
		drumJPanel.setDrumPanelOrder(panelOrder);
		drumJPanel.initComponents();
		drumJPanel.setPitch(MidiUtils.getInstByIndex(drumInst.getSelectedIndex(),
				MidiUtils.PART_INST_NAMES.get(PARTS.DRUMS)));
		drumPanels.add(drumJPanel);
		((JPanel) drumScrollPane.getViewport().getView()).add(drumJPanel);
		return drumJPanel;
	}
	
	private void removeDrumPanel(int order, boolean singleRemove) {
		DrumPanel panel = getDrumPanelByOrder(order, drumPanels);
		((JPanel) drumScrollPane.getViewport().getView()).remove(panel);
		drumPanels.remove(panel);
		
		if (singleRemove) {
			//reorderDrumPanels();
			pack();
			repaint();
		}
	}
	
	private List<DrumPart> getDrumPartsFromDrumPanels() {
		List<DrumPart> parts = new ArrayList<>();
		for (DrumPanel p : drumPanels) {
			parts.add(p.toDrumPart(lastRandomSeed));
		}
		return parts;
	}
	
	private void recreateDrumPanelsFromDrumParts(List<DrumPart> parts) {
		for (DrumPanel panel : drumPanels) {
			((JPanel) drumScrollPane.getViewport().getView()).remove(panel);
		}
		drumPanels.clear();
		for (DrumPart part : parts) {
			DrumPanel panel = addDrumPanelToLayout();
			panel.setFromDrumPart(part);
		}
		
		pack();
		repaint();
	}
	
	private void createRandomDrumPanels(int panelCount) {
		Random drumPanelGenerator = new Random();
		for (DrumPanel panel : drumPanels) {
			((JPanel) drumScrollPane.getViewport().getView()).remove(panel);
		}
		drumPanels.clear();
		
		int slide = 0;
		
		if (randomDrumSlide.isSelected()) {
			slide = drumPanelGenerator.nextInt(300) - 150;
		}
		
		for (int i = 0; i < panelCount; i++) {
			DrumPanel dp = addDrumPanelToLayout();
			dp.setPitch(MidiUtils.getInstByIndex(
					drumPanelGenerator.nextInt(MidiUtils.PART_INST_NAMES.get(PARTS.DRUMS).length),
					MidiUtils.PART_INST_NAMES.get(PARTS.DRUMS)));
			//dp.setPitch(32 + drumPanelGenerator.nextInt(33));
			
			
			dp.setChordSpan(drumPanelGenerator.nextInt(2) + 1);
			int patternOrder = 0;
			if (randomDrumPattern.isSelected()) {
				patternOrder = drumPanelGenerator.nextInt(RhythmPattern.values().length);
			}
			int hits = 4;
			while (drumPanelGenerator.nextInt(10) < 5 && hits < 32) {
				hits *= 2;
			}
			if ((hits / dp.getChordSpan() >= 16)) {
				hits /= 2;
			}
			
			dp.setHitsPerPattern(hits);
			
			int adjustVelocity = (dp.getHitsPerPattern() / 2) / dp.getChordSpan();
			
			dp.setPattern(RhythmPattern.values()[patternOrder]);
			int velocityMin = drumPanelGenerator.nextInt(50 - adjustVelocity) + 20;
			dp.setVelocityMin(velocityMin);
			dp.setVelocityMax(1 + velocityMin + drumPanelGenerator.nextInt(40 - adjustVelocity));
			
			if (patternOrder > 0) {
				dp.setPauseChance(drumPanelGenerator.nextInt(5) + 0);
			} else {
				dp.setPauseChance(drumPanelGenerator.nextInt(40) + 40);
			}
			dp.setExceptionChance(drumPanelGenerator.nextInt(15));
			
			if (dp.getPitch() > 40) {
				dp.setSlideMiliseconds(slide);
			}
			
			dp.setIsVelocityPattern(drumPanelGenerator.nextInt(100) < Integer
					.valueOf(velocityPatternChance.getText()));
			
			if (drumPanelGenerator.nextInt(100) < Integer.valueOf(rotationChance.getText())
					&& patternOrder > 0) {
				dp.setPatternRotation(
						drumPanelGenerator.nextInt(dp.getPattern().pattern.length - 1) + 1);
			}
			
		}
		
		pack();
		repaint();
	}
	
	
	private static int getHighestDrumPanelNumber(List<DrumPanel> panels) {
		int highest = 1;
		for (DrumPanel p : panels) {
			highest = (p.getDrumPanelOrder() > highest) ? p.getDrumPanelOrder() : highest;
		}
		return highest;
	}
	
	private static DrumPanel getDrumPanelByOrder(int order, List<DrumPanel> panels) {
		return panels.stream().filter(e -> e.getDrumPanelOrder() == order).findFirst().get();
	}
	
	public ChordPanel addChordPanelToLayout() {
		int panelOrder = (chordPanels.size() > 0) ? getHighestChordPanelNumber(chordPanels) + 1 : 1;
		
		constraints.gridy = chordGenPanelStart + panelOrder;
		constraints.anchor = GridBagConstraints.CENTER;
		
		ChordPanel chordJPanel = new ChordPanel(this);
		chordJPanel.setChordPanelOrder(panelOrder);
		chordJPanel.initComponents();
		chordJPanel.setInstrument(MidiUtils.getInstByIndex(chordsInst.getSelectedIndex(),
				MidiUtils.PART_INST_NAMES.get(PARTS.CHORDS)));
		chordPanels.add(chordJPanel);
		((JPanel) chordScrollPane.getViewport().getView()).add(chordJPanel);
		return chordJPanel;
	}
	
	private void removeChordPanel(int order, boolean singleRemove) {
		ChordPanel panel = getChordPanelByOrder(order, chordPanels);
		((JPanel) chordScrollPane.getViewport().getView()).remove(panel);
		chordPanels.remove(panel);
		
		if (singleRemove) {
			//reorderChordPanels();
			pack();
			repaint();
		}
	}
	
	private List<ChordPart> getChordPartsFromChordPanels() {
		List<ChordPart> parts = new ArrayList<>();
		for (ChordPanel p : chordPanels) {
			parts.add(p.toChordPart(lastRandomSeed));
		}
		return parts;
	}
	
	private void recreateChordPanelsFromChordParts(List<ChordPart> parts) {
		for (ChordPanel panel : chordPanels) {
			((JPanel) chordScrollPane.getViewport().getView()).remove(panel);
		}
		chordPanels.clear();
		for (ChordPart part : parts) {
			ChordPanel panel = addChordPanelToLayout();
			panel.setFromChordPart(part);
		}
		
		pack();
		repaint();
	}
	
	private void createRandomChordPanels(int panelCount) {
		Random chordPanelGenerator = new Random();
		for (ChordPanel panel : chordPanels) {
			((JPanel) chordScrollPane.getViewport().getView()).remove(panel);
		}
		chordPanels.clear();
		
		for (int i = 0; i < panelCount; i++) {
			ChordPanel cp = addChordPanelToLayout();
			
			List<Integer> availableInstruments = MidiUtils
					.getInstNumbers(MidiUtils.PART_INST_NAMES.get(PARTS.CHORDS));
			
			cp.setInstrument(availableInstruments
					.get(chordPanelGenerator.nextInt(availableInstruments.size())));
			cp.setTransitionChance(chordPanelGenerator.nextInt(50));
			cp.setTransitionSplit(
					(int) (getRandomFromArray(chordPanelGenerator, MILISECOND_ARRAY_LARGE)));
			cp.setTranspose((chordPanelGenerator.nextInt(3) - 1) * 12);
			
			cp.setStrum(((int) (getRandomFromArray(chordPanelGenerator, MILISECOND_ARRAY))));
			cp.setDelay(((int) (getRandomFromArray(chordPanelGenerator, MILISECOND_ARRAY_SMALL))));
			
			int patternOrder = 0;
			if (randomChordPattern.isSelected()) {
				patternOrder = chordPanelGenerator.nextInt(RhythmPattern.values().length);
			}
			cp.setPattern(RhythmPattern.values()[patternOrder]);
			
			if (chordPanelGenerator.nextInt(100) < Integer.valueOf(chordRotationChance.getText())
					&& patternOrder > 0) {
				cp.setPatternRotation(
						chordPanelGenerator.nextInt(cp.getPattern().pattern.length - 1) + 1);
			}
			
		}
		
		pack();
		repaint();
	}
	
	
	private static int getHighestChordPanelNumber(List<ChordPanel> panels) {
		int highest = 1;
		for (ChordPanel p : panels) {
			highest = (p.getChordPanelOrder() > highest) ? p.getChordPanelOrder() : highest;
		}
		return highest;
	}
	
	private static ChordPanel getChordPanelByOrder(int order, List<ChordPanel> panels) {
		return panels.stream().filter(e -> e.getChordPanelOrder() == order).findFirst().get();
	}
	
	private static double getRandomFromArray(Random generator, double[] array) {
		return array[generator.nextInt(array.length)];
	}
}
