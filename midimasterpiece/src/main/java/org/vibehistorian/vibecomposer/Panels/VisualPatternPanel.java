package org.vibehistorian.vibecomposer.Panels;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.vibehistorian.vibecomposer.MidiUtils;
import org.vibehistorian.vibecomposer.VibeComposerGUI;
import org.vibehistorian.vibecomposer.Enums.RhythmPattern;
import org.vibehistorian.vibecomposer.Helpers.CheckBoxIcon;
import org.vibehistorian.vibecomposer.Helpers.ScrollComboBox;

public class VisualPatternPanel extends JPanel {

	private static final long serialVersionUID = 6963518339035392918L;

	private KnobPanel hitsPanel = null;
	private ScrollComboBox<String> patternType = null;
	private KnobPanel shiftPanel = null;
	private KnobPanel chordSpanPanel = null;
	private JButton doublerButton = null;

	private int lastHits = 0;

	private List<Integer> truePattern = new ArrayList<>();
	private JCheckBox[] hitChecks = new JCheckBox[32];
	private JLabel[] separators = new JLabel[3];

	private JPanel parentPanel = null;

	public static int width = 8 * CheckBoxIcon.width;
	public static int height = 2 * CheckBoxIcon.width;

	public static int mouseButton = 0;

	/*public static List<Integer> sextuplets = Arrays.asList(new Integer[] { 6, 12, 24 });
	public static List<Integer> quintuplets = Arrays.asList(new Integer[] { 5 });
	public static List<Integer> triplets = Arrays.asList(new Integer[] { 3 });*/

	private boolean viewOnly = false;
	private boolean needShift = false;
	private boolean bigModeAllowed = true;

	public void setBigModeAllowed(boolean bigModeAllowed) {
		this.bigModeAllowed = bigModeAllowed;
	}

	public static Map<Integer, Insets> smallModeInsetMap = new HashMap<>();
	static {
		smallModeInsetMap.put(2, new Insets(0, 0, 0, CheckBoxIcon.width * 6 / 2));
		smallModeInsetMap.put(3, new Insets(0, 0, 0, CheckBoxIcon.width * 5 / 3));
		smallModeInsetMap.put(4, new Insets(0, 0, 0, CheckBoxIcon.width * 4 / 4));
		smallModeInsetMap.put(5, new Insets(0, 0, 0, CheckBoxIcon.width * 3 / 5));
		smallModeInsetMap.put(6, new Insets(0, 0, 0, CheckBoxIcon.width * 2 / 6));
		smallModeInsetMap.put(10, new Insets(0, 0, 0, CheckBoxIcon.width * 3 / 5));
		smallModeInsetMap.put(12, new Insets(0, 0, 0, CheckBoxIcon.width * 2 / 6));
		//smallModeInsetMap.put(18, new Insets(0, 0, 0, CheckBoxIcon.width * 2 / 6));
		smallModeInsetMap.put(24, new Insets(0, 0, 0, CheckBoxIcon.width * 2 / 6));
	}

	public static Map<Integer, Insets> bigModeInsetMap = new HashMap<>();
	static {

		for (int i = 2; i < 32; i++) {
			bigModeInsetMap.put(i, new Insets(0, 0, 0, CheckBoxIcon.width * (32 - i) / i));
		}
	}

	public static Map<Integer, Insets> bigModeDoubleChordGeneralInsetMap = new HashMap<>();
	static {

		for (int i = 2; i <= 32; i++) {
			bigModeDoubleChordGeneralInsetMap.put(i,
					new Insets(0, 0, 0, 2 * CheckBoxIcon.width * (32 - i / 2) / i));
		}
	}

	public static Map<Integer, Insets> bigModeDoubleChordTransitionInsetMap = new HashMap<>();
	static {

		for (int i = 2; i <= 32; i++) {
			bigModeDoubleChordTransitionInsetMap.put(i,
					new Insets(0, CheckBoxIcon.width * (32 - i / 2) / i, 0,
							CheckBoxIcon.width * (32 - i / 2) / i));
		}
	}


	public VisualPatternPanel(KnobPanel hitsPanel, ScrollComboBox<String> patternType,
			KnobPanel shiftPanel, KnobPanel chordSpanPanel, JButton doubler, JPanel parentPanel) {
		super();
		//setBackground(new Color(50, 50, 50));
		FlowLayout layout = new FlowLayout(FlowLayout.CENTER, 0, 0);
		//layout.setVgap(0);
		//layout.setHgap(0);
		setLayout(layout);
		setPreferredSize(new Dimension(width, height));
		//setBorder(new BevelBorder(BevelBorder.LOWERED));
		this.hitsPanel = hitsPanel;
		this.patternType = patternType;
		this.shiftPanel = shiftPanel;
		this.parentPanel = parentPanel;
		this.chordSpanPanel = chordSpanPanel;
		doublerButton = doubler;
		lastHits = hitsPanel.getInt();
		int sepCounter = 0;
		for (int i = 0; i < 32; i++) {
			final int fI = i;
			truePattern.add(0);
			hitChecks[i] = new JCheckBox("", new CheckBoxIcon());
			//hitChecks[i].setBackground(new Color(128, 128, 128));
			hitChecks[i].addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					//System.out.println("True pattern size: " + truePattern.size());
					boolean change = false;
					if (mouseButton == 2) {
						hitChecks[fI].setSelected(true);
						change = true;
					} else if (mouseButton == 3) {
						hitChecks[fI].setSelected(false);
						change = true;
					}
					if (change) {
						needShift = true;
						int shI = (fI - shiftPanel.getInt() + 32) % 32;
						truePattern.set(shI, hitChecks[fI].isSelected() ? 1 : 0);
						if (RhythmPattern.valueOf(patternType.getVal()) != RhythmPattern.CUSTOM) {
							patternType.setSelectedItem(RhythmPattern.CUSTOM.toString());
						}
					}

				}

			});
			hitChecks[i].addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					int mouseButt = e.getButton();
					if (mouseButt == 1) {
						mouseButton = -1;
					} else if (mouseButt > 1) {
						mouseButton = mouseButt;
						if (RhythmPattern.valueOf(patternType.getVal()) != RhythmPattern.CUSTOM) {
							patternType.setSelectedItem(RhythmPattern.CUSTOM.toString());
						}
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					mouseButton = -1;
					/*for (DrumPanel dp : VibeComposerGUI.drumPanels) {
						if (dp.getComboPanel().needShift) {
							dp.getComboPanel().reapplyShift();
							dp.getComboPanel().needShift = false;
							DrumLoopPopup.dhpps.get(dp).reapplyShift();
						}
					}*/
				}
			});
			hitChecks[i].setMargin(new Insets(0, 0, 0, 0));
			if (i >= hitsPanel.getInt()) {
				hitChecks[i].setVisible(false);
			}
			hitChecks[i].setHorizontalAlignment(SwingConstants.LEFT);

			hitChecks[i].addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					int shI = (fI - shiftPanel.getInt() + 32) % 32;
					truePattern.set(shI, hitChecks[fI].isSelected() ? 1 : 0);
					if (RhythmPattern.valueOf(patternType.getVal()) != RhythmPattern.CUSTOM) {
						patternType.setSelectedItem(RhythmPattern.CUSTOM.toString());
					}
				}

			});
			add(hitChecks[i]);
			/*if (i > 0 && i < 31 && ((i + 1) % 8) == 0) {
				JLabel sep = new JLabel("|");
				sep.setVisible(false);
				separators[sepCounter++] = sep;
				add(sep);
			}*/
		}

		patternType.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					VisualPatternPanel.this.setVisible(false);
					RhythmPattern d = RhythmPattern.valueOf(patternType.getVal());
					if (d != RhythmPattern.CUSTOM) {
						truePattern = d.getPatternByLength(32, 0);
					}

					for (int i = 0; i < 32; i++) {
						int shI = (i + shiftPanel.getInt()) % 32;
						hitChecks[shI].setSelected(truePattern.get(i) != 0);
					}
					VisualPatternPanel.this.setVisible(true);
				}

			}

		});

		hitsPanel.getKnob().addMouseListener(new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent e) {
				reapplyHits();

			}
		});

		if (doubler != null) {
			doubler.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseReleased(MouseEvent e) {
					List<Integer> halfPattern = truePattern.subList(0, 16);
					Collections.rotate(halfPattern, shiftPanel.getInt());
					truePattern = MidiUtils.intersperse(0, 1, halfPattern);
					//Collections.rotate(halfPattern, -1 * shiftPanel.getInt());
					patternType.setSelectedItem(RhythmPattern.CUSTOM.toString());
					if (shiftPanel.getInt() > 0) {
						shiftPanel.setInt(0);
					}
					reapplyShift();
					if (lastHits != 24 && lastHits != 10) {
						hitsPanel.getKnob().setValue(2 * lastHits);
					}


				}
			});
		}

		hitsPanel.getKnob().getTextValue().getDocument()
				.addDocumentListener(new DocumentListener() {

					@Override
					public void insertUpdate(DocumentEvent e) {
						reapplyHits();

					}

					@Override
					public void removeUpdate(DocumentEvent e) {
						reapplyHits();

					}

					@Override
					public void changedUpdate(DocumentEvent e) {
						reapplyHits();

					}

				});

		shiftPanel.getKnob().addMouseListener(new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent e) {
				reapplyShift();

			}
		});

		shiftPanel.getKnob().getTextValue().getDocument()
				.addDocumentListener(new DocumentListener() {

					@Override
					public void insertUpdate(DocumentEvent e) {
						reapplyShift();

					}

					@Override
					public void removeUpdate(DocumentEvent e) {
						reapplyShift();

					}

					@Override
					public void changedUpdate(DocumentEvent e) {
						reapplyShift();

					}

				});

		chordSpanPanel.getKnob().addMouseListener(new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent e) {
				reapplyHits();

			}
		});

		chordSpanPanel.getKnob().getTextValue().getDocument()
				.addDocumentListener(new DocumentListener() {

					@Override
					public void insertUpdate(DocumentEvent e) {
						reapplyHits();

					}

					@Override
					public void removeUpdate(DocumentEvent e) {
						reapplyHits();

					}

					@Override
					public void changedUpdate(DocumentEvent e) {
						reapplyHits();

					}

				});
	}

	public List<Integer> getTruePattern() {
		return truePattern;
	}

	public void setTruePattern(List<Integer> truePattern) {
		this.truePattern = truePattern;
		reapplyShift();
	}

	public void reapplyShift() {
		if (truePattern == null || truePattern.isEmpty()) {
			return;
		}
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				VisualPatternPanel.this.setVisible(false);
				for (int i = 0; i < 32; i++) {
					int shI = (i + shiftPanel.getInt()) % 32;
					hitChecks[shI].setSelected(truePattern.get(i) != 0);
				}
				VisualPatternPanel.this.setVisible(true);
			}
		});
	}

	public void reapplyHits() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				VisualPatternPanel.this.setVisible(false);
				boolean showBIG = (VibeComposerGUI.isBigMonitorMode || viewOnly) && bigModeAllowed;
				int nowHits = hitsPanel.getInt();
				if (nowHits > 32)
					nowHits = 32;
				if (nowHits > lastHits) {
					for (int i = lastHits; i < nowHits; i++) {
						hitChecks[i].setVisible(true);
					}

				} else if (nowHits < lastHits) {
					for (int i = nowHits; i < lastHits; i++) {
						hitChecks[i].setVisible(false);
					}
				}
				lastHits = nowHits;

				int chords = chordSpanPanel.getInt();

				if (showBIG) {
					width = 32 * CheckBoxIcon.width;
					height = 1 * CheckBoxIcon.width;
					if (chords == 1) {
						if (bigModeInsetMap.containsKey(lastHits)) {
							for (int i = 0; i < lastHits; i++) {
								hitChecks[i].setMargin(bigModeInsetMap.get(lastHits));
							}
						} else {
							for (int i = 0; i < lastHits; i++) {
								hitChecks[i].setMargin(new Insets(0, 0, 0, 0));
							}
						}
					} else {
						for (int i = 0; i < lastHits; i++) {
							if (lastHits % 2 == 0) {
								hitChecks[i]
										.setMargin(bigModeDoubleChordGeneralInsetMap.get(lastHits));
							} else {
								if (i == lastHits / 2) {
									hitChecks[i].setMargin(
											bigModeDoubleChordTransitionInsetMap.get(lastHits));
								} else {
									hitChecks[i].setMargin(
											bigModeDoubleChordGeneralInsetMap.get(lastHits));
								}
							}

						}
					}
					/*if (!viewOnly) {
						if (lastHits == 32 && chords == 1) {
							for (JLabel lab : separators) {
								lab.setVisible(true);
							}
						} else if (lastHits == 32 && chords == 2) {
							separators[0].setVisible(true);
							separators[1].setVisible(false);
							separators[2].setVisible(true);
						} else if (lastHits == 16 && chords == 1) {
							separators[0].setVisible(true);
							separators[1].setVisible(false);
							separators[2].setVisible(false);
						} else {
							for (JLabel lab : separators) {
								lab.setVisible(false);
							}
						}
					} else {
						for (JLabel lab : separators) {
							lab.setVisible(false);
						}
					}*/

				} else {
					width = 8 * CheckBoxIcon.width;
					height = 2 * CheckBoxIcon.width;
					if (smallModeInsetMap.containsKey(lastHits)) {
						for (int i = 0; i < lastHits; i++) {
							hitChecks[i].setMargin(smallModeInsetMap.get(lastHits));
						}
					} else {
						for (int i = 0; i < lastHits; i++) {
							hitChecks[i].setMargin(new Insets(0, 0, 0, 0));
						}
					}
					/*for (JLabel lab : separators) {
						lab.setVisible(false);
					}*/
				}
				int bigModeWidthOffset = (showBIG) ? 10 : 0;
				if (lastHits > 16 || (chords == 2 && showBIG)) {
					VisualPatternPanel.this.setPreferredSize(
							new Dimension(width + bigModeWidthOffset, height * 2));
					if (!showBIG) {
						parentPanel.setMaximumSize(new Dimension(3000, 90));
					}
				} else {
					VisualPatternPanel.this
							.setPreferredSize(new Dimension(width + bigModeWidthOffset, height));
					parentPanel.setMaximumSize(new Dimension(3000, 50));
				}
				VisualPatternPanel.this.setVisible(true);
				repaint();
			}

		});

	}

	public boolean isViewOnly() {
		return viewOnly;
	}

	public void setViewOnly(boolean viewOnly) {
		this.viewOnly = viewOnly;
	}

}
