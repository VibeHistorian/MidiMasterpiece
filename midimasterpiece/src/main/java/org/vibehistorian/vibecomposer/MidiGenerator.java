/* --------------------
* @author Vibe Historian
* ---------------------

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or any
later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/

package org.vibehistorian.vibecomposer;

import static org.vibehistorian.vibecomposer.MidiUtils.applyChordFreqMap;
import static org.vibehistorian.vibecomposer.MidiUtils.cIonianScale4;
import static org.vibehistorian.vibecomposer.MidiUtils.convertChordToLength;
import static org.vibehistorian.vibecomposer.MidiUtils.cpRulesMap;
import static org.vibehistorian.vibecomposer.MidiUtils.getBasicChordsFromRoots;
import static org.vibehistorian.vibecomposer.MidiUtils.maX;
import static org.vibehistorian.vibecomposer.MidiUtils.mappedChord;
import static org.vibehistorian.vibecomposer.MidiUtils.pickDurationWeightedRandom;
import static org.vibehistorian.vibecomposer.MidiUtils.squishChordProgression;
import static org.vibehistorian.vibecomposer.MidiUtils.transposeScale;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.vibehistorian.vibecomposer.MidiUtils.ScaleMode;
import org.vibehistorian.vibecomposer.Enums.ArpPattern;
import org.vibehistorian.vibecomposer.Enums.KeyChangeType;
import org.vibehistorian.vibecomposer.Enums.RhythmPattern;
import org.vibehistorian.vibecomposer.Panels.ArpGenSettings;
import org.vibehistorian.vibecomposer.Panels.DrumGenSettings;
import org.vibehistorian.vibecomposer.Panels.InstPanel;
import org.vibehistorian.vibecomposer.Parts.ArpPart;
import org.vibehistorian.vibecomposer.Parts.BassPart;
import org.vibehistorian.vibecomposer.Parts.ChordPart;
import org.vibehistorian.vibecomposer.Parts.DrumPart;
import org.vibehistorian.vibecomposer.Parts.InstPart;
import org.vibehistorian.vibecomposer.Parts.MelodyPart;
import org.vibehistorian.vibecomposer.Popups.VariationPopup;

import jm.JMC;
import jm.gui.show.ShowScore;
import jm.music.data.CPhrase;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Score;
import jm.music.tools.Mod;
import jm.util.Write;

public class MidiGenerator implements JMC {

	public static double noteMultiplier = 2.0;

	public static class Durations {

		public static double NOTE_32ND = 0.125 * noteMultiplier;
		public static double NOTE_DOTTED_32ND = 0.1875 * noteMultiplier;
		public static double SIXTEENTH_NOTE = 0.25 * noteMultiplier;
		public static double DOTTED_SIXTEENTH_NOTE = 0.375 * noteMultiplier;
		public static double EIGHTH_NOTE = 0.5 * noteMultiplier;
		public static double DOTTED_EIGHTH_NOTE = 0.75 * noteMultiplier;
		public static double QUARTER_NOTE = 1.0 * noteMultiplier;
		public static double DOTTED_QUARTER_NOTE = 1.5 * noteMultiplier;
		public static double HALF_NOTE = 2.0 * noteMultiplier;
		public static double DOTTED_HALF_NOTE = 3.0 * noteMultiplier;
		public static double WHOLE_NOTE = 4.0 * noteMultiplier;

	}

	public static void recalculateDurations() {
		Durations.NOTE_32ND = 0.125 * noteMultiplier;
		Durations.NOTE_DOTTED_32ND = 0.1875 * noteMultiplier;
		Durations.SIXTEENTH_NOTE = 0.25 * noteMultiplier;
		Durations.DOTTED_SIXTEENTH_NOTE = 0.375 * noteMultiplier;
		Durations.EIGHTH_NOTE = 0.5 * noteMultiplier;
		Durations.DOTTED_EIGHTH_NOTE = 0.75 * noteMultiplier;
		Durations.QUARTER_NOTE = 1.0 * noteMultiplier;
		Durations.DOTTED_QUARTER_NOTE = 1.5 * noteMultiplier;
		Durations.HALF_NOTE = 2.0 * noteMultiplier;
		Durations.DOTTED_HALF_NOTE = 3.0 * noteMultiplier;
		Durations.WHOLE_NOTE = 4.0 * noteMultiplier;

		START_TIME_DELAY = Durations.EIGHTH_NOTE;
		MELODY_DUR_ARRAY = new double[] { Durations.QUARTER_NOTE, Durations.DOTTED_EIGHTH_NOTE,
				Durations.EIGHTH_NOTE, Durations.SIXTEENTH_NOTE };
		CHORD_DUR_ARRAY = new double[] { Durations.WHOLE_NOTE, Durations.DOTTED_HALF_NOTE,
				Durations.HALF_NOTE, Durations.QUARTER_NOTE };
	}

	public static final double[] SECOND_ARRAY_STRUM = { 0, 0.016666, 0.03125, 0.0625, 0.0625, 0.125,
			0.16666667, 0.250, 0.333333, 0.50000, 0.750, 1.000 };

	private static final boolean debugEnabled = true;
	private static final PrintStream originalStream = System.out;

	// big G
	public static GUIConfig gc;

	// opened windows
	public static List<ShowScore> showScores = new ArrayList<>();
	public static int windowLoc = 5;

	// track map for Solo
	public static List<InstPart> trackList = new ArrayList<>();

	// constants
	public static final boolean MAXIMIZE_CHORUS_MAIN_MELODY = false;
	public static final int MAXIMUM_PATTERN_LENGTH = 8;
	public static final int OPENHAT_CHANCE = 0;
	private static final int maxAllowedScaleNotes = 7;
	public static double START_TIME_DELAY = Durations.EIGHTH_NOTE;
	private static final double DEFAULT_CHORD_SPLIT = 625;
	private static final String ARP_PATTERN_KEY = "ARP_PATTERN";
	private static final String ARP_OCTAVE_KEY = "ARP_OCTAVE";
	private static final String ARP_PAUSES_KEY = "ARP_PAUSES";

	// visibles/settables
	public static DrumGenSettings DRUM_SETTINGS = new DrumGenSettings();
	public static ArpGenSettings ARP_SETTINGS = new ArpGenSettings();

	public static List<String> userChords = new ArrayList<>();
	public static List<Double> userChordsDurations = new ArrayList<>();
	public static Phrase userMelody = null;
	public static List<String> chordInts = new ArrayList<>();
	public static double GENERATED_MEASURE_LENGTH = 0;

	public static String FIRST_CHORD = null;
	public static String LAST_CHORD = null;

	public static boolean DISPLAY_SCORE = false;
	public static int showScoreMode = 0;

	public static boolean COLLAPSE_DRUM_TRACKS = true;
	public static boolean COLLAPSE_MELODY_TRACKS = true;


	// for internal use only
	private static double[] MELODY_DUR_ARRAY = { Durations.QUARTER_NOTE,
			Durations.DOTTED_EIGHTH_NOTE, Durations.EIGHTH_NOTE, Durations.SIXTEENTH_NOTE };
	private double[] MELODY_DUR_CHANCE = { 0.3, 0.6, 1.0, 1.0 };

	private static double[] CHORD_DUR_ARRAY = { Durations.WHOLE_NOTE, Durations.DOTTED_HALF_NOTE,
			Durations.HALF_NOTE, Durations.QUARTER_NOTE };
	private double[] CHORD_DUR_CHANCE = { 0.0, 0.20, 0.80, 1.0 };
	private static Map<Integer, Integer> customDrumMappingNumbers = null;

	private List<Integer> MELODY_SCALE = cIonianScale4;
	private List<Double> progressionDurations = new ArrayList<>();
	private List<int[]> chordProgression = new ArrayList<>();
	private List<int[]> rootProgression = new ArrayList<>();

	public static Map<Integer, List<Note>> userMelodyMap = new HashMap<>();
	private Map<Integer, List<Note>> chordMelodyMap1 = new HashMap<>();
	private List<int[]> melodyBasedChordProgression = new ArrayList<>();
	private List<int[]> melodyBasedRootProgression = new ArrayList<>();

	// global parts
	private List<BassPart> bassParts = null;
	private List<MelodyPart> melodyParts = null;
	private List<ChordPart> chordParts = null;
	private List<DrumPart> drumParts = null;
	private List<ArpPart> arpParts = null;

	public List<? extends InstPart> getInstPartList(int order) {
		if (order < 0 || order > 4) {
			throw new IllegalArgumentException("Inst part list order wrong.");
		}
		switch (order) {
		case 0:
			return melodyParts;
		case 1:
			return bassParts;
		case 2:
			return chordParts;
		case 3:
			return arpParts;
		case 4:
			return drumParts;
		}
		return null;
	}

	public static List<Integer> melodyNotePattern = null;

	private int samePitchCount = 0;
	private int previousPitch = 0;

	private int modTrans = 0;

	public MidiGenerator(GUIConfig gc) {
		MidiGenerator.gc = gc;
	}

	private List<Integer> patternFromNotes(Collection<Note> notes) {
		// strategy: use 64 hits in pattern, then simplify if needed


		int chordsTotal = chordInts.size();
		int hits = chordsTotal * 16;
		double measureTotal = chordsTotal
				* ((gc.isDoubledDurations()) ? Durations.WHOLE_NOTE : Durations.HALF_NOTE);
		double timeForHit = measureTotal / hits;
		List<Integer> pattern = new ArrayList<>();
		List<Double> durationBuckets = new ArrayList<>();
		for (int i = 1; i <= hits; i++) {
			durationBuckets.add(timeForHit * i - 0.01);
			pattern.add(0);
		}
		pattern.set(0, 1);
		double currentDuration = 0;
		int explored = 0;
		for (Note n : notes) {
			/*System.out.println(
					"Current dur: " + currentDuration + ", + rhythm: " + n.getRhythmValue());*/
			currentDuration += n.getRhythmValue();
			for (int i = explored; i < hits; i++) {
				if (currentDuration < durationBuckets.get(i)) {
					pattern.set(i, 1);
					explored = i;
					break;
				}
			}
		}
		if (gc.isMelodyPatternFlip()) {
			for (int i = 0; i < pattern.size(); i++) {
				pattern.set(i, 1 - pattern.get(i));
			}
		}
		//System.out.println(StringUtils.join(pattern, ", "));
		return pattern;
	}

	private int selectClosestIndexFromChord(int[] chord, int previousNotePitch,
			boolean directionUp) {
		if (directionUp) {
			for (int i = 0; i < chord.length; i++) {
				if (previousNotePitch < chord[i]) {
					return i;
				}
			}
			return chord.length - 1;
		} else {
			for (int i = chord.length - 1; i > 0; i--) {
				if (previousNotePitch > chord[i]) {
					return i;
				}
			}
			return 0;
		}

	}

	private int pickRandomBetweenIndexesInclusive(int[] chord, int startIndex, int endIndex,
			Random generator, double posInChord) {
		//clamp
		if (startIndex < 0)
			startIndex = 0;
		if (endIndex > chord.length - 1) {
			endIndex = chord.length - 1;
		}
		if (((chord[startIndex] % 12 == 11) && (chord[endIndex] % 12 == 11)) || posInChord > 0.66) {
			// do nothing
			//System.out.println("The forced B case, " + posInChord);
		} else if (chord[startIndex] % 12 == 11) {
			startIndex++;
			//System.out.println("B start avoided");
		} else if (chord[endIndex] % 12 == 11) {
			endIndex--;
			//System.out.println("B end avoided");
		}
		int index = generator.nextInt(endIndex - startIndex + 1) + startIndex;
		return chord[index];
	}


	private List<Boolean> generateMelodyDirectionsFromChordProgression(List<int[]> progression,
			boolean roots) {

		List<Boolean> ascDirectionList = new ArrayList<>();

		for (int i = 0; i < progression.size(); i++) {
			if (roots) {
				int current = progression.get(i)[0];
				int next = progression.get((i + 1) % progression.size())[0];
				ascDirectionList.add(Boolean.valueOf(current <= next));
			} else {
				int current = progression.get(i)[progression.get(i).length - 1];
				int next = progression.get((i + 1)
						% progression.size())[progression.get((i + 1) % progression.size()).length
								- 1];
				ascDirectionList.add(Boolean.valueOf(current <= next));
			}

		}

		return ascDirectionList;
	}

	private static List<Double> generateMelodyDirectionChordDividers(int chords, Random dirGen) {
		List<Double> map = new ArrayList<>();
		for (int i = 0; i < chords; i++) {
			double divider = dirGen.nextDouble() * 0.80 + 0.20;
			map.add(divider);

		}
		return map;
	}

	private Vector<Note> generateMelodySkeletonFromChords(MelodyPart mp, List<int[]> chords,
			List<int[]> roots, int measures, int notesSeedOffset, Section sec,
			List<Integer> variations) {

		boolean genVars = variations == null;

		boolean fillChordMelodyMap = false;
		if (chordMelodyMap1.isEmpty() && notesSeedOffset == 0
				&& (roots.size() == chordInts.size())) {
			fillChordMelodyMap = true;
		}

		int MAX_JUMP_SKELETON_CHORD = gc.getMaxNoteJump();
		int SAME_RHYTHM_CHANCE = gc.getMelodySameRhythmChance();
		int ALTERNATE_RHYTHM_CHANCE = gc.getMelodyAlternateRhythmChance();
		int EXCEPTION_CHANCE = gc.getMelodyExceptionChance();

		int seed = mp.getPatternSeed();

		Vector<Note> noteList = new Vector<>();

		Random algoGenerator = new Random(gc.getRandomSeed());
		if (algoGenerator.nextInt(100) < gc.getMelodyUseOldAlgoChance()) {
			return oldAlgoGenerateMelodySkeletonFromChords(mp, measures, roots);
		}

		Random generator = new Random(seed + notesSeedOffset);
		Random exceptionGenerator = new Random(seed + 2 + notesSeedOffset);
		Random sameRhythmGenerator = new Random(seed + 3);
		Random alternateRhythmGenerator = new Random(seed + 4);
		Random variationGenerator = new Random(seed + notesSeedOffset);
		Random durationGenerator = new Random(seed + notesSeedOffset + 5);
		Random directionGenerator = new Random(seed + 10);
		Random surpriseGenerator = new Random(seed + notesSeedOffset + 15);
		int numberOfVars = Section.variationDescriptions[0].length - 2;

		double[] melodySkeletonDurations = { Durations.NOTE_32ND, Durations.SIXTEENTH_NOTE,
				Durations.EIGHTH_NOTE, Durations.DOTTED_EIGHTH_NOTE, Durations.QUARTER_NOTE };

		int weight3rd = gc.getMelodyQuickness() / 3;
		// 0% ->
		// 0, 0, 0, 40, 80, 100
		// 50% ->
		// 5 11 16 51 85 100
		// 100% ->
		// 11 22 33 72 91 100
		int[] melodySkeletonDurationWeights = { 0 + weight3rd / 3, 0 + weight3rd,
				40 + weight3rd * 2 / 3, 80 + weight3rd / 3, 100 };

		List<int[]> usedChords = null;
		if (gc.isMelodyBasicChordsOnly()) {
			List<int[]> basicChordsUnsquished = getBasicChordsFromRoots(roots);
			for (int i = 0; i < chords.size(); i++) {
				basicChordsUnsquished.set(i, convertChordToLength(basicChordsUnsquished.get(i),
						chords.get(i).length, true));
			}

			/* 
			 * if...
			 * usedChords = squishChordProgression(basicChordsUnsquished, gc.isSpiceFlattenBigChords(),
					gc.getRandomSeed(), gc.getChordGenSettings().getFlattenVoicingChance());
			 * */
			usedChords = basicChordsUnsquished;

		} else {
			usedChords = chords;
		}

		List<int[]> stretchedChords = usedChords.stream().map(e -> convertChordToLength(e, 4, true))
				.collect(Collectors.toList());
		List<Double> directionChordDividers = (!gc.isMelodyUseDirectionsFromProgression())
				? generateMelodyDirectionChordDividers(stretchedChords.size(), directionGenerator)
				: null;
		directionGenerator.setSeed(seed + 10);
		boolean currentDirection = directionGenerator.nextBoolean();
		if (!gc.isMelodyUseDirectionsFromProgression()) {
			System.out.println("Direction dividers: " + directionChordDividers.toString()
					+ ", start at: " + currentDirection);
		}

		List<Boolean> directionsFromChords = (gc.isMelodyUseDirectionsFromProgression())
				? generateMelodyDirectionsFromChordProgression(usedChords, true)
				: null;

		boolean alternateRhythm = alternateRhythmGenerator.nextInt(100) < ALTERNATE_RHYTHM_CHANCE;
		//System.out.println("Alt: " + alternateRhythm);

		for (int o = 0; o < measures; o++) {
			int previousNotePitch = 0;
			int firstPitchInTwoChords = 0;

			for (int i = 0; i < stretchedChords.size(); i++) {
				// either after first measure, or after first half of combined chord prog

				if (genVars && (i == 0)) {
					variations = fillVariations(sec, variationGenerator, variations, numberOfVars,
							0);
				}

				if ((variations != null) && (i == 0)) {
					for (Integer var : variations) {
						if (o == measures - 1) {
							System.out.println("Melody variation: " + var);
						}

						switch (var) {
						case 0:
							// only add, processed later
							break;
						case 1:
							MAX_JUMP_SKELETON_CHORD = Math.min(4, MAX_JUMP_SKELETON_CHORD + 1);
							break;
						default:
							throw new IllegalArgumentException("Too much variation!");
						}
					}
				}

				if (fillChordMelodyMap && o == 0) {
					if (!chordMelodyMap1.containsKey(Integer.valueOf(i))) {
						chordMelodyMap1.put(Integer.valueOf(i), new ArrayList<>());
					}
				}
				if (i % 2 == 0) {
					previousNotePitch = firstPitchInTwoChords;
					generator.setSeed(seed + notesSeedOffset);
					exceptionGenerator.setSeed(seed + 2 + notesSeedOffset);
					if (alternateRhythm) {
						sameRhythmGenerator.setSeed(seed + 3);
					}
				}

				boolean sameRhythmTwice = sameRhythmGenerator.nextInt(100) < SAME_RHYTHM_CHANCE;

				double rhythmDuration = sameRhythmTwice ? progressionDurations.get(i) / 2.0
						: progressionDurations.get(i);
				int rhythmSeed = (alternateRhythm && i % 2 == 1) ? seed + 1 : seed;
				Rhythm rhythm = new Rhythm(rhythmSeed, rhythmDuration, melodySkeletonDurations,
						melodySkeletonDurationWeights);

				List<Double> durations = rhythm.regenerateDurations(sameRhythmTwice ? 1 : 2);
				if (gc.isMelodyArpySurprises()) {
					if (sameRhythmTwice) {
						if ((i % 2 == 0) || (durations.size() < 3)) {
							durations.addAll(durations);
						} else {
							List<Double> arpedDurations = makeSurpriseTrioArpedDurations(durations);
							if (arpedDurations != null) {
								System.out.println("Double pattern - surprise!");
								durations.addAll(arpedDurations);
							} else {
								durations.addAll(durations);
							}
						}
					} else if (i % 2 == 1 && durations.size() >= 4) {

						List<Double> arpedDurations = makeSurpriseTrioArpedDurations(durations);
						if (arpedDurations != null) {
							System.out.println("Single pattern - surprise!");
							durations = arpedDurations;
						}
					}
				} else {
					if (sameRhythmTwice) {
						durations.addAll(durations);
					}
				}


				int[] chord = stretchedChords.get(i);
				int exceptionCounter = gc.getMaxExceptions();
				boolean allowException = true;
				double durCounter = 0.0;
				boolean changedDirectionByDivider = false;
				if (gc.isMelodyUseDirectionsFromProgression()) {
					currentDirection = directionsFromChords.get(i);
				}
				for (int j = 0; j < durations.size(); j++) {
					boolean tempChangedDir = false;
					int tempSaveMaxJump = MAX_JUMP_SKELETON_CHORD;
					if (allowException && j > 0 && exceptionCounter > 0
							&& exceptionGenerator.nextInt(100) < EXCEPTION_CHANCE) {
						currentDirection = !currentDirection;
						if (gc.isMelodySingleNoteExceptions()) {
							tempChangedDir = true;
							MAX_JUMP_SKELETON_CHORD = Math.max(0, MAX_JUMP_SKELETON_CHORD - 1);
						}

						exceptionCounter--;
					}
					int pitch = 0;
					int startIndex = 0;
					int endIndex = chord.length - 1;

					if (previousNotePitch != 0) {
						// up, or down
						if (currentDirection) {
							startIndex = selectClosestIndexFromChord(chord, previousNotePitch,
									true);
							while (endIndex - startIndex > MAX_JUMP_SKELETON_CHORD) {
								endIndex--;
							}
						} else {
							endIndex = selectClosestIndexFromChord(chord, previousNotePitch, false);
							while (endIndex - startIndex > MAX_JUMP_SKELETON_CHORD) {
								startIndex++;
							}
						}
					}
					double positionInChord = durCounter / progressionDurations.get(i);
					pitch = pickRandomBetweenIndexesInclusive(chord, startIndex, endIndex,
							generator, positionInChord);

					double swingDuration = durations.get(j);
					Note n = new Note(pitch, swingDuration, 100);
					n.setDuration(swingDuration * (0.75 + durationGenerator.nextDouble() / 4)
							* Note.DEFAULT_DURATION_MULTIPLIER);

					if (tempChangedDir && gc.isMelodySingleNoteExceptions()) {
						currentDirection = !currentDirection;
						MAX_JUMP_SKELETON_CHORD = tempSaveMaxJump;
					}
					if (!gc.isMelodySingleNoteExceptions()) {
						if (previousNotePitch == pitch) {
							currentDirection = !currentDirection;
							allowException = false;
						} else {
							allowException = true;
						}
					}

					if (i % 2 == 0 && j == 0 && gc.isMelodyAvoidChordJumps()) {
						firstPitchInTwoChords = pitch;
					}
					previousNotePitch = pitch;
					noteList.add(n);
					if (fillChordMelodyMap && o == 0) {
						chordMelodyMap1.get(Integer.valueOf(i)).add(n);
					}
					durCounter += swingDuration;
					if (!gc.isMelodyUseDirectionsFromProgression() && !changedDirectionByDivider
							&& durCounter > directionChordDividers.get(i)) {
						changedDirectionByDivider = true;
						currentDirection = !currentDirection;
					}
				}
				if (!gc.isMelodyUseDirectionsFromProgression() && !changedDirectionByDivider) {
					currentDirection = !currentDirection;
				}

			}
		}

		if (fillChordMelodyMap) {
			makeMelodyPitchFrequencyMap(1, chordMelodyMap1.keySet().size() - 1, 2);
		}
		if (genVars && variations != null) {
			sec.setVariation(0, getAbsoluteOrder(0, mp), variations);
		}
		return noteList;
	}

	private List<Double> makeSurpriseTrioArpedDurations(List<Double> durations) {

		List<Double> arpedDurations = new ArrayList<>(durations);
		for (int trioIndex = 0; trioIndex < arpedDurations.size() - 2; trioIndex++) {
			double sumThirds = arpedDurations.subList(trioIndex, trioIndex + 3).stream()
					.mapToDouble(e -> e).sum();
			boolean valid = false;
			if (isDottedNote(sumThirds)) {
				sumThirds /= 3.0;
				for (int trio = trioIndex; trio < trioIndex + 3; trio++) {
					arpedDurations.set(trio, sumThirds);
				}
				valid = true;
			} else if (isMultiple(sumThirds, Durations.QUARTER_NOTE)) {
				if (sumThirds > Durations.DOTTED_QUARTER_NOTE) {
					sumThirds /= 4.0;
					for (int trio = trioIndex; trio < trioIndex + 3; trio++) {
						arpedDurations.set(trio, sumThirds);
					}
					arpedDurations.add(trioIndex, sumThirds);
				} else {
					sumThirds /= 2.0;
					for (int trio = trioIndex + 1; trio < trioIndex + 3; trio++) {
						arpedDurations.set(trio, sumThirds);
					}
					arpedDurations.remove(trioIndex);
				}
				valid = true;
			}

			if (valid) {
				return arpedDurations;
			}

		}
		return null;
	}

	public static boolean isDottedNote(double note) {
		if (roughlyEqual(Durations.DOTTED_EIGHTH_NOTE, note))
			return true;
		if (roughlyEqual(Durations.DOTTED_HALF_NOTE, note))
			return true;
		if (roughlyEqual(Durations.DOTTED_QUARTER_NOTE, note))
			return true;
		if (roughlyEqual(Durations.DOTTED_SIXTEENTH_NOTE, note))
			return true;
		if (roughlyEqual(Durations.NOTE_DOTTED_32ND, note))
			return true;
		return false;
	}

	public static boolean isMultiple(double first, double second) {
		double result = first / second;
		double rounded = Math.round(result);
		if (roughlyEqual(result, rounded)) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean roughlyEqual(double first, double second) {
		return Math.abs(first - second) < 0.001;
	}

	private int getAllowedPitchFromRange(int min, int max, double posInChord, Random splitNoteGen) {

		boolean allowBs = posInChord > 0.66;

		List<Integer> allowedPitches = MELODY_SCALE;
		int adjustment = 0;
		while (max < allowedPitches.get(0)) {
			min += 12;
			max += 12;
			adjustment -= 12;
		}
		while (min > allowedPitches.get(allowedPitches.size() - 1)) {
			min -= 12;
			max -= 12;
			adjustment += 12;
		}
		for (int i = 0; i < allowedPitches.size(); i++) {
			int pitch = allowedPitches.get(i);

			// skip Bs if possible
			if (pitch == 11 && !allowBs && fits(12, min, max, false)) {
				continue;
			}
			// accept if: fits and (either nothing else fits or also accept with 50% chance)
			if (fits(pitch, min, max, false)
					&& ((splitNoteGen.nextBoolean()) || i == allowedPitches.size() - 1
							|| !fits(allowedPitches.get(i + 1), min, max, false))) {
				return pitch + adjustment;
			}
		}

		for (int i = 0; i < allowedPitches.size(); i++) {
			int pitch = allowedPitches.get(i);
			// skip Bs if possible
			if (pitch == 11 && !allowBs && fits(12, min, max, false)) {
				continue;
			}

			// accept if: fits and (either nothing else fits or also accept with 50% chance)
			if (fits(pitch, min, max, true)
					&& ((splitNoteGen.nextBoolean()) || i == allowedPitches.size() - 1
							|| !fits(allowedPitches.get(i + 1), min, max, true))) {
				return pitch + adjustment;
			}
		}
		/*
				for (Integer i : allowedPitches) {
					if (i > min && i < max) {
						return i + adjustment;
					}
				}
				for (Integer i : allowedPitches) {
					if (i >= min && i <= max) {
						return i + adjustment;
					}
				}*/
		return 40;
	}

	private boolean fits(int pitch, int min, int max, boolean isInclusive) {
		if (isInclusive) {
			if (pitch >= min && pitch <= max) {
				return true;
			} else {
				return false;
			}
		} else {
			if (pitch > min && pitch < max) {
				return true;
			} else {
				return false;
			}
		}
	}

	private Vector<Note> convertMelodySkeletonToFullMelody(MelodyPart mp, List<Double> durations,
			int measures, Section sec, Vector<Note> skeleton, int notesSeedOffset) {

		int RANDOM_SPLIT_NOTE_PITCH_EXCEPTION_RANGE = 4;

		List<Integer> melodyVars = sec.getVariation(0, getAbsoluteOrder(0, mp));

		int seed = mp.getPatternSeed() + mp.getOrder();
		Random splitGenerator = new Random(seed + 4);
		Random pauseGenerator = new Random(seed + 5);
		Random pauseGenerator2 = new Random(seed + 7);
		Random variationGenerator = new Random(seed + 6);
		Random velocityGenerator = new Random(seed + 1 + notesSeedOffset);
		Random splitNoteGenerator = new Random(seed + 8);
		Random splitNoteExceptionGenerator = new Random(seed + 9);
		Random chordLeadingGenerator = new Random(seed + notesSeedOffset + 15);


		int splitChance = gc.getMelodySplitChance();
		Vector<Note> fullMelody = new Vector<>();
		int chordCounter = 0;
		double durCounter = 0.0;
		double currentChordDur = durations.get(0);
		if (gc.isScaleMidiVelocityInArrangement()) {
		}

		int firstPitch = skeleton.get(0).getPitch();

		int volMultiplier = (gc.isScaleMidiVelocityInArrangement()) ? sec.getVol(0) : 100;
		int minVel = multiplyVelocity(mp.getVelocityMin(), volMultiplier, 0, 1);
		int maxVel = multiplyVelocity(mp.getVelocityMax(), volMultiplier, 1, 0);

		int[] pitches = new int[12];
		int[] chordSeparators = new int[durations.size() * measures + 1];
		int chordSepIndex = 0;
		chordSeparators[chordSepIndex++] = 0;

		for (int i = 0; i < skeleton.size(); i++) {
			Note n1 = skeleton.get(i);
			double adjDur = n1.getRhythmValue();
			if ((durCounter + adjDur) > (currentChordDur + 0.01)) {
				chordCounter = (chordCounter + 1) % durations.size();
				if (chordCounter == 0) {
					// when measure resets
					if (variationGenerator.nextInt(100) < gc.getArrangementPartVariationChance()) {
						splitChance = (int) (splitChance * 1.2);
					}
				}
				durCounter = 0.0;
				currentChordDur = durations.get(chordCounter);
				//splitGenerator.setSeed(seed + 4);
				pauseGenerator.setSeed(seed + 5);
				//pauseGenerator2.setSeed(seed + 7);
				splitNoteGenerator.setSeed(seed + 8);
				splitNoteExceptionGenerator.setSeed(seed + 9);
				chordSeparators[chordSepIndex++] = fullMelody.size() + 1;
			}

			int velocity = velocityGenerator.nextInt(maxVel - minVel) + minVel;

			int p = pauseGenerator.nextInt(100);
			int p2 = pauseGenerator2.nextInt(100);
			boolean pause1 = p < mp.getPauseChance();
			boolean pause2 = p2 < (mp.getPauseChance());


			n1.setDynamic(velocity);
			double positionInChord = durCounter / durations.get(chordCounter);

			durCounter += adjDur;

			boolean splitLastNoteInChord = (chordLeadingGenerator.nextInt(100) < gc
					.getMelodyLeadChords()) && (adjDur > Durations.NOTE_DOTTED_32ND * 1.1)
					&& (i < skeleton.size() - 1)
					&& ((durCounter + skeleton.get(i + 1).getRhythmValue()) > currentChordDur);


			if ((adjDur > Durations.SIXTEENTH_NOTE * 1.4
					&& splitGenerator.nextInt(100) < splitChance) || splitLastNoteInChord) {

				Note n2 = skeleton.get((i + 1) % skeleton.size());
				int pitch2 = 0;
				if (n1.getPitch() >= n2.getPitch()) {
					int higherNote = n1.getPitch();
					if (splitNoteExceptionGenerator.nextInt(100) < 33 && !splitLastNoteInChord) {
						higherNote += RANDOM_SPLIT_NOTE_PITCH_EXCEPTION_RANGE;
					}
					pitch2 = getAllowedPitchFromRange(n2.getPitch(), higherNote, positionInChord,
							splitNoteGenerator);
				} else {
					int lowerNote = n1.getPitch();
					if (splitNoteExceptionGenerator.nextInt(100) < 33 && !splitLastNoteInChord) {
						lowerNote -= RANDOM_SPLIT_NOTE_PITCH_EXCEPTION_RANGE;
					}
					pitch2 = getAllowedPitchFromRange(lowerNote, n2.getPitch(), positionInChord,
							splitNoteGenerator);
				}

				double multiplier = (isDottedNote(adjDur) && splitGenerator.nextBoolean())
						? (1.0 / 3.0)
						: 0.5;

				double swingDuration1 = adjDur * multiplier;
				double swingDuration2 = swingDuration1;
				int pitch1 = n1.getPitch();
				Note n1split1 = new Note(pitch1, swingDuration1, velocity);
				Note n1split2 = new Note(pitch2, swingDuration2, velocity - 10);


				fullMelody.add(
						pause1 ? new Note(Integer.MIN_VALUE, swingDuration1, velocity) : n1split1);
				fullMelody.add(
						pause2 ? new Note(Integer.MIN_VALUE, swingDuration2, velocity) : n1split2);


				if (!pause1) {
					pitches[pitch1 % 12]++;
				}
				if (!pause2) {
					pitches[pitch2 % 12]++;
				}

				if (multiplier < 0.4) {
					int pitch3 = (splitGenerator.nextBoolean()) ? pitch1 : pitch2;
					boolean pause3 = (splitGenerator.nextBoolean()) ? pause1 : pause2;
					;
					double swingDuration3 = swingDuration1;
					Note n1split3 = new Note(pitch3, swingDuration3, velocity - 20);
					fullMelody.add(pause3 ? new Note(Integer.MIN_VALUE, swingDuration3, velocity)
							: n1split3);
					if (!pause3) {
						pitches[pitch3 % 12]++;
					}
				}

			} else {
				int pitch = n1.getPitch();
				fullMelody.add(pause1 ? new Note(Integer.MIN_VALUE, adjDur, velocity) : n1);
				if (!pause1) {
					pitches[pitch % 12]++;
				}
			}
		}
		chordSeparators[chordSepIndex] = fullMelody.size();


		// fill pauses toggle
		if (mp.isFillPauses()) {
			if (fullMelody.get(0).getPitch() < 0) {
				fullMelody.get(0).setPitch(firstPitch);
			}

			Note fillPauseNote = fullMelody.get(0);
			double addedDuration = 0;
			for (int i = 1; i < fullMelody.size(); i++) {
				Note n = fullMelody.get(i);
				if (n.getPitch() < 0) {
					addedDuration += n.getRhythmValue();
				} else {
					fillPauseNote.setDuration(fillPauseNote.getDuration() + addedDuration);
					addedDuration = 0;
					fillPauseNote = n;
				}
			}
			if (addedDuration > 0.01) {
				fillPauseNote.setDuration(fillPauseNote.getDuration() + addedDuration);
			}
		}


		// for main sections: try to adjust notes towards C if there isn't enough C's
		if (gc.isMelodyTonicize() && notesSeedOffset == 0) {
			double requiredPercentageCs = 0.25;
			int needed = (int) Math.floor(fullMelody.size() * requiredPercentageCs);

			System.out.println("Found C's: " + pitches[0] + ", needed: " + needed);
			if (pitches[0] < needed) {

				int difference = needed - pitches[0];
				//System.out.println("Correcting melody!");
				int investigatedChordIndex = chordSeparators.length - 1;


				// adjust in pairs starting from last
				while (investigatedChordIndex > 0 && difference > 0) {
					int end = chordSeparators[investigatedChordIndex] - 1;
					int investigatedChordStart = chordSeparators[investigatedChordIndex - 1];
					for (int i = end; i >= investigatedChordStart; i--) {
						Note n = fullMelody.get(i);
						int p = n.getPitch();
						// D
						if (p % 12 == 2) {
							n.setPitch(p - 2);
							difference--;
							break;
						}
						// B
						if (p % 12 == 11) {
							n.setPitch(p + 1);
							difference--;
							break;
						}
					}
					investigatedChordIndex -= 2;
				}

				//System.out.println("Remaining difference after last pairs: " + difference);

				// adjust in pairs starting from last-1
				investigatedChordIndex = chordSeparators.length - 2;
				while (investigatedChordIndex > 0 && difference > 0) {
					int end = chordSeparators[investigatedChordIndex] - 1;
					int investigatedChordStart = chordSeparators[investigatedChordIndex - 1];
					for (int i = end; i >= investigatedChordStart; i--) {
						Note n = fullMelody.get(i);
						int p = n.getPitch();
						// D
						if (p % 12 == 2) {
							n.setPitch(p - 2);
							difference--;
							break;
						}
						// B
						if (p % 12 == 11) {
							n.setPitch(p + 1);
							difference--;
							break;
						}
					}
					investigatedChordIndex -= 2;
				}

				System.out.println("Remaining difference after first pairs: " + difference);

			}

		}

		// extraTranspose variation
		if (melodyVars != null && !melodyVars.isEmpty()
				&& melodyVars.contains(Integer.valueOf(0))) {
			for (int i = 0; i < fullMelody.size(); i++) {
				Note n = fullMelody.get(i);
				if (n.getPitch() >= 0) {
					n.setPitch(n.getPitch() + 12);
				}
			}
		}

		return fullMelody;
	}

	private void swingPhrase(Phrase phr, int swingPercent, double swingUnitOfTime) {
		if (swingPercent == 50) {
			return;
		}
		Vector<Note> fullMelody = phr.getNoteList();
		double currentChordDur = progressionDurations.get(0);
		int chordCounter = 0;

		boolean logSwing = false;

		int swingPercentAmount = swingPercent;
		double swingAdjust = swingUnitOfTime * (swingPercentAmount / ((double) 50.0))
				- swingUnitOfTime;
		double durCounter = 0.0;

		List<Double> durationBuckets = new ArrayList<>();
		List<Integer> chordSeparators = new ArrayList<>();
		for (int i = 0; i < fullMelody.size(); i++) {
			durCounter += fullMelody.get(i).getRhythmValue();
			durationBuckets.add(durCounter);
			if (durCounter + 0.001 > currentChordDur) {
				chordSeparators.add(i);
				chordCounter = (chordCounter + 1) % progressionDurations.size();
				currentChordDur = progressionDurations.get(chordCounter);
				durCounter = 0.0;
			}
			if (logSwing)
				System.out.println("Dur: " + durCounter + ", chord counter: " + chordCounter);
		}
		// fix short notes at the end not going to next chord
		if (durCounter > currentChordDur / 2) {
			chordCounter = (chordCounter + 1) % progressionDurations.size();
			chordSeparators.add(fullMelody.size() - 1);
			durCounter = 0.0;
			currentChordDur = progressionDurations.get(chordCounter);
		}
		int chordSepIndex = 0;
		Note swungNote = null;
		Note latestSuitableNote = null;
		durCounter = 0.0;
		for (int i = 0; i < fullMelody.size(); i++) {
			Note n = fullMelody.get(i);
			double adjDur = n.getRhythmValue();
			if (i > chordSeparators.get(chordSepIndex)) {
				chordSepIndex++;
				swingAdjust = swingUnitOfTime * (swingPercentAmount / ((double) 50.0))
						- swingUnitOfTime;
				durCounter = 0.0;
			}
			durCounter += adjDur;
			boolean processed = false;

			// try to find latest note which can be added/subtracted with swingAdjust
			if (swungNote == null) {
				if (adjDur - Math.abs(swingAdjust) > 0.01) {
					latestSuitableNote = n;
				}
				processed = true;
			} else {
				if ((adjDur - Math.abs(swingAdjust) > 0.01) && latestSuitableNote == null) {
					latestSuitableNote = n;
					processed = true;
				}
			}

			// apply swing to best note from previous section when landing on "exact" hits
			if (isMultiple(durCounter, swingUnitOfTime)) {

				if (logSwing)
					System.out.println(durCounter + " is Multiple of Unit");
				// nothing was caught in first half, SKIP swinging for this 2-unit bit of time
				if (swungNote == null && isMultiple(durCounter, 2 * swingUnitOfTime)) {
					swungNote = null;
					latestSuitableNote = null;
					if (logSwing)
						System.out.println("Can't swing this!");
				} else {
					if (latestSuitableNote != null) {
						double suitableDur = latestSuitableNote.getRhythmValue();
						if (swungNote == null) {
							latestSuitableNote.setRhythmValue(suitableDur + swingAdjust);
							latestSuitableNote.setDuration(
									(suitableDur + swingAdjust) * Note.DEFAULT_DURATION_MULTIPLIER);
							swingAdjust *= -1;
							swungNote = latestSuitableNote;
							latestSuitableNote = null;
							if (logSwing)
								System.out.println("Processed 1st swing!");
						} else {
							latestSuitableNote.setRhythmValue(suitableDur + swingAdjust);
							latestSuitableNote.setDuration(
									(suitableDur + swingAdjust) * Note.DEFAULT_DURATION_MULTIPLIER);
							swingAdjust *= -1;
							swungNote = null;
							latestSuitableNote = null;
							if (logSwing)
								System.out.println("Processed 2nd swing!");
						}
					} else {
						if (swungNote != null) {
							double swungDur = swungNote.getRhythmValue();
							swungNote.setRhythmValue(swungDur + swingAdjust);
							swungNote.setDuration(
									(swungDur + swingAdjust) * Note.DEFAULT_DURATION_MULTIPLIER);
							swingAdjust *= -1;
							swungNote = null;
							latestSuitableNote = null;
							if (logSwing)
								System.out.println("Unswung swung note!");
						}
					}
				}

			}

			// 
			if (!processed && !isMultiple(durCounter, 2 * swingUnitOfTime)) {
				if (swungNote != null) {
					if ((adjDur - Math.abs(swingAdjust) > 0.01) && latestSuitableNote == null) {
						latestSuitableNote = n;
					}
				}
			}
		}

		if (logSwing) {
			System.out.println("AFTER:");
			durCounter = 0.0;
			chordCounter = 0;
			durationBuckets = new ArrayList<>();
			for (int i = 0; i < fullMelody.size(); i++) {
				durCounter += fullMelody.get(i).getRhythmValue();
				if (durCounter + 0.001 > currentChordDur) {
					chordCounter = (chordCounter + 1) % progressionDurations.size();
					currentChordDur = progressionDurations.get(chordCounter);
					durCounter = 0.0;
				}
				System.out.println("Dur: " + durCounter + ", chord counter: " + chordCounter);
			}
		}
	}

	private List<String> makeMelodyPitchFrequencyMap(int start, int end, int orderOfMatch) {
		// only affect chords between start and end <start;end)
		List<int[]> alternateChordProg = new ArrayList<>();
		List<String> chordStrings = new ArrayList<>();
		String prevChordString = null;
		if (start > 0) {
			alternateChordProg
					.add(Arrays.copyOf(chordProgression.get(0), chordProgression.get(0).length));
			melodyBasedRootProgression
					.add(Arrays.copyOf(rootProgression.get(0), rootProgression.get(0).length));
			prevChordString = chordInts.get(start - 1);
		}

		for (int i = start; i < end; i++) {

			List<Integer> chordFreqs = new ArrayList<>();
			double totalDuration = 0;
			for (Note n : chordMelodyMap1.get(i)) {
				double dur = n.getRhythmValue();
				double durCounter = 0.0;
				int index = i;
				if (index >= progressionDurations.size()) {
					index = progressionDurations.size() - 1;
				}
				while (durCounter < dur && totalDuration < progressionDurations.get(index)) {
					chordFreqs.add(n.getPitch() % 12);
					durCounter += Durations.SIXTEENTH_NOTE;
					totalDuration += Durations.SIXTEENTH_NOTE;
				}
			}

			Map<Integer, Long> freqCounts = chordFreqs.stream()
					.collect(Collectors.groupingBy(e -> e, Collectors.counting()));

			Map<Integer, Long> top3 = freqCounts.entrySet().stream()
					.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(10)
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
							(e1, e2) -> e1, LinkedHashMap::new));

			//top3.entrySet().stream().forEach(System.out::println);
			String chordString = applyChordFreqMap(top3, orderOfMatch, prevChordString);
			System.out.println("Alternate chord #" + i + ": " + chordString);
			int[] chordLongMapped = mappedChord(chordString);
			melodyBasedRootProgression.add(Arrays.copyOf(chordLongMapped, chordLongMapped.length));
			alternateChordProg.add(chordLongMapped);
			chordStrings.add(chordString);
			prevChordString = chordString;
		}
		if (end < chordMelodyMap1.keySet().size()) {
			alternateChordProg
					.add(Arrays.copyOf(chordProgression.get(chordMelodyMap1.keySet().size() - 1),
							chordProgression.get(chordMelodyMap1.keySet().size() - 1).length));
			melodyBasedRootProgression
					.add(Arrays.copyOf(rootProgression.get(rootProgression.size() - 1),
							rootProgression.get(rootProgression.size() - 1).length));
		}

		melodyBasedChordProgression = squishChordProgression(alternateChordProg,
				gc.isSpiceFlattenBigChords(), gc.getRandomSeed(),
				gc.getChordGenSettings().getFlattenVoicingChance());
		return chordStrings;
	}

	public void generatePrettyUserChords(int mainGeneratorSeed, int fixedLength,
			double maxDuration) {
		generateChordProgression(mainGeneratorSeed, gc.getFixedDuration(),
				2 * Durations.WHOLE_NOTE);
	}

	public int multiplyVelocity(int velocity, int multiplierPercentage, int maxAdjust,
			int minAdjust) {
		if (multiplierPercentage == 100) {
			return velocity;
		} else if (multiplierPercentage > 100) {
			return Math.min(127 - maxAdjust, velocity * multiplierPercentage / 100);
		} else {
			return Math.max(0 + minAdjust, velocity * multiplierPercentage / 100);
		}
	}

	private List<int[]> generateChordProgression(int mainGeneratorSeed, int fixedLength,
			double maxDuration) {

		if (!userChords.isEmpty()) {
			List<int[]> userProgression = new ArrayList<>();
			chordInts.clear();
			chordInts.addAll(userChords);
			for (String chordString : userChords) {
				userProgression.add(mappedChord(chordString));
			}
			System.out.println(
					"Using user's custom progression: " + StringUtils.join(userChords, ","));
			return userProgression;
		}

		Random generator = new Random();
		generator.setSeed(mainGeneratorSeed);

		Random durationGenerator = new Random();
		durationGenerator.setSeed(mainGeneratorSeed);

		Map<String, List<String>> r = cpRulesMap;
		chordInts.clear();

		int maxLength = (fixedLength > 0) ? fixedLength : 8;
		if (fixedLength == 8) {
			maxDuration *= 2;
		}
		double fixedDuration = maxDuration / maxLength;
		int currentLength = 0;
		double currentDuration = 0.0;
		List<String> next = r.get("S");
		if (LAST_CHORD != null) {
			next = new ArrayList<String>();
			next.add(String.valueOf(LAST_CHORD));
		}
		List<String> debugMsg = new ArrayList<>();

		List<String> allowedSpiceChordsMiddle = new ArrayList<>();
		for (int i = 2; i < MidiUtils.SPICE_NAMES_LIST.size(); i++) {
			String chordString = MidiUtils.SPICE_NAMES_LIST.get(i);
			if (!gc.isDimAugDom7thEnabled()
					&& MidiUtils.BANNED_DIM_AUG_7_LIST.contains(chordString)) {
				continue;
			}
			if (!gc.isEnable9th13th() && MidiUtils.BANNED_9_13_LIST.contains(chordString)) {
				continue;
			}
			allowedSpiceChordsMiddle.add(chordString);
		}

		List<String> allowedSpiceChords = new ArrayList<>();
		for (String s : allowedSpiceChordsMiddle) {
			if (MidiUtils.BANNED_DIM_AUG_7_LIST.contains(s)
					|| MidiUtils.BANNED_SUSSY_LIST.contains(s)) {
				continue;
			}
			allowedSpiceChords.add(s);
		}


		List<int[]> cpr = new ArrayList<>();
		int[] prevChord = null;
		boolean canRepeatChord = true;
		String lastUnspicedChord = null;
		Random chordRepeatGenerator = new Random(mainGeneratorSeed);
		while ((currentDuration <= maxDuration - Durations.EIGHTH_NOTE)
				&& currentLength < maxLength) {
			double durationLeft = maxDuration - Durations.EIGHTH_NOTE - currentDuration;

			double dur = (fixedLength > 0) ? fixedDuration
					: pickDurationWeightedRandom(durationGenerator, durationLeft, CHORD_DUR_ARRAY,
							CHORD_DUR_CHANCE, Durations.QUARTER_NOTE);

			if (next.size() == 0 && prevChord != null) {
				cpr.add(prevChord);
				break;
			}
			int nextInt = generator.nextInt(next.size());

			// if last and not empty first chord
			boolean isLastChord = durationLeft - dur < 0.01;
			String chordString = null;
			if (isLastChord && FIRST_CHORD != null) {
				chordString = FIRST_CHORD;
			} else {
				if (gc.isAllowChordRepeats() && (fixedLength < 8 || !isLastChord) && canRepeatChord
						&& chordInts.size() > 0 && chordRepeatGenerator.nextInt(100) < 10) {
					chordString = String.valueOf(lastUnspicedChord);
					canRepeatChord = false;
				} else {
					chordString = next.get(nextInt);
				}
			}


			String firstLetter = chordString.substring(0, 1);
			List<String> spicyChordList = (!isLastChord && prevChord != null)
					? allowedSpiceChordsMiddle
					: allowedSpiceChords;
			String spicyChordString = firstLetter
					+ spicyChordList.get(generator.nextInt(spicyChordList.size()));
			if (chordString.endsWith("m") && spicyChordString.contains("maj")) {
				spicyChordString = spicyChordString.replace("maj", "m");
			} else if (chordString.length() == 1 && spicyChordString.contains("m")
					&& !spicyChordString.contains("dim") && !spicyChordString.contains("maj")) {
				spicyChordString = spicyChordString.replace("m", "maj");
			}

			// SPICE CHANCE
			if (generator.nextInt(100) < gc.getSpiceChance()
					&& (chordInts.size() < 7 || FIRST_CHORD == null)) {
				// keep spiciness
			} else {
				// remove spiciness
				spicyChordString = chordString;
			}

			if (!gc.isDimAugDom7thEnabled()) {
				if (gc.getScaleMode() != ScaleMode.IONIAN && gc.getScaleMode().ordinal() < 7) {
					int scaleOrder = gc.getScaleMode().ordinal();
					if (MidiUtils.MAJOR_CHORDS.indexOf(chordString) == 6 - scaleOrder) {
						spicyChordString = "Bdim";
					}
				}
			}

			chordInts.add(spicyChordString);

			//System.out.println("Fetching chord: " + chordInt);
			int[] mappedChord = mappedChord(spicyChordString);
			/*mappedChord = transposeChord(mappedChord, Mod.MAJOR_SCALE,
					gc.getScaleMode().noteAdjustScale);*/


			debugMsg.add("Generated int: " + nextInt + ", for chord: " + spicyChordString
					+ ", dur: " + dur + ", C[" + Arrays.toString(mappedChord) + "]");
			cpr.add(mappedChord);
			progressionDurations.add(dur);

			prevChord = mappedChord;
			next = r.get(chordString);

			if (fixedLength == 8 && chordInts.size() == 4 && FIRST_CHORD == null) {
				FIRST_CHORD = chordString;
			}

			// if last and empty first chord
			if (durationLeft - dur < 0 && FIRST_CHORD == null) {
				FIRST_CHORD = chordString;
			}
			currentLength += 1;
			currentDuration += dur;
			lastUnspicedChord = chordString;

		}
		System.out.println("CHORD PROG LENGTH: " + cpr.size());
		Collections.reverse(progressionDurations);
		Collections.reverse(cpr);
		Collections.reverse(debugMsg);
		Collections.reverse(chordInts);

		for (String s : debugMsg) {
			System.out.println(s);
		}

		if (progressionDurations.size() > 1
				&& (progressionDurations.get(0) != progressionDurations.get(2))) {
			double middle = (progressionDurations.get(0) + progressionDurations.get(2)) / 2.0;
			progressionDurations.set(0, middle);
			progressionDurations.set(2, middle);

		}

		return cpr;
	}

	private Note oldAlgoGenerateNote(MelodyPart mp, int[] chord, boolean isAscDirection,
			List<Integer> chordScale, Note previousNote, Random generator, double durationLeft) {
		// int randPitch = generator.nextInt(8);
		int velMin = mp.getVelocityMin();
		int velSpace = mp.getVelocityMax() - velMin;

		int direction = (isAscDirection) ? 1 : -1;
		double dur = pickDurationWeightedRandom(generator, durationLeft, MELODY_DUR_ARRAY,
				MELODY_DUR_CHANCE, Durations.SIXTEENTH_NOTE);
		boolean isPause = (generator.nextInt(100) < mp.getPauseChance());
		if (previousNote == null) {
			int[] firstChord = chord;
			int chordNote = (gc.isFirstNoteRandomized()) ? generator.nextInt(firstChord.length) : 0;

			int chosenPitch = 60 + (firstChord[chordNote] % 12);

			previousPitch = chordScale.indexOf(Integer.valueOf(chosenPitch));
			if (previousPitch == -1) {
				System.out.println("ERROR PITCH -1 for: " + chosenPitch);
				previousPitch = chordScale.indexOf(Integer.valueOf(chosenPitch + 1));
				if (previousPitch == -1) {
					System.out.println("NOT EVEN +1 pitch exists for " + chosenPitch + "!");
				}
			}

			//System.out.println(firstChord[chordNote] + " > from first chord");
			if (isPause) {
				return new Note(Integer.MIN_VALUE, dur);
			}

			return new Note(chosenPitch, dur, velMin + generator.nextInt(velSpace));
		}

		int change = generator.nextInt(gc.getMaxNoteJump() + 1);
		// weighted against same note
		if (change == 0) {
			change = generator.nextInt((gc.getMaxNoteJump() + 1) / 2);
		}

		int generatedPitch = previousPitch + direction * change;
		//fit into 0-7 scale
		generatedPitch = maX(generatedPitch, maxAllowedScaleNotes);


		if (generatedPitch == previousPitch && !isPause) {
			samePitchCount++;
		} else {
			samePitchCount = 0;
		}
		//if 3 or more times same note, swap direction for this case
		if (samePitchCount >= 2) {
			//System.out.println("UNSAMING NOTE!: " + previousPitch + ", BY: " + (-direction * change));
			generatedPitch = maX(previousPitch - direction * change, maxAllowedScaleNotes);
			samePitchCount = 0;
		}
		previousPitch = generatedPitch;
		if (isPause) {
			return new Note(Integer.MIN_VALUE, dur);
		}
		return new Note(chordScale.get(generatedPitch), dur, velMin + generator.nextInt(velSpace));

	}

	private Note[] oldAlgoGenerateMelodyForChord(MelodyPart mp, int[] chord, double maxDuration,
			Random generator, Note previousChordsNote, boolean isAscDirection) {
		List<Integer> scale = transposeScale(MELODY_SCALE, 0, false);

		double currentDuration = 0.0;

		Note previousNote = (gc.isFirstNoteFromChord()) ? null : previousChordsNote;
		List<Note> notes = new ArrayList<>();

		int exceptionsLeft = gc.getMaxExceptions();

		while (currentDuration <= maxDuration - Durations.SIXTEENTH_NOTE) {
			double durationLeft = maxDuration - Durations.SIXTEENTH_NOTE - currentDuration;
			boolean exceptionChangeUsed = false;
			// generate note,
			boolean actualDirection = isAscDirection;
			if ((generator.nextInt(100) < 33) && (exceptionsLeft > 0)) {
				//System.out.println("Exception used for chordnote: " + chord[0]);
				exceptionChangeUsed = true;
				actualDirection = !actualDirection;
			}
			Note note = oldAlgoGenerateNote(mp, chord, actualDirection, scale, previousNote,
					generator, durationLeft);
			if (exceptionChangeUsed) {
				exceptionsLeft--;
			}
			previousNote = note;
			currentDuration += note.getRhythmValue();
			Note transposedNote = new Note(note.getPitch(), note.getRhythmValue(),
					note.getDynamic());
			notes.add(transposedNote);
		}
		return notes.toArray(new Note[0]);
	}

	private Vector<Note> oldAlgoGenerateMelodySkeletonFromChords(MelodyPart mp, int measures,
			List<int[]> genRootProg) {
		List<Boolean> directionProgression = generateMelodyDirectionsFromChordProgression(
				genRootProg, true);

		Note previousChordsNote = null;

		Note[] pair024 = null;
		Note[] pair15 = null;
		Random melodyGenerator = new Random();
		if (!mp.isMuted() && mp.getPatternSeed() != 0) {
			melodyGenerator.setSeed(mp.getPatternSeed());
		} else {
			melodyGenerator.setSeed(gc.getRandomSeed());
		}
		System.out.println("LEGACY ALGORITHM!");
		Vector<Note> fullMelody = new Vector<>();
		for (int i = 0; i < measures; i++) {
			for (int j = 0; j < genRootProg.size(); j++) {
				Note[] generatedMelody = null;

				if ((i > 0 || j > 0) && (j == 0 || j == 2)) {
					generatedMelody = deepCopyNotes(mp, pair024, genRootProg.get(j),
							melodyGenerator);
				} else if (i > 0 && j == 1) {
					generatedMelody = deepCopyNotes(mp, pair15, null, null);
				} else {
					generatedMelody = oldAlgoGenerateMelodyForChord(mp, genRootProg.get(j),
							progressionDurations.get(j), melodyGenerator, previousChordsNote,
							directionProgression.get(j));
				}

				previousChordsNote = generatedMelody[generatedMelody.length - 1];

				if (i == 0 && j == 0) {
					pair024 = deepCopyNotes(mp, generatedMelody, null, null);
				}
				if (i == 0 && j == 1) {
					pair15 = deepCopyNotes(mp, generatedMelody, null, null);
				}
				fullMelody.addAll(Arrays.asList(generatedMelody));
			}
		}
		return fullMelody;
	}

	public void generateMasterpiece(int mainGeneratorSeed, String fileName) {
		System.out.println("--- GENERATING MASTERPIECE.. ---");
		long systemTime = System.currentTimeMillis();
		customDrumMappingNumbers = null;
		trackList.clear();
		//MELODY_SCALE = gc.getScaleMode().absoluteNotesC;

		Score score = new Score("MainScore", 120);
		Part bassRoots = new Part("BassRoots",
				(!gc.getBassPart().isMuted()) ? gc.getBassPart().getInstrument() : 0, 8);

		List<Part> melodyParts = new ArrayList<>();
		for (int i = 0; i < gc.getMelodyParts().size(); i++) {
			Part p = new Part("Melodies" + i, gc.getMelodyParts().get(0).getInstrument(),
					gc.getMelodyParts().get(i).getMidiChannel() - 1);
			melodyParts.add(p);
		}

		List<Part> chordParts = new ArrayList<>();
		for (int i = 0; i < gc.getChordParts().size(); i++) {
			Part p = new Part("Chords" + i, gc.getChordParts().get(i).getInstrument(),
					gc.getChordParts().get(i).getMidiChannel() - 1);
			chordParts.add(p);
		}
		List<Part> arpParts = new ArrayList<>();
		for (int i = 0; i < gc.getArpParts().size(); i++) {
			Part p = new Part("Arps" + i, gc.getArpParts().get(i).getInstrument(),
					gc.getArpParts().get(i).getMidiChannel() - 1);
			arpParts.add(p);
		}
		List<Part> drumParts = new ArrayList<>();
		for (int i = 0; i < gc.getDrumParts().size(); i++) {
			Part p = new Part("MainDrums", 0, 9);
			drumParts.add(p);
		}


		List<int[]> generatedRootProgression = generateChordProgression(mainGeneratorSeed,
				gc.getFixedDuration(), 2 * Durations.WHOLE_NOTE);
		if (!userChordsDurations.isEmpty()) {
			progressionDurations = userChordsDurations;
		}
		if (gc.isDoubledDurations()) {
			for (int i = 0; i < progressionDurations.size(); i++) {
				progressionDurations.set(i, progressionDurations.get(i) * 2);
			}
		}

		List<Double> actualDurations = progressionDurations;

		List<int[]> actualProgression = squishChordProgression(generatedRootProgression,
				gc.isSpiceFlattenBigChords(), gc.getRandomSeed(),
				gc.getChordGenSettings().getFlattenVoicingChance());

		if (!debugEnabled) {
			PrintStream dummyStream = new PrintStream(new OutputStream() {
				public void write(int b) {
					// NO-OP
				}
			});
			System.setOut(dummyStream);
		}

		// Arrangement process..
		System.out.println("Starting arrangement..");


		// prepare progressions
		chordProgression = actualProgression;
		rootProgression = generatedRootProgression;

		// run one empty pass through melody generation
		if (userMelody != null) {
			processUserMelody(userMelody);
			actualProgression = chordProgression;
			generatedRootProgression = rootProgression;
			actualDurations = progressionDurations;
		} else {
			fillMelodyFromPart(gc.getMelodyParts().get(0), actualProgression,
					generatedRootProgression, 1, 0, new Section(), new ArrayList<>());
		}
		double measureLength = 0;
		for (Double d : progressionDurations) {
			measureLength += d;
		}
		GENERATED_MEASURE_LENGTH = measureLength;
		int counter = 0;

		Arrangement arr = null;
		boolean overridden = false;
		if (gc.getArrangement().isOverridden()) {
			arr = gc.getActualArrangement();
			overridden = true;
		} else {
			if (gc.getArrangement().isPreviewChorus()) {
				arr = new Arrangement();
				gc.setArrangementPartVariationChance(0);
				gc.setArrangementVariationChance(0);
			} else {
				arr = gc.getArrangement();
			}
		}


		if (false) {
			InputStream is = new InputStream() {
				public int read() throws IOException {
					return 0;
				}
			};
		}
		boolean isPreview = arr.getSections().size() == 1;
		System.out.println("MidiGenerator - Overridden: " + overridden);
		int arrSeed = (arr.getSeed() != 0) ? arr.getSeed() : mainGeneratorSeed;

		int originalPartVariationChance = gc.getArrangementPartVariationChance();
		int secOrder = -1;

		storeGlobalParts();

		int transToSet = 0;
		boolean twoFiveOneChanged = false;
		double sectionStartTimer = 0;
		for (Section sec : arr.getSections()) {
			if (overridden) {
				sec.recalculatePartVariationMapBoundsIfNeeded();
			}
			sec.setSectionDuration(-1);
			sec.setSectionBeatDurations(null);
			boolean gcPartsReplaced = replaceGuiConfigInstParts(sec);
			secOrder++;
			System.out.println(
					"============== Processing section.. " + sec.getType() + " ================");
			sec.setStartTime(sectionStartTimer);

			Random rand = new Random();

			modTrans = transToSet;
			System.out.println("Key extra transpose: " + modTrans);

			if (sec.getType().equals("CLIMAX")) {
				// increase variations in follow-up CLIMAX sections, reset when climax ends
				gc.setArrangementPartVariationChance(
						gc.getArrangementPartVariationChance() + originalPartVariationChance / 2);
			} else {
				gc.setArrangementPartVariationChance(originalPartVariationChance);
			}

			if (sec.getType().equals("BUILDUP")) {
				if (rand.nextInt(100) < gc.getArrangementVariationChance()) {
					List<Integer> exceptionChanceList = new ArrayList<>();
					exceptionChanceList.add(1);
					if (sec.getPartPresenceVariationMap().get(4) != null) {
						for (int i = 0; i < sec.getPartPresenceVariationMap().get(4).length; i++) {
							if (rand.nextInt(100) < 66) {
								sec.setVariation(4, i, exceptionChanceList);
							}

						}
					}
				}
			}

			int notesSeedOffset = sec.getTypeMelodyOffset();
			System.out.println("Note offset category: " + notesSeedOffset);

			Random variationGen = new Random(arrSeed + sec.getTypeSeedOffset());
			List<Boolean> riskyVariations = sec.getRiskyVariations();
			if (riskyVariations == null) {
				riskyVariations = new ArrayList<>();
				for (int i = 0; i < Section.riskyVariationNames.length; i++) {
					boolean isVariation = variationGen.nextInt(100) < gc
							.getArrangementVariationChance();
					// generate only if not last AND next section is same type
					if (i == 0 || i == 4) {
						isVariation &= (secOrder < arr.getSections().size() - 1 && arr.getSections()
								.get(secOrder + 1).getType().equals(sec.getType()));
					}
					// generate only for non-critical sections with offset > 0
					if (i == 1 || i == 2 || i == 4) {
						isVariation &= notesSeedOffset > 0;
					}
					riskyVariations.add(isVariation);
				}
			}

			int usedMeasures = sec.getMeasures();

			// reset back to normal?
			boolean sectionChordsReplaced = false;
			if (sec.isCustomChordsDurationsEnabled()) {
				sectionChordsReplaced = replaceWithSectionCustomChordDurations(sec);
			}
			if (!sectionChordsReplaced) {
				if (riskyVariations.get(1)) {
					System.out.println("Risky Variation: Chord Swap!");
					rootProgression = melodyBasedRootProgression;
					chordProgression = melodyBasedChordProgression;
					progressionDurations = actualDurations;
				} else {
					rootProgression = generatedRootProgression;
					chordProgression = actualProgression;
					progressionDurations = actualDurations;

				}
			} else if (rootProgression.size() == generatedRootProgression.size()) {
				if (riskyVariations.get(1)) {
					System.out.println("Risky Variation: Chord Swap!");
					rootProgression = melodyBasedRootProgression;
					chordProgression = melodyBasedChordProgression;
					progressionDurations = actualDurations;
				}
			}

			if (riskyVariations.get(4)) {
				System.out.println("Risky Variation: Key Change (on next chord)!");
				transToSet = generateKeyChange(generatedRootProgression, arrSeed);
			}

			boolean twoFiveOneChords = gc.getKeyChangeType() == KeyChangeType.TWOFIVEONE
					&& riskyVariations.get(4);
			if (riskyVariations.get(0) && !twoFiveOneChords) {
				System.out.println("Risky Variation: Skip N-1 Chord!");

				skipN1Chord();
			}


			// copied into empty sections
			Note emptyMeasureNote = new Note(Integer.MIN_VALUE, measureLength);
			Phrase emptyPhrase = new Phrase();
			emptyPhrase.setStartTime(START_TIME_DELAY);
			emptyPhrase.add(emptyMeasureNote);
			CPhrase emptyCPhrase = new CPhrase();
			emptyCPhrase.setStartTime(START_TIME_DELAY);
			emptyCPhrase.addChord(new int[] { Integer.MIN_VALUE }, measureLength);


			rand.setSeed(arrSeed);
			variationGen.setSeed(arrSeed);
			if (!gc.getMelodyParts().isEmpty()) {
				List<Phrase> copiedPhrases = new ArrayList<>();
				Set<Integer> presences = sec.getPresence(0);
				for (int i = 0; i < gc.getMelodyParts().size(); i++) {
					MelodyPart mp = (MelodyPart) gc.getMelodyParts().get(i);
					int melodyChanceMultiplier = (sec.getTypeMelodyOffset() == 0 && i == 0) ? 2 : 1;
					// temporary increase for chance of main (#1) melody
					int oldChance = sec.getMelodyChance();
					sec.setMelodyChance(Math.min(100, oldChance * melodyChanceMultiplier));
					boolean added = !mp.isMuted()
							&& ((overridden && presences.contains(mp.getOrder()))
									|| (!overridden && rand.nextInt(100) < sec.getMelodyChance()));
					if (!MAXIMIZE_CHORUS_MAIN_MELODY && melodyChanceMultiplier > 1) {
						sec.setMelodyChance(oldChance);
					}
					if (added) {
						List<int[]> usedMelodyProg = chordProgression;
						List<int[]> usedRoots = rootProgression;

						// if n-1, do not also swap melody
						if (riskyVariations.get(2) && !riskyVariations.get(0)
								&& !sectionChordsReplaced) {
							usedMelodyProg = melodyBasedChordProgression;
							usedRoots = melodyBasedRootProgression;
							System.out.println("Risky Variation: Melody Swap!");
						}
						List<Integer> variations = (overridden) ? sec.getVariation(0, i) : null;
						Phrase m = fillMelodyFromPart(mp, usedMelodyProg, usedRoots, usedMeasures,
								notesSeedOffset, sec, variations);

						// DOUBLE melody with -12 trans, if there was a variation of +12 and it's a major part and it's the first (full) melody
						// risky variation - wacky melody transpose
						boolean laxCheck = notesSeedOffset == 0
								&& sec.getVariation(0, i).contains(Integer.valueOf(0));
						if (!riskyVariations.get(3)) {
							laxCheck &= (i == 0);
						}

						if (laxCheck) {
							Phrase m2 = m.copy();
							Mod.transpose(m2, -12);
							Part melPart = new Part();
							melPart.add(m2);
							melPart.add(m);
							if (riskyVariations.get(3)) {
								Mod.consolidate(melPart);
							} else {
								JMusicUtilsCustom.consolidate(melPart);
							}

							m = melPart.getPhrase(0);
						}
						copiedPhrases.add(m);
						if (!overridden)
							sec.setPresence(0, i);
					} else {
						copiedPhrases.add(emptyPhrase.copy());
					}
					sec.setMelodyChance(oldChance);
				}
				sec.setMelodies(copiedPhrases);

			}


			if (twoFiveOneChanged) {
				twoFiveOneChanged = false;
				replaceFirstChordForTwoFiveOne(transToSet);
			}

			if (twoFiveOneChords && chordInts.size() > 2) {
				twoFiveOneChanged = replaceLastChordsForTwoFiveOne(transToSet);
			}
			if (gc.getKeyChangeType() == KeyChangeType.TWOFIVEONE) {
				if (transToSet == -1 * modTrans && transToSet != 0) {
					transToSet = 0;
				}
			}

			rand.setSeed(arrSeed + 10);
			variationGen.setSeed(arrSeed + 10);
			if (!gc.getBassPart().isMuted()) {
				Set<Integer> presences = sec.getPresence(1);
				boolean added = (overridden && presences.contains(gc.getBassPart().getOrder()))
						|| (!overridden && rand.nextInt(100) < sec.getBassChance());
				if (added) {
					List<Integer> variations = (overridden) ? sec.getVariation(1, 0) : null;
					BassPart bp = gc.getBassPart();
					Phrase b = fillBassFromPart(bp, rootProgression, usedMeasures, sec, variations);
					if (variationGen.nextInt(100) < gc.getArrangementPartVariationChance()) {
						// TODO
					}

					if (bp.isDoubleOct()) {
						Phrase b2 = b.copy();
						Mod.transpose(b2, 12);
						Mod.increaseDynamic(b2, -15);
						Part bassPart = new Part();
						bassPart.addPhrase(b2);
						bassPart.addPhrase(b);
						JMusicUtilsCustom.consolidate(bassPart);

						b = bassPart.getPhrase(0);
						b.setStartTime(START_TIME_DELAY);
					}

					sec.setBass(b);
					if (!overridden)
						sec.setPresence(1, 0);
				} else {
					sec.setBass(emptyPhrase.copy());
				}

			}

			if (!gc.getChordParts().isEmpty()) {
				List<Phrase> copiedPhrases = new ArrayList<>();
				Set<Integer> presences = sec.getPresence(2);
				boolean useChordSlash = false;
				for (int i = 0; i < gc.getChordParts().size(); i++) {
					ChordPart cp = (ChordPart) gc.getChordParts().get(i);
					rand.setSeed(arrSeed + 100 + cp.getOrder());
					variationGen.setSeed(arrSeed + 100 + cp.getOrder());
					boolean added = (overridden && presences.contains(cp.getOrder()))
							|| (!overridden && rand.nextInt(100) < sec.getChordChance());
					if (added && !cp.isMuted()) {
						if (i == 0) {
							useChordSlash = true;
						}
						List<Integer> variations = (overridden) ? sec.getVariation(2, i) : null;
						Phrase c = fillChordsFromPart(cp, chordProgression, usedMeasures, sec,
								variations);
						if (variationGen.nextInt(100) < gc.getArrangementPartVariationChance()) {
							// TODO Mod.transpose(c, 12);
						}
						copiedPhrases.add(c);
						if (!overridden)
							sec.setPresence(2, i);
					} else {
						copiedPhrases.add(emptyPhrase.copy());
					}
				}
				sec.setChords(copiedPhrases);
				if (useChordSlash) {
					sec.setChordSlash(fillChordSlash(chordProgression, usedMeasures));
				} else {
					sec.setChordSlash(emptyCPhrase.copy());
				}

			}

			if (!gc.getArpParts().isEmpty()) {
				List<Phrase> copiedPhrases = new ArrayList<>();
				Set<Integer> presences = sec.getPresence(3);
				for (int i = 0; i < gc.getArpParts().size(); i++) {
					ArpPart ap = (ArpPart) gc.getArpParts().get(i);
					rand.setSeed(arrSeed + 200 + ap.getOrder());
					variationGen.setSeed(arrSeed + 200 + ap.getOrder());
					// if arp1 supports melody with same instrument, always introduce it in second half
					List<Integer> variations = (overridden) ? sec.getVariation(3, i) : null;
					boolean added = (overridden && presences.contains(ap.getOrder()))
							|| (!overridden && rand.nextInt(100) < sec.getArpChance() && i > 0
									&& !ap.isMuted());
					added |= (!overridden && i == 0
							&& ((isPreview || counter > ((arr.getSections().size() - 1) / 2))
									&& !ap.isMuted()));
					/* 
							&& ap.getInstrument() == gc.getMelodyParts().get(0).getInstrument()*/


					if (added) {
						Phrase a = fillArpFromPart(ap, chordProgression, usedMeasures, sec,
								variations);
						if (variationGen.nextInt(100) < gc.getArrangementPartVariationChance()) {
							// TODO Mod.transpose(a, 12);
						}
						copiedPhrases.add(a);
						if (!overridden)
							sec.setPresence(3, i);
					} else {
						copiedPhrases.add(emptyPhrase.copy());
					}
				}
				sec.setArps(copiedPhrases);
			}

			if (!gc.getDrumParts().isEmpty()) {
				List<Phrase> copiedPhrases = new ArrayList<>();
				Set<Integer> presences = sec.getPresence(4);
				for (int i = 0; i < gc.getDrumParts().size(); i++) {
					DrumPart dp = (DrumPart) gc.getDrumParts().get(i);
					rand.setSeed(arrSeed + 300 + dp.getOrder());
					variationGen.setSeed(arrSeed + 300 + dp.getOrder());

					// multiply drum chance using section note type + what drum it is
					int drumChanceMultiplier = 1;
					if (sec.getTypeMelodyOffset() == 0
							&& VibeComposerGUI.PUNCHY_DRUMS.contains(dp.getInstrument())) {
						drumChanceMultiplier = 2;
					}

					boolean added = (overridden && presences.contains(dp.getOrder()))
							|| (!overridden && rand.nextInt(100) < sec.getDrumChance()
									* drumChanceMultiplier);
					if (added && !dp.isMuted()) {
						boolean sectionForcedDynamics = (sec.getType().contains("CLIMAX"))
								&& variationGen.nextInt(100) < gc
										.getArrangementPartVariationChance();
						List<Integer> variations = (overridden) ? sec.getVariation(4, i) : null;
						Phrase d = fillDrumsFromPart(dp, chordProgression, usedMeasures,
								sectionForcedDynamics, sec, variations);
						if (variationGen.nextInt(100) < gc.getArrangementPartVariationChance()) {
							// TODO Mod.accent(d, 0.25);
						}
						copiedPhrases.add(d);
						if (!overridden)
							sec.setPresence(4, i);
					} else {
						copiedPhrases.add(emptyPhrase.copy());
					}
				}
				sec.setDrums(copiedPhrases);
			}
			if (sec.getRiskyVariations() == null) {
				sec.setRiskyVariations(riskyVariations);
			}
			if (gcPartsReplaced) {
				restoreGlobalPartsToGuiConfig();
			}
			counter += sec.getMeasures();
			sectionStartTimer += ((sec.getSectionDuration() > 0) ? sec.getSectionDuration()
					: measureLength) * sec.getMeasures();
		}
		System.out.println("Added phrases/cphrases to sections..");

		for (Section sec : arr.getSections()) {
			for (int i = 0; i < gc.getMelodyParts().size(); i++) {
				Phrase p = sec.getMelodies().get(i);
				p.setStartTime(p.getStartTime() + sec.getStartTime());
				p.setAppend(false);
				if (gc.getMelodyParts().get(0).isMuted()) {
					melodyParts.get(i).addPhrase(p);
				} else {
					melodyParts.get(0).addPhrase(p);
				}
			}

			if (!gc.getBassPart().isMuted()) {
				Phrase bp = sec.getBass();
				bp.setStartTime(bp.getStartTime() + sec.getStartTime());
				bassRoots.addPhrase(bp);
			}

			for (int i = 0; i < gc.getChordParts().size(); i++) {
				Phrase cp = sec.getChords().get(i);
				cp.setStartTime(cp.getStartTime() + sec.getStartTime());
				chordParts.get(i).addPhrase(cp);
			}

			for (int i = 0; i < gc.getArpParts().size(); i++) {
				Phrase cp = sec.getArps().get(i);
				cp.setStartTime(cp.getStartTime() + sec.getStartTime());
				arpParts.get(i).addPhrase(cp);
			}

			for (int i = 0; i < gc.getDrumParts().size(); i++) {
				Phrase p = sec.getDrums().get(i);
				p.setStartTime(p.getStartTime() + sec.getStartTime());
				if (COLLAPSE_DRUM_TRACKS) {
					p.setAppend(false);
					drumParts.get(0).addPhrase(p);
				} else {
					drumParts.get(i).addPhrase(p);
				}

			}
			if (gc.getChordParts().size() > 0) {
				CPhrase cscp = sec.getChordSlash();
				cscp.setStartTime(cscp.getStartTime() + sec.getStartTime());
				cscp.setAppend(false);
				chordParts.get(0).addCPhrase(cscp);
			}

		}
		System.out.println("Added sections to parts..");
		int trackCounter = 1;

		for (int i = 0; i < gc.getMelodyParts().size(); i++) {
			InstPanel ip = VibeComposerGUI.getPanelByOrder(gc.getMelodyParts().get(i).getOrder(),
					VibeComposerGUI.melodyPanels);
			if (!gc.getMelodyParts().get(i).isMuted()) {
				score.add(melodyParts.get(i));
				ip.setSequenceTrack(trackCounter++);
				//if (VibeComposerGUI.apSm)
			} else {
				ip.setSequenceTrack(-1);
				if (i == 0) {
					COLLAPSE_MELODY_TRACKS = false;
				}
			}
			if (COLLAPSE_MELODY_TRACKS) {
				break;
			}
		}

		for (int i = 0; i < gc.getArpParts().size(); i++) {

			InstPanel ip = VibeComposerGUI.getPanelByOrder(gc.getArpParts().get(i).getOrder(),
					VibeComposerGUI.arpPanels);
			if (!gc.getArpParts().get(i).isMuted()) {
				score.add(arpParts.get(i));
				ip.setSequenceTrack(trackCounter++);
				//if (VibeComposerGUI.apSm)
			} else {
				ip.setSequenceTrack(-1);
			}
		}

		if (!gc.getBassPart().isMuted()) {
			score.add(bassRoots);
			VibeComposerGUI.bassPanel.setSequenceTrack(trackCounter++);
		} else {
			VibeComposerGUI.bassPanel.setSequenceTrack(-1);
		}

		for (int i = 0; i < gc.getChordParts().size(); i++) {

			InstPanel ip = VibeComposerGUI.getPanelByOrder(gc.getChordParts().get(i).getOrder(),
					VibeComposerGUI.chordPanels);
			if (!gc.getChordParts().get(i).isMuted()) {
				score.add(chordParts.get(i));
				ip.setSequenceTrack(trackCounter++);
			} else {
				ip.setSequenceTrack(-1);
			}

		}
		/*if (gc.getScaleMode() != ScaleMode.IONIAN) {
			for (Part p : score.getPartArray()) {
				for (Phrase phr : p.getPhraseArray()) {
					MidiUtils.transposePhrase(phr, ScaleMode.IONIAN.noteAdjustScale,
							gc.getScaleMode().noteAdjustScale);
				}
			}
		}*/
		//int[] backTranspose = { 0, 2, 4, 5, 7, 9, 11, 12 };
		Mod.transpose(score, gc.getTranspose());

		// add drums after transposing transposable parts

		for (int i = 0; i < gc.getDrumParts().size(); i++) {
			score.add(drumParts.get(i));
			InstPanel ip = VibeComposerGUI.getPanelByOrder(gc.getDrumParts().get(i).getOrder(),
					VibeComposerGUI.drumPanels);
			if (gc.getDrumParts().get(i).isMuted()) {
				ip.setSequenceTrack(-1);
				trackCounter++;
			} else {
				ip.setSequenceTrack(trackCounter++);
			}
			if (COLLAPSE_DRUM_TRACKS) {
				break;
			}
		}


		System.out.println("Added parts to score..");


		score.setTempo(gc.getBpm());

		// write midi without log

		PrintStream dummyStream = new PrintStream(new OutputStream() {
			public void write(int b) {
				// NO-OP
			}
		});
		System.setOut(dummyStream);

		Write.midi(score, fileName);
		if (VibeComposerGUI.dconsole == null || !VibeComposerGUI.dconsole.getFrame().isVisible()) {
			System.setOut(originalStream);
		} else {
			VibeComposerGUI.dconsole.redirectOut();
		}


		// view midi
		if (DISPLAY_SCORE) {
			List<Part> partsToRemove = new ArrayList<>();
			for (Object p : score.getPartList()) {
				Part part = (Part) p;
				if ((part.getTitle().equalsIgnoreCase("MainDrums")
						|| part.getTitle().startsWith("Chords")) && showScoreMode == 0) {
					partsToRemove.add(part);
					continue;
				} else if (!part.getTitle().equalsIgnoreCase("MainDrums") && showScoreMode == 1) {
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
			partsToRemove.forEach(e -> score.removePart(e));
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					pianoRoll(score);

				}

			});

		}

		gc.setActualArrangement(arr);
		System.out.println(
				"MidiGenerator time: " + (System.currentTimeMillis() - systemTime) + " ms");
		System.out.println("********Viewing midi seed: " + mainGeneratorSeed + "************* ");
	}

	private boolean replaceWithSectionCustomChordDurations(Section sec) {
		Pair<List<String>, List<Double>> chordsDurations = VibeComposerGUI
				.solveUserChords(sec.getCustomChords(), sec.getCustomDurations());
		if (chordsDurations == null) {
			return false;
		}

		List<int[]> mappedChords = new ArrayList<>();
		List<int[]> mappedRootChords = new ArrayList<>();
		/*chordInts.clear();
		chordInts.addAll(userChords);*/
		for (String chordString : chordsDurations.getLeft()) {
			int[] mapped = mappedChord(chordString);
			mappedChords.add(mapped);
			mappedRootChords.add(new int[] { mapped[0] });
		}
		chordProgression = mappedChords;
		rootProgression = mappedRootChords;
		progressionDurations = chordsDurations.getRight();
		sec.setSectionBeatDurations(progressionDurations);
		sec.setSectionDuration(progressionDurations.stream().mapToDouble(e -> e).sum());
		System.out.println("Using SECTION custom progression: "
				+ StringUtils.join(chordsDurations.getLeft(), ","));

		return true;
	}

	private void storeGlobalParts() {
		melodyParts = gc.getMelodyParts();
		bassParts = Collections.singletonList(gc.getBassPart());
		chordParts = gc.getChordParts();
		arpParts = gc.getArpParts();
		drumParts = gc.getDrumParts();

	}

	private void restoreGlobalPartsToGuiConfig() {
		gc.setMelodyParts(melodyParts);
		gc.setBassPart(bassParts.get(0));
		gc.setChordParts(chordParts);
		gc.setArpParts(arpParts);
		gc.setDrumParts(drumParts);
	}

	private boolean replaceGuiConfigInstParts(Section sec) {
		boolean needsReplace = false;
		if (sec.getMelodyParts() != null) {
			gc.setMelodyParts(sec.getMelodyParts());
			needsReplace = true;
		}
		if (sec.getBassParts() != null) {
			gc.setBassPart(sec.getBassParts().get(0));
			needsReplace = true;
		}
		if (sec.getChordParts() != null) {
			gc.setChordParts(sec.getChordParts());
			needsReplace = true;
		}
		if (sec.getArpParts() != null) {
			gc.setArpParts(sec.getArpParts());
			needsReplace = true;
		}
		if (sec.getDrumParts() != null) {
			gc.setDrumParts(sec.getDrumParts());
			needsReplace = true;
		}
		return needsReplace;
	}

	private void replaceFirstChordForTwoFiveOne(int transToSet) {
		if (chordInts.get(0).startsWith("C")) {
			return;
		}

		List<int[]> altChordProgression = new ArrayList<>();
		List<int[]> altRootProgression = new ArrayList<>();
		altChordProgression.addAll(chordProgression);
		altRootProgression.addAll(rootProgression);

		int[] c = MidiUtils.mappedChord("CGCE");
		altChordProgression.set(0, c);
		altRootProgression.set(0, Arrays.copyOfRange(c, 0, 1));

		chordProgression = altChordProgression;
		rootProgression = altRootProgression;

		System.out.println("Replaced FIRST");
	}

	private boolean replaceLastChordsForTwoFiveOne(int transToSet) {
		int size = chordProgression.size();
		if (size < 3) {
			return false;
		}
		List<int[]> altChordProgression = new ArrayList<>();
		List<int[]> altRootProgression = new ArrayList<>();
		altChordProgression.addAll(chordProgression);
		altRootProgression.addAll(rootProgression);
		int[] dm = MidiUtils.transposeChord(MidiUtils.mappedChord("Dm"), transToSet);
		int[] g7 = MidiUtils.transposeChord(MidiUtils.mappedChord("G7"), transToSet);
		//if (transToSet != -2) {
		altChordProgression.set(size - 2, dm);
		altRootProgression.set(size - 2, Arrays.copyOf(dm, 1));
		//}

		altChordProgression.set(size - 1, g7);
		altRootProgression.set(size - 1, Arrays.copyOf(g7, 1));
		chordProgression = altChordProgression;
		rootProgression = altRootProgression;

		System.out.println("Replaced LAST");
		return true;

	}

	private int generateKeyChange(List<int[]> chords, int arrSeed) {
		int transToSet = 0;
		if (modTrans == 0) {
			KeyChangeType chg = gc.getKeyChangeType();
			switch (chg) {
			case PIVOT:
				transToSet = pivotKeyChange(chords);
				break;
			case DIRECT:
				transToSet = directKeyChange(arrSeed);
				break;
			case TWOFIVEONE:
				transToSet = twoFiveOneKeyChange(arrSeed);
				break;
			default:
				break;
			}
		} else {
			if (gc.getKeyChangeType() == KeyChangeType.TWOFIVEONE) {
				transToSet = -1 * modTrans;
			}
		}

		return transToSet;
	}

	private int twoFiveOneKeyChange(int arrSeed) {
		// Dm -> Em, Am, or Am octave below
		int[] transChoices = { 5, 2 };
		Random rand = new Random(arrSeed);
		return transChoices[rand.nextInt(transChoices.length)];
	}

	private int pivotKeyChange(List<int[]> chords) {
		int transToSet = 0;
		List<String> allCurrentChordsAsBasic = MidiUtils.getBasicChordStringsFromRoots(chords);
		String baseChordLast = allCurrentChordsAsBasic.get(allCurrentChordsAsBasic.size() - 1);
		String baseChordFirst = allCurrentChordsAsBasic.get(0);
		transToSet = 0;
		Pair<String, String> test = Pair.of(baseChordFirst, baseChordLast);
		for (Integer trans : MidiUtils.modulationMap.keySet()) {
			boolean hasValue = MidiUtils.modulationMap.get(trans).contains(test);
			if (hasValue) {
				transToSet = (trans < -4) ? (trans + 12) : trans;
				System.out.println("Trans up by: " + transToSet);
				break;
			}
		}
		if (transToSet == 0) {
			System.out.println("Pivot chord not found between last and first chord!");
		}
		return transToSet;
	}

	private int directKeyChange(int arrSeed) {
		Random rand = new Random(arrSeed);
		int[] pool = new int[] { -4, -3, 3, 4 };
		return pool[rand.nextInt(pool.length)];

	}

	public static void pianoRoll(Score s) {
		if (showScores.size() > 2) {
			ShowScore scr = showScores.get(0);
			showScores.remove(0);
			scr.dispose();
		}
		int x = (windowLoc % 10 == 0) ? 50 : 0;
		int y = (windowLoc % 15 == 0) ? 50 : 0;
		ShowScore nextScr = new ShowScore(s, x, y);
		windowLoc += 5;
		if (windowLoc > 15) {
			windowLoc = 5;
		}
		showScores.add(nextScr);

	}

	private void skipN1Chord() {
		List<Double> altProgressionDurations = new ArrayList<>();
		List<int[]> altChordProgression = new ArrayList<>();
		List<int[]> altRootProgression = new ArrayList<>();

		// TODO: other variations on how to generate alternates?
		// 1: chord trick, max two measures
		// 60 30 4 1 -> 60 30 1 - , 60 30 4 1
		altProgressionDurations.addAll(progressionDurations);
		altChordProgression.addAll(chordProgression);
		altRootProgression.addAll(rootProgression);

		int size = progressionDurations.size();
		if (size < 3) {
			return;
		}

		double duration = progressionDurations.get(size - 1) + progressionDurations.get(size - 2);
		altProgressionDurations.set(size - 2, duration);
		altProgressionDurations.remove(size - 1);

		altChordProgression.remove(size - 2);
		altRootProgression.remove(size - 2);

		progressionDurations = altProgressionDurations;
		chordProgression = altChordProgression;
		rootProgression = altRootProgression;
	}

	private void processUserMelody(Phrase userMelody) {
		if (!chordMelodyMap1.isEmpty()) {
			return;
		}

		int chordCounter = 0;
		double separatorValue = (gc.isDoubledDurations()) ? Durations.WHOLE_NOTE
				: Durations.HALF_NOTE;
		double chordSeparator = separatorValue;
		Vector<Note> noteList = userMelody.getNoteList();
		if (!chordMelodyMap1.containsKey(Integer.valueOf(0))) {
			chordMelodyMap1.put(Integer.valueOf(0), new ArrayList<>());
		}
		double rhythmCounter = 0;
		List<Double> progDurations = new ArrayList<>();
		progDurations.add(separatorValue);
		for (Note n : noteList) {
			System.out.println("Rhythm counter: " + rhythmCounter);
			if (rhythmCounter >= chordSeparator - 0.001) {
				System.out.println("NEXT CHORD!");
				chordSeparator += separatorValue;
				chordCounter++;
				progDurations.add(separatorValue);
				if (!chordMelodyMap1.containsKey(Integer.valueOf(chordCounter))) {
					chordMelodyMap1.put(Integer.valueOf(chordCounter), new ArrayList<>());
				}
			}
			chordMelodyMap1.get(Integer.valueOf(chordCounter)).add(n);
			rhythmCounter += n.getRhythmValue();
		}
		System.out.println("Rhythm counter end: " + rhythmCounter);
		while (rhythmCounter >= chordSeparator + 0.001) {
			System.out.println("NEXT CHORD!");
			chordSeparator += separatorValue;
			chordCounter++;
			progDurations.add(separatorValue);
			if (!chordMelodyMap1.containsKey(Integer.valueOf(chordCounter))) {
				chordMelodyMap1.put(Integer.valueOf(chordCounter), new ArrayList<>());
			}
			chordMelodyMap1.get(Integer.valueOf(chordCounter))
					.add(noteList.get(noteList.size() - 1));
		}
		System.out.println("Processed melody, chords: " + (chordCounter + 1));
		List<String> chordStrings = makeMelodyPitchFrequencyMap(0, chordMelodyMap1.keySet().size(),
				1);
		if (userChords == null || userChords.isEmpty()) {
			System.out.println(StringUtils.join(chordStrings, ","));
			chordInts = chordStrings;

			chordProgression = melodyBasedChordProgression;
			rootProgression = melodyBasedRootProgression;
			progressionDurations = progDurations;
		}
	}

	protected Phrase fillMelodyFromPart(MelodyPart mp, List<int[]> actualProgression,
			List<int[]> generatedRootProgression, int measures, int notesSeedOffset, Section sec,
			List<Integer> variations) {
		Phrase melodyPhrase = new Phrase();
		Vector<Note> skeletonNotes = null;
		if (userMelody != null) {
			skeletonNotes = userMelody.copy().getNoteList();
		} else {
			skeletonNotes = generateMelodySkeletonFromChords(mp, actualProgression,
					generatedRootProgression, measures, notesSeedOffset, sec, variations);
		}
		Vector<Note> fullMelody = convertMelodySkeletonToFullMelody(mp, progressionDurations,
				measures, sec, skeletonNotes, notesSeedOffset);
		if (mp.getOrder() == 1) {
			melodyNotePattern = patternFromNotes(fullMelody);
		}

		melodyPhrase.addNoteList(fullMelody, true);
		swingPhrase(melodyPhrase, mp.getSwingPercent(), Durations.EIGHTH_NOTE);

		if (gc.getScaleMode() != ScaleMode.IONIAN) {
			MidiUtils.transposePhrase(melodyPhrase, ScaleMode.IONIAN.noteAdjustScale,
					gc.getScaleMode().noteAdjustScale);
		}
		Mod.transpose(melodyPhrase, mp.getTranspose() + modTrans);
		melodyPhrase.setStartTime(START_TIME_DELAY);
		return melodyPhrase;
	}


	protected Phrase fillBassFromPart(BassPart bp, List<int[]> generatedRootProgression,
			int measures, Section sec, List<Integer> variations) {
		boolean genVars = variations == null;

		double[] durationPool = new double[] { Durations.NOTE_32ND, Durations.SIXTEENTH_NOTE,
				Durations.EIGHTH_NOTE, Durations.DOTTED_EIGHTH_NOTE, Durations.QUARTER_NOTE,
				Durations.SIXTEENTH_NOTE + Durations.QUARTER_NOTE, Durations.DOTTED_QUARTER_NOTE,
				Durations.HALF_NOTE };

		int[] durationWeights = new int[] { 5, 25, 45, 55, 75, 85, 95, 100 };

		int seed = bp.getPatternSeed();

		Phrase bassPhrase = new Phrase();
		int volMultiplier = (gc.isScaleMidiVelocityInArrangement()) ? sec.getVol(1) : 100;
		int minVel = multiplyVelocity(bp.getVelocityMin(), volMultiplier, 0, 1);
		int maxVel = multiplyVelocity(bp.getVelocityMax(), volMultiplier, 1, 0);
		Random variationGenerator = new Random(seed + sec.getTypeMelodyOffset());
		Random rhythmPauseGenerator = new Random(seed + sec.getTypeMelodyOffset());
		Random noteVariationGenerator = new Random(seed + sec.getTypeMelodyOffset() + 2);
		boolean rhythmPauses = false;
		int numberOfVars = Section.variationDescriptions[1].length - 2;
		for (int i = 0; i < measures; i++) {
			int extraSeed = 0;
			for (int j = 0; j < generatedRootProgression.size(); j++) {
				if (genVars && (j == 0) && sec.getTypeMelodyOffset() > 0) {
					variations = fillVariations(sec, variationGenerator, variations, numberOfVars,
							1);
				}

				if ((variations != null) && (j == 0)) {
					for (Integer var : variations) {
						if (i == measures - 1) {
							System.out.println("Bass #1 variation: " + var);
						}

						switch (var) {
						case 0:
							extraSeed = 100;
							break;
						case 1:
							rhythmPauses = true;
							break;
						default:
							throw new IllegalArgumentException("Too much variation!");
						}
					}
				}


				Random bassDynamics = new Random(gc.getRandomSeed());
				int velSpace = maxVel - minVel;
				if (bp.isUseRhythm()) {
					int seedCopy = seed;
					seedCopy += extraSeed;
					if (bp.isAlternatingRhythm()) {
						seedCopy += (j % 2);
					}
					Rhythm bassRhythm = new Rhythm(seedCopy, progressionDurations.get(j),
							durationPool, durationWeights);
					int counter = 0;
					for (Double dur : bassRhythm.regenerateDurations(4)) {

						int randomNote = 0;
						// note variation for short notes, low chance, only after first
						if (counter > 0 && dur < Durations.EIGHTH_NOTE
								&& noteVariationGenerator.nextInt(100) < bp.getNoteVariation()
								&& generatedRootProgression.get(j).length > 1) {
							randomNote = noteVariationGenerator
									.nextInt(generatedRootProgression.get(j).length - 1) + 1;
						}

						int pitch = (rhythmPauses && dur < Durations.EIGHTH_NOTE
								&& rhythmPauseGenerator.nextInt(100) < 33) ? Integer.MIN_VALUE
										: generatedRootProgression.get(j)[randomNote];


						bassPhrase.addNote(
								new Note(pitch, dur, bassDynamics.nextInt(velSpace) + minVel));
						counter++;
					}
				} else {
					bassPhrase.addNote(new Note(generatedRootProgression.get(j)[0],
							progressionDurations.get(j), bassDynamics.nextInt(velSpace) + minVel));
				}
			}
		}
		if (gc.getScaleMode() != ScaleMode.IONIAN) {
			MidiUtils.transposePhrase(bassPhrase, ScaleMode.IONIAN.noteAdjustScale,
					gc.getScaleMode().noteAdjustScale);
		}
		Mod.transpose(bassPhrase, -24 + bp.getTranspose() + modTrans);
		bassPhrase.setStartTime(START_TIME_DELAY);
		if (genVars && variations != null) {
			sec.setVariation(1, 0, variations);
		}
		return bassPhrase;

	}

	protected Phrase fillChordsFromPart(ChordPart cp, List<int[]> actualProgression, int measures,
			Section sec, List<Integer> variations) {
		boolean genVars = variations == null;

		int mainGeneratorSeed = (int) cp.getPatternSeed() + cp.getOrder();
		Phrase phr = new Phrase();
		List<Chord> chords = new ArrayList<>();
		Random variationGenerator = new Random(
				cp.getPatternSeed() + cp.getOrder() + sec.getTypeSeedOffset());
		int numberOfVars = Section.variationDescriptions[2].length - 2;
		int stretch = cp.getChordNotesStretch();
		boolean maxStrum = false;
		List<Integer> fillPattern = cp.getChordSpanFill()
				.getPatternByLength(actualProgression.size());

		int volMultiplier = (gc.isScaleMidiVelocityInArrangement()) ? sec.getVol(2) : 100;
		int minVel = multiplyVelocity(cp.getVelocityMin(), volMultiplier, 0, 1);
		int maxVel = multiplyVelocity(cp.getVelocityMax(), volMultiplier, 1, 0);


		for (int i = 0; i < measures; i++) {
			Random transitionGenerator = new Random(mainGeneratorSeed);
			int extraTranspose = 0;
			boolean ignoreChordSpanFill = false;
			boolean skipSecondNote = false;

			// fill chords
			for (int j = 0; j < actualProgression.size(); j++) {
				if (genVars && (j == 0)) {
					variations = fillVariations(sec, variationGenerator, variations, numberOfVars,
							2);
				}

				if ((variations != null) && (j == 0)) {
					for (Integer var : variations) {
						if (i == measures - 1) {
							System.out.println("Chord #" + cp.getOrder() + " variation: " + var);
						}

						switch (var) {
						case 0:
							extraTranspose = 12;
							break;
						case 1:
							ignoreChordSpanFill = true;
							break;
						case 2:
							if (stretch < 6) {
								int randomStretchAdd = variationGenerator.nextInt(6 - stretch) + 1;
								stretch += randomStretchAdd;
							}
							break;
						case 3:
							skipSecondNote = true;
							break;
						case 4:
							maxStrum = true;
							break;
						default:
							throw new IllegalArgumentException("Too much variation!");
						}
					}
				}

				Chord c = Chord.EMPTY(progressionDurations.get(j));

				Random velocityGenerator = new Random(mainGeneratorSeed + j);
				c.setVelocity(velocityGenerator.nextInt(maxVel - minVel) + minVel);

				boolean transition = transitionGenerator.nextInt(100) < cp.getTransitionChance();
				int transChord = (transitionGenerator.nextInt(100) < cp.getTransitionChance())
						? (j + 1) % actualProgression.size()
						: j;

				if (!ignoreChordSpanFill) {
					if (fillPattern.get(j) < 1) {
						chords.add(c);
						continue;
					}
				}

				c.setDurationRatio(cp.getNoteLengthMultiplier() / 100.0);

				int[] mainChordNotes = actualProgression.get(j);
				int[] transChordNotes = actualProgression.get(transChord);

				//only skip if not already an interval (2 notes)
				if (skipSecondNote) {
					if (mainChordNotes.length > 2) {
						int[] newMainChordNotes = new int[mainChordNotes.length - 1];
						for (int m = 0; m < mainChordNotes.length; m++) {
							if (m == 1)
								continue;
							int index = (m > 1) ? m - 1 : m;
							newMainChordNotes[index] = mainChordNotes[m];

						}
						mainChordNotes = newMainChordNotes;
					}
					if (transChordNotes.length > 2) {
						int[] newTransChordNotes = new int[transChordNotes.length - 1];
						for (int m = 0; m < transChordNotes.length; m++) {
							if (m == 1)
								continue;
							int index = (m > 1) ? m - 1 : m;
							newTransChordNotes[index] = transChordNotes[m];
						}

						transChordNotes = newTransChordNotes;
					}
				}
				mainChordNotes = convertChordToLength(mainChordNotes, stretch,
						cp.isStretchEnabled());
				transChordNotes = convertChordToLength(transChordNotes, stretch,
						cp.isStretchEnabled());

				c.setTranspose(extraTranspose);
				c.setNotes(mainChordNotes);

				// for transition:
				double splitTime = progressionDurations.get(j)
						* (gc.getChordGenSettings().isUseSplit() ? cp.getTransitionSplit()
								: DEFAULT_CHORD_SPLIT)
						/ 1000.0;
				//System.out.println("Split time: " + splitTime);

				List<Integer> pattern = cp.getFinalPatternCopy();
				pattern = pattern.subList(0, cp.getHitsPerPattern());
				double duration = progressionDurations.get(j) / pattern.size();
				double durationCounter = 0;
				for (int p = 0; p < pattern.size(); p++) {
					//System.out.println("Duration counter: " + durationCounter);
					Chord cC = Chord.copy(c);
					cC.setRhythmValue(duration);
					// less plucky
					cC.setDurationRatio(cC.getDurationRatio() + (1 - cC.getDurationRatio()) / 2);
					if (pattern.get(p) < 1) {
						cC.setNotes(new int[] { Integer.MIN_VALUE });
					} else if (transition && durationCounter >= splitTime) {
						cC.setNotes(transChordNotes);
					}
					int nextP = p + 1;
					int durMultiplier = 1;
					if (cp.isDurationStretch()) {
						while (nextP < pattern.size()) {
							if (pattern.get(nextP) < 1) {
								durMultiplier++;
							} else {
								break;
							}
							nextP++;
						}
					}

					cC.setDurationRatio(cC.getDurationRatio() * durMultiplier);
					chords.add(cC);
					durationCounter += duration;
				}
			}
		}// chord strum
		double flamming = 0.0;
		if (gc.getChordGenSettings().isUseStrum()) {
			if (maxStrum) {
				flamming = SECOND_ARRAY_STRUM[SECOND_ARRAY_STRUM.length - 1];
			} else {
				int index = -1;
				for (int i = 0; i < VibeComposerGUI.MILISECOND_ARRAY_STRUM.length; i++) {
					if (cp.getStrum() == VibeComposerGUI.MILISECOND_ARRAY_STRUM[i]) {
						index = i;
						break;
					}
				}
				if (index != -1) {
					flamming = SECOND_ARRAY_STRUM[index] * noteMultiplier;
				} else {
					flamming = (noteMultiplier * (double) cp.getStrum()) / 1000.0;
					System.out.println("Chord strum CUSTOM! " + cp.getStrum());
				}
			}

		}
		MidiUtils.addChordsToPhrase(phr, chords, flamming);
		// transpose
		int extraTranspose = gc.getChordGenSettings().isUseTranspose() ? cp.getTranspose() : 0;
		if (gc.getScaleMode() != ScaleMode.IONIAN) {
			MidiUtils.transposePhrase(phr, ScaleMode.IONIAN.noteAdjustScale,
					gc.getScaleMode().noteAdjustScale);
		}
		Mod.transpose(phr, -12 + extraTranspose + modTrans);

		// delay
		double additionalDelay = 0;
		if (gc.getChordGenSettings().isUseDelay()) {
			additionalDelay = ((noteMultiplier * cp.getDelay()) / 1000.0);
		}
		phr.setStartTime(START_TIME_DELAY + additionalDelay);


		if (genVars && variations != null) {
			sec.setVariation(2, getAbsoluteOrder(2, cp), variations);
		}
		return phr;
	}

	protected Phrase fillArpFromPart(ArpPart ap, List<int[]> actualProgression, int measures,
			Section sec, List<Integer> variations) {
		boolean genVars = variations == null;
		Phrase arpPhrase = new Phrase();

		ArpPart apClone = (ArpPart) ap.clone();

		Map<String, List<Integer>> arpMap = generateArpMap(ap.getPatternSeed(),
				ap.equals(gc.getArpParts().get(0)), ap);

		List<Integer> arpPattern = arpMap.get(ARP_PATTERN_KEY);
		List<Integer> arpOctavePattern = arpMap.get(ARP_OCTAVE_KEY);
		List<Integer> arpPausesPattern = arpMap.get(ARP_PAUSES_KEY);

		List<Boolean> directions = null;


		// TODO: divide
		int repeatedArpsPerChord = ap.getHitsPerPattern() * ap.getPatternRepeat();

		double longestChord = progressionDurations.stream().max((e1, e2) -> Double.compare(e1, e2))
				.get();
		Random variationGenerator = new Random(
				ap.getPatternSeed() + ap.getOrder() + sec.getTypeSeedOffset());

		boolean melodic = (ap.getPattern() == RhythmPattern.MELODY1 && melodyNotePattern != null);
		/*if (melodic) {
			repeatedArpsPerChord /= ap.getChordSpan();
		}*/

		int volMultiplier = (gc.isScaleMidiVelocityInArrangement()) ? sec.getVol(3) : 100;
		int minVel = multiplyVelocity(ap.getVelocityMin(), volMultiplier, 0, 1);
		int maxVel = multiplyVelocity(ap.getVelocityMax(), volMultiplier, 1, 0);

		boolean fillLastBeat = false;
		List<Integer> fillPattern = ap.getChordSpanFill()
				.getPatternByLength(actualProgression.size());
		int numberOfVars = Section.variationDescriptions[3].length - 2;
		for (int i = 0; i < measures; i++) {
			int chordSpanPart = 0;
			int extraTranspose = 0;
			boolean ignoreChordSpanFill = false;
			boolean forceRandomOct = false;

			Random velocityGenerator = new Random(ap.getPatternSeed());
			Random exceptionGenerator = new Random(ap.getPatternSeed() + 1);
			for (int j = 0; j < actualProgression.size(); j++) {
				if (genVars && (j == 0)) {
					variations = fillVariations(sec, variationGenerator, variations, numberOfVars,
							3);
				}

				if ((variations != null) && (j == 0)) {
					for (Integer var : variations) {
						if (i == measures - 1) {
							System.out.println("Arp #" + ap.getOrder() + " variation: " + var);
						}

						switch (var) {
						case 0:
							extraTranspose = 12;
							break;
						case 1:
							ignoreChordSpanFill = true;
							break;
						case 2:
							forceRandomOct = true;
							break;
						case 3:
							fillLastBeat = true;
							break;
						case 4:
							if (directions == null) {
								directions = generateMelodyDirectionsFromChordProgression(
										actualProgression, true);
							}
							break;
						default:
							throw new IllegalArgumentException("Too much variation!");
						}
					}
				}

				double chordDurationArp = longestChord / ((double) repeatedArpsPerChord);
				int[] chord = convertChordToLength(actualProgression.get(j),
						ap.getChordNotesStretch(), ap.isStretchEnabled());
				if (directions != null) {
					ArpPattern pat = (directions.get(j)) ? ArpPattern.UP : ArpPattern.DOWN;
					arpPattern = pat.getPatternByLength(ap.getHitsPerPattern(), chord.length,
							ap.getPatternRepeat());
					arpPattern = MidiUtils.intersperse(0, ap.getChordSpan() - 1, arpPattern);
				} else {
					if (ap.getArpPattern() != ArpPattern.RANDOM) {
						arpPattern = ap.getArpPattern().getPatternByLength(ap.getHitsPerPattern(),
								chord.length, ap.getPatternRepeat());
						arpPattern = MidiUtils.intersperse(0, ap.getChordSpan() - 1, arpPattern);
					}
				}

				double durationNow = 0;
				int swingPercentAmount = (repeatedArpsPerChord == 4 || repeatedArpsPerChord == 8)
						? gc.getMaxArpSwing()
						: 50;

				// reset every 2
				if (j % 2 == 0) {
					exceptionGenerator.setSeed(ap.getPatternSeed() + 1);
				}
				List<Integer> pitchPatternSpanned = partOfList(chordSpanPart, ap.getChordSpan(),
						arpPattern);
				List<Integer> octavePatternSpanned = partOfList(chordSpanPart, ap.getChordSpan(),
						arpOctavePattern);
				List<Integer> pausePatternSpanned = partOfList(chordSpanPart, ap.getChordSpan(),
						arpPausesPattern);

				for (int p = 0; p < repeatedArpsPerChord; p++) {

					int velocity = velocityGenerator.nextInt(maxVel - minVel) + minVel;

					Integer patternNum = pitchPatternSpanned.get(p);

					int octaveAdjustGenerated = octavePatternSpanned.get(p);
					int octaveAdjustmentFromPattern = (patternNum < 2) ? -12
							: ((patternNum < 6) ? 0 : 12);

					int pitch = chord[patternNum % chord.length];
					if (gc.isUseOctaveAdjustments() || forceRandomOct) {
						pitch += octaveAdjustmentFromPattern + octaveAdjustGenerated;
					}

					pitch += extraTranspose;
					if (!fillLastBeat || j < actualProgression.size() - 1) {
						if (pausePatternSpanned.get(p) == 0) {
							pitch = Integer.MIN_VALUE;
						}
						if (!ignoreChordSpanFill) {
							if (fillPattern.get(j) < 1) {
								pitch = Integer.MIN_VALUE;
							}
						}
					}


					double swingDuration = chordDurationArp
							* (swingPercentAmount / ((double) 50.0));
					swingPercentAmount = 100 - swingPercentAmount;

					if (durationNow + swingDuration > progressionDurations.get(j)) {
						double fillerDuration = progressionDurations.get(j) - durationNow;
						Note fillerNote = new Note(
								fillerDuration < 0.05 ? Integer.MIN_VALUE : pitch, fillerDuration,
								velocity);
						arpPhrase.addNote(fillerNote);
						break;
					} else {
						if (exceptionGenerator.nextInt(100) < ap.getExceptionChance()) {
							double splitDuration = swingDuration / 2;
							arpPhrase.addNote(new Note(pitch, splitDuration, velocity));
							arpPhrase.addNote(new Note(pitch, splitDuration, velocity - 15));
						} else {
							arpPhrase.addNote(new Note(pitch, swingDuration, velocity));
						}
					}
					durationNow += swingDuration;
				}
				chordSpanPart++;
				if (chordSpanPart >= ap.getChordSpan()) {
					chordSpanPart = 0;
				}
			}
		}
		int extraTranspose = ARP_SETTINGS.isUseTranspose() ? ap.getTranspose() : 0;
		if (gc.getScaleMode() != ScaleMode.IONIAN) {
			MidiUtils.transposePhrase(arpPhrase, ScaleMode.IONIAN.noteAdjustScale,
					gc.getScaleMode().noteAdjustScale);
		}
		Mod.transpose(arpPhrase, -24 + extraTranspose + modTrans);

		double additionalDelay = 0;
		/*if (ARP_SETTINGS.isUseDelay()) {
			additionalDelay = (gc.getArpParts().get(i).getDelay() / 1000.0);
		}*/
		if (genVars && variations != null) {
			sec.setVariation(3, getAbsoluteOrder(3, ap), variations);
		}
		if (fillLastBeat) {
			Mod.crescendo(arpPhrase, arpPhrase.getEndTime() * 3 / 4, arpPhrase.getEndTime(),
					Math.max(minVel, 55), Math.max(maxVel, 110));
		}
		ap.setPatternShift(apClone.getPatternShift());
		//dp.setVelocityPattern(false);
		ap.setChordSpan(apClone.getChordSpan());
		ap.setHitsPerPattern(apClone.getHitsPerPattern());
		ap.setPatternRepeat(apClone.getPatternRepeat());
		arpPhrase.setStartTime(START_TIME_DELAY + additionalDelay);
		return arpPhrase;
	}


	protected Phrase fillDrumsFromPart(DrumPart dp, List<int[]> actualProgression, int measures,
			boolean sectionForcedDynamics, Section sec, List<Integer> variations) {
		boolean genVars = variations == null;
		Phrase drumPhrase = new Phrase();

		DrumPart dpClone = (DrumPart) dp.clone();

		sectionForcedDynamics &= (dp.getInstrument() < 38 || dp.getInstrument() > 40);

		int chordsCount = actualProgression.size();

		List<Integer> drumPattern = generateDrumPatternFromPart(dp);

		if (!dp.isVelocityPattern() && drumPattern.indexOf(dp.getInstrument()) == -1) {
			//drumPhrase.addNote(new Note(Integer.MIN_VALUE, patternDurationTotal, 100));
			drumPhrase.setStartTime(START_TIME_DELAY + ((noteMultiplier * dp.getDelay()) / 1000.0));
			return drumPhrase;
		}

		List<Integer> drumVelocityPattern = generateDrumVelocityPatternFromPart(sec, dp);
		Random variationGenerator = new Random(
				dp.getPatternSeed() + dp.getOrder() + sec.getTypeSeedOffset());
		int numberOfVars = Section.variationDescriptions[4].length - 2;
		// bar iter
		int hits = dp.getHitsPerPattern();
		int swingPercentAmount = (hits % 2 == 0) ? dp.getSwingPercent() : 50;
		List<Integer> fillPattern = dp.getChordSpanFill()
				.getPatternByLength(actualProgression.size());

		for (int o = 0; o < measures; o++) {
			// exceptions are generated the same for each bar, but differently for each pattern within bar (if there is more than 1)
			Random exceptionGenerator = new Random(dp.getPatternSeed() + dp.getOrder());
			int chordSpan = dp.getChordSpan();
			int oneChordPatternSize = drumPattern.size() / chordSpan;
			boolean ignoreChordSpanFill = false;
			int extraExceptionChance = 0;

			// chord iter
			for (int j = 0; j < chordsCount; j += chordSpan) {

				if (genVars && ((j == 0) || (j == chordInts.size()))) {
					variations = fillVariations(sec, variationGenerator, variations, numberOfVars,
							4);
				}

				if ((variations != null) && (j == 0)) {
					for (Integer var : variations) {
						if (o == measures - 1) {
							System.out.println("Drum #" + dp.getOrder() + " variation: " + var);
						}

						switch (var) {
						case 0:
							ignoreChordSpanFill = true;
							break;
						case 1:
							extraExceptionChance = (dp.getInstrument() < 38
									&& dp.getInstrument() > 40) ? dp.getExceptionChance() + 10
											: dp.getExceptionChance();
							break;
						default:
							throw new IllegalArgumentException("Too much variation!");
						}
					}
				}

				double patternDurationTotal = 0.0;
				for (int k = 0; k < chordSpan; k++) {
					patternDurationTotal += (progressionDurations.size() > j + k)
							? progressionDurations.get(j + k)
							: 0.0;
				}

				double drumDuration = patternDurationTotal / hits;

				for (int k = 0; k < drumPattern.size(); k++) {
					int drum = drumPattern.get(k);
					int velocity = drumVelocityPattern.get(k);
					int pitch = (drum >= 0) ? drum : Integer.MIN_VALUE;
					if (drum < 0 && (dp.isVelocityPattern() || (o > 0 && sectionForcedDynamics))) {
						velocity = (velocity * 5) / 10;
						pitch = dp.getInstrument();
					}

					int chordNum = j + (k / oneChordPatternSize);
					if (!ignoreChordSpanFill) {
						if (fillPattern.get(chordNum % actualProgression.size()) < 1) {
							pitch = Integer.MIN_VALUE;
						}
					}

					if (pitch != Integer.MIN_VALUE && gc.isDrumCustomMapping()) {
						pitch = mapDrumPitchByCustomMapping(pitch, true);
					}

					boolean exception = exceptionGenerator
							.nextInt(100) < (dp.getExceptionChance() + extraExceptionChance);
					if (exception) {
						int secondVelocity = (velocity * 8) / 10;
						Note n1 = new Note(pitch, drumDuration / 2, velocity);
						Note n2 = new Note(pitch, drumDuration / 2, secondVelocity);
						n1.setDuration(0.5 * n1.getRhythmValue());
						n2.setDuration(0.5 * n2.getRhythmValue());
						drumPhrase.addNote(n1);
						drumPhrase.addNote(n2);
					} else {
						Note n1 = new Note(pitch, drumDuration, velocity);
						n1.setDuration(0.5 * n1.getRhythmValue());
						drumPhrase.addNote(n1);
					}

				}
			}
		}
		if (genVars && variations != null) {
			sec.setVariation(4, getAbsoluteOrder(4, dp), variations);
		}
		swingPhrase(drumPhrase, swingPercentAmount, Durations.EIGHTH_NOTE);
		drumPhrase.setStartTime(START_TIME_DELAY + ((noteMultiplier * dp.getDelay()) / 1000.0));
		dp.setHitsPerPattern(dpClone.getHitsPerPattern());
		dp.setPatternShift(dpClone.getPatternShift());
		dp.setChordSpan(dpClone.getChordSpan());
		return drumPhrase;

	}

	private List<Integer> fillVariations(Section sec, Random varGenerator, List<Integer> variations,
			int numVars, int part) {
		int failsafeCounter = 0;

		while (varGenerator.nextInt(100) < gc.getArrangementPartVariationChance()
				&& (variations == null || variations.size() < numVars)) {
			// pick one variation
			int variationInt = varGenerator.nextInt(numVars);
			while (VariationPopup.bannedInstVariations.get(part).contains(variationInt + 2)) {
				failsafeCounter++;
				if (failsafeCounter > numVars) {
					break;
				}
				variationInt = varGenerator.nextInt(numVars);
			}
			if (failsafeCounter > numVars) {
				break;
			}
			if (variations == null) {
				variations = new ArrayList<>();
			}

			if (!variations.contains(variationInt)) {
				variations.add(variationInt);
			}
		}
		return variations;
	}

	public static int mapDrumPitchByCustomMapping(int pitch, boolean cached) {
		if (cached && customDrumMappingNumbers != null) {
			int mapped = customDrumMappingNumbers.get(pitch);
			if (mapped == -1) {
				throw new IllegalArgumentException(
						"Pitch not found in custom drum mapping: " + pitch);
			}
			return customDrumMappingNumbers.get(pitch);
		}
		List<Integer> customMappingNumbers = null;
		if (gc != null) {
			String customMapping = gc.getDrumCustomMappingNumbers();
			String[] customMappingNumberStrings = customMapping.split(",");
			customMappingNumbers = Arrays.asList(customMappingNumberStrings).stream()
					.map(e -> Integer.valueOf(e.trim())).collect(Collectors.toList());
		} else {
			customMappingNumbers = Arrays.asList(MidiUtils.DRUM_INST_NUMBERS_SEMI);
		}

		List<Integer> defaultMappingNumbers = MidiUtils.getInstNumbers(MidiUtils.DRUM_INST_NAMES);
		int defaultIndex = defaultMappingNumbers.indexOf(pitch);
		if (defaultIndex < 0) {
			throw new IllegalArgumentException("Pitch not found in default drum mapping: " + pitch);
		} else if (defaultMappingNumbers.size() != customMappingNumbers.size()) {
			throw new IllegalArgumentException("Custom mapping has incorrect number of elements!");
		}
		if (cached) {
			customDrumMappingNumbers = new HashMap<>();
			for (int i = 0; i < defaultMappingNumbers.size(); i++) {
				customDrumMappingNumbers.put(defaultMappingNumbers.get(i),
						customMappingNumbers.get(i));
			}
		}

		return customMappingNumbers.get(defaultIndex);
	}

	private int getAbsoluteOrder(int partNum, InstPart part) {
		List<? extends InstPanel> panels = VibeComposerGUI.getInstList(partNum);
		for (int i = 0; i < panels.size(); i++) {
			if (panels.get(i).getPanelOrder() == part.getOrder()) {
				return i;
			}
		}
		throw new IllegalArgumentException("Absolute order not found!");
	}

	protected CPhrase fillChordSlash(List<int[]> actualProgression, int measures) {
		CPhrase chordSlashCPhrase = new CPhrase();
		Random chordSlashGenerator = new Random(gc.getRandomSeed() + 2);
		for (int i = 0; i < measures; i++) {
			// fill slash chord slashes
			for (int j = 0; j < actualProgression.size(); j++) {
				// pick random chord, take first/root pitch
				boolean isChordSlash = chordSlashGenerator.nextInt(100) < gc.getChordSlashChance();
				String slashChord = MidiUtils.MAJOR_CHORDS.get(chordSlashGenerator.nextInt(6));
				int[] mappedChord = mappedChord(slashChord);
				if (isChordSlash && mappedChord != null) {
					chordSlashCPhrase.addChord(new int[] { mappedChord[0] },
							progressionDurations.get(j));
				} else {
					chordSlashCPhrase.addChord(new int[] { Integer.MIN_VALUE },
							progressionDurations.get(j));
				}
			}
		}
		Mod.transpose(chordSlashCPhrase, -24);
		chordSlashCPhrase.setStartTime(START_TIME_DELAY);
		return chordSlashCPhrase;


	}

	private <T> List<T> partOfList(int part, int partCount, List<T> list) {
		double size = Math.ceil(list.size() / ((double) partCount));
		List<T> returnList = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			if (i >= part * size && i <= (part + 1) * size) {
				returnList.add(list.get(i));
			}
		}
		return returnList;
	}

	private void applyRuleToMelody(Note[] melody, Consumer<Note[]> melodyRule) {
		melodyRule.accept(melody);
	}

	private Note[] deepCopyNotes(MelodyPart mp, Note[] originals, int[] chord,
			Random melodyGenerator) {
		Note[] copied = new Note[originals.length];
		for (int i = 0; i < originals.length; i++) {
			Note n = originals[i];
			copied[i] = new Note(n.getPitch(), n.getRhythmValue());
		}
		if (chord != null && melodyGenerator != null && gc.isFirstNoteFromChord()) {
			Note n = oldAlgoGenerateNote(mp, chord, true, MELODY_SCALE, null, melodyGenerator,
					Durations.HALF_NOTE);
			copied[0] = new Note(n.getPitch(), originals[0].getRhythmValue(),
					originals[0].getDynamic());
		}
		return copied;
	}

	private void processPausePattern(ArpPart ap, List<Integer> arpPausesPattern,
			Random pauseGenerator) {
		for (int i = 0; i < ap.getHitsPerPattern(); i++) {
			if (pauseGenerator.nextInt(100) < ap.getPauseChance()) {
				arpPausesPattern.set(i, 0);
			}
		}
	}

	private Map<String, List<Integer>> generateArpMap(int mainGeneratorSeed, boolean needToReport,
			ArpPart ap) {
		Random uiGenerator2arpPattern = new Random(mainGeneratorSeed + 1);
		Random uiGenerator3arpOctave = new Random(mainGeneratorSeed + 2);
		Random uiGenerator4arpPauses = new Random(mainGeneratorSeed + 3);

		List<Integer> arpPausesPattern = new ArrayList<>();
		if (ap.getPattern() == RhythmPattern.FULL) {
			for (int i = 0; i < ap.getHitsPerPattern(); i++) {
				arpPausesPattern.add(1);
			}
			Collections.rotate(arpPausesPattern, ap.getPatternShift());
		} else if (ap.getPattern() == RhythmPattern.MELODY1 && melodyNotePattern != null) {
			//System.out.println("Setting note pattern!");
			arpPausesPattern = melodyNotePattern;
			ap.setPatternShift(0);
			//dp.setVelocityPattern(false);
			ap.setChordSpan(chordInts.size());
			ap.setHitsPerPattern(melodyNotePattern.size() / chordInts.size());
			ap.setPatternRepeat(1);
		} else {
			arpPausesPattern = ap.getFinalPatternCopy();
			arpPausesPattern = arpPausesPattern.subList(0, ap.getHitsPerPattern());
		}

		processPausePattern(ap, arpPausesPattern, uiGenerator4arpPauses);

		int[] arpPatternArray = IntStream.iterate(0, e -> (e + 1) % MAXIMUM_PATTERN_LENGTH)
				.limit(ap.getHitsPerPattern() * 2).toArray();
		int[] arpOctaveArray = IntStream.iterate(0, e -> (e + 12) % 24)
				.limit(ap.getHitsPerPattern() * 2).toArray();
		List<Integer> arpPattern = Arrays.stream(arpPatternArray).boxed()
				.collect(Collectors.toList());
		if (ap.isRepeatableNotes()) {
			arpPattern.addAll(arpPattern);
		}


		List<Integer> arpOctavePattern = Arrays.stream(arpOctaveArray).boxed()
				.collect(Collectors.toList());

		// TODO: note pattern, different from rhythm pattern
		//if (ap.getPattern() == RhythmPattern.RANDOM) {
		Collections.shuffle(arpPattern, uiGenerator2arpPattern);
		Collections.shuffle(arpOctavePattern, uiGenerator3arpOctave);
		//}
		// always generate ap.getHitsPerPattern(), 
		// cut off however many are needed (support for seed randoms)
		if (!(ap.getPattern() == RhythmPattern.MELODY1 && melodyNotePattern != null)) {
			arpPausesPattern = arpPausesPattern.subList(0, ap.getHitsPerPattern());
		}
		arpPattern = arpPattern.subList(0, ap.getHitsPerPattern());
		arpOctavePattern = arpOctavePattern.subList(0, ap.getHitsPerPattern());

		if (needToReport) {
			//System.out.println("Arp count: " + ap.getHitsPerPattern());
			//System.out.println("Arp pattern: " + arpPattern.toString());
			//System.out.println("Arp octaves: " + arpOctavePattern.toString());
		}
		//System.out.println("Arp pauses : " + arpPausesPattern.toString());

		if (ap.getChordSpan() > 1) {
			if (!(ap.getPattern() == RhythmPattern.MELODY1 && melodyNotePattern != null)) {
				arpPausesPattern = MidiUtils.intersperse(0, ap.getChordSpan() - 1,
						arpPausesPattern);
			}
			arpPattern = MidiUtils.intersperse(0, ap.getChordSpan() - 1, arpPattern);
			arpOctavePattern = MidiUtils.intersperse(0, ap.getChordSpan() - 1, arpOctavePattern);
		}

		// pattern repeat

		List<Integer> repArpPattern = new ArrayList<>();
		List<Integer> repOctPattern = new ArrayList<>();
		List<Integer> repPausePattern = new ArrayList<>();
		for (int i = 0; i < ap.getPatternRepeat(); i++) {
			repArpPattern.addAll(arpPattern);
			repOctPattern.addAll(arpOctavePattern);
			repPausePattern.addAll(arpPausesPattern);
		}


		Map<String, List<Integer>> arpMap = new HashMap<>();
		arpMap.put(ARP_PATTERN_KEY, repArpPattern);
		arpMap.put(ARP_OCTAVE_KEY, repOctPattern);
		arpMap.put(ARP_PAUSES_KEY, repPausePattern);


		return arpMap;
	}

	public static List<Integer> generateDrumPatternFromPart(DrumPart dp) {
		Random uiGenerator1drumPattern = new Random(dp.getPatternSeed() + dp.getOrder() - 1);
		List<Integer> premadePattern = null;
		if (melodyNotePattern != null && dp.getPattern() == RhythmPattern.MELODY1) {
			//System.out.println("Setting note pattern!");
			dp.setHitsPerPattern(melodyNotePattern.size());
			premadePattern = melodyNotePattern;
			dp.setPatternShift(0);
			//dp.setVelocityPattern(false);
			dp.setChordSpan(chordInts.size());
		} else {
			premadePattern = dp.getFinalPatternCopy();
		}

		List<Integer> drumPattern = new ArrayList<>();
		for (int j = 0; j < dp.getHitsPerPattern(); j++) {
			// if random pause or not present in pattern: pause
			if (uiGenerator1drumPattern.nextInt(100) < dp.getPauseChance()
					|| !premadePattern.get(j).equals(1)) {
				drumPattern.add(-1);
			} else {
				if (dp.getInstrument() == 42
						&& uiGenerator1drumPattern.nextInt(100) < OPENHAT_CHANCE) {
					drumPattern.add(mapDrumPitchByCustomMapping(46, true));
				} else {
					drumPattern.add(dp.getInstrument());
				}

			}
		}

		/*System.out
				.println("Drum pattern for " + dp.getInstrument() + " : " + drumPattern.toString());*/
		return drumPattern;
	}

	private List<Integer> generateDrumVelocityPatternFromPart(Section sec, DrumPart dp) {
		Random uiGenerator1drumVelocityPattern = new Random(dp.getPatternSeed() + dp.getOrder());
		List<Integer> drumVelocityPattern = new ArrayList<>();
		int multiplier = (gc.isScaleMidiVelocityInArrangement()) ? sec.getVol(4) : 100;
		int minVel = multiplyVelocity(dp.getVelocityMin(), multiplier, 0, 1);
		int maxVel = multiplyVelocity(dp.getVelocityMax(), multiplier, 1, 0);
		int velocityRange = maxVel - minVel;
		for (int j = 0; j < dp.getHitsPerPattern(); j++) {
			int velocity = uiGenerator1drumVelocityPattern.nextInt(velocityRange) + minVel;
			drumVelocityPattern.add(velocity);
		}
		/*System.out.println("Drum velocity pattern for " + dp.getInstrument() + " : "
				+ drumVelocityPattern.toString());*/
		return drumVelocityPattern;
	}
}
