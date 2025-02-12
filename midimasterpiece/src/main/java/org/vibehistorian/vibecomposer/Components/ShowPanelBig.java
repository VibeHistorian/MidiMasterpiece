/*

<This Java Class is part of the jMusic API version 1.5, March 2004.>:37  2001

Copyright (C) 2000 Andrew Sorensen & Andrew Brown

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or any
later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not,
see <https://www.gnu.org/licenses/>.

*/

/*-------------------------------------------
* A jMusic tool which displays a score as a
* piano roll display on Common Practice Notation staves.
* @author Andrew Brown 
 * @version 1.0,Sun Feb 25 18:43
* ---------------------
*/
package org.vibehistorian.vibecomposer.Components;

import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Score;
import org.vibehistorian.vibecomposer.JMusicUtilsCustom;
import org.vibehistorian.vibecomposer.LG;
import org.vibehistorian.vibecomposer.MidiGenerator;
import org.vibehistorian.vibecomposer.OMNI;
import org.vibehistorian.vibecomposer.SwingUtils;
import org.vibehistorian.vibecomposer.VibeComposerGUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShowPanelBig extends JPanel {
	private static final long serialVersionUID = 1464206032589622048L;
	public Score score;
	protected double beatWidth; //10.0;
	public static final int beatWidthBaseDefault = 1600;
	public static int beatWidthBase = 1600;
	public static final List<Integer> beatWidthBasesBig = Arrays.asList(new Integer[] { 1600, 1800,
			2200, 2700, 3300, 4000, 4800, 5700, 6800, 8200, 10000, 12500 });
	public static final List<Integer> beatWidthBasesSmall = Arrays.asList(
			new Integer[] { 630, 800, 1050, 1300, 1550, 1800, 2200, 2700, 3300, 4000, 4800, 5700 });
	public static List<Integer> beatWidthBases = beatWidthBasesBig;
	public static int beatWidthBaseIndex = 0;
	public static int panelMaxHeight = VibeComposerGUI.scrollPaneDimension.height;
	private ShowAreaBig sa;
	private ShowRulerBig ruler;
	private JPanel pan;
	private int panelHeight;
	public static JScrollPane areaScrollPane;
	public static JScrollPane rulerScrollPane;
	public static JScrollPane horizontalPane;
	public static CheckButton soloMuterHighlight;
	public static double maxEndTime = 10.0;

	private static JPanel scorePartPanel;
	private static CheckButton[] partsShown;
	private static JButton toggler;
	public static ScrollComboBox2<Integer> scoreBox;
	public static ScrollComboBox<String> trimNoteLengthBox;

	public ShowPanelBig() {
		this(new Dimension(beatWidthBase, panelMaxHeight));
	}

	public ShowPanelBig(Dimension size) {
		super();

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setAlignmentX(LEFT_ALIGNMENT);
		// Because the ScrollPanel can only take one componenet 
		// a panel called apn is created to hold all comoponenets
		// then only pan is added to this classes ScrollPane
		partsShown = new CheckButton[5];
		scoreBox = new ScrollComboBox2<Integer>(false);
		ScrollComboBox.addAll(new Integer[] { 0 }, scoreBox);
		scoreBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				setScore();
			}
		});
		scoreBox.setMaximumSize(new Dimension(40, ShowRulerBig.maxHeight));

		pan = new JPanel();
		setOpaque(false);
		pan.setOpaque(false);
		this.setSize(size);
		pan.setSize(size);
		pan.setLayout(new BorderLayout());

		horizontalPane = new JScrollPane() {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(beatWidthBases.get(0), getHeight() - 50);
			}

			@Override
			public Dimension getMinimumSize() {
				return new Dimension(beatWidthBases.get(0), getHeight() - 50);
			}
		};
		horizontalPane.setViewportView(pan);
		horizontalPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		horizontalPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		horizontalPane.getHorizontalScrollBar().setUnitIncrement(16);
		horizontalPane.setAlignmentX(LEFT_ALIGNMENT);


		scorePartPanel = new JPanel();
		scorePartPanel.setLayout(new BoxLayout(scorePartPanel, BoxLayout.X_AXIS));
		scorePartPanel
				.setMaximumSize(new Dimension(ShowPanelBig.beatWidthBase, ShowRulerBig.maxHeight));

		scorePartPanel.add(new JLabel("Score History"));
		scorePartPanel.add(scoreBox);
		scorePartPanel.add(new JLabel("Included Parts"));
		for (int i = 0; i < 5; i++) {
			partsShown[i] = new CheckButton(VibeComposerGUI.instNames[i], true,
					OMNI.alphen(VibeComposerGUI.instColors[i], 75));
			partsShown[i].setRunnable(() -> setScore());
			int fI = i;
			partsShown[i].addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent evt) {
					if (SwingUtilities.isMiddleMouseButton(evt)) {
						boolean enableAll = true;
						for (int j = 0; j < 5; j++) {
							if (j != fI && partsShown[j].isSelected()) {
								enableAll = false;
							}
						}

						for (int j = 0; j < 5; j++) {
							partsShown[j].setSelectedRaw(j == fI || enableAll);
						}
						setScore();
					}
				}
			});
			partsShown[i].setMargin(new Insets(0, 0, 0, 0));
			scorePartPanel.add(partsShown[i]);
		}
		{
			toggler = new JButton("All");
			toggler.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					boolean turnedOn = false;
					for (CheckButton c : partsShown) {
						if (!c.isSelected()) {
							c.setSelectedRaw(true);
							turnedOn = true;
						}
					}
					if (!turnedOn) {
						for (CheckButton c : partsShown) {
							c.setSelectedRaw(false);
						}
					}
					setScore();
				}
			});
			scorePartPanel.add(toggler);

			soloMuterHighlight = new CheckButton("Highlight Audible", true);
			soloMuterHighlight.setRunnable(() -> {
				if (soloMuterHighlight.isSelected()) {
					/*for (int i = 0; i < 5; i++) {
						partsShown[i].setSelectedRaw(true);
						partsShown[i].setEnabled(false);
					}*/
					setScore();
					//toggler.setEnabled(false);
				} else {
					/*for (int i = 0; i < 5; i++) {
						partsShown[i].setEnabled(true);
					}*/
					//toggler.setEnabled(true);
					setScore();
				}
			});
			scorePartPanel.add(soloMuterHighlight);
		}

		scorePartPanel.add(new JLabel(" Trim Note Length:"));
		trimNoteLengthBox = new ScrollComboBox<>(false);
		trimNoteLengthBox.setMaximumSize(new Dimension(80, ShowRulerBig.maxHeight));
		trimNoteLengthBox.setFunc(e -> update());
		ScrollComboBox.addAll(new String[] { "NONE", "1/32", "1/16", "1/8" }, trimNoteLengthBox);
		scorePartPanel.add(trimNoteLengthBox);

		// add the score
		sa = new ShowAreaBig(this); //score, maxWidth, maxParts);
		sa.setVisible(true);

		MouseListener ml = new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent evt) {
				if (SwingUtilities.isLeftMouseButton(evt)) {
					if (ShowAreaBig.consumed) {
						ShowAreaBig.consumed = false;
						return;
					}
					Double percentage = getSequencePosFromMousePos(SwingUtils.getMouseLocation());
					if (percentage == null) {
						return;
					}

					boolean sequenceRunning = VibeComposerGUI.sequencer.isRunning();
					if (sequenceRunning) {
						VibeComposerGUI.sequencer.stop();
					}

					int valueToSet = (int) (percentage * VibeComposerGUI.slider.getMaximum());
					//LG.d("Value to set: " + valueToSet);
					VibeComposerGUI.setSliderEnd(valueToSet);
					VibeComposerGUI.savePauseInfo();
					if (sequenceRunning) {
						VibeComposerGUI.sequencer.start();
					}

				} else if (SwingUtilities.isRightMouseButton(evt)) {
					ShowAreaBig.consumed = false;
				}
			}
		};

		sa.addMouseListener(ml);
		sa.setAlignmentX(LEFT_ALIGNMENT);

		JPanel areaPanel = new JPanel();
		areaPanel.setMaximumSize(new Dimension(beatWidthBases.get(beatWidthBases.size() - 1),
				ShowAreaBig.areaHeight));
		areaPanel.setAlignmentX(LEFT_ALIGNMENT);
		areaPanel.add(sa);

		areaScrollPane = new JScrollPane() {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(ShowPanelBig.beatWidthBase, getHeight() - 70);
			}
		};
		areaScrollPane.setViewportView(areaPanel);
		areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		areaScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		areaScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		areaScrollPane.setAlignmentX(LEFT_ALIGNMENT);
		setupMouseWheelListener();

		areaPanel.setVisible(true);
		pan.add("Center", areaScrollPane);
		//add a ruler
		ruler = new ShowRulerBig(this);
		ruler.setVisible(true);
		ruler.addMouseListener(ml);
		ruler.setAlignmentX(LEFT_ALIGNMENT);

		JPanel rulerPanel = new JPanel();
		rulerPanel.setMaximumSize(new Dimension(beatWidthBases.get(beatWidthBases.size() - 1),
				ShowRulerBig.maxHeight));
		rulerPanel.setAlignmentX(LEFT_ALIGNMENT);
		rulerPanel.add(ruler);

		rulerScrollPane = new JScrollPane() {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(ShowPanelBig.beatWidthBase, ShowRulerBig.maxHeight + 10);
			}
		};
		rulerScrollPane.setViewportView(rulerPanel);
		// for parity with area
		rulerScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		rulerScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		rulerScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		rulerScrollPane.setAlignmentX(LEFT_ALIGNMENT);


		rulerPanel.setVisible(true);
		pan.add("South", rulerScrollPane);
		panelHeight = panelMaxHeight;
		this.setSize(new Dimension(beatWidthBaseDefault, panelHeight));
		scorePartPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		this.add(scorePartPanel);
		this.add(horizontalPane);

		//getHAdjustable().setUnitIncrement(50); //set scroll speed
		//getHAdjustable().setBlockIncrement(50);

		//zoomIn(areaScrollPane, new Point(0, 200), 0.0, 1.0);

		//soloMuterHighlight.setSelected(true);
		repaint();

		// set up a more zoomed in first view
		double originalHeight = ShowAreaBig.noteHeight;
		sa.setNoteHeight(ShowAreaBig.noteHeight + 2);
		setScore();
		double changeY = ShowAreaBig.noteHeight / originalHeight;
		VibeComposerGUI.scoreScrollPane.repaint();

		SwingUtilities.invokeLater(() -> {
			zoomIn(areaScrollPane, new Point(0, 300), 0.0, changeY - 1.0);
		});
	}

	protected Double getSequencePosFromMousePos(Point xy) {
		SwingUtilities.convertPointFromScreen(xy, sa);

		int lastUsableSliderTime = VibeComposerGUI.sliderMeasureStartTimes
				.get(VibeComposerGUI.sliderMeasureStartTimes.size() - 1);

		int sliderExtension = (VibeComposerGUI.sliderExtended < 0)
				? lastUsableSliderTime - VibeComposerGUI.slider.getMaximum()
				: VibeComposerGUI.sliderExtended;

		double correctionPercentage = 1.0
				- (sliderExtension / (double) lastUsableSliderTime);

		double usableEnd = correctionPercentage * (maxEndTime * beatWidth);
					/*LG.d("XY: " + xy.toString() + ", start: " + usableStart
							+ ", end: " + usableEnd);*/

		double percentage = xy.getX() / usableEnd;
		//LG.d("Percentage in MIDI: " + percentage);
					/*LG.i("Slider ratio: "
							+ (VibeComposerGUI.slider.getMaximum() + VibeComposerGUI.delayed())
									/ VibeComposerGUI.beatFromBpm(0));
					LG.i("Score ratio: " + usableEnd / beatWidth);
					LG.i("Delayed: " + VibeComposerGUI.delayed());
					LG.i("Total div 144: "
							+ (VibeComposerGUI.slider.getMaximum() - VibeComposerGUI.delayed())
									/ 144);
					LG.i(StringUtils.join(VibeComposerGUI.sliderBeatStartTimes, ","));
					LG.i(VibeComposerGUI.sliderExtended);*/

		if (xy.getX() > usableEnd || xy.getX() < beatWidth) {
			return null;
		}
		return percentage;
	}

	public void setupMouseWheelListener() {
		if (areaScrollPane.getMouseListeners() != null) {
			for (MouseWheelListener mwl : areaScrollPane.getMouseWheelListeners()) {
				areaScrollPane.removeMouseWheelListener(mwl);
			}
		}

		areaScrollPane.addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (e.isAltDown()) {
					if (e.getWheelRotation() > 0 && ShowAreaBig.noteHeight <= 4) {
						return;
					}
					double originalHeight = ShowAreaBig.noteHeight;
					sa.setNoteHeight(
							ShowAreaBig.noteHeight + ((e.getWheelRotation() > 0) ? -1 : 1));
					setScore();
					double changeY = ShowAreaBig.noteHeight / originalHeight;
					VibeComposerGUI.scoreScrollPane.repaint();

					if (e.getWheelRotation() > 0) {
						zoomIn(areaScrollPane, e.getPoint(), 0.0, changeY - 1.0);
					} else {
						SwingUtilities.invokeLater(() -> {
							zoomIn(areaScrollPane, e.getPoint(), 0.0, changeY - 1.0);
						});
					}


					//areaScrollPane.getVerticalScrollBar().setVisible(true);
					/*SwingUtils.setScrolledPosition(VibeComposerGUI.scoreScrollPane, true,
							positionPercentage);*/
				} else if (e.isControlDown()) {
					double originalWidth = Math.round(
							(ShowAreaBig.noteOffsetXMargin + ShowPanelBig.maxEndTime) * beatWidth);

					Point horPanePoint = SwingUtilities.convertPoint(areaScrollPane, e.getPoint(),
							horizontalPane);
					beatWidthBaseIndex = OMNI.clamp(
							beatWidthBaseIndex + ((e.getWheelRotation() > 0) ? -1 : 1), 0,
							beatWidthBases.size() - 1);
					beatWidthBase = beatWidthBases.get(beatWidthBaseIndex);
					setScore();
					double changeX = Math.round(
							(ShowAreaBig.noteOffsetXMargin + ShowPanelBig.maxEndTime) * beatWidth)
							/ originalWidth;
					VibeComposerGUI.scoreScrollPane.repaint();

					if (e.getWheelRotation() > 0) {
						zoomIn(horizontalPane, horPanePoint, changeX - 1.0, 0.0);
					} else {
						SwingUtilities.invokeLater(() -> {
							zoomIn(horizontalPane, horPanePoint, changeX - 1.0, 0.0);
						});
					}


					/*SwingUtils.setScrolledPosition(VibeComposerGUI.scoreScrollPane, true,
							positionPercentage);*/
				} else {
					if (e.isShiftDown()) {
						// Horizontal scrolling
						Adjustable adj = horizontalPane.getHorizontalScrollBar();
						int scroll = e.getUnitsToScroll() * adj.getBlockIncrement() * 3;
						adj.setValue(adj.getValue() + scroll);
					} else {
						// Vertical scrolling
						Adjustable adj = areaScrollPane.getVerticalScrollBar();
						int scroll = e.getUnitsToScroll() * adj.getBlockIncrement();
						adj.setValue(adj.getValue() + scroll);
						VibeComposerGUI.scoreScrollPane.repaint();
					}
				}
			}
		});
	}

	public void setScore() {
		if (MidiGenerator.LAST_SCORES.isEmpty()) {
			return;
		}
		int selectableScore = OMNI.clamp(scoreBox.getVal(), 0,
				MidiGenerator.LAST_SCORES.size() - 1);
		if (scoreBox.getLastVal() < MidiGenerator.LAST_SCORES_LIMIT - 1
				&& MidiGenerator.LAST_SCORES.size() > scoreBox.getLastVal() + 1) {
			scoreBox.addItem(scoreBox.getLastVal() + 1);
		}
		setScore(MidiGenerator.LAST_SCORES.get(selectableScore));
	}


	public void setScore(Score score) {
		List<Integer> includedParts = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			if (partsShown[i].isSelected()) {
				includedParts.add(i);
			}
		}
		setScore(score, includedParts);
	}

	// this method can be used to update the score continets of an existing ShowScore panel
	public void setScore(Score score, List<Integer> includedParts) {

		Score scrCopy = JMusicUtilsCustom.scoreCopy(score);

		List<Part> partsToRemove = new ArrayList<>();
		for (Object p : scrCopy.getPartList()) {
			Part part = (Part) p;
			int partIndex = ShowAreaBig.getIndexForPartName(part.getTitle());
			if (!includedParts.contains(partIndex)) {
				partsToRemove.add(part);
				continue;
			}
			List<Phrase> phrasesToRemove = new ArrayList<>();
			for (Object vec : part.getPhraseList()) {
				Phrase ph = (Phrase) vec;
				if (ph.getHighestPitch() < 0) {
					phrasesToRemove.add(ph);
				}

			}
			phrasesToRemove.forEach(e -> part.removePhrase(e));
		}
		partsToRemove.forEach(e -> scrCopy.removePart(e));
		maxEndTime = score.getEndTime();
		//LG.i("New score set with maxEndTime: " + maxEndTime);
		this.score = scrCopy;
		beatWidth = beatWidthBase / (ShowAreaBig.noteOffsetXMargin + ShowPanelBig.maxEndTime);
		if (beatWidth < 1.0)
			beatWidth = 1.0;
		update();
		//LG.d();
		//areaScrollPane.getVerticalScrollBar().setValue(50);
		//repaint();
	}

	/**
	 * Used to adjust the height when the size of display is changed.
	 */
	public void updatePanelHeight(int height) {
		panelHeight = height;
		this.setSize(new Dimension(beatWidthBase - 50, panelHeight));
	}

	/**
	 * Report the current height of th e panel in this object.
	 */
	public int getHeight() {
		return panelHeight;
	}

	/*
	* Return the currently active ShowArea object
	*/
	public ShowAreaBig getShowArea() {
		return sa;
	}

	public void update() {
		int sizeX = (int) Math
				.round((ShowAreaBig.noteOffsetXMargin + ShowPanelBig.maxEndTime) * beatWidth);
		sa.setSize(sizeX, panelHeight);
		ruler.setSize(sizeX, ShowRulerBig.maxHeight);
		pan.repaint();
		repaintMinimum();
		areaScrollPane.setMaximumSize(new Dimension(beatWidthBase, panelHeight - 70));
		rulerScrollPane.setMaximumSize(new Dimension(beatWidthBase, ShowRulerBig.maxHeight + 10));
		horizontalPane.setMaximumSize(new Dimension(beatWidthBases.get(0), panelHeight - 30));
		horizontalPane.setSize(new Dimension(beatWidthBases.get(0), panelHeight - 30));
		this.repaint();
	}

	public void repaintMinimum() {
		SwingUtilities.invokeLater(() -> {
			sa.repaint();
			ruler.repaint();
		});
	}

	public static void zoomIn(JScrollPane pane, Point point, double zoomX, double zoomY) {
		Point pos = pane.getViewport().getViewPosition();
		LG.i("ZoomX: " + zoomX + ", zoomY: " + zoomY);
		LG.i("PointO: " + point.toString());
		int newX = (int) (point.x * zoomX + (1 + zoomX) * pos.x);
		int newY = (int) (point.y * zoomY + (1 + zoomY) * pos.y);
		Point newPoint = new Point(newX, newY);
		LG.i("PointN: " + newPoint.toString());
		pane.getViewport().setViewPosition(newPoint);

		pane.repaint();
	}
}
