package org.vibehistorian.vibecomposer.Components;

import jm.music.data.Note;
import org.vibehistorian.vibecomposer.Helpers.PhraseNote;
import org.vibehistorian.vibecomposer.Helpers.PhraseNotes;
import org.vibehistorian.vibecomposer.LG;
import org.vibehistorian.vibecomposer.MidiGenerator;
import org.vibehistorian.vibecomposer.MidiUtils;
import org.vibehistorian.vibecomposer.OMNI;
import org.vibehistorian.vibecomposer.Popups.MidiEditPopup;
import org.vibehistorian.vibecomposer.Popups.TemporaryInfoPopup;
import org.vibehistorian.vibecomposer.Popups.TextProcessingPopup;
import org.vibehistorian.vibecomposer.Section;
import org.vibehistorian.vibecomposer.VibeComposerGUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.vibehistorian.vibecomposer.MidiGenerator.DBL_ERR;

public class MidiEditArea extends JComponent {

	public static final double[] TIME_GRID = new double[] { 0.125, 1 / 6.0, MidiGenerator.Durations.SIXTEENTH_NOTE, 1 / 3.0, MidiGenerator.Durations.EIGHTH_NOTE,
			2 / 3.0, MidiGenerator.Durations.QUARTER_NOTE, 4 / 3.0, MidiGenerator.Durations.HALF_NOTE, MidiGenerator.Durations.WHOLE_NOTE };

	public enum DM {
		POSITION, DURATION, NOTE_START, VELOCITY, PITCH, PITCH_SHAPE, VELOCITY_SHAPE, MULTIPLE;
	}

	private static final long serialVersionUID = -2972572935738976623L;
	public int currentMin = -10;
	public int currentMax = 10;
	public int rangeMin = -10;
	public int rangeMax = 10;
	PhraseNotes values = null;
	public int part = 0;

	public int marginX = 80;
	public int marginY = 40;

	int markWidth = 6;
	int numHeight = 6;
	int numWidth = 4;

	public List<PhraseNote> selectedNotes = new ArrayList<>();
	public List<PhraseNote> selectedNotesCopy = new ArrayList<>();
	PhraseNote draggedNote;
	PhraseNote draggedNoteCopy;
	Point orderValPressed;
	Set<DM> dragMode = new HashSet<>();
	Integer dragLocation;
	long lastPlayedNoteTime;
	boolean lockTimeGrid;
	Integer dragX;
	Integer dragY;
	public double sectionLength = 16.0;
	PhraseNote highlightedNote;
	Integer highlightedDragLocation;
	Integer prevHighlightedDragLocation;
	Point mousePoint;

	public Double lastUsedDuration = MidiGenerator.Durations.EIGHTH_NOTE;

	public boolean drawNoteStrings = true;
	public boolean splitNotesByGrid = false;
	public int timeGridChoice = TIME_GRID.length-1;
	public List<Double> chordSpacingDurations;
	public boolean forceMarginTime = false;

	public static double phraseMarginX = 2.0;

	int noteDragMarginX = 5;

	MidiEditPopup pop = null;
	public int notesHistoryIndex = 0;
	public List<PhraseNotes> notesHistory = new ArrayList<>();

	public MidiEditArea(int minimum, int maximum, PhraseNotes vals) {
		super();
		setRange(minimum, maximum);
		setCurrentMin(minimum);
		setCurrentMax(maximum);
		values = vals;
		resetBase();

		addMouseWheelListener(e -> {
            if (!e.isAltDown()) {
                int rot = (e.getWheelRotation() > 0) ? -1 : 1;
                if ((rot > 0 && currentMax > rangeMax -7) || (rot < 0 && currentMin < rangeMin +10)) {
                    return;
                }
                int originalTrackScopeUpDown = MidiEditPopup.trackScope;
                MidiEditPopup.trackScopeUpDown = OMNI
                        .clamp(MidiEditPopup.trackScopeUpDown + rot, -4, 4);
                if (originalTrackScopeUpDown != MidiEditPopup.trackScopeUpDown) {
                    currentMin += MidiEditPopup.baseMargin * rot;
                    currentMax += MidiEditPopup.baseMargin * rot;
                    setAndRepaint();
                }
            } else {
                int rot = (e.getWheelRotation() > 0) ? 1 : -1;
                int originalTrackScope = MidiEditPopup.trackScope;
                if (rot > 0 && (currentMax > rangeMax -7 || currentMin < rangeMin +10)) {
                    return;
                }
                MidiEditPopup.trackScope = originalTrackScope + rot;
                if (rot < 0 && ((currentMax - currentMin) <= MidiEditPopup.baseMargin * 4)) {
                    MidiEditPopup.trackScope++;
                }
                if (originalTrackScope != MidiEditPopup.trackScope) {
                    if (!(rot > 0 && currentMax > rangeMax -7)) {
                        currentMax += MidiEditPopup.baseMargin * rot;
                    }
                    if (!(rot > 0 && currentMin < rangeMin +10)) {
                        currentMin -= MidiEditPopup.baseMargin * rot;
                    }
                    setAndRepaint();
                }
            }
        });

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent evt) {
				if (!isEnabled()) {
					return;
				}

				dragMode.clear();
				dragX = evt.getPoint().x;
				dragY = evt.getPoint().y;
				draggedNote = getDraggedNote(evt.getPoint());
				dragLocation = getMouseNoteLocationPixelated(draggedNote, evt.getPoint());

				lockTimeGrid = !evt.isShiftDown();

				if (SwingUtilities.isLeftMouseButton(evt)) {
					handleLeftPress(evt);
				} else if (SwingUtilities.isRightMouseButton(evt)) {
					orderValPressed = getOrderAndValueFromPosition(evt.getPoint());
				} else if (SwingUtilities.isMiddleMouseButton(evt)) {
					handleMiddlePress(evt);
				}
				draggedNoteCopy = (draggedNote != null) ? new PhraseNote(draggedNote.toNote())
						: null;
				if (dragLocation != null) {
					highlightedNote = draggedNote;
					highlightedDragLocation = dragLocation;
				}
				if (selectedNotes.contains(draggedNote)) {
					dragMode.add(DM.MULTIPLE);
					LG.i("Multi-drag!");
				}
				setAndRepaint();
			}

			@Override
			public void mouseReleased(MouseEvent evt) {
				if (!isEnabled()) {
					return;
				}
				boolean saveToHistory = true;
				if (SwingUtilities.isRightMouseButton(evt)) {
					saveToHistory = handleRightRelease(evt);
				} else {
					if (draggingAny(DM.VELOCITY)
							&& draggedNoteCopy.getDynamic() != draggedNote.getDynamic()
							&& (System.currentTimeMillis() - lastPlayedNoteTime) > 500
							&& !MidiEditPopup.regenerateInPlaceChoice) {
						playNote(draggedNote, 300);
					}

					if (!draggingAny(DM.MULTIPLE)) {
						selectedNotes.clear();
						selectedNotesCopy.clear();
					} else {
						makeSelectedNotesCopy();
					}
				}
				if (saveToHistory
						&& (SwingUtilities.isLeftMouseButton(evt) ||
							SwingUtilities.isRightMouseButton(evt) ||
							SwingUtilities.isMiddleMouseButton(evt))) {
					values.remakeNoteStartTimes(true);
					saveToHistory();
				}

				reset();
				if (pop != null && SwingUtilities.isLeftMouseButton(evt)
						&& MidiEditPopup.regenerateInPlaceChoice) {
					pop.apply();
					selectedNotes.clear();
					selectedNotesCopy.clear();
					VibeComposerGUI.vibeComposerGUI.regenerateInPlace();
				}
			}
		});

		addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseDragged(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					mousePoint = e.getPoint();
				}
				highlightedNote = draggedNote;
				highlightedDragLocation = dragLocation;
				processDragEvent(e);
				setAndRepaint();
				//LG.d("Mouse dragged");
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				mousePoint = e.getPoint();
				processHighlight(e.getPoint());
				processDragEvent(e);
				setAndRepaint();
				//LG.d("Mouse moved");
			}

		});
	}

	public void undo() {
		loadFromHistory(notesHistoryIndex - 1);

		if (pop != null && MidiEditPopup.regenerateInPlaceChoice) {
			selectedNotes.clear();
			selectedNotesCopy.clear();
			VibeComposerGUI.vibeComposerGUI.regenerateInPlace();
		}
	}

	public void redo() {
		loadFromHistory(notesHistoryIndex + 1);
	}

	public void saveToHistory() {
		if (notesHistoryIndex + 1 < notesHistory.size() && notesHistory.size() > 0) {
			notesHistory = notesHistory.subList(0, notesHistoryIndex + 1);
		}

		notesHistory.add(getValues().copy());
		notesHistoryIndex = notesHistory.size() - 1;
		if (pop != null) {
			pop.updateHistoryBox();
		}
	}

	public void loadFromHistory(int index) {
		if (notesHistoryIndex == index) {
			return;
		}
		LG.i("Loading notes with index: " + index);
		if (notesHistory.size() > 0 && index >= 0 && index < notesHistory.size()) {
			setValues(notesHistory.get(index).copy());
			notesHistoryIndex = index;
			if (pop != null) {
				pop.editHistoryBox.setSelectedIndex(index);
				pop.repaintMvea();
			}
		}
	}

	public void deleteSelected() {
		if (selectedNotes.size() > 0) {
			selectedNotes.forEach(e -> {
				if (e.getRv() < MidiGenerator.DBL_ERR) {
					getValues().remove(e);
				} else {
					e.setPitch(Note.REST);
				}
			});
			selectedNotes.clear();
			selectedNotesCopy.clear();
			reset();
			saveToHistory();

			if (MidiEditPopup.regenerateInPlaceChoice) {
				VibeComposerGUI.vibeComposerGUI.regenerateInPlace();
			}
		}
	}

	public void transposeSelected() {
		new TextProcessingPopup("Transpose amount", e -> {
			if (selectedNotes == null || selectedNotes.isEmpty()) {
				new TemporaryInfoPopup("No notes selected!", 1000);
				return;
			}
			try {
				int parsedInt = Integer.valueOf(e);
				for (PhraseNote n : selectedNotes) {
					// 0..127 midi value
					n.setPitch(OMNI.clampMidi(n.getPitch() + parsedInt));
				}
				setCustomValues(getValues());

				if (MidiEditPopup.regenerateInPlaceChoice) {
					selectedNotes.clear();
					selectedNotesCopy.clear();
					VibeComposerGUI.vibeComposerGUI.regenerateInPlace();
				}
			} catch (Exception ex) {
				new TemporaryInfoPopup("Invalid number entered!", 1500);
			}
		});
	}

	public void selectAll() {
		selectedNotes.clear();
		selectedNotes.addAll(getValues().stream().filter(e -> e.getPitch() >= 0)
				.collect(Collectors.toList()));
		makeSelectedNotesCopy();
	}

	public void addKeyboardControls(JPanel topControlPanel) {
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
		topControlPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
		topControlPanel.getActionMap().put("undo", undoAction);
		topControlPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");
		topControlPanel.getActionMap().put("redo", redoAction);
		topControlPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		topControlPanel.getActionMap().put("delete", deleteAction);
		topControlPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "selectAll");
		topControlPanel.getActionMap().put("selectAll", selectAllAction);
		topControlPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK),
				"transposeSelected");
		topControlPanel.getActionMap().put("transposeSelected", transposeSelectedAction);
	}

	public void setCustomValues(PhraseNotes values) {
		setCustomValues(values, MidiEditPopup.baseMargin, MidiEditPopup.trackScope);
	}

	public void setCustomValues(PhraseNotes values, int baseMargin, int trackScope) {
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
		setCurrentMin(Math.min(currentMin, vmin));
		setCurrentMax(Math.max(currentMax, vmax));

		if (pop != null) {
			part = pop.part;
		}
		marginX = (part == 4) ? 160 : 80;
		setValues(values);
		saveToHistory();

		if (pop != null) {
			pop.repaintMvea();
		} else {
			setAndRepaint();
		}
	}

	public double getTimeGrid() {
		return TIME_GRID[pop != null ? MidiEditPopup.snapToTimeGridChoice : timeGridChoice];
	}


	private void handleMiddlePress(MouseEvent evt) {
		if (!isEnabled()) {
			return;
		}
		Point orderVal = getOrderAndValueFromPosition(evt.getPoint());
		boolean isAlt = (evt.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) > 0;
		if (isAlt) {
			dragMode.add(DM.DURATION);
		} else if (evt.isControlDown()) {
			if (orderVal != null && values.get(orderVal.x).getPitch() >= 0) {
				PhraseNote note = values.get(orderVal.x);
				int velocity = OMNI.clampVel((127 * (orderVal.y - currentMin) / (double) (currentMax - currentMin)));
				if (velocity != note.getDynamic()) {
					note.setDynamic(velocity);
					playNote(note);
				}
			}
			dragMode.add(DM.VELOCITY_SHAPE);

		} else if (evt.isShiftDown()) {
			dragMode.add(DM.PITCH);
		} else if (draggedNote != null) {
			lastPlayedNoteTime = System.currentTimeMillis();
			playNote(draggedNote);

			dragMode.add(DM.VELOCITY);
		} else if (pop != null && pop.displayDrumHelper.isSelected() && pop.getSec() != null) {
			int row = getPitchFromPosition(evt.getPoint().y) - currentMin;
			List<PhraseNotes> noteNotes = pop.getSec().getPatterns(4);
			if (row >= 0 && row < noteNotes.size()) {
				pop.setupIdentifiers(4, VibeComposerGUI.getInstList(4).get(row).getPanelOrder());
				// TODO
				pop.setup(pop.getSec());
			}

		}

	}

	private boolean handleRightRelease(MouseEvent evt) {
		if (!isEnabled()) {
			return false;
		}
		Point orderVal = getOrderAndValueFromPosition(evt.getPoint());
		boolean isAlt = (evt.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) > 0;
		boolean sameNotePressed = orderVal != null && orderVal.equals(orderValPressed);
		if (isAlt && sameNotePressed) {
			splitNotes(orderVal.x, evt.getPoint());
		} else if (selectedNotes.size() < 2 && sameNotePressed) {
			setVal(orderVal.x, Note.REST);
		} else {
			selectAllNotes(evt);
			return false;
		}

		return true;
	}

	private void splitNotes(int noteIndex, Point point) {
		values.remakeNoteStartTimes();
		double time = getTimeFromPosition(point);
		if (splitNotesByGrid) {
			time = getClosestToTimeGrid(time);
		}
		PhraseNote splitNote = values.get(noteIndex);
		List<PhraseNote> notesToSplit = selectedNotes.contains(splitNote) ? selectedNotes
				: Collections.singletonList(splitNote);
		for (PhraseNote pn : notesToSplit) {
			double noteEnd = (pn.getStartTime() + pn.getDuration());
			if (pn.getStartTime() < time && time < noteEnd) {
				int newNoteIndex = values.indexOf(pn);
				pn.setDuration(time - pn.getStartTime());
				PhraseNote newPn = new PhraseNote();
				newPn.setPitch(pn.getPitch());
				newPn.setRv(0);
				newPn.setDynamic(pn.getDynamic());
				newPn.setDuration(noteEnd - time);
				newPn.setOffset(pn.getOffset() + pn.getDuration());
				values.add(newNoteIndex, newPn);
			}
		}
	}


	private void handleLeftPress(MouseEvent evt) {
		if (evt.isAltDown()) {
			dragMode.add(DM.NOTE_START);
		} else if (evt.isControlDown()) {
			if (draggedNote == null || selectedNotes.isEmpty()) {
				dragMode.add(DM.PITCH_SHAPE);
			} else {
				List<PhraseNote> newSelectedNotes = new ArrayList<>();
				for (int i = 0; i < selectedNotes.size(); i++) {
					PhraseNote newNote = selectedNotes.get(i).clone();
					newNote.setRv(0);
					if (selectedNotes.get(i) == draggedNote) {
						LG.i("Found draggedNote in selected notes!");
						draggedNote = newNote;
					}
					int insertionIndex = values.indexOf(selectedNotes.get(i));
					values.add(insertionIndex, newNote);
					newSelectedNotes.add(newNote);
				}
				selectedNotes = newSelectedNotes;
				makeSelectedNotesCopy();

				dragMode.add(DM.POSITION);
				if (!evt.isShiftDown())
					dragMode.add(DM.PITCH);

				playNote(draggedNote);
				setAndRepaint();
			}
		} else {
			if (draggedNote == null) {
				Point orderVal = getOrderAndValueFromPosition(evt.getPoint(), false, true);
				if (orderVal != null && !values.isEmpty()) {
					/*if (pn.getPitch() == Note.REST && pn.getRv() > MidiGenerator.DBL_ERR) {
					
						LG.d("UnRESTing existing original note..");
						pn.setOffset(0);
						setVal(orderVal.x, orderVal.y);
						draggedNote = pn;
					}*/

					LG.d("Inserting new note..");
					int closestNormalized = MidiUtils.getClosestFromList(MidiUtils.MAJ_SCALE,
							orderVal.y % 12);
					PhraseNote insertedPn = new PhraseNote(pop != null && pop.isSnapPitch()
							? (MidiUtils.octavePitch(orderVal.y) + closestNormalized)
							: orderVal.y);
					insertedPn.setDuration((lastUsedDuration != null && lastUsedDuration + DBL_ERR > MidiGenerator.Durations.SIXTEENTH_NOTE / 2.0)
							? lastUsedDuration
							: MidiGenerator.Durations.EIGHTH_NOTE);
					insertedPn.setRv(0);
					insertedPn.setOffset(values.get(orderVal.x).getOffset());
					insertedPn.setStartTime(values.get(orderVal.x).getStartTime());
					insertedPn.setAbsoluteStartTime(values.get(orderVal.x).getAbsoluteStartTime());
					getValues().add(orderVal.x, insertedPn);


					draggedNote = insertedPn;
					double time = getTimeFromPosition(evt.getPoint());
					LG.d("Time pre:" + time);
					double offset = time - draggedNote.getAbsoluteStartTime();
					if (lockTimeGrid) {
						offset = getClosestToTimeGrid(time) - draggedNote.getAbsoluteStartTime();
					}
					draggedNote.setOffset(offset);
					LG.d("Time post:"
							+ (draggedNote.getAbsoluteStartTime() + draggedNote.getOffset()));

					dragMode.add(DM.PITCH);
					dragMode.add(DM.POSITION);
					dragLocation = 1;
					playNote(draggedNote, 300);
				}
			} else {
				switch (dragLocation) {
				case 0:
					dragMode.add(DM.NOTE_START);
					break;
				case 1:
					dragMode.add(DM.POSITION);
					if (!evt.isShiftDown())
						dragMode.add(DM.PITCH);
					break;
				case 2:
					dragMode.add(DM.DURATION);
					break;
				}
				playNote(draggedNote);
			}
		}
	}

	protected void selectAllNotes(MouseEvent evt) {
		Rectangle rect = getRectFromPoint(evt.getPoint());
		// any part of note within rectangle 
		values.remakeNoteStartTimes();
		List<PhraseNote> newSelection = values.stream().filter(e -> noteInRect(e, rect))
				.collect(Collectors.toList());
		if (evt.isControlDown()) {
			newSelection.forEach(e -> {
				if (selectedNotes.contains(e)) {
					selectedNotes.remove(e);
				} else {
					selectedNotes.add(e);
				}
			});
		} else {
			selectedNotes = newSelection;
		}
		makeSelectedNotesCopy();
	}

	public void makeSelectedNotesCopy() {
		selectedNotesCopy.clear();
		selectedNotesCopy
				.addAll(selectedNotes.stream().map(e -> e.clone()).collect(Collectors.toList()));
	}

	private boolean noteInRect(PhraseNote pn, Rectangle rect) {
		// = for X -> note start > MAX is NOK, else note start > MIN is OK, else note start < MIN is OK if note end > MIN
		// = for Y -> note pitch OK if > y.MIN and < y.MAX

		int yMax = getPitchFromPosition(rect.y);
		if (pn.getPitch() > yMax) {
			return false;
		}
		int yMin = getPitchFromPosition(rect.y + rect.height);
		if (pn.getPitch() < yMin) {
			return false;
		}
		double xMax = getTimeFromPosition(rect.x + rect.width);
		if (pn.getStartTime() > xMax) {
			return false;
		}
		double xMin = getTimeFromPosition(rect.x);
		if (pn.getStartTime() > xMin) {
			return true;
		}
		return pn.getStartTime() + pn.getDuration() > xMin;
	}

	private void processHighlight(Point xy) {
		highlightedNote = getDraggedNote(xy);
		highlightedDragLocation = getMouseNoteLocationPixelated(highlightedNote, xy);
		if (prevHighlightedDragLocation != null
				&& !prevHighlightedDragLocation.equals(highlightedDragLocation)) {
			prevHighlightedDragLocation = highlightedDragLocation;
		} else if (prevHighlightedDragLocation == null && highlightedDragLocation != null) {
			prevHighlightedDragLocation = highlightedDragLocation;
		}
	}

	private Integer getMouseNoteLocationPixelated(PhraseNote pn, Point loc) {
		if (pn != null) {
			int noteStart = getPositionFromTime(pn.getStartTime()) - noteDragMarginX;
			int noteEnd = getPositionFromTime(pn.getStartTime() + pn.getDuration())
					+ noteDragMarginX;

			if (noteStart <= loc.x && loc.x <= noteEnd) {
				if (pn.getDuration() > MidiGenerator.Durations.SIXTEENTH_NOTE / 2.0) {
					if (noteStart + noteDragMarginX * 2 >= loc.x) {
						return 0;
					} else if (noteEnd - noteDragMarginX * 2 <= loc.x) {
						return 2;
					} else {
						return 1;
					}
				} else {
					return 1;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	private Integer getMouseNoteLocation(PhraseNote pn, Point loc) {
		if (pn != null) {
			double timeDifference = getTimeFromPosition(loc) - pn.getStartTime();
			double positionInNote = timeDifference / pn.getDuration();

			if (positionInNote >= 0 && positionInNote <= 1.0) {
				if (pn.getDuration() > MidiGenerator.Durations.SIXTEENTH_NOTE / 2.0) {
					if (positionInNote < 0.15) {
						return 0;
					} else if (positionInNote > 0.85) {
						return 2;
					} else {
						return 1;
					}
				} else {
					return 1;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	private void playNote(PhraseNote pn) {
		playNote(pn, 500);
	}

	private void playNote(PhraseNote pn, int durationMs) {
		if (pn != null && pop != null) {
			if (selectedNotes.size() > 1 && selectedNotes.contains(pn)) {
				selectedNotes.stream().map(e -> e.getPitch()).distinct()
						.forEach(e -> VibeComposerGUI.playNote(e, durationMs, pn.getDynamic(),
								pop.part, pop.partOrder, pop.getSec(), true));
			} else {
				VibeComposerGUI.playNote(pn.getPitch(), durationMs, pn.getDynamic(), pop.part,
						pop.partOrder, pop.getSec(), false);
			}

		}

	}

	private void resetBase() {
		draggedNote = null;
		draggedNoteCopy = null;
		dragMode.clear();
		lastPlayedNoteTime = 0;
		lockTimeGrid = false;

		dragX = null;
		dragY = null;
		dragLocation = null;

		highlightedNote = null;
		highlightedDragLocation = null;
		mousePoint = null;

		orderValPressed = null;
	}

	public void reset() {
		resetBase();
		setAndRepaint();
	}

	public void setAndRepaint() {
		values.remakeNoteStartTimes();
		repaint();
	}


	protected void processDragEvent(MouseEvent evt) {
		if (!isEnabled()) {
			return;
		}
		if (!dragMode.isEmpty()) {
			if (draggingAny(DM.PITCH_SHAPE, DM.VELOCITY_SHAPE)) {
				// PITCH SHAPE
				if (draggingAny(DM.PITCH_SHAPE)) {
					Point orderVal = getOrderAndValueFromPosition(evt.getPoint());
					if (orderVal != null && values.get(orderVal.x).getPitch() >= 0) {
						int prevPitch = values.get(orderVal.x).getPitch();
						setVal(orderVal.x, orderVal.y);
						if (values.get(orderVal.x).getPitch() != prevPitch) {
							playNote(values.get(orderVal.x), 300);
						}
					}
				}
				// VELOCITY SHAPE
				if (draggingAny(DM.VELOCITY_SHAPE)) {
					Point orderVal = getOrderAndValueFromPosition(evt.getPoint());
					if (orderVal != null && values.get(orderVal.x).getPitch() >= 0) {
						PhraseNote note = values.get(orderVal.x);
						int velocity = OMNI
								.clampVel((127 * (orderVal.y - currentMin) / (double) (currentMax - currentMin)));
						if (velocity != note.getDynamic()) {
							note.setDynamic(velocity);
							playNote(note);
						}

					}
				}
			} else if (draggedNote != null) {

				// POSITION
				if (draggingAny(DM.POSITION)) {
					double offset = getTimeFromPosition(evt.getPoint())
							- getTimeFromPosition(new Point(dragX, dragY))
							+ draggedNoteCopy.getOffset();
					if (lockTimeGrid) {
						offset = getClosestToTimeGrid(offset + draggedNote.getAbsoluteStartTime())
								- draggedNote.getAbsoluteStartTime();
					}
					if (forceMarginTime) {
						offset = OMNI.clamp(offset, -draggedNote.getAbsoluteStartTime(), sectionLength - draggedNote.getAbsoluteStartTime() - draggedNote.getDuration());
					}
					if (draggingAny(DM.MULTIPLE)) {
						double offsetChange = offset - draggedNoteCopy.getOffset();
						for (int i = 0; i < selectedNotesCopy.size(); i++) {
							selectedNotes.get(i)
									.setOffset(selectedNotesCopy.get(i).getOffset() + offsetChange);
						}
					} else {
						draggedNote.setOffset(offset);
					}

				}

				// DURATION
				if (draggingAny(DM.DURATION)) {
					double duration = getDurationFromPosition(evt.getPoint());
					if (lockTimeGrid) {
						duration = timeGridValue(duration);
					}
					duration = Math.max(MidiGenerator.Durations.SIXTEENTH_NOTE / 2, duration);
					if (draggingAny(DM.MULTIPLE)) {
						double durationChange = duration - draggedNoteCopy.getDuration();
						for (int i = 0; i < selectedNotesCopy.size(); i++) {
							selectedNotes.get(i).setDuration(Math.max(
									MidiGenerator.Durations.SIXTEENTH_NOTE / 2,
									selectedNotesCopy.get(i).getDuration() + durationChange));
						}
					} else {
						draggedNote.setDuration(duration);
						lastUsedDuration = duration;
					}

				}

				// NOTE START
				if (draggingAny(DM.NOTE_START)) {
					double offset = getTimeFromPosition(evt.getPoint())
							- getTimeFromPosition(new Point(dragX, dragY))
							+ draggedNoteCopy.getOffset();
					if (lockTimeGrid) {
						offset = getClosestToTimeGrid(offset + draggedNote.getAbsoluteStartTime())
								- draggedNote.getAbsoluteStartTime();
					}
					if (forceMarginTime) {
						offset = OMNI.clamp(offset, -draggedNote.getAbsoluteStartTime(), sectionLength - draggedNote.getAbsoluteStartTime() - draggedNote.getDuration());
					}
					double duration = draggedNoteCopy.getDuration() + draggedNoteCopy.getOffset()
							- offset;
					if (duration > MidiGenerator.Durations.SIXTEENTH_NOTE / 2.5) {
						if (draggingAny(DM.MULTIPLE)) {
							double offsetChange = offset - draggedNoteCopy.getOffset();
							double durationChange = duration - draggedNoteCopy.getDuration();
							for (int i = 0; i < selectedNotesCopy.size(); i++) {
								double newDuration = selectedNotesCopy.get(i).getDuration()
										+ durationChange;
								if (newDuration > MidiGenerator.Durations.SIXTEENTH_NOTE / 2.5) {
									selectedNotes.get(i).setOffset(
											selectedNotesCopy.get(i).getOffset() + offsetChange);
									selectedNotes.get(i).setDuration(newDuration);
								}

							}
						} else {
							draggedNote.setOffset(offset);
							draggedNote.setDuration(duration);
						}
					}
				}

				// VELOCITY
				if (draggingAny(DM.VELOCITY)) {
					int velocity = getVelocityFromPosition(evt.getPoint());
					velocity = OMNI.clampMidi(velocity);
					if (draggingAny(DM.MULTIPLE)) {
						int velocityChange = velocity - draggedNoteCopy.getDynamic();
						for (int i = 0; i < selectedNotesCopy.size(); i++) {
							selectedNotes.get(i).setDynamic(OMNI.clampMidi(
									selectedNotesCopy.get(i).getDynamic() + velocityChange));
						}
					} else {
						draggedNote.setDynamic(velocity);
					}

					if ((System.currentTimeMillis() - lastPlayedNoteTime) > 500) {
						playNote(draggedNote, 300);
						lastPlayedNoteTime = System.currentTimeMillis();
					}
				}

				// PITCH
				if (draggingAny(DM.PITCH)) {
					int pitch = getPitchFromPosition(evt.getPoint().y);
					if (pop != null && pop.isSnapPitch()) {
						pitch = MidiUtils.getClosestFromList(MidiUtils.MAJ_SCALE, pitch % 12)
								+ MidiUtils.octavePitch(pitch);
					}
					boolean playNote = pitch != draggedNote.getPitch();
					if (draggingAny(DM.MULTIPLE)) {
						int pitchChange = pitch - draggedNoteCopy.getPitch();
						for (int i = 0; i < selectedNotesCopy.size(); i++) {
							int newPitchAbsolute = selectedNotesCopy.get(i).getPitch()
									+ pitchChange;
							int newPitch = MidiUtils.getClosestFromList(MidiUtils.MAJ_SCALE,
									newPitchAbsolute % 12)
									+ MidiUtils.octavePitch(newPitchAbsolute);
							selectedNotes.get(i).setPitch(OMNI.clampPitch(newPitch));
						}
					} else {
						draggedNote.setPitch(OMNI.clampPitch(pitch));
					}

					if (playNote) {
						playNote(draggedNote);
					}
				}
			}
		}
	}

	private double timeGridValue(double val) {
		double timeGrid = getTimeGrid();
		double newVal = Math.round(val / timeGrid) * timeGrid;
		return newVal;
	}

	private double getClosestToTimeGrid(double val) {
		//LG.i("Time val: " + val);
		List<Double> timeGridLocations = new ArrayList<>();
		double timeGrid = getTimeGrid();
		double currentTime = -1 * getPhraseMarginX();
		while (currentTime < (sectionLength + getPhraseMarginX())) {
			timeGridLocations.add(currentTime);
			currentTime += timeGrid;
		}
		timeGridLocations
				.addAll(values.stream().map(e -> e.getStartTime()).collect(Collectors.toSet()));
		timeGridLocations.stream().distinct().collect(Collectors.toList());
		Collections.sort(timeGridLocations);

		double closestVal = MidiUtils.getClosestDoubleFromList(timeGridLocations, val, false);
		//LG.i("Time - closest: " + closestVal);
		return closestVal;
	}

	void setVal(int pos, int pitch) {
		if (pitch == Note.REST && values.get(pos).getRv() < DBL_ERR) {
			values.remove(pos);
		} else {
			if (pop != null && pop.isSnapPitch()) {
				int closestNormalized = MidiUtils.getClosestFromList(MidiUtils.MAJ_SCALE,
						pitch % 12);

				values.get(pos).setPitch(MidiUtils.octavePitch(pitch) + closestNormalized);
			} else {
				values.get(pos).setPitch(pitch);
			}
		}
	}

	private String dblDraw2(double drawnDouble) {
		return String.format(Locale.GERMAN, "%.2f", drawnDouble);
	}

	private String dblDraw3(double drawnDouble) {
		return String.format(Locale.GERMAN, "%.3f", drawnDouble);
	}

	@Override
	public void paintComponent(Graphics guh) {
		if (guh instanceof Graphics2D) {
			Graphics2D g = (Graphics2D) guh;
			int w = getWidth();
			int h = getHeight();
			int numValues = values.size();
			int rowDivisors = currentMax - currentMin;
			int usableHeight = h - marginY * 2;
			double rowHeight = usableHeight / (double) rowDivisors;
			// clear screen
			g.setColor(VibeComposerGUI.isDarkMode ? VibeComposerGUI.panelColorHigh
					: new Color(180, 184, 188));
			g.fillRect(0, 0, w, h);

			// draw graph lines - first to last value X, min to max value Y
			g.setColor(OMNI.alphen(VibeComposerGUI.uiColor(), 80));

			Point bottomLeft = new Point(marginX, usableHeight + marginY);
			g.drawLine(bottomLeft.x, bottomLeft.y, bottomLeft.x, 0);
			g.drawLine(bottomLeft.x, bottomLeft.y, w, bottomLeft.y);

			double quarterNoteLength = getQuarterNoteLength();

			// to draw scale/key helpers
			Color highlightedScaleKeyColor = OMNI.alphen(VibeComposerGUI.uiColor(),
					VibeComposerGUI.isDarkMode ? 65 : 90);
			Color highlightedScaleKeyHelperColor = OMNI.alphen(highlightedScaleKeyColor, 20);
			List<Integer> highlightedScaleKey = calculateHighlightedScaleKey();
			Color nonHighlightedColor = VibeComposerGUI.isDarkMode ? new Color(150, 100, 30, 65)
					: new Color(150, 150, 150, 100);
			//Color nonHighlightedHelperColor = OMNI.alphen(nonHighlightedColor, 50);

			Color highlightedChordNoteColor = VibeComposerGUI.isDarkMode
					? new Color(220, 180, 150, 100)
					: new Color(0, 0, 0, 150);
			//Color highlightedChordNoteHelperColor = OMNI.alphen(highlightedChordNoteColor, 60);
			Map<Integer, Set<Integer>> chordHighlightedNotes = calculateHighlightedChords(
					pop == null ? null : pop.getSec());

			int partNum = part;

			// draw numbers left of Y line
			// draw line marks

			int drawEveryX = Math.max((int) Math.ceil((numHeight + 4) / rowHeight), 1);

			for (int i = 0; i < 1 + (currentMax - currentMin); i++) {
				int drawnInt = currentMin + i;
				String drawnValue = "" + (drawnInt) + (!drawNoteStrings ? "" : (" | "
						+ MidiUtils.pitchOrDrumToString(drawnInt, partNum, true)));
				int valueLength = drawnValue.startsWith("-") ? drawnValue.length() + 1
						: drawnValue.length();
				int drawValueX = bottomLeft.x / 2 - (numWidth * valueLength) / 2;
				int drawMarkX = bottomLeft.x - markWidth / 2;
				int drawY = bottomLeft.y - (int) (rowHeight * (i + 1));

				boolean highlighted = false;
				if (pop == null || pop.highlightMode.getSelectedIndex() % 2 == 1) {
					// scalekey highlighting
					if (highlightedScaleKey != null
							&& highlightedScaleKey.contains((drawnInt + 1200) % 12)) {
						g.setColor(highlightedScaleKeyColor);
						highlighted = true;
					}
				}
				if (!highlighted) {
					g.setColor(nonHighlightedColor);
				}

				g.drawLine(bottomLeft.x, drawY, w, drawY);

				g.setColor(VibeComposerGUI.uiColor());
				if (drawnInt % drawEveryX == 0) {
					g.drawString(drawnValue, drawValueX, drawY + numHeight / 2);
				}

				g.drawLine(drawMarkX, drawY, drawMarkX + markWidth, drawY);


			}


			List<Double> timeGridLocations = new ArrayList<>();
			double timeGrid = getTimeGrid();
			double currentTime = -1 * getPhraseMarginX();
			while (currentTime < (sectionLength + getPhraseMarginX())) {
				timeGridLocations.add(currentTime);
				currentTime += timeGrid;
			}
			timeGridLocations
					.addAll(values.stream().map(e -> e.getStartTime()).collect(Collectors.toSet()));
			timeGridLocations.stream().distinct().collect(Collectors.toList());
			Collections.sort(timeGridLocations);


			// draw numbers below X line
			// draw line marks
			double lineSpacing = (timeGrid < 0.24) ? 0.25 : timeGrid;
			double prev = -1000;
			double lastDrawnVal = -1000;
			for (int i = 0; i < timeGridLocations.size(); i++) {
				double curr = timeGridLocations.get(i);
				if (MidiUtils.roughlyEqual(curr, prev)) {
					continue;
				}
				g.setColor(VibeComposerGUI.uiColor());
				//String drawnValue = "" + (i + 1);
				//int valueLength = drawnValue.startsWith("-") ? drawnValue.length() + 1
				//		: drawnValue.length();
				int drawValueY = numHeight + (bottomLeft.y + h) / 2;
				int drawMarkY = (bottomLeft.y - markWidth / 2);
				int drawX = bottomLeft.x + (int) (quarterNoteLength * (curr + getPhraseMarginX()));
				double currAbsolute = Math.abs(curr);

				double remainderToOne = currAbsolute % 1.0;
				double remainderToTwo = currAbsolute % 2.0;
				if (curr - lastDrawnVal > 0.1) {
					if (remainderToOne < 0.05
							|| MidiUtils.isMultiple(remainderToTwo, lineSpacing)) {
						String drawnValue = null;
						if (remainderToOne < 0.05) {
							drawnValue = String.format("%.0f", curr);
						} else {
							if (lineSpacing > 0.49 || MidiUtils.roughlyEqual(remainderToOne, 0.5)) {
								drawnValue = ".5";
							}
						}
						if (drawnValue != null) {
							g.drawString(drawnValue, drawX - (numWidth * drawnValue.length()) / 2,
									drawValueY);
							lastDrawnVal = curr;
						}

					}
				}

				g.drawLine(drawX, drawMarkY, drawX, drawMarkY + markWidth);

				// draw line helpers/dots
				g.setColor(highlightedScaleKeyHelperColor);
				/*for (int j = 0; j < 1 + max - min; j++) {
					int drawDotY = bottomLeft.y - (int) (rowHeight * (j + 1));
					g.drawLine(drawX, drawDotY - 2, drawX, drawDotY + 2);
				}*/
				g.drawLine(drawX, bottomLeft.y, drawX,
						bottomLeft.y - (int) rowHeight * (currentMax - currentMin + 2));

				prev = curr;

			}


			// draw chord spacing
			List<Double> chordSpacings = getChordSpacingDurations();
			if (chordSpacings != null) {
				int numMeasures = (pop == null ? 1 : pop.getSec().getMeasures());
				double spacingSum = chordSpacings.stream().mapToDouble(e -> e).sum()
						* numMeasures;
				if (sectionLength > DBL_ERR && spacingSum > DBL_ERR
						&& !MidiUtils.roughlyEqual(spacingSum, sectionLength)) {
					for (int i = 0; i < chordSpacings.size(); i++) {
						chordSpacings.set(i, chordSpacings.get(i) * sectionLength / spacingSum);
					}
				}
				List<Double> chordSpacingsTemp = new ArrayList<>(chordSpacings);
				for (int i = 1; i < numMeasures; i++) {
					chordSpacings.addAll(chordSpacingsTemp);
				}

				double line = getPhraseMarginX();
				for (int i = 0; i < chordSpacings.size(); i++) {
					g.setColor(
							OMNI.alphen(VibeComposerGUI.isDarkMode ? Color.green : Color.red, 90));
					int drawX = bottomLeft.x + (int) (quarterNoteLength * line);
					// vertical separators
					if ((i > 0) || (getPhraseMarginX() > DBL_ERR)) {
						g.drawLine(drawX, bottomLeft.y, drawX, 0);
					}

					if (chordHighlightedNotes != null) {
						Set<Integer> chordNotes = chordHighlightedNotes
								.get(i % chordHighlightedNotes.size());
						if (pop != null && pop.highlightMode.getSelectedIndex() >= 2
								&& chordNotes != null) {
							// horizontal helper lines
							int drawXEnd = drawX + (int) (quarterNoteLength * chordSpacings.get(i));
							g.setColor(highlightedChordNoteColor);
							for (int j = 0; j < 1 + (currentMax - currentMin); j++) {
								int noteTest = (currentMin + j + 1200) % 12;
								if (chordNotes.contains(noteTest)) {
									g.setColor(highlightedChordNoteColor);
									int drawY = bottomLeft.y - (int) (rowHeight * (j + 1));
									g.drawLine(drawX, drawY, drawXEnd, drawY);
								}
							}
						}
					}


					line += chordSpacings.get(i);
				}

				if (getPhraseMarginX() > DBL_ERR) {
					g.setColor(
							OMNI.alphen(VibeComposerGUI.isDarkMode ? Color.green : Color.red, 90));
					int drawX = bottomLeft.x + (int) (quarterNoteLength * line);
					// vertical separators
					g.drawLine(drawX, bottomLeft.y, drawX, 0);
				}
			}


			// draw actual values

			boolean drawDragPosition = draggingAny(DM.NOTE_START, DM.POSITION, DM.DURATION);
			for (int i = 0; i < numValues; i++) {
				PhraseNote pn = values.get(i);
				int pitch = pn.getPitch();
				if (pitch < 0) {
					continue;
				}
				int pitchForText = pitch;
				int drawX = bottomLeft.x
						+ (int) (quarterNoteLength * (pn.getStartTime() + getPhraseMarginX()));
				int drawY = bottomLeft.y - (int) (rowHeight * (pitch + 1 - currentMin));
				int width = (int) (quarterNoteLength * pn.getDuration());

				// draw straight line connecting values
				// TODO: checkbox to enable it
				if (false && i < numValues - 1) {
					PhraseNote nextPn = values.getIterationOrder().stream().skip(i + 1).filter(e -> e.getPitch() >= 0).findFirst().orElse(null);
					if (nextPn != null) {
						g.setColor(OMNI.alphen(VibeComposerGUI.uiColor(), 50));
						int drawXNext = bottomLeft.x
								+ (int) (quarterNoteLength * (nextPn.getStartTime() + getPhraseMarginX()));
						int drawYNext = bottomLeft.y - (int) (rowHeight * (nextPn.getPitch() + 1 - currentMin));
						g.drawLine(drawX, drawY, drawXNext, drawYNext);
					}
				}

				boolean currentlyHighlighted = (highlightedNote != null) && (pn == highlightedNote)
						&& (highlightedDragLocation != null);

				g.setColor(OMNI.alphen(VibeComposerGUI.uiColor(), 140));
				g.drawLine(drawX, drawY - 5, drawX, drawY + 5);
				g.drawLine(drawX + width, drawY - 5, drawX + width, drawY + 5);
				String drawnString = (width > 20 || currentlyHighlighted)
						? (pitchForText + "(" + MidiUtils.pitchToString(pitchForText) + ") :"
								+ pn.getDynamic())
						: String.valueOf(pitchForText);
				if (currentlyHighlighted) {
					g.setColor(OMNI.alphen(
							OMNI.mixColor(VibeComposerGUI.uiColor(),
									VibeComposerGUI.isDarkMode ? Color.WHITE : Color.black, 0.8),
							(int) (140 + 70 * (pn.getDynamic() / 127.0))));
				}
				g.drawString(drawnString, drawX + 1, drawY - numHeight - 1);

				if ((draggedNote != null && pn == draggedNote) || selectedNotes.contains(pn)) {
					g.setColor(OMNI.alphen(OMNI.mixColor(VibeComposerGUI.uiColor(), Color.red, 0.7),
							(int) (30 + 140 * (pn.getDynamic() / 127.0))));
				} else {
					g.setColor(OMNI.alphen(VibeComposerGUI.uiColor(),
							(int) (30 + 140 * (pn.getDynamic() / 127.0))));
				}

				g.fillRect(drawX, drawY - 4, width, 8);
				if (currentlyHighlighted) {

					switch (highlightedDragLocation) {
					case 0:
						g.fillRect(drawX - noteDragMarginX, drawY - 5, noteDragMarginX * 2, 10);
						break;
					case 1:
						g.fillRect(drawX + noteDragMarginX, drawY - 5, width - noteDragMarginX * 2,
								10);
						break;
					case 2:
						g.fillRect(drawX + width - noteDragMarginX, drawY - 5, noteDragMarginX * 2,
								10);
						break;
					}

					g.setColor(OMNI.alphen(VibeComposerGUI.uiColor(), 140));
					if (drawDragPosition) {
						switch (highlightedDragLocation) {
						case 0:
							g.drawString(dblDraw3(pn.getStartTime()), drawX - 40, drawY + 15);
							break;
						case 1:
							g.drawString(dblDraw3(pn.getStartTime()), drawX - 20, drawY + 15);
							break;
						case 2:
							g.drawString(dblDraw3(pn.getStartTime() + pn.getDuration()),
									drawX + width + 20, drawY + 15);
							break;
						}
					}

				}

				if (draggingAny(DM.VELOCITY_SHAPE)) {
					g.setColor(OMNI.alphen(VibeComposerGUI.uiColor(),
							(int) (30 + 140 * (pn.getDynamic() / 127.0))));
					g.drawLine(drawX + width / 2, drawY, drawX + width / 2,
							drawY + 63 - pn.getDynamic());
				}
			}

			if (mousePoint != null) {
				g.setColor(OMNI.alphen(VibeComposerGUI.uiColor(), 150));

				if (dragX != null) {
					Rectangle rect = getRectFromPoint(mousePoint);
					g.drawRect(rect.x, rect.y, rect.width, rect.height);
				}

				if (dragX != null && highlightedNote != null && highlightedDragLocation != null) {
					int drawX = bottomLeft.x + (int) (quarterNoteLength
							* (highlightedNote.getStartTime() + getPhraseMarginX()));
					int width = (int) (quarterNoteLength * highlightedNote.getDuration());
					if (highlightedDragLocation == 2) {
						drawX += width;
					}
					g.drawLine(drawX, 0, drawX, h);
				} else {
					double time = getTimeFromPosition(mousePoint);
					time = getClosestToTimeGrid(time);
					int drawX = bottomLeft.x
							+ (int) (quarterNoteLength * (time + getPhraseMarginX()));
					g.drawLine(drawX, 0, drawX, h);

					if (!drawDragPosition) {
						g.drawString(dblDraw3(time), drawX, mousePoint.y);
					}

				}
			}

			if (pop != null && pop.displayDrumHelper.isSelected() && pop.getSec() != null) {
				g.setColor(OMNI.alphen(nonHighlightedColor, VibeComposerGUI.isDarkMode ? 40 : 80));
				List<PhraseNotes> noteNotes = pop.getSec().getPatterns(4);
				for (int i = 0; i < noteNotes.size(); i++) {
					noteNotes.get(i).remakeNoteStartTimes();
					for (int j = 0; j < noteNotes.get(i).size(); j++) {
						PhraseNote pn = noteNotes.get(i).get(j);
						int pitch = pn.getPitch();
						if (pitch < 0) {
							continue;
						}
						int drawX = bottomLeft.x + (int) (quarterNoteLength
								* (pn.getStartTime() + getPhraseMarginX()));
						int drawY = bottomLeft.y - (int) (rowHeight * (i + 1));
						int width = (int) (quarterNoteLength * pn.getDuration());
						g.fillRect(drawX, drawY - 4, width, 8);
					}
				}
			}
		}
	}

	private List<Double> getChordSpacingDurations() {
		if ((pop != null) && (pop.getSec() != null)
				&& (pop.getSec().getGeneratedDurations() != null)) {
			return new ArrayList<>(pop.getSec().getGeneratedDurations());
		} else {
			chordSpacingDurations = VibeComposerGUI.getUserChordDurations();
			return chordSpacingDurations;
		}
	}

	private Rectangle getRectFromPoint(Point p) {
		return new Rectangle(Math.min(p.x, dragX), Math.min(p.y, dragY), Math.abs(p.x - dragX),
				Math.abs(p.y - dragY));
	}

	private boolean draggingAny(DM singleValue) {
		return dragMode.contains(singleValue);
	}

	private boolean draggingAny(DM... values) {
		for (DM val : values) {
			if (dragMode.contains(val)) {
				return true;
			}
		}
		return false;
	}

	private int getChordIndexByStartTime(List<Double> chordLengths, double noteStartTime) {
		double chordLengthSum = 0;
		for (int i = 0; i < chordLengths.size(); i++) {
			chordLengthSum += chordLengths.get(i);
			if (noteStartTime < chordLengthSum) {
				return i;
			}
		}
		return chordLengths.size() - 1;
	}

	public static List<Integer> calculateHighlightedScaleKey() {
		return MidiUtils.MAJ_SCALE;
	}

	public static Map<Integer, Set<Integer>> calculateHighlightedChords(Section sec) {
		if (MidiGenerator.chordInts.isEmpty()) {
			return null;
		}

		if (sec == null || !sec.isCustomChordsEnabled()) {
			return MidiUtils.getHighlightTargetsFromChords(MidiGenerator.chordInts, false);
		} else {
			return MidiUtils.getHighlightTargetsFromChords(sec.getCustomChordsList(), false);
		}
	}

	public PhraseNotes getValues() {
		return values;
	}

	public void setValues(PhraseNotes vals) {
		values = vals;
	}

	protected double getQuarterNoteLength() {
		return (getWidth() - marginX) / (sectionLength + getPhraseMarginX() * 2);
	}

	protected double getPhraseMarginX() {
		return (MidiEditPopup.displayingPhraseMarginX ? phraseMarginX : 0);
	}

	protected PhraseNote getDraggedNote(Point xy) {
		int yValue = getPitchFromPosition(xy.y);
		List<PhraseNote> possibleNotes = values.stream().filter(e -> yValue == e.getPitch())
				.collect(Collectors.toList());
		if (possibleNotes.isEmpty()) {
			return null;
		}
		values.remakeNoteStartTimes();
		double quarterNoteLength = getQuarterNoteLength();
		double noteDragMarginTime = noteDragMarginX / quarterNoteLength;
		double mouseClickTime = ((xy.x - marginX) / quarterNoteLength) - getPhraseMarginX();
		Integer index = getNoteByTime(possibleNotes, noteDragMarginTime, mouseClickTime);
		if (index != null) {
			return possibleNotes.get(index);
		}
		return null;
	}


	public static Integer getNoteByTime(List<PhraseNote> notes, double noteDragMarginTime,
			double time) {
		for (int i = 0; i < notes.size(); i++) {
			PhraseNote pn = notes.get(i);
			if (time > (pn.getStartTime() - noteDragMarginTime)
					&& time < (pn.getStartTime() + pn.getDuration() + noteDragMarginTime)) {
				return i;
			}
		}

		return null;
	}

	private int getPitchFromPosition(int y) {
		int rowDivisors = currentMax - currentMin;
		int usableHeight = getHeight() - marginY * 2;
		double rowHeight = usableHeight / (double) rowDivisors;

		Point bottomLeftAdjusted = new Point(marginX,
				usableHeight + marginY - (int) (rowHeight / 2));

		int yValue = (int) ((bottomLeftAdjusted.y - y) / rowHeight) + currentMin;
		return yValue;

	}

	private int getVelocityFromPosition(Point xy) {
		if (draggedNote == null) {
			return 0;
		} else if (dragY == null) {
			return draggedNoteCopy.getDynamic();
		}
		int yDiff = dragY - xy.y;
		return draggedNoteCopy.getDynamic() + yDiff / 5;

	}

	private double getDurationFromPosition(Point xy) {
		if (draggedNote == null || dragX == null) {
			return 0;
		}
		values.remakeNoteStartTimes();
		double startTime = draggedNote.getStartTime();
		double quarterNoteLength = getQuarterNoteLength();

		double durationTime = (xy.x - marginX) / quarterNoteLength;
		double mouseCorrectionTime = (dragX - marginX) / quarterNoteLength;

		return draggedNoteCopy.getDuration() + durationTime - mouseCorrectionTime;
	}

	private double getOffsetFromPosition(Point xy, PhraseNote dragNote) {
		if (dragNote == null || dragX == null) {
			return 0;
		}
		values.remakeNoteStartTimes();
		double startTime = dragNote.getStartTime();
		double quarterNoteLength = getQuarterNoteLength();

		double offsetTime = (xy.x - marginX) / quarterNoteLength;
		double mouseCorrectionTime = (dragX - marginX) / quarterNoteLength;

		return draggedNoteCopy.getOffset() + offsetTime - mouseCorrectionTime;
	}

	private double getTimeFromPosition(Point xy) {
		return getTimeFromPosition(xy.x);
	}

	private double getTimeFromPosition(int x) {
		double quarterNoteLength = getQuarterNoteLength();
		return ((x - marginX) / quarterNoteLength) - getPhraseMarginX();
	}

	private int getPositionFromTime(double time) {
		double quarterNoteLength = getQuarterNoteLength();
		return marginX + (int) (quarterNoteLength * (time + getPhraseMarginX()));
	}

	public Point getOrderAndValueFromPosition(Point xy) {
		return getOrderAndValueFromPosition(xy, true, false);
	}

	public Point getOrderAndValueFromPosition(Point xy, boolean offsetted,
			boolean getClosestOriginal) {
		int w = getWidth();
		int h = getHeight();
		int rowDivisors = currentMax - currentMin;
		int usableHeight = h - marginY * 2;
		double rowHeight = usableHeight / (double) rowDivisors;

		Point bottomLeftAdjusted = new Point(marginX,
				usableHeight + marginY - (int) (rowHeight / 2));

		values.remakeNoteStartTimes();
		double quarterNoteLength = getQuarterNoteLength();

		int yValue = (int) ((bottomLeftAdjusted.y - xy.y) / rowHeight) + currentMin;

		double searchTime = ((xy.x - bottomLeftAdjusted.x) / quarterNoteLength)
				- getPhraseMarginX();
		//LG.d(searchX);
		Integer foundX = searchTime < DBL_ERR ? 0 : null;
		if (foundX == null) {
			List<Integer> possibleNotes = new ArrayList<>();
			if (getClosestOriginal) {
				for (int i = 0; i < values.size(); i++) {
					double start = values.get(i).getStart(offsetted);
					double end = i < values.size() - 1 ? values.get(i + 1).getStart(offsetted)
							: sectionLength;
					if (start < searchTime && searchTime < end) {
						possibleNotes.add(i);
						break;
					}
				}
			} else {
				for (int i = 0; i < values.size(); i++) {
					if (searchTime + DBL_ERR > values.get(i).getStart(offsetted)
							&& searchTime - DBL_ERR < values.get(i)
									.getStart(offsetted) + values.get(i).getDuration()) {
						possibleNotes.add(i);
					}
				}
			}


			if (possibleNotes.isEmpty()) {
				return null;
			}


			int difference = Integer.MAX_VALUE;
			for (int i = 0; i < possibleNotes.size(); i++) {
				int newDiff = Math.abs(values.get(possibleNotes.get(i)).getPitch() - yValue);
				if (newDiff < difference) {
					difference = newDiff;
					foundX = possibleNotes.get(i);
				}
			}

		}

		int xValue = foundX;

		xValue = OMNI.clamp(xValue, 0, values.size() - 1);
		yValue = OMNI.clamp(yValue, currentMin, currentMax);

		Point orderValue = new Point(xValue, yValue);
		//LG.d("Incoming point: " + xy.toString());
		//LG.d("Order Value: " + orderValue.toString());

		return orderValue;
	}

	public MidiEditPopup getPop() {
		return pop;
	}

	public void setPop(MidiEditPopup pop) {
		this.pop = pop;
	}

	public void setCurrentMin(int currentMin) {
		this.currentMin = Math.max(rangeMin, currentMin);
	}

	public void setCurrentMax(int currentMax) {
		this.currentMax = Math.min(rangeMax, currentMax);
	}

	public void setRange(int min, int max) {
		rangeMin = min;
		rangeMax = max;
		setCurrentMin(currentMin);
		setCurrentMax(currentMax);
	}

}
