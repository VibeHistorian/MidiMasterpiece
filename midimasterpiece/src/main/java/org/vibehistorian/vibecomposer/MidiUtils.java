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
along with this program; if not,
see <https://www.gnu.org/licenses/>.
*/

package org.vibehistorian.vibecomposer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import jm.constants.Pitches;
import jm.music.data.Note;
import jm.music.data.Phrase;

public class MidiUtils {

	public enum PARTS {
		MELODY, ARPS, CHORDS, BASSROOTS, DRUMS;
	}

	public interface Scales {

		public static final Integer[] CHROMATIC_SCALE = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 },
				MAJOR_SCALE = { 0, 2, 4, 5, 7, 9, 11 }, MINOR_SCALE = { 0, 2, 3, 5, 7, 8, 10 },
				HARMONIC_MINOR_SCALE = { 0, 2, 3, 5, 7, 8, 11 },
				MELODIC_MINOR_SCALE = { 0, 2, 3, 5, 7, 8, 9, 10, 11 }, // mix of ascend and descend
				NATURAL_MINOR_SCALE = { 0, 2, 3, 5, 7, 8, 10 },
				DIATONIC_MINOR_SCALE = { 0, 2, 3, 5, 7, 8, 10 },
				AEOLIAN_SCALE = { 0, 2, 3, 5, 7, 8, 10 }, DORIAN_SCALE = { 0, 2, 3, 5, 7, 9, 10 },
				PHRYGIAN_SCALE = { 0, 1, 3, 5, 7, 8, 10 }, LYDIAN_SCALE = { 0, 2, 4, 6, 7, 9, 11 },
				MIXOLYDIAN_SCALE = { 0, 2, 4, 5, 7, 9, 10 }, PENTATONIC_SCALE = { 0, 2, 4, 7, 9 },
				BLUES_SCALE = { 0, 2, 3, 4, 7, 9, 12 }, TURKISH_SCALE = { 0, 1, 3, 5, 7, 10, 11 },
				INDIAN_SCALE = { 0, 1, 1, 4, 5, 8, 10 }, LOCRIAN_SCALE = { 0, 1, 3, 4, 6, 8, 10 };

	}

	//full scale
	public static final List<Integer> cIonianScale4 = new ArrayList<>(Arrays.asList(Pitches.C4,
			Pitches.D4, Pitches.E4, Pitches.F4, Pitches.G4, Pitches.A4, Pitches.B4, Pitches.C5));
	public static final List<Integer> cDorianScale4 = new ArrayList<>(Arrays.asList(Pitches.C4,
			Pitches.D4, Pitches.EF4, Pitches.F4, Pitches.G4, Pitches.A4, Pitches.BF4, Pitches.C5));
	public static final List<Integer> cPhrygianScale4 = new ArrayList<>(
			Arrays.asList(Pitches.C4, Pitches.DF4, Pitches.EF4, Pitches.F4, Pitches.G4, Pitches.AF4,
					Pitches.BF4, Pitches.C5));
	public static final List<Integer> cLydianScale4 = new ArrayList<>(Arrays.asList(Pitches.C4,
			Pitches.D4, Pitches.E4, Pitches.FS4, Pitches.G4, Pitches.A4, Pitches.B4, Pitches.C5));
	public static final List<Integer> cMixolydianScale4 = new ArrayList<>(Arrays.asList(Pitches.C4,
			Pitches.D4, Pitches.E4, Pitches.F4, Pitches.G4, Pitches.A4, Pitches.BF4, Pitches.C5));
	public static final List<Integer> cAeolianScale4 = new ArrayList<>(Arrays.asList(Pitches.C4,
			Pitches.D4, Pitches.EF4, Pitches.F4, Pitches.G4, Pitches.AF4, Pitches.BF4, Pitches.C5));
	public static final List<Integer> cLocrianScale4 = new ArrayList<>(
			Arrays.asList(Pitches.C4, Pitches.DF4, Pitches.EF4, Pitches.F4, Pitches.GF4,
					Pitches.AF4, Pitches.BF4, Pitches.C5));
	public static final List<Integer> cBluesScale4 = new ArrayList<>(Arrays.asList(Pitches.C4,
			Pitches.D4, Pitches.EF4, Pitches.E4, Pitches.G4, Pitches.A4, Pitches.C5));

	public enum ScaleMode {
		IONIAN(Scales.MAJOR_SCALE, 0), DORIAN(Scales.DORIAN_SCALE, 2),
		PHRYGIAN(Scales.PHRYGIAN_SCALE, 1), LYDIAN(Scales.LYDIAN_SCALE, 3),
		MIXOLYDIAN(Scales.MIXOLYDIAN_SCALE, 6), AEOLIAN(Scales.AEOLIAN_SCALE, 5),
		LOCRIAN(Scales.LOCRIAN_SCALE, 4), BLUES(Scales.BLUES_SCALE, -1),
		HARM_MINOR(Scales.HARMONIC_MINOR_SCALE, 5), TURKISH(Scales.TURKISH_SCALE, -1),
		INDIAN(Scales.INDIAN_SCALE, 1);

		public Integer[] noteAdjustScale;
		public Integer modeTargetNote;

		private ScaleMode(Integer[] adjust, Integer targetNote) {
			this.noteAdjustScale = adjust;
			modeTargetNote = targetNote;
		}
	}

	public static final List<ScaleMode> majorishModes = Arrays
			.asList(new ScaleMode[] { ScaleMode.IONIAN, ScaleMode.LYDIAN, ScaleMode.MIXOLYDIAN,
					ScaleMode.BLUES, ScaleMode.TURKISH });
	public static final List<ScaleMode> minorishModes = Arrays
			.asList(new ScaleMode[] { ScaleMode.DORIAN, ScaleMode.PHRYGIAN, ScaleMode.AEOLIAN,
					ScaleMode.HARM_MINOR, ScaleMode.LOCRIAN, ScaleMode.INDIAN });

	//chords
	public static final int[] cMaj4 = { Pitches.C4, Pitches.E4, Pitches.G4 };
	public static final int[] cMin4 = { Pitches.C4, Pitches.EF4, Pitches.G4 };
	public static final int[] cAug4 = { Pitches.C4, Pitches.E4, Pitches.GS4 };
	public static final int[] cDim4 = { Pitches.C4, Pitches.EF4, Pitches.GF4 };
	public static final int[] c7th4 = { Pitches.C4, Pitches.E4, Pitches.G4, Pitches.BF4 };
	public static final int[] cMaj7th4 = { Pitches.C4, Pitches.E4, Pitches.G4, Pitches.B4 };
	public static final int[] cMin7th4 = { Pitches.C4, Pitches.EF4, Pitches.G4, Pitches.BF4 };
	public static final int[] cMaj9th4 = { Pitches.C4, Pitches.E4, Pitches.G4, Pitches.B4,
			Pitches.D5 };
	public static final int[] cMin9th4 = { Pitches.C4, Pitches.E4, Pitches.G4, Pitches.B4,
			Pitches.D5 };
	public static final int[] cMaj13th4 = { Pitches.C4, Pitches.E4, Pitches.G4, Pitches.B4,
			Pitches.D5, Pitches.A5 };
	public static final int[] cMin13th4 = { Pitches.C4, Pitches.EF4, Pitches.G4, Pitches.BF4,
			Pitches.D5, Pitches.A5 };
	public static final int[] cSus4th4 = { Pitches.C4, Pitches.F4, Pitches.G4 };
	public static final int[] cSus2nd4 = { Pitches.C4, Pitches.D4, Pitches.G4 };
	public static final int[] cSus7th4 = { Pitches.C4, Pitches.F4, Pitches.G4, Pitches.BF4 };
	public static final int[] cMaj6th4 = { Pitches.C4, Pitches.E4, Pitches.G4, Pitches.A4 };
	public static final int[] cMin6th4 = { Pitches.C4, Pitches.EF4, Pitches.G4, Pitches.A4 };

	public static final List<int[]> SPICE_CHORDS_LIST = new ArrayList<>();

	static {
		SPICE_CHORDS_LIST.add(cMaj4);
		SPICE_CHORDS_LIST.add(cMin4);

		SPICE_CHORDS_LIST.add(cAug4);
		SPICE_CHORDS_LIST.add(cDim4);

		SPICE_CHORDS_LIST.add(c7th4);
		SPICE_CHORDS_LIST.add(cMaj7th4);
		SPICE_CHORDS_LIST.add(cMin7th4);
		SPICE_CHORDS_LIST.add(cMaj9th4);
		SPICE_CHORDS_LIST.add(cMin9th4);
		SPICE_CHORDS_LIST.add(cMaj13th4);
		SPICE_CHORDS_LIST.add(cMin13th4);

		SPICE_CHORDS_LIST.add(cSus4th4);
		SPICE_CHORDS_LIST.add(cSus2nd4);
		SPICE_CHORDS_LIST.add(cSus7th4);

		SPICE_CHORDS_LIST.add(cMaj6th4);
		SPICE_CHORDS_LIST.add(cMin6th4);
	}

	public static final List<String> BANNED_DIM_AUG_6_LIST = Arrays
			.asList(new String[] { "dim", "aug", "maj6", "m6" });
	public static final List<String> BANNED_9_13_LIST = Arrays
			.asList(new String[] { "maj9", "m9", "maj13", "m13" });
	public static final List<String> BANNED_SUSSY_LIST = Arrays
			.asList(new String[] { "sus4", "sus2", "sus7" });

	public static final List<String> SPICE_NAMES_LIST = Arrays
			.asList(new String[] { "", "m", "aug", "dim", "7", "maj7", "m7", "maj9", "m9", "maj13",
					"m13", "sus4", "sus2", "sus7", "maj6", "m6" });
	// index 0 unused
	public static final List<String> CHORD_FIRST_LETTERS = Arrays
			.asList(new String[] { "X", "C", "D", "E", "F", "G", "A", "B" });
	public static final List<String> MAJOR_CHORDS = Arrays
			.asList(new String[] { "C", "Dm", "Em", "F", "G", "Am", "Bdim" });
	public static final List<String> MINOR_CHORDS = Arrays
			.asList(new String[] { "Cm", "Ddim", "D#", "Fm", "Gm", "G#", "A#" });
	public static final List<String> SEMITONE_LETTERS = Arrays.asList(
			new String[] { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" });

	public static final List<Integer> majorChordsModRating = Arrays
			.asList(new Integer[] { 3, 2, 1, 3, 3, 1, -10 });

	public static final List<String> progressionCircle = Arrays
			.asList(new String[] { "C", "F", "Bdim", "Em", "Am", "Dm", "G", "C" });

	public static final List<Integer> MAJ_SCALE = Arrays.asList(Scales.MAJOR_SCALE);
	public static final List<Integer> MIN_SCALE = Arrays.asList(Scales.AEOLIAN_SCALE);

	// relevancy order for % 12: 0, 7, 2, 5, 9, 4, 11
	public static final List<Integer> relevancyOrder = Arrays
			.asList(new Integer[] { 0, 7, 2, 5, 9, 4, 11 });


	public static final Map<String, List<String>> cpRulesMap = createChordProgressionRulesMap();
	public static final Map<String, List<String>> cpRulesForwardMap = createChordProgressionForwardRulesMap();
	//public static final Map<String, List<String>> cpRulesForwardMinorMap = createChordProgressionForwardRulesMinorMap();
	public static final Map<Integer, Integer> diaTransMap = createDiaTransMap();
	public static final Map<String, int[]> chordsMap = createChordMap();

	public static final Map<String, Set<Integer>> freqMap = createChordFreqMap(chordsMap.keySet());
	public static final Map<String, Set<Integer>> baseFreqMap = createChordFreqMap(MAJOR_CHORDS);

	public static final Map<Integer, List<Pair<String, String>>> modulationMap = createKeyModulationMap();

	private static Map<String, List<String>> createChordProgressionRulesMap() {
		Map<String, List<String>> cpMap = new HashMap<>();
		//"S" is an imaginary last element which can grow into the correct last elements
		cpMap.put("S", new ArrayList<>(Arrays.asList("C", "F", "G", "Am")));
		cpMap.put("C", new ArrayList<>(Arrays.asList("F", "G")));
		cpMap.put("Dm", new ArrayList<>(Arrays.asList("Am")));
		cpMap.put("Em", new ArrayList<>(Arrays.asList("Am", "G")));
		cpMap.put("F", new ArrayList<>(Arrays.asList("C", "Dm", "Am")));
		cpMap.put("G", new ArrayList<>(Arrays.asList("C", "Dm", "Em", "F", "Am")));
		cpMap.put("Am", new ArrayList<>(Arrays.asList("C", "Dm", "Em", "G")));
		cpMap.put("Bdim", new ArrayList<>(Arrays.asList("C", "Em", "F")));
		return cpMap;

	}

	private static Map<String, List<String>> createChordProgressionForwardRulesMap() {
		Map<String, List<String>> cpMap = new HashMap<>();
		cpMap.put("S", new ArrayList<>(MAJOR_CHORDS));
		cpMap.put("C", new ArrayList<>(Arrays.asList("Dm", "Em", "F", "G", "Am")));
		cpMap.put("Dm", new ArrayList<>(Arrays.asList("G", "Bdim")));
		cpMap.put("Em", new ArrayList<>(Arrays.asList("Am", "F", "Dm")));
		cpMap.put("F", new ArrayList<>(Arrays.asList("Dm", "G", "Am", "Bdim")));
		cpMap.put("G", new ArrayList<>(Arrays.asList("C", "Am", "Bdim")));
		cpMap.put("Am", new ArrayList<>(Arrays.asList("Dm", "F")));
		cpMap.put("Bdim", new ArrayList<>(Arrays.asList("C", "G", "Am")));
		return cpMap;
	}

	private static Map<String, List<String>> createChordProgressionForwardRulesMinorMap() {
		Map<String, List<String>> cpMap = new HashMap<>();
		cpMap.put("S",
				new ArrayList<>(Arrays.asList("Cm", "Ddim", "E", "Fm", "G", "A", "A#", "Bdim")));
		cpMap.put("Cm", new ArrayList<>(Arrays.asList("Ddim", "E", "Fm", "G", "A", "A#", "Bdim")));
		cpMap.put("Ddim", new ArrayList<>(Arrays.asList("G", "Bdim")));
		cpMap.put("E", new ArrayList<>(Arrays.asList("Ddim", "Fm", "A")));
		cpMap.put("Fm", new ArrayList<>(Arrays.asList("Ddim", "G", "Bdim")));
		cpMap.put("G", new ArrayList<>(Arrays.asList("Cm", "A", "Bdim")));
		cpMap.put("A", new ArrayList<>(Arrays.asList("Ddim", "Fm")));
		cpMap.put("A#", new ArrayList<>(Arrays.asList("Ddim", "E", "Fm")));
		cpMap.put("Bdim", new ArrayList<>(Arrays.asList("Cm", "G", "A")));
		return cpMap;
	}

	// diaTransMap.get(i) == MAJOR_SCALE.get(i) ? 
	private static Map<Integer, Integer> createDiaTransMap() {
		Map<Integer, Integer> diaMap = new HashMap<>();
		diaMap.put(1, 0);
		diaMap.put(2, 2);
		diaMap.put(3, 4);
		diaMap.put(4, 5);
		diaMap.put(5, 7);
		diaMap.put(6, 9);
		diaMap.put(7, 11);
		return diaMap;

	}

	private static Map<String, int[]> createChordMap() {
		Map<String, int[]> chordMap = new HashMap<>();

		for (int i = 1; i <= 7; i++) {
			for (int j = 0; j < SPICE_CHORDS_LIST.size(); j++) {
				chordMap.put(CHORD_FIRST_LETTERS.get(i) + SPICE_NAMES_LIST.get(j),
						transposeChord(SPICE_CHORDS_LIST.get(j), diaTransMap.get(i)));
			}
		}
		return chordMap;

	}

	private static Map<Integer, List<Pair<String, String>>> createKeyModulationMap() {
		Map<Integer, List<Pair<String, String>>> modMap = new HashMap<>();

		for (int i = -5; i <= 6; i++) {
			if (i == 0) {
				continue;
			} else {
				List<Pair<String, String>> pair = getKeyModPairs(i, freqMap);
				if (pair != null) {
					modMap.put(i, pair);
					/*LG.i(
							"Trans: " + i + ", pair: " + (pair == null ? "NULL" : pair.toString()));*/

				}
			}
		}

		return modMap;

	}

	private static List<Pair<String, String>> getKeyModPairs(int toKey,
			Map<String, Set<Integer>> freqMap) {
		List<Pair<String, String>> pairs = new ArrayList<>();
		for (int i = 0; i < 7; i++) {
			// for each chord of major scale, check if this chord transposed up by toKey is also a chord of the major scale
			String chordString = MAJOR_CHORDS.get(i);
			Set<Integer> baseFreqs = freqMap.get(chordString);
			Set<Integer> transFreqs = new HashSet<>();
			baseFreqs.forEach(e -> transFreqs.add((e + toKey + 12) % 12));
			for (String s : freqMap.keySet()) {
				Set<Integer> comparedFreqs = freqMap.get(s);
				if (comparedFreqs.containsAll(transFreqs)) {
					Pair<String, String> goodPair = Pair.of(chordString, s);
					/*LG.i("Good pair: " + goodPair.toString() + ", rating: "
							+ ratePairForModulation(goodPair));*/
					pairs.add(goodPair);
				}
			}
		}
		return pairs.isEmpty() ? null : pairs;
	}


	public static int ratePairForModulation(Pair<String, String> pair) {
		int val1 = majorChordsModRating.get(MAJOR_CHORDS.indexOf(pair.getLeft()));
		int val2 = majorChordsModRating.get(MAJOR_CHORDS.indexOf(pair.getRight()));
		return val1 + val2;
	}

	// order freq map by which chord contains most of the passed in notes
	// -> create map 
	public static String applyChordFreqMap(Map<Integer, Long> frequentNotes, int orderOfMatch,
			String prevChordString, Map<String, Set<Integer>> freqMap) {
		if (orderOfMatch == 0) {
			orderOfMatch++;
		}
		Map<String, Long> chordMatchesMap = new LinkedHashMap<>();
		long bestMatch = 0;
		for (String l : freqMap.keySet()) {
			int counter = 0;
			for (Integer i : frequentNotes.keySet()) {
				if (freqMap.get(l).contains(i)) {
					counter += frequentNotes.get(i);
				}
			}
			chordMatchesMap.put(l, Long.valueOf(counter));
			if (counter > bestMatch) {
				bestMatch = counter;
			}
		}

		Map<String, Long> orderedBestMatches = chordMatchesMap.entrySet().stream()
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(5)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1,
						LinkedHashMap::new));

		int circleIndex = progressionCircle.indexOf(prevChordString);
		if (circleIndex != -1) {
			final long finalBestMatch = bestMatch;
			long bestMatchCount = orderedBestMatches.values().stream()
					.filter(e -> finalBestMatch == e).count();
			if (bestMatchCount > 1) {
				LG.i(bestMatchCount + " best chords for:  " + bestMatch + " notes.");

				if (bestMatchCount >= orderOfMatch) {
					orderedBestMatches.values().removeIf(e -> e != finalBestMatch);
				}

				String expectedNextChordString = progressionCircle
						.get((circleIndex) + 1 % progressionCircle.size());
				if (orderedBestMatches.containsKey(expectedNextChordString)) {
					LG.i("Circle: " + expectedNextChordString);
					return expectedNextChordString;
				} else {
					LG.i("Circle chord not a best match: " + expectedNextChordString);
				}
			}
		}

		//top3.entrySet().stream().forEach(System.out::println);
		// return n-th most matching chord 
		if (orderedBestMatches.keySet().size() > orderOfMatch - 1) {
			return (String) orderedBestMatches.keySet().toArray()[orderOfMatch - 1];
		}
		LG.i("Only one chord matches? Huh..");
		return (String) orderedBestMatches.keySet().toArray()[0];
	}

	private static Map<String, Set<Integer>> createChordFreqMap(Collection<String> chords) {
		Map<String, Set<Integer>> cfMap = new HashMap<>();
		List<Integer> targetScale = Arrays.asList(ScaleMode.IONIAN.noteAdjustScale);

		for (String ch : chords) {
			int transposeByLetter = targetScale
					.get(CHORD_FIRST_LETTERS.indexOf(ch.substring(0, 1)) - 1);
			if (ch.contains("6")
					|| !isSpiceValid(transposeByLetter, ch.substring(1), targetScale)) {
				continue;
			}
			int[] chord = chordsMap.get(ch);
			if (chord.length <= 4) {
				cfMap.put(ch,
						intArrToList(chord).stream().map(e -> e % 12).collect(Collectors.toSet()));
			}

		}
		return cfMap;
	}

	public static List<Integer> intArrToList(int[] intArr) {
		List<Integer> intList = new ArrayList<Integer>(intArr.length);
		for (int i : intArr) {
			intList.add(i);
		}
		return intList;
	}

	public static List<Integer> chordToPitches(int[] chord) {
		List<Integer> intList = new ArrayList<Integer>(chord.length);
		for (int i : chord) {
			intList.add(i % 12);
		}
		return intList;
	}

	public static boolean isSpiceValid(int transposeByLetter, String spice,
			List<Integer> targetScale) {
		int[] chord = SPICE_CHORDS_LIST.get(SPICE_NAMES_LIST.indexOf(spice));
		for (int i = 0; i < chord.length; i++) {
			if (!targetScale.contains((chord[i] + transposeByLetter) % 12)) {
				return false;
			}
		}
		return true;
	}

	public static int compareNotesByDistanceFromChordPitches(Note n1, Note n2,
			List<Integer> pitches) {
		int dist1 = Math
				.abs((n1.getPitch() % 12) - getClosestFromList(pitches, n1.getPitch() % 12));
		int dist2 = Math
				.abs((n2.getPitch() % 12) - getClosestFromList(pitches, n2.getPitch() % 12));
		return Integer.compare(dist1, dist2);
	}

	public static List<Pair<ScaleMode, Integer>> detectKeyAndMode(Phrase phr, ScaleMode targetMode,
			boolean forceDifferentTranspose) {
		int bestNotContained = Integer.MAX_VALUE;
		ScaleMode bestMode = null;
		int transposeUpBy = 0;

		// 12 pitches
		int[] pitchCounts = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		Set<Integer> pitches = new HashSet<>();
		Vector<Note> noteList = phr.getNoteList();
		int mostFrequents = 0;
		int mostFrequentPitch = -1;
		for (Note n : noteList) {
			if (n.getPitch() <= 0) {
				continue;
			}

			int normalized = n.getPitch() % 12;
			pitches.add(normalized);
			pitchCounts[normalized] += (int) (n.getDuration()
					/ (MidiGenerator.Durations.SIXTEENTH_NOTE / 4.0));
			if (pitchCounts[normalized] > mostFrequents) {
				mostFrequents = pitchCounts[normalized];
				mostFrequentPitch = normalized;
			}
		}
		LG.i("Examining pitches: " + StringUtils.join(pitches, ", "));
		LG.i("# of pitches: " + pitches.size());
		LG.i("Pitch array: " + Arrays.toString(pitchCounts));
		List<Pair<ScaleMode, Integer>> validResults = new ArrayList<>();
		Pair<ScaleMode, Integer> returnPair = null;
		for (ScaleMode mode : ScaleMode.values()) {
			Pair<Integer, Integer> detectionResult = detectKey(pitches, mode.noteAdjustScale,
					forceDifferentTranspose);
			//LG.i("Result for " + mode.toString() + ": " + detectionResult.toString());
			boolean bestForSure = false;


			if (detectionResult.getKey() == 0 && (targetMode != null) && (targetMode == mode)) {
				bestForSure = true;
				bestNotContained = detectionResult.getKey();
				bestMode = mode;
				transposeUpBy = detectionResult.getValue();
				LG.i("Found target mode: " + targetMode.toString());
			}

			if (returnPair == null) {
				if (detectionResult.getKey() == 0
						&& (((mostFrequentPitch + ((12 + detectionResult.getValue()) % 12))
								% 12) == 0)) {
					//LG.i("Best for sure: " + detectionResult.toString());
					bestForSure = true;
				}
				if (detectionResult.getKey() < bestNotContained || bestForSure) {
					bestNotContained = detectionResult.getKey();
					bestMode = mode;
					transposeUpBy = detectionResult.getValue();
				}
			}


			if (bestForSure) {
				returnPair = Pair.of(bestMode, transposeUpBy);
			} else {
				if (detectionResult.getKey() == 0) {
					validResults.add(Pair.of(mode, detectionResult.getValue()));
				}
			}

		}
		if (bestNotContained > 0) {
			return null;
		}
		if (returnPair != null) {
			LG.i("Returning best: " + returnPair.toString());
			validResults.add(returnPair);
			return validResults;
		}

		LG.i("Returning: " + bestMode.toString() + ", " + transposeUpBy);
		return validResults;
	}

	public static Pair<Integer, Integer> detectKey(Set<Integer> pitches, Integer[] scale,
			boolean forceDifferentTranspose) {


		Set<Integer> desiredPitches = new HashSet<>();
		for (int i = 0; i < scale.length; i++) {
			desiredPitches.add(scale[i]);
		}

		int bestNotContained = pitches.size();
		int transposeUpBy = 0;
		for (int i = -6; i <= 6; i++) {
			int notContained = pitches.size();
			for (Integer p : pitches) {
				Integer transposedPitch = (p + i + 12) % 12;
				if (desiredPitches.contains(transposedPitch)) {
					notContained--;
				}
			}
			if (notContained < bestNotContained
					|| (notContained == bestNotContained && forceDifferentTranspose)) {
				bestNotContained = notContained;
				transposeUpBy = i;
			}
			if (notContained == 0 && (!forceDifferentTranspose || (i != 0))) {
				//LG.i("Found best transpose match: " + i);
				break;
			}
		}
		return Pair.of(bestNotContained, transposeUpBy);
	}

	/*
		public static String prettyChord(long chordNum) {
			String chordString = String.valueOf(chordNum);
			int firstNum = Character.digit(chordString.charAt(0), 10);
			String chordLetter = NUM_TO_LETTER.get(firstNum);
			String chordQualifier = "";
			//Long normalizedNum = Long.valueOf(chordNum / firstNum);
			//LG.i("Normalized: " + normalizedNum);
			if (chordNum < 10) {
				return chordLetter;
			} else if (chordNum < 100) {
				return chordLetter + "m";
			} else {
				int numIndex = SPICE_SELECT_LIST.indexOf(Long.valueOf((chordNum / firstNum) / 10L));
				chordQualifier = SPICE_SELECT_PRETTY.get(numIndex);
				return chordLetter + chordQualifier;
			}
		}
	
		public static long unprettyChord(String chord) {
			int firstNum = NUM_TO_LETTER.indexOf(String.valueOf(chord.charAt(0)));
			if (chord.length() == 1) {
				return firstNum;
			}
			if (chord.length() == 2 && chord.charAt(1) == 'm') {
				return firstNum * 10;
			}
			int chordQualifierIndex = SPICE_SELECT_PRETTY.indexOf(chord.substring(1));
			long chordLong = SPICE_SELECT_LIST.get(chordQualifierIndex) * 10;
			return chordLong * firstNum;
	
		}*/

	public static void addShortenedNote(Phrase pr, Note n, double shortenedTo) {
		double rv = n.getRhythmValue();
		n.setRhythmValue(shortenedTo * rv);
		pr.addNote(n);
		if (shortenedTo > 0.999) {
			return;
		}
		pr.addNote(Integer.MIN_VALUE, (1 - shortenedTo) * rv);
	}

	public static int[] transposeChord(int[] chord, int transposeBy) {
		if (transposeBy == 0)
			return chord;
		int[] transposed = Arrays.copyOf(chord, chord.length);
		for (int i = 0; i < chord.length; i++) {
			transposed[i] += transposeBy;
		}
		return transposed;
	}

	public static List<Integer> transposeScale(List<Integer> scale, int transposeBy,
			boolean diatonic) {
		List<Integer> newScale = new ArrayList<>();
		if (diatonic) {
			for (int i = 0; i < 8; i++) {

				if (transposeBy > 0 && i + transposeBy > 7) {
					newScale.add(scale.get((i + transposeBy) % 8) + 12);
				} else if (transposeBy < 0 && i + transposeBy < 0) {
					newScale.add(scale.get((i + transposeBy) % 8) - 12);
				} else {
					newScale.add(scale.get((i + transposeBy) % 8));
				}
			}
			return newScale;
		}
		for (int i = 0; i < 8; i++) {
			newScale.add(scale.get(i) + transposeBy);
		}
		return newScale;
	}

	public static int maX(int value, int x) {
		if (value > x)
			return x;
		if (value < 0)
			return 0;
		return value;
	}

	public static int[] mappedChord(String chordString) {
		int[] mappedChord = getNormalMappedChord(chordString);

		if (mappedChord == null) {
			mappedChord = getSpelledChord(chordString);
		}
		if (mappedChord == null) {
			return null;
			//throw new IllegalArgumentException("Unmappable string: " + chordString);
		}
		return Arrays.copyOf(mappedChord, mappedChord.length);
	}

	public static List<Integer> mappedChordList(String chordString, boolean normalized) {
		int[] chord = mappedChord(chordString);
		if (chord == null) {
			return null;
		} else {
			if (normalized) {
				chord = normalizeChord(chord);
			}
			List<Integer> chordList = new ArrayList<>();
			for (int num : chord) {
				chordList.add(num);
			}
			return chordList;
		}
	}

	public static int[] getNormalMappedChord(String chordString) {
		int len = chordString.length();
		Integer inversion = null;
		int dotIndex = chordString.indexOf(".");
		if (len >= 3 && dotIndex >= 0) {
			inversion = Integer.valueOf(chordString.substring(dotIndex + 1));
			chordString = chordString.substring(0, dotIndex);
		}

		int[] mappedChord = null;
		if (chordString.length() >= 2 && "#".equals(chordString.substring(1, 2))) {
			String testChordString = chordString;
			testChordString = testChordString.replaceFirst("#", "");
			mappedChord = chordsMap.get(testChordString);
			if (mappedChord != null) {
				mappedChord = Arrays.copyOf(mappedChord, mappedChord.length);
				for (int i = 0; i < mappedChord.length; i++) {
					mappedChord[i] = mappedChord[i] + 1;
				}

				if (inversion != null) {
					return chordInversion(mappedChord, inversion);
				}
				return mappedChord;
			}
		}

		mappedChord = chordsMap.get(chordString);
		if (mappedChord == null) {
			return null;
		}
		if (inversion != null) {
			return chordInversion(mappedChord, inversion);
		}
		return Arrays.copyOf(mappedChord, mappedChord.length);
	}

	public static int[] chordInversion(int[] chord, Integer inversion) {
		if (inversion == null) {
			return chord;
		}
		int index = (inversion + chord.length * 100) % chord.length;
		if (index == 0) {
			return chord;
		}
		// % safeguard
		//inversion = (inversion + chord.length * 100) % chord.length;
		int[] newChord = new int[chord.length];

		for (int i = 0; i < chord.length; i++) {
			newChord[i] = chord[i];
			if (inversion > 0 && i < index) {
				newChord[i] += 12;
			} else if (inversion < 0 && i >= index) {
				newChord[i] -= 12;
			}
		}
		Arrays.sort(newChord);
		return newChord;

	}

	public static int[] convertChordToLength(int[] chord, int length) {
		return convertChordToLength(chord, length, true);
	}

	public static int[] convertChordToLength(int[] chord, int length, boolean conversionNeeded) {
		int[] chordCopy = Arrays.copyOf(chord, chord.length);

		if (!conversionNeeded || chord.length == length) {
			return chordCopy;
		}
		int[] converted = new int[length];
		if (chord.length < length) {
			// repeat from start with +12 transpose

			// adjustment -> C4 E G B D5 -> convert to 6 length -> last note must be C6 not C5
			int octaveAdjustment = 1;
			if (Math.abs(chordCopy[chord.length - 1] - chordCopy[0]) >= 12) {
				octaveAdjustment += Math.abs(chordCopy[chord.length - 1] - chordCopy[0]) / 12;
			}
			for (int i = 0; i < length; i++) {
				int pitch = chordCopy[(i % chord.length)];
				converted[i] = pitch + 12 * (i / chord.length) * octaveAdjustment;
			}
		} else {
			// alternate from beginning and end
			int filled = 0;
			int frontIndex = 0;
			int backIndex = 0;
			while (filled < length) {
				if (filled % 2 == 0) {
					converted[frontIndex] = chordCopy[frontIndex];
					frontIndex++;
				} else {
					converted[length - backIndex - 1] = chordCopy[chord.length - backIndex - 1];
					backIndex++;
				}
				filled++;
			}
		}
		return converted;
	}

	public static Integer getXthChordNote(int x, int[] chord) {
		//LG.i(StringUtils.join(chord, ','));
		int octaveMultiplier = 1;
		int pitch = chord[((x + 10 * chord.length) % chord.length)];
		int octaveAdjust = 0;
		if (x >= chord.length) {
			octaveAdjust = 12 * (x / chord.length);
		} else if (x < 0) {
			octaveAdjust = -12 + 12 * (x / chord.length);
		}
		if (Math.abs(chord[chord.length - 1] - chord[0]) >= 12) {
			octaveMultiplier += Math.abs(chord[chord.length - 1] - chord[0]) / 12;
		}
		Integer note = pitch + octaveAdjust * octaveMultiplier;
		//LG.i("Note: " + note);
		return (note <= 0 || note >= 127) ? null : note;
	}

	public static int getStandardizedPitch(int pitch, int scaleTranspose, int tolerance) {
		int result = pitch;
		int lowBound = Pitches.C4 + scaleTranspose - tolerance;
		int highBound = Pitches.C5 + scaleTranspose + tolerance;

		while (result > highBound) {
			result -= 12;
		}
		while (result < lowBound) {
			result += 12;
		}
		return result;
	}

	public static List<Integer> extendScaleByOctaveUpDown(List<Integer> scale) {
		List<Integer> extended = new ArrayList<>();
		extended.addAll(transposeScale(scale, -12, false));
		extended.addAll(transposeScale(scale, 0, false));
		extended.addAll(transposeScale(scale, 12, false));
		return extended;
	}

	public static double pickDurationWeightedRandom(Random generator, double durationLeft,
			double[] durs, double[] chances, double defaultValue) {
		if (durs.length != chances.length) {
			return defaultValue;
		}
		double rnd = generator.nextDouble();
		for (int i = 0; i < durs.length; i++) {
			if (rnd < chances[i] && durationLeft >= durs[i]) {
				return durs[i];
			}
		}
		return defaultValue;
	}

	public static double calculateAverageNote(List<int[]> chords) {
		double noteCount = 0.001;
		double noteSum = 0;
		for (int[] c : chords) {
			noteCount += c.length;
			for (int i = 0; i < c.length; i++) {
				noteSum += c[i];
			}
		}

		return noteSum / noteCount;
	}

	public static List<int[]> getBasicChordsFromRoots(List<int[]> roots) {
		List<Integer> majorScaleNormalized = MAJ_SCALE;
		List<int[]> basicChords = new ArrayList<>();
		for (int[] r : roots) {
			int index = majorScaleNormalized.indexOf(r[0] % 12);
			if (index == -1) {
				index = majorScaleNormalized.indexOf((r[0] + 11) % 12);
			}
			String chordLong = MAJOR_CHORDS.get(index);
			basicChords.add(mappedChord(chordLong));
		}
		return basicChords;
	}

	public static List<String> getBasicChordStringsFromRoots(List<int[]> roots) {
		List<Integer> majorScaleNormalized = MAJ_SCALE;
		List<String> basicChords = new ArrayList<>();
		for (int[] r : roots) {
			int index = majorScaleNormalized.indexOf(r[0] % 12);
			String chordLong = MAJOR_CHORDS.get(index);
			basicChords.add(chordLong);
		}
		return basicChords;
	}

	public static List<int[]> squishChordProgression(List<int[]> chords, boolean squishBigChords,
			long seed, int chance) {
		Random r = new Random(seed);
		double avg = calculateAverageNote(chords);
		//LG.i("AVG: " + avg);

		List<int[]> squishedChords = new ArrayList<>();
		for (int i = 0; i < chords.size(); i++) {
			int[] c = Arrays.copyOf(chords.get(i), chords.get(i).length);
			if (r.nextInt(100) < chance && (c.length <= 3 || squishBigChords)) {
				if (avg - c[0] > 6) {
					c[0] += 12;
					//LG.i("SWAP UP: " + i);
				}
				if (c[c.length - 1] - avg > 6) {
					c[c.length - 1] -= 12;
					//LG.i("SWAP DOWN: " + i);
				}
			}

			Arrays.sort(c);
			squishedChords.add(c);
		}
		//LG.i("NEW AVG: " + calculateAverageNote(squishedChords));
		return squishedChords;
	}

	public static List<int[]> squishChordProgressionProgressively(List<int[]> chords,
			boolean squishBigChords, long seed, int chance) {
		Random r = new Random(seed);
		double avg = calculateAverageNote(chords.subList(0, 1));
		//LG.i("AVG: " + avg);

		List<int[]> squishedChords = new ArrayList<>();
		for (int i = 0; i < chords.size(); i++) {
			int[] c = Arrays.copyOf(chords.get(i), chords.get(i).length);
			Arrays.sort(c);
			if (r.nextInt(100) < chance && (c.length <= 3 || squishBigChords)) {
				if (avg - c[0] > 6) {
					c[0] += 12;
					//LG.i("SWAP UP: " + i);
				}
				if (c[c.length - 1] - avg > 6) {
					c[c.length - 1] -= 12;
					//LG.i("SWAP DOWN: " + i);
				}
			}

			Arrays.sort(c);
			squishedChords.add(c);
			avg = calculateAverageNote(squishedChords);
		}
		//LG.i("NEW AVG: " + calculateAverageNote(squishedChords));
		return squishedChords;
	}

	public static int[] transposeChord(int[] chord, final Integer[] mode, final Integer[] modeTo) {
		int[] transposedChord = new int[chord.length];

		List<Integer> modeList = new ArrayList<>();
		for (int num : mode) {
			modeList.add(num);
		}
		List<Integer> modeToList = new ArrayList<>();
		for (int num : modeTo) {
			modeToList.add(num);
		}

		for (int j = 0; j < chord.length; j++) {
			int pitch = chord[j];
			int searchPitch = Integer.valueOf(pitch % 12);
			int originalIndex = modeList.indexOf(searchPitch);

			if (originalIndex == -1) {
				if (modeToList.contains(searchPitch)) {
					//LG.i("Pitch found only in modeTo, not changing: " + pitch);
				} else {
					int closestPitch = getClosestFromList(modeToList, searchPitch);
					int difference = searchPitch - closestPitch;
					transposedChord[j] = pitch - difference;
					/*LG.i(
							"Not indexed pitch.. " + pitch + ", lowered by.. " + difference);*/
				}
				continue;
			}


			int originalMovement = mode[originalIndex];
			int newMovement = modeTo[originalIndex];

			if (pitch != Note.REST) {
				transposedChord[j] = pitch - originalMovement + newMovement;
			}
		}
		return transposedChord;
	}

	public static void transposePhrase(Phrase phr, final Integer[] mode, final Integer[] modeTo) {
		transposeNotes(phr.getNoteList(), mode, modeTo);
	}

	public static void transposeNotes(List<Note> notes, final Integer[] mode,
			final Integer[] modeTo) {
		List<Integer> modeList = new ArrayList<>();
		for (int num : mode) {
			modeList.add(num);
		}

		List<Integer> modeToList = new ArrayList<>();
		for (int num : modeTo) {
			modeToList.add(num);
		}


		for (int j = 0; j < notes.size(); j++) {
			Note n = notes.get(j);
			int pitch = n.getPitch();
			if (pitch < 0) {
				continue;
			}
			int searchPitch = Integer.valueOf(pitch % 12);
			int originalIndex = modeList.indexOf(searchPitch);

			if (originalIndex == -1) {
				if (modeToList.contains(searchPitch)) {
					//LG.i("Pitch found only in modeTo, not changing: " + pitch);
				} else {
					int closestPitch = getClosestFromList(modeToList, searchPitch);
					int difference = searchPitch - closestPitch;
					n.setPitch(pitch - difference);
					/*LG.i(
							"Not indexed pitch.. " + pitch + ", lowered by.. " + difference);*/
				}
				continue;
			}


			int originalMovement = mode[originalIndex];
			int newMovement = modeTo[originalIndex];

			n.setPitch(pitch - originalMovement + newMovement);
		}
	}

	public static int getClosestFromList(List<Integer> list, int valToFind) {
		if (list == null || list.isEmpty()) {
			return Integer.MIN_VALUE;
		}
		int closest = list.get(0);
		int closestDistance = Math.abs(valToFind - closest);
		for (int i = 1; i < list.size(); i++) {
			int distance = Math.abs(valToFind - list.get(i));
			if (distance < closestDistance) {
				closestDistance = distance;
				closest = list.get(i);
			}
		}
		return closest;
	}

	public static int[] getSpelledChord(String chordString) {
		List<Character> validChars = Arrays
				.asList(new Character[] { '#', 'C', 'D', 'E', 'F', 'G', 'A', 'B' });
		boolean expectOnlyLetter = true;
		List<Integer> intervalInts = new ArrayList<>();
		int currentOctaveAdjust = 0;
		int lastValue = 0;
		int current = -1;
		for (int i = 0; i < chordString.length(); i++) {
			Character chr = chordString.charAt(i);
			int index = validChars.indexOf(chr);
			if (index > 0) {
				current++;
				int newValue = 60 + diaTransMap.get(index) + currentOctaveAdjust;
				// smaller than last, or equal but next char isn't #
				if (newValue < lastValue
						|| ((newValue == lastValue) && (i == chordString.length() - 1
								|| validChars.indexOf(chordString.charAt(i + 1)) != 0))) {
					currentOctaveAdjust += 12;
					newValue += 12;
				}
				intervalInts.add(newValue);
				lastValue = newValue;
				expectOnlyLetter = false;
			} else if (index == 0) {
				if (expectOnlyLetter) {
					return null;
				} else {
					lastValue = intervalInts.get(current) + 1;
					intervalInts.set(current, lastValue);
					expectOnlyLetter = true;
				}
			} else {
				return null;
			}
		}
		return intervalInts.stream().mapToInt(i -> i).toArray();
	}

	public static String makeSpelledChord(int[] chord) {
		String spelledChord = "";
		for (int i : chord) {
			spelledChord += SEMITONE_LETTERS.get(i % 12);
		}
		return spelledChord;
	}


	public static <T> T getRandom(Random generator, T[] array) {
		return getRandom(generator, array, 0, array.length);
	}

	public static <T> T getRandom(Random generator, T[] array, int from, int to) {
		from = Math.max(from, 0);
		to = Math.min(to, array.length);
		return array[generator.nextInt(to - from) + from];
	}

	public static List<Integer> intersperse(Integer number, int times, List<Integer> list) {
		List<Integer> interspersed = new ArrayList<>();
		for (Integer i : list) {
			interspersed.add(i);
			for (int j = 0; j < times; j++) {
				interspersed.add(number != null ? number : i);
			}
		}
		return interspersed;
	}

	public static void addChordsToPhrase(Phrase phr, List<Chord> chords, double flam) {
		for (Chord c : chords) {
			c.setFlam(flam);
			Note[] notes = c.getNotesBackwards().toArray(new Note[] {});
			//Note lastNote = notes[notes.length - 1];
			//lastNote.setDuration(lastNote.getDuration() * 3);
			phr.addNoteList(notes);
		}
	}

	public static List<Chord> convertChordStringsToChords(List<String> chordStrings) {
		List<Chord> chords = new ArrayList<>();
		for (String s : chordStrings) {
			Chord c = Chord.EMPTY(MidiGenerator.Durations.WHOLE_NOTE);
			int[] mapped = mappedChord(s);
			if (mapped == null)
				return null;

			c.setNotes(mapped);
			chords.add(c);
		}
		return chords;
	}

	public static List<Pair<ScaleMode, Integer>> getKeyModesForChordsAndTarget(String rawChords,
			ScaleMode targetMode) {
		List<String> rawChordsList = Arrays.asList(rawChords.replaceAll(" ", "").split(","));
		List<Chord> chords = convertChordStringsToChords(rawChordsList);
		if (chords == null) {
			return null;
		}
		Phrase phr = new Phrase();
		addChordsToPhrase(phr, chords, 0.125);

		List<Pair<ScaleMode, Integer>> detectionResults = detectKeyAndMode(phr, targetMode, true);
		return detectionResults;
	}

	public static List<String> processRawChords(String rawChords, ScaleMode targetMode) {
		List<String> rawChordsList = Arrays.asList(rawChords.replaceAll(" ", "").split(","));
		List<Chord> chords = convertChordStringsToChords(rawChordsList);
		if (chords == null) {
			return null;
		}
		List<Pair<ScaleMode, Integer>> detectionResults = getKeyModesForChordsAndTarget(rawChords,
				targetMode);
		if (detectionResults == null) {
			return null;
		}

		Pair<ScaleMode, Integer> detectionResult = detectionResults
				.get(detectionResults.size() - 1);

		int transposeUpBy = detectionResult.getValue();
		if (transposeUpBy != 0) {
			for (Chord c : chords) {
				c.setNotes(transposeChord(c.getNotes(), transposeUpBy));
			}
		}
		if (detectionResult.getKey() != targetMode) {
			return null;
			/*for (Chord c : chords) {
				c.setNotes(transposeChord(c.getNotes(),
						detectionResult.getKey().noteAdjustScale,
						ScaleMode.IONIAN.noteAdjustScale));
			}*/
		}
		for (Chord c : chords) {
			c.setNotes(transposeChord(c.getNotes(), detectionResult.getKey().noteAdjustScale,
					ScaleMode.IONIAN.noteAdjustScale));
		}


		List<String> solvedChords = new ArrayList<>();
		List<Integer> majorScaleNormalized = MAJ_SCALE;
		String firstLetterFirstChord = rawChords.substring(0, 1);
		int firstPitchFirstChord = majorScaleNormalized
				.get(CHORD_FIRST_LETTERS.indexOf(firstLetterFirstChord) - 1);
		if (rawChords.length() > 1 && "#".equals(rawChords.substring(1, 2))) {
			firstPitchFirstChord++;
		}

		for (Chord c : chords) {
			int[] notes = c.getNotes();

			// C,Fsus2,G,Am
			LG.i("Pitches: " + StringUtils.join(notes, ','));
			boolean solved = false;
			for (int i = 0; i < notes.length; i++) {
				int firstPitch = notes[i] % 12;
				int index = majorScaleNormalized.indexOf(firstPitch);
				if (index < 0) {
					return null;
				}

				String firstLetter = CHORD_FIRST_LETTERS.get(index + 1);
				for (String spice : SPICE_NAMES_LIST) {
					String combinedChord = firstLetter + spice;
					int[] mapped = mappedChord(combinedChord);
					if (Arrays.equals(normalizeChord(mapped), normalizeChord(notes))) {
						solvedChords.add(combinedChord);
						solved = true;
						break;
					}
				}
				if (solved)
					break;
			}
		}


		LG.i(solvedChords.toString());
		if (solvedChords.size() == chords.size()) {
			String firstletterFirstSolvedChord = solvedChords.get(0).substring(0, 1);
			int firstPitchFirstSolvedChord = majorScaleNormalized
					.get(CHORD_FIRST_LETTERS.indexOf(firstletterFirstSolvedChord) - 1);
			if (firstPitchFirstChord > firstPitchFirstSolvedChord && transposeUpBy > 0) {
				//transposeUpBy -= 12;
			} else if (firstPitchFirstChord < firstPitchFirstSolvedChord && transposeUpBy < 0) {
				//transposeUpBy += 12;
			}
			VibeComposerGUI.transposeScore
					.setInt(VibeComposerGUI.transposeScore.getInt() + (transposeUpBy * -1));
			//VibeComposerGUI.scaleMode.setVal(detectionResult.getKey().toString());
			return solvedChords;
		} else {
			return null;
		}

	}

	public static int[] normalizeChord(int[] chord) {
		int[] returnChord = Arrays.copyOf(chord, chord.length);
		for (int i = 0; i < returnChord.length; i++) {
			returnChord[i] = returnChord[i] % 12;
		}
		Arrays.sort(returnChord);
		return returnChord;
	}

	public static String getNoteForPitch(int pitch) {
		if (pitch < 0) {
			return "";
		}
		int pitchNormalized = pitch % 12;
		List<Integer> majorLetterPitches = MAJ_SCALE;
		int chordLetter = majorLetterPitches.indexOf(pitchNormalized);
		if (chordLetter < 0) {
			chordLetter = majorLetterPitches.indexOf(pitchNormalized - 1);
			String realLetter = CHORD_FIRST_LETTERS.get(chordLetter + 1) + "#";
			return realLetter + pitch / 12;
		} else {
			String realLetter = CHORD_FIRST_LETTERS.get(chordLetter + 1);
			return realLetter + pitch / 12;
		}
	}

	public static List<String> respiceChords(String chordsString, GUIConfig gc) {
		List<String> allowedSpiceChordsMiddle = new ArrayList<>();
		for (int i = 2; i < SPICE_NAMES_LIST.size(); i++) {
			String chordString = SPICE_NAMES_LIST.get(i);
			if (!gc.isDimAugDom7thEnabled() && BANNED_DIM_AUG_6_LIST.contains(chordString)) {
				continue;
			}
			if (!gc.isEnable9th13th() && BANNED_9_13_LIST.contains(chordString)) {
				continue;
			}
			allowedSpiceChordsMiddle.add(chordString);
		}

		List<String> allowedSpiceChords = new ArrayList<>();
		for (String s : allowedSpiceChordsMiddle) {
			if (BANNED_DIM_AUG_6_LIST.contains(s) || BANNED_SUSSY_LIST.contains(s)) {
				continue;
			}
			allowedSpiceChords.add(s);
		}

		Random rand = new Random();

		List<String> chordsList = Arrays.asList(chordsString.replaceAll(" ", "").split(","));
		List<String> respicedChordsList = new ArrayList<>();
		for (int i = 0; i < chordsList.size(); i++) {
			String chord = chordsList.get(i);
			boolean mapped = getNormalMappedChord(chord) != null;
			if (mapped) {
				//LG.i("Mapped!");
				String firstLetter = chord.substring(0, 1);

				int firstIndex = CHORD_FIRST_LETTERS.indexOf(firstLetter);
				String baseChord = MAJOR_CHORDS.get(firstIndex - 1);
				List<String> spicyChordList = (i > 0 && i < chordsList.size() - 1)
						? allowedSpiceChordsMiddle
						: allowedSpiceChords;

				if (rand.nextInt(100) >= gc.getSpiceChance()) {
					respicedChordsList.add(baseChord);
				} else {
					String spicyChordString = firstLetter
							+ spicyChordList.get(rand.nextInt(spicyChordList.size()));
					if (baseChord.endsWith("m") && spicyChordString.contains("maj")) {
						spicyChordString = spicyChordString.replace("maj", "m");
					} else if (baseChord.length() == 1 && spicyChordString.contains("m")
							&& !spicyChordString.contains("dim")
							&& !spicyChordString.contains("maj")) {
						spicyChordString = spicyChordString.replace("m", "maj");
					}


					if (chord.length() > 1 && chord.substring(1, 2).equals("#")) {
						// insert at index 1
						if (spicyChordString.length() > 2) {
							spicyChordString = spicyChordString.substring(0, 1) + "#"
									+ spicyChordString.substring(1, spicyChordString.length());
						} else {
							spicyChordString = spicyChordString.substring(0, 1) + "#";
						}

					}
					respicedChordsList.add(spicyChordString);
				}
			} else {
				//LG.i("Not mapped!");
				respicedChordsList.add(chord);
			}
		}
		LG.i("Returning respiced chords: " + respicedChordsList.toString());
		return respicedChordsList;
	}

	public static Set<Integer> avoidNotesFromChord(int[] chord, int notesToAvoid) {
		Set<Integer> avoidNotes = new HashSet<>();
		Set<Integer> safeNotes = new HashSet<>();
		safeNotes.add(0);
		safeNotes.add(7);
		for (int i = 0; i < chord.length; i++) {
			if (notesToAvoid >= 1) {
				avoidNotes.add((chord[i] + 1) % 12);
			}
			if (notesToAvoid >= 2) {
				avoidNotes.add((chord[i] + 11) % 12);
				avoidNotes.add((chord[i] + 2) % 12);
			}
			safeNotes.add(chord[i] % 12);
		}
		avoidNotes.removeAll(safeNotes);
		return avoidNotes;
	}

	public static Integer[] adjustScaleByChord(Integer[] noteAdjustScale, int[] chord) {
		Integer[] adjustedScale = Arrays.copyOf(noteAdjustScale, noteAdjustScale.length);
		int[] normalizedChord = normalizeChord(chord);
		for (int i = 0; i < normalizedChord.length; i++) {
			boolean changedNote = MAJ_SCALE.indexOf(normalizedChord[i]) == -1;
			if (changedNote) {
				int indexToChange = MIN_SCALE.indexOf(normalizedChord[i]);
				adjustedScale[indexToChange] = normalizedChord[i];
				LG.d("Changed at index: " + indexToChange + ", to: " + normalizedChord[i]);
			}

		}
		return adjustedScale;
	}

}
