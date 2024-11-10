package org.vibehistorian.vibecomposer;

import jm.constants.Pitches;
import jm.music.data.Note;
import jm.music.data.Phrase;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.vibehistorian.vibecomposer.Enums.BlockType;
import org.vibehistorian.vibecomposer.Helpers.PartPhraseNotes;
import org.vibehistorian.vibecomposer.Helpers.PhraseExt;
import org.vibehistorian.vibecomposer.Helpers.PhraseNote;
import org.vibehistorian.vibecomposer.Helpers.PhraseNotes;
import org.vibehistorian.vibecomposer.Parts.MelodyPart;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.vibehistorian.vibecomposer.MidiGenerator.DBL_ERR;
import static org.vibehistorian.vibecomposer.MidiUtils.*;

public class MelodyGenerator {

    // shared constants
    public static final int EMBELLISHMENT_CHANCE = 20;
    public static final int maxAllowedScaleNotes = 7;

    // internal only
    private List<Integer> MELODY_SCALE = cIonianScale4;
    private int samePitchCount = 0;
    private int previousPitch = 0;

    // pass in from outside
    private final GUIConfig gc;
    private MidiGenerator mg;

    // freely use via object
    public Map<Integer, List<Note>> chordMelodyMap1 = new HashMap<>();
    public List<int[]> melodyBasedChordProgression = new ArrayList<>();
    public List<int[]> melodyBasedRootProgression = new ArrayList<>();
    public String alternateChords = null;

    // shared - freely use anywhere
    public static Map<Integer, List<Integer>> TARGET_NOTES = null;
    public static boolean RANDOMIZE_TARGET_NOTES = false;
    public static Phrase userMelody = null;
    public static double[] MELODY_SKELETON_DURATIONS_SHORT = { MidiGenerator.Durations.SIXTEENTH_NOTE / 2.0,
            MidiGenerator.Durations.SIXTEENTH_NOTE, MidiGenerator.Durations.EIGHTH_NOTE, MidiGenerator.Durations.DOTTED_EIGHTH_NOTE,
            MidiGenerator.Durations.QUARTER_NOTE, MidiGenerator.Durations.DOTTED_QUARTER_NOTE, MidiGenerator.Durations.HALF_NOTE };

    public static double[] MELODY_SKELETON_DURATIONS = { MidiGenerator.Durations.SIXTEENTH_NOTE,
            MidiGenerator.Durations.EIGHTH_NOTE, MidiGenerator.Durations.DOTTED_EIGHTH_NOTE, MidiGenerator.Durations.QUARTER_NOTE,
            MidiGenerator.Durations.DOTTED_QUARTER_NOTE, MidiGenerator.Durations.HALF_NOTE };


    public MelodyGenerator(GUIConfig gc, MidiGenerator mg) {
        this.gc = gc;
        this.mg = mg;
    }

    public Map<Integer, List<Note>> makeFullMelodyMap(MelodyPart ip, List<int[]> actualProgression,
                                     List<int[]> generatedRootProgression, int notesSeedOffset, Section sec,
                                     List<Integer> variations, List<Integer> melodyBlockJumpPreference) {
        LG.d("Processing: " + ip.partInfo());
        Phrase phr = new PhraseExt(0, ip.getOrder(), mg.secOrder);

        int measures = sec.getMeasures();

        Vector<Note> skeletonNotes = null;
        if (userMelody != null) {
            skeletonNotes = (Vector<Note>) userMelody.copy().getNoteList();
        } else {
            if (gc.isMelodyLegacyMode()) {
                LG.i("OLD MELODY ALGO");
                skeletonNotes = algoGen2GenerateMelodySkeletonFromChords(ip, actualProgression,
                        generatedRootProgression, measures, notesSeedOffset, sec, variations);
            } else {
                skeletonNotes = generateMelodyBlockSkeletonFromChords(ip, actualProgression,
                        generatedRootProgression, measures, notesSeedOffset, sec, variations, melodyBlockJumpPreference);
            }

        }
        Map<Integer, List<Note>> fullMelodyMap = convertMelodySkeletonToFullMelody(ip,
                mg.progressionDurations, sec, skeletonNotes, notesSeedOffset, actualProgression,
                measures);

        for (int i = 0; i < generatedRootProgression.size() * measures; i++) {
            for (int j = 0; j < MidiUtils.MINOR_CHORDS.size(); j++) {
                int[] minorChord = MidiUtils.mappedChord(MidiUtils.MINOR_CHORDS.get(j));
                boolean isMinor = Arrays.equals(MidiUtils.normalizeChord(minorChord),
                        MidiUtils.normalizeChord(
                                generatedRootProgression.get(i % generatedRootProgression.size())));
                if (isMinor) {
                    MidiUtils.transposeNotes(fullMelodyMap.get(i), MidiUtils.ScaleMode.IONIAN.noteAdjustScale,
                            MidiUtils.adjustScaleByChord(MidiUtils.ScaleMode.IONIAN.noteAdjustScale,
                                    minorChord),
                            MidiGenerator.gc.isTransposedNotesForceScale());
                    LG.d("Transposing melody to match minor chord! Chord#: " + i);
                    break;
                }
            }
        }
        return fullMelodyMap;
    }

    protected Vector<Note> generateMelodyBlockSkeletonFromChords(MelodyPart mp, List<int[]> chords,
                                                                 List<int[]> roots, int measures, int notesSeedOffset, Section sec,
                                                                 List<Integer> variations, List<Integer> melodyBlockJumpPreference) {

        boolean genVars = variations == null;

        boolean fillChordMelodyMap = false;
        if (chordMelodyMap1.isEmpty() && notesSeedOffset == 0
                && (roots.size() == MidiGenerator.chordInts.size())) {
            fillChordMelodyMap = true;
        }

        if (sec.getSectionVariations() != null && sec.isSectionVar(2)) {
            notesSeedOffset += 100;
        }

        int MAX_JUMP_SKELETON_CHORD = mp.getBlockJump();
        int SAME_RHYTHM_CHANCE = mp.getDoubledRhythmChance();
        int EXCEPTION_CHANCE = mp.getNoteExceptionChance();
        int CHORD_STRETCH = 4;
        int BLOCK_TARGET_MODE = gc.getMelodyBlockTargetMode();

        int seed = mp.getPatternSeedWithPartOffset();
        int melodyBlockGeneratorSeed = seed + notesSeedOffset;
        LG.d("Seed: " + seed);


        // A B A C pattern
        List<Integer> blockSeedOffsets = (mp.getMelodyPatternOffsets() != null)
                ? mp.getMelodyPatternOffsets()
                : new ArrayList<>(Arrays.asList(1, 2, 1, 3));

        while (blockSeedOffsets.size() < chords.size()) {
            blockSeedOffsets.addAll(blockSeedOffsets);
        }

        Map<Integer, Pair<Pair<List<Integer>, Integer>, List<MelodyBlock>>> changesAndBlocksMap = new HashMap<>();
        Map<Integer, List<Double>> blockDurationsMap = new HashMap<>();


        // Chord note choices
        List<Integer> blockChordNoteChoices;
        if (TARGET_NOTES == null) {
            TARGET_NOTES = new HashMap<>();
        }
        if (RANDOMIZE_TARGET_NOTES) {
            if (gc.getActualArrangement().getSections().indexOf(sec) < 1) {
                int targetNoteSeed = VibeComposerGUI.vibeComposerGUI.melody1ForcePatterns.isSelected()
                        ? (seed + 1)
                        : (seed + mp.getOrder());
                blockChordNoteChoices = MidiGeneratorUtils.generateNoteTargetOffsets(roots, targetNoteSeed,
                        gc.getMelodyBlockTargetMode(), gc.getMelodyTargetNoteVariation(), gc.getNoteTargetDirectionChoice());
            } else {
                blockChordNoteChoices = TARGET_NOTES.get(mp.getOrder());
            }
        } else {
            blockChordNoteChoices = (mp.getChordNoteChoices() != null)
                    ? mp.getChordNoteChoices()
                    : new ArrayList<>(Arrays.asList(0, 2, 2, 4));

            while (chords.size() > blockChordNoteChoices.size()) {
                blockChordNoteChoices.addAll(blockChordNoteChoices);
            }
        }
        TARGET_NOTES.put(mp.getOrder(), blockChordNoteChoices);
        LG.d("Choices: " + blockChordNoteChoices);

        Vector<Note> noteList = new Vector<>();

        // if notes seed offset > 0, add it only to one of: rhythms, pitches
        //Random nonMainMelodyGenerator = new Random(seed + 30);
        int pitchPickerOffset = notesSeedOffset;
        int rhythmOffset = notesSeedOffset;

        int firstBlockOffset = Math.abs(blockSeedOffsets.get(0));
        if (firstBlockOffset > 0) {
            firstBlockOffset--;
        }
        List<Integer> usedMelodyBlockJumpPreference = (melodyBlockJumpPreference != null) && (melodyBlockJumpPreference.size() == MelodyUtils.BLOCK_CHANGE_JUMP_PREFERENCE.size())
                && MelodyUtils.BLOCK_CHANGE_JUMP_PREFERENCE.containsAll(melodyBlockJumpPreference)
                ? melodyBlockJumpPreference :
                Collections.emptyList();

        Random pitchPickerGenerator = new Random(seed + pitchPickerOffset + firstBlockOffset);
        Random exceptionGenerator = new Random(melodyBlockGeneratorSeed + 2 + firstBlockOffset);
        Random sameRhythmGenerator = new Random(seed + 3 + firstBlockOffset);
        Random alternateRhythmGenerator = new Random(seed + 4);
        Random durationGenerator = new Random(melodyBlockGeneratorSeed + 5);
        Random embellishmentGenerator = new Random(melodyBlockGeneratorSeed + 10);
        Random soloGenerator = new Random(melodyBlockGeneratorSeed + 25);
        //Random surpriseGenerator = new Random(seed + notesSeedOffset + 15);

        double[] melodySkeletonDurations = { MidiGenerator.Durations.QUARTER_NOTE, MidiGenerator.Durations.HALF_NOTE,
                MidiGenerator.Durations.DOTTED_HALF_NOTE, MidiGenerator.Durations.WHOLE_NOTE };

        List<int[]> usedChords = null;
        if (gc.isMelodyBasicChordsOnly()) {
            List<int[]> basicChordsUnsquished = getBasicChordsFromRoots(roots);
            for (int i = 0; i < chords.size(); i++) {
                basicChordsUnsquished.set(i,
                        convertChordToLength(basicChordsUnsquished.get(i), chords.get(i).length));
            }
            usedChords = basicChordsUnsquished;
        } else {
            usedChords = chords;
        }

        //List<int[]> stretchedChords = usedChords.stream()
        //		.map(e -> convertChordToLength(e, CHORD_STRETCH)).collect(Collectors.toList());
        //LG.d("Alt: " + alternateRhythm);
        MidiUtils.ScaleMode scale = (mg.modScale != null) ? mg.modScale : gc.getScaleMode();
        List<Integer> emphasizeKeyNoteOrder = new ArrayList<>(MidiUtils.keyEmphasisOrder);
        if (scale != null && scale.modeTargetNote >= 0) {
            Integer ionianTargetNote = MidiUtils.MAJ_SCALE.get(scale.modeTargetNote);
            emphasizeKeyNoteOrder.remove(ionianTargetNote);
            emphasizeKeyNoteOrder.add(1, ionianTargetNote);
        }
        int maxBlockChangeAdjustment = 0;
        boolean embellish = false;
        for (int o = 0; o < measures; o++) {

            for (int chordIndex = 0; chordIndex < usedChords.size(); chordIndex++) {
                // either after first measure, or after first half of combined chord prog

                if (genVars && (chordIndex == 0)) {
                    variations = MidiGenerator.fillVariations(sec, mp, variations, 0);
                    // never generate MaxJump for important melodies
                    if ((variations != null) && sec.getTypeMelodyOffset() == 0) {
                        variations.removeIf(e -> e == 1);
                    }
                }

                if ((variations != null) && (chordIndex == 0)) {
                    for (Integer var : variations) {
                        if (o == measures - 1) {
                            LG.d("Melody variation: " + var);
                        }

                        switch (var) {
                            case 0:
                                // transpose - only add, processed later
                                break;
                            case 1:
                                maxBlockChangeAdjustment++;
                                break;
                            case 2:
                                embellish = true;
                                break;
                            case 3:
                                // solo (also processed later)
                                SAME_RHYTHM_CHANCE = 100;
                                List<Integer> soloTargetPattern = MelodyUtils.SOLO_MELODY_PATTERNS.get(
                                        soloGenerator.nextInt(MelodyUtils.SOLO_MELODY_PATTERNS.size()));
                                blockSeedOffsets = new ArrayList<>(soloTargetPattern);
                                LG.i("Chosen solo pattern: "
                                        + StringUtils.join(soloTargetPattern, ","));

                                while (blockSeedOffsets.size() < chords.size()) {
                                    blockSeedOffsets.addAll(blockSeedOffsets);
                                }
                                break;
                            default:
                                throw new IllegalArgumentException("Too much variation!");
                        }
                    }
                }

                if (false) {
                    new Object() {

                    };
                }

                int blockOffset = blockSeedOffsets.get(chordIndex % blockSeedOffsets.size());
                int originalBlockOffset = blockOffset;
                blockOffset = Math.abs(blockOffset);

                if (blockOffset > 0) {
                    blockOffset--;
                }

                if (fillChordMelodyMap && o == 0) {
                    if (!chordMelodyMap1.containsKey(Integer.valueOf(chordIndex))) {
                        chordMelodyMap1.put(Integer.valueOf(chordIndex), new ArrayList<>());
                    }
                }
                pitchPickerGenerator.setSeed(seed + pitchPickerOffset + blockOffset);
                exceptionGenerator.setSeed(seed + 2 + notesSeedOffset + blockOffset);
                sameRhythmGenerator.setSeed(seed + 3 + blockOffset);

                List<Double> durations = (gc.getMelodyPatternEffect() != 1)
                        ? blockDurationsMap.get(blockOffset)
                        : null;
                boolean badDuration = false;
                if (durations != null
                        && !MidiUtils.roughlyEqual(durations.stream().mapToDouble(e -> e).sum(),
                        mg.progressionDurations.get(chordIndex))) {
                    durations = null;
                    badDuration = true;
                }
                boolean sameRhythmTwice = sameRhythmGenerator.nextInt(100) < SAME_RHYTHM_CHANCE;
                int rhythmSeed = seed + blockOffset + rhythmOffset;

                if (durations == null) {
                    double rhythmDuration = sameRhythmTwice
                            ? mg.progressionDurations.get(chordIndex) / 2.0
                            : mg.progressionDurations.get(chordIndex);

                    int speed = MidiGeneratorUtils.adjustChanceParamForTransition(mp.getSpeed(),
                            sec, chordIndex, chords.size(), 40, 0.25, false, false);
                    speed = OMNI.clamp(speed, -100, 100);
                    int addQuick = (speed - 50) * 4;
                    int addSlow = addQuick * -1;

                    int[] melodySkeletonDurationWeights = MelodyUtils
                            .normalizedCumulativeWeights(200 + addQuick, Math.max(1, 200 + addQuick / 2), 200 + addQuick, 200 + addSlow);


                    Rhythm rhythm = new Rhythm(rhythmSeed, rhythmDuration, melodySkeletonDurations,
                            melodySkeletonDurationWeights);

                    durations = rhythm.regenerateDurations(10, melodySkeletonDurations[0]);
                    if (sameRhythmTwice) {
                        durations.addAll(durations);
                    }
                    blockDurationsMap.put(blockOffset, durations);
                }
                LG.d("Overall Block Durations: " + StringUtils.join(durations, ",")
                        + ", Doubled rhythm: " + sameRhythmTwice);
                int chord1 = MidiGeneratorUtils.getStartingNote(roots,
                        blockChordNoteChoices, chordIndex, BLOCK_TARGET_MODE);
                int chord2 = MidiGeneratorUtils.getStartingNote(roots,
                        blockChordNoteChoices, chordIndex + 1, BLOCK_TARGET_MODE);
                int startingOct = chord1 / 7;

                Pair<Pair<List<Integer>, Integer>, List<MelodyBlock>> existingPattern = (badDuration)
                        ? null
                        : changesAndBlocksMap.get(blockOffset);

                int remainingDirChanges = gc.getMelodyMaxDirChanges();
                Pair<List<Integer>, Integer> blockChangesPair;


                int originalBlockOffsetIndex = (originalBlockOffset < 0) ? (blockSeedOffsets.indexOf(originalBlockOffset * -1)) : blockSeedOffsets.indexOf(originalBlockOffset);
                Map<Integer, List<PhraseNote>> customUserDurationsByBlock = convertCustomUserDurations(mp, melodyBlockGeneratorSeed,
                        chordIndex, (gc.isMelodyCustomDurationsRandomWeighting() && existingPattern != null) ? originalBlockOffsetIndex : blockOffset);
                int numBlocks = !customUserDurationsByBlock.isEmpty() ? customUserDurationsByBlock.size() : durations.size();
                if (existingPattern != null
                        && gc.getMelodyPatternEffect() > 0) {
                    blockChangesPair = existingPattern.getLeft();
                } else {
                    blockChangesPair = MelodyUtils.blockChangeSequence(chord1, chord2,
                            melodyBlockGeneratorSeed, numBlocks,
                            OMNI.clamp(
                                    mp.getMaxBlockChange() + maxBlockChangeAdjustment,
                                    0, 7),
                            remainingDirChanges);
                }
                remainingDirChanges -= blockChangesPair.getRight();
                List<Integer> blockChanges = blockChangesPair.getLeft();
                boolean FLEXIBLE_PATTERN = mp.isPatternFlexible() && blockChanges.size() > 1;
                if (FLEXIBLE_PATTERN && existingPattern != null) {
                    int blockChangesSum = blockChanges.stream().mapToInt(e -> e).sum();
                    int newLastBlockChange = blockChanges.get(blockChanges.size() - 1);
                    newLastBlockChange += (chord2 - chord1) - blockChangesSum;
                    List<Integer> newBlockChanges = new ArrayList<>(blockChanges);
                    newBlockChanges.set(blockChanges.size() - 1, newLastBlockChange);
                    blockChanges = newBlockChanges;

                }
                LG.d("Block changes: " + blockChanges);
                int startingNote = chord1 % 7;

                List<Integer> forcedLengths = (existingPattern != null
                        && gc.getMelodyPatternEffect() != 1)
                        ? existingPattern.getRight().stream().map(e -> e.durations.size())
                        .collect(Collectors.toList())
                        : null;

                List<MelodyBlock> melodyBlocks = (existingPattern != null
                        && gc.getMelodyPatternEffect() > 0 && !FLEXIBLE_PATTERN)
                        ? existingPattern.getRight()
                        : generateMelodyBlocksForDurations(mp, sec, durations, roots,
                        melodyBlockGeneratorSeed, blockOffset, blockChanges,
                        MAX_JUMP_SKELETON_CHORD, startingNote, chordIndex,
                        forcedLengths, remainingDirChanges, usedMelodyBlockJumpPreference,
                        customUserDurationsByBlock, numBlocks);
                if (FLEXIBLE_PATTERN && existingPattern != null
                        && gc.getMelodyPatternEffect() > 0) {
                    List<MelodyBlock> storedMelodyBlocks = new ArrayList<>(
                            existingPattern.getRight());
                    LG.d("HALF PATTERN - set last melody block to newly generated block! Sizes equal?: "
                            + (storedMelodyBlocks.size() == melodyBlocks.size()));
                    storedMelodyBlocks.set(storedMelodyBlocks.size() - 1,
                            melodyBlocks.get(melodyBlocks.size() - 1));
                    melodyBlocks = storedMelodyBlocks;
                }
                //LG.d("Starting note: " + startingNote);

                if (existingPattern == null) {
                    LG.n("Stored pattern: " + blockOffset + ", for chord index:" + chordIndex
                            + ", Pattern effect: " + gc.getMelodyPatternEffect());
                    changesAndBlocksMap.put(blockOffset, Pair.of(blockChangesPair, melodyBlocks));
                } else {
                    LG.n("Loaded pattern: " + blockOffset + ", for chord index:" + chordIndex
                            + ", Pattern effect: " + gc.getMelodyPatternEffect());
                }

                int adjustment = 0;
                int exceptionCounter = mp.getMaxNoteExceptions();
                boolean invertedPattern = originalBlockOffset < 0;
                for (int blockIndex = 0; blockIndex < melodyBlocks.size(); blockIndex++) {
                    MelodyBlock mb = melodyBlocks.get(blockIndex);
                    if (invertedPattern) {
                        mb = new MelodyBlock(MelodyUtils.inverse(mb.notes), mb.durations, true);
                    }
                    List<Integer> pitches = new ArrayList<>();
                    if (blockIndex > 0) {
                        adjustment += blockChanges.get(blockIndex - 1) * (invertedPattern ? -1 : 1);
                    }
                    //LG.d("Adjustment: " + adjustment);
                    for (int k = 0; k < mb.durations.size(); k++) {
                        int note = mb.notes.get(k);
                        int pitch = startingOct * 12;
                        int combinedNote = startingNote + note;
                        //LG.d("1st combined: " + combinedNote);
                        Pair<Integer, Integer> notePitch = MidiGeneratorUtils
                                .normalizeNotePitch(combinedNote, pitch);
                        combinedNote = notePitch.getLeft();
                        pitch = notePitch.getRight();
                        if (adjustment != 0) {
                            combinedNote = combinedNote + adjustment;
                            notePitch = MidiGeneratorUtils.normalizeNotePitch(combinedNote, pitch);
                            combinedNote = notePitch.getLeft();
                            pitch = notePitch.getRight();
                        }

                        pitch += MidiUtils.MAJ_SCALE.get(combinedNote);
                        //LG.d("Combined note: " + combinedNote + ", pitch: " + pitch);
                        pitches.add(pitch);

                    }

                    List<Double> sortedDurs = new ArrayList<>(mb.durations);
                    if (existingPattern == null) {
                        if (gc.isMelodyEmphasizeKey()) {
                            // re-order durations to make most relevant notes the longest
                            for (int k = 0; k < mb.durations.size(); k++) {
                                for (int l = 0; l < mb.durations.size(); l++) {
                                    if (!pitches.get(k).equals(pitches.get(l))) {
                                        boolean swap = false;
                                        if (emphasizeKeyNoteOrder.indexOf(
                                                pitches.get(k) % 12) < emphasizeKeyNoteOrder
                                                .indexOf(pitches.get(l) % 12)) {
                                            swap = sortedDurs.get(k) + DBL_ERR < sortedDurs.get(l);
                                        } else {
                                            swap = sortedDurs.get(k) - DBL_ERR > sortedDurs.get(l);
                                        }
                                        if (swap) {
                                            double temp = sortedDurs.get(k);
                                            sortedDurs.set(k, sortedDurs.get(l));
                                            sortedDurs.set(l, temp);
                                        }
                                    }
                                }
                            }
                            // if rhythm or rhythm+notes
                            if (gc.getMelodyPatternEffect() != 1) {
                                mb.durations = sortedDurs;
                            }
                        }
                    } else if (gc.getMelodyPatternEffect() == 0) {
                        // rhythm only - get from stored melodyblock
                        sortedDurs = new ArrayList<>(
                                existingPattern.getRight().get(blockIndex).durations);
                    }

                    MelodyBlock convertedMb = convertMelodyBlockWithCustomMap(mb, customUserDurationsByBlock, blockIndex);
                    boolean converted = convertedMb != mb;
                    mb = convertedMb;

                    //LG.d(StringUtils.join(mb.durations, ","));
                    //LG.d("After: " + StringUtils.join(sortedDurs, ","));
                    int validNoteCounter = 0;
                    for (int k = 0; k < mb.durations.size(); k++) {
                        int pitch = mb.notes.get(k) != Pitches.REST ? pitches.get(validNoteCounter) : Pitches.REST;
                        // single note exc. = last note in chord
                        // other exc. = any note first note in block
                        boolean exceptionIndexValid = (gc.isMelodySingleNoteExceptions())
                                ? (k == mb.durations.size() - 1
                                && blockIndex == melodyBlocks.size() - 1)
                                : (k > 0);
                        if (exceptionIndexValid && exceptionCounter > 0
                                && exceptionGenerator.nextInt(100) < EXCEPTION_CHANCE
                                && pitch != Pitches.REST) {
                            int upDown = exceptionGenerator.nextBoolean() ? 1 : -1;
                            int excPitch = MidiUtils.MAJ_SCALE.get(exceptionGenerator
                                    .nextInt(gc.isMelodySingleNoteExceptions() ? 7 : 4));
                            pitch += upDown * excPitch;
                            int closestPitch = MidiUtils.getClosestFromList(MidiUtils.MAJ_SCALE,
                                    pitch % 12);
                            pitch -= pitch % 12;
                            pitch += closestPitch;
                            exceptionCounter--;
                        }

                        double swingDuration = (pitch != Pitches.REST && (!converted || !gc.isMelodyCustomDurationsStrictMode())) ? sortedDurs.get(validNoteCounter) : mb.durations.get(k);
                        if (pitch != Pitches.REST) {
                            validNoteCounter++;
                        }
                        Note n = new Note(pitch, swingDuration);
                        n.setDuration(swingDuration * (0.75 + durationGenerator.nextDouble() / 4)
                                * MidiGenerator.GLOBAL_DURATION_MULTIPLIER);
                        if (embellish
                                && (n.getRhythmValue() > MidiGenerator.Durations.DOTTED_EIGHTH_NOTE - DBL_ERR)) {
                            List<Note> embNotes = addEmbellishedNotes(n, embellishmentGenerator);
                            noteList.addAll(embNotes);
                            if (fillChordMelodyMap && o == 0) {
                                chordMelodyMap1.get(Integer.valueOf(chordIndex)).addAll(embNotes);
                            }
                        } else {
                            noteList.add(n);
                            if (fillChordMelodyMap && o == 0) {
                                chordMelodyMap1.get(Integer.valueOf(chordIndex)).add(n);
                            }
                        }
                    }
                }

            }
        }

        if (fillChordMelodyMap) {
            List<String> chordStrings = MidiGenerator.getChordsFromMelodyPitches(2, mg.progressionDurations, chordMelodyMap1,
                    MidiUtils.baseFreqMap);
            int start = 1;
            int end = chordMelodyMap1.keySet().size() - 1;
            populateMelodyBasedProgression(chordStrings, start, end);
            for (int i = 0; i < start; i++) {
                chordStrings.set(i, MidiGenerator.chordInts.get(i));
            }
            for (int i = end; i < chordStrings.size(); i++) {
                chordStrings.set(i, MidiGenerator.chordInts.get(i));
            }
            alternateChords = StringUtils.join(chordStrings, ",");
        }
        if (genVars && variations != null) {
            sec.setVariation(0, mp.getAbsoluteOrder(), variations);
        }
        return noteList;
    }



    private List<Double> makeSurpriseTrioArpedDurations(List<Double> durations) {

        List<Double> arpedDurations = new ArrayList<>(durations);
        for (int trioIndex = 0; trioIndex < arpedDurations.size() - 2; trioIndex++) {
            double sumThirds = arpedDurations.subList(trioIndex, trioIndex + 3).stream()
                    .mapToDouble(e -> e).sum();
            boolean valid = false;
            if (MidiGeneratorUtils.isDottedNote(sumThirds)) {
                sumThirds /= 3.0;
                for (int trio = trioIndex; trio < trioIndex + 3; trio++) {
                    arpedDurations.set(trio, sumThirds);
                }
                valid = true;
            } else if (MidiUtils.isMultiple(sumThirds, MidiGenerator.Durations.HALF_NOTE)) {
                if (sumThirds > MidiGenerator.Durations.DOTTED_HALF_NOTE) {
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

    private Vector<Note> algoGen2GenerateMelodySkeletonFromChords(MelodyPart mp, List<int[]> chords,
                                                                  List<int[]> roots, int measures, int notesSeedOffset, Section sec,
                                                                  List<Integer> variations) {

        boolean genVars = variations == null;

        boolean fillChordMelodyMap = false;
        if (chordMelodyMap1.isEmpty() && notesSeedOffset == 0
                && (roots.size() == MidiGenerator.chordInts.size())) {
            fillChordMelodyMap = true;
        }

        int MAX_JUMP_SKELETON_CHORD = mp.getBlockJump();
        int SAME_RHYTHM_CHANCE = mp.getDoubledRhythmChance();
        int ALTERNATE_RHYTHM_CHANCE = mp.getAlternatingRhythmChance();
        int EXCEPTION_CHANCE = mp.getNoteExceptionChance();
        int CHORD_STRETCH = 4;

        int seed = mp.getPatternSeedWithPartOffset();

        Vector<Note> noteList = new Vector<>();

        Random algoGenerator = new Random(gc.getRandomSeed());
        if (algoGenerator.nextInt(100) < gc.getMelodyUseOldAlgoChance()) {
            return algoGen1GenerateMelodySkeletonFromChords(mp, measures, roots);
        }

        // if notes seed offset > 0, add it only to one of: rhythms, pitches
        //Random nonMainMelodyGenerator = new Random(seed + 30);
        int pitchPickerOffset = notesSeedOffset;
        int rhythmOffset = notesSeedOffset;

        Random pitchPickerGenerator = new Random(seed + pitchPickerOffset);
        Random exceptionGenerator = new Random(seed + 2 + notesSeedOffset);
        Random sameRhythmGenerator = new Random(seed + 3);
        Random alternateRhythmGenerator = new Random(seed + 4);
        Random durationGenerator = new Random(seed + notesSeedOffset + 5);
        Random directionGenerator = new Random(seed + 10);
        //Random surpriseGenerator = new Random(seed + notesSeedOffset + 15);
        Random exceptionTypeGenerator = new Random(seed + 20 + notesSeedOffset);

        double[] melodySkeletonDurations = { MidiGenerator.Durations.SIXTEENTH_NOTE, MidiGenerator.Durations.EIGHTH_NOTE,
                MidiGenerator.Durations.QUARTER_NOTE, MidiGenerator.Durations.DOTTED_QUARTER_NOTE, MidiGenerator.Durations.HALF_NOTE };

        int weight3rd = mp.getSpeed() / 3;
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
                basicChordsUnsquished.set(i,
                        convertChordToLength(basicChordsUnsquished.get(i), chords.get(i).length));
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

        List<int[]> stretchedChords = usedChords.stream()
                .map(e -> convertChordToLength(e, CHORD_STRETCH)).collect(Collectors.toList());
        List<Double> directionChordDividers = (!gc.isMelodyUseDirectionsFromProgression())
                ? MidiGeneratorUtils.generateMelodyDirectionChordDividers(stretchedChords.size(),
                directionGenerator)
                : null;
        directionGenerator.setSeed(seed + 10);
        boolean currentDirection = directionGenerator.nextBoolean();
        if (!gc.isMelodyUseDirectionsFromProgression()) {
            LG.d("Direction dividers: " + directionChordDividers.toString() + ", start at: "
                    + currentDirection);
        }

        List<Boolean> directionsFromChords = (gc.isMelodyUseDirectionsFromProgression())
                ? MidiGeneratorUtils.generateMelodyDirectionsFromChordProgression(usedChords, true)
                : null;

        boolean alternateRhythm = alternateRhythmGenerator.nextInt(100) < ALTERNATE_RHYTHM_CHANCE;
        //LG.d("Alt: " + alternateRhythm);

        for (int o = 0; o < measures; o++) {
            int previousNotePitch = 0;
            int firstPitchInTwoChords = 0;

            for (int i = 0; i < stretchedChords.size(); i++) {
                // either after first measure, or after first half of combined chord prog

                if (genVars && (i == 0)) {
                    variations = MidiGenerator.fillVariations(sec, mp, variations, 0);
                }

                if ((variations != null) && (i == 0)) {
                    for (Integer var : variations) {
                        if (o == measures - 1) {
                            LG.i("Melody variation: " + var);
                        }

                        switch (var) {
                            case 0:
                                // only add, processed later
                                break;
                            case 1:
                                MAX_JUMP_SKELETON_CHORD = Math.min(4, MAX_JUMP_SKELETON_CHORD + 1);
                                break;
                            default:
                                break;
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
                    pitchPickerGenerator.setSeed(seed + pitchPickerOffset);
                    exceptionGenerator.setSeed(seed + 2 + notesSeedOffset);
                    if (alternateRhythm) {
                        sameRhythmGenerator.setSeed(seed + 3);
                    }
                }

                boolean sameRhythmTwice = sameRhythmGenerator.nextInt(100) < SAME_RHYTHM_CHANCE;

                double rhythmDuration = sameRhythmTwice ? mg.progressionDurations.get(i) / 2.0
                        : mg.progressionDurations.get(i);
                int rhythmSeed = (alternateRhythm && i % 2 == 1) ? seed + 1 : seed;
                rhythmSeed += rhythmOffset;
                Rhythm rhythm = new Rhythm(rhythmSeed, rhythmDuration, melodySkeletonDurations,
                        melodySkeletonDurationWeights);

                List<Double> durations = rhythm.regenerateDurations(sameRhythmTwice ? 1 : 2,
                        MidiGenerator.Durations.SIXTEENTH_NOTE / 2.0);
                if (gc.isMelodyArpySurprises()) {
                    if (sameRhythmTwice) {
                        if ((i % 2 == 0) || (durations.size() < 3)) {
                            durations.addAll(durations);
                        } else {
                            List<Double> arpedDurations = makeSurpriseTrioArpedDurations(durations);
                            if (arpedDurations != null) {
                                LG.d("Double pattern - surprise!");
                                durations.addAll(arpedDurations);
                            } else {
                                durations.addAll(durations);
                            }
                        }
                    } else if (i % 2 == 1 && durations.size() >= 4) {

                        List<Double> arpedDurations = makeSurpriseTrioArpedDurations(durations);
                        if (arpedDurations != null) {
                            LG.d("Single pattern - surprise!");
                            durations = arpedDurations;
                        }
                    }
                } else {
                    if (sameRhythmTwice) {
                        durations.addAll(durations);
                    }
                }


                int[] chord = stretchedChords.get(i);
                int exceptionCounter = mp.getMaxNoteExceptions();
                boolean allowException = true;
                double durCounter = 0.0;
                boolean changedDirectionByDivider = false;
                if (gc.isMelodyUseDirectionsFromProgression()) {
                    currentDirection = directionsFromChords.get(i);
                }
                for (int j = 0; j < durations.size(); j++) {
                    boolean tempChangedDir = false;
                    int tempSaveMaxJump = MAX_JUMP_SKELETON_CHORD;
                    boolean hasSingleNoteException = false;
                    if (allowException && j > 0 && exceptionCounter > 0
                            && exceptionGenerator.nextInt(100) < EXCEPTION_CHANCE) {
                        if (gc.isMelodySingleNoteExceptions()) {
                            hasSingleNoteException = true;
                            if (exceptionTypeGenerator.nextBoolean()) {
                                MAX_JUMP_SKELETON_CHORD = Math.max(0, MAX_JUMP_SKELETON_CHORD - 1);
                                tempChangedDir = true;
                                currentDirection = !currentDirection;
                            } else {
                                MAX_JUMP_SKELETON_CHORD = Math.min(6, MAX_JUMP_SKELETON_CHORD + 2);
                            }
                        } else {
                            currentDirection = !currentDirection;
                        }

                        exceptionCounter--;
                    }
                    int pitch = 0;
                    int startIndex = 0;
                    int endIndex = chord.length - 1;

                    if (previousNotePitch != 0) {
                        // up, or down
                        if (currentDirection) {
                            startIndex = MidiGeneratorUtils.selectClosestIndexFromChord(chord,
                                    previousNotePitch, true);
                            endIndex = Math.min(endIndex, startIndex + MAX_JUMP_SKELETON_CHORD);
                        } else {
                            endIndex = MidiGeneratorUtils.selectClosestIndexFromChord(chord,
                                    previousNotePitch, false);
                            startIndex = Math.max(endIndex - MAX_JUMP_SKELETON_CHORD, startIndex);
                        }
                    }
                    double positionInChord = durCounter / mg.progressionDurations.get(i);
                    pitch = MidiGeneratorUtils.pickRandomBetweenIndexesInclusive(chord, startIndex,
                            endIndex, pitchPickerGenerator, positionInChord);

                    double swingDuration = durations.get(j);
                    Note n = new Note(pitch, swingDuration, 100);
                    n.setDuration(swingDuration * (0.75 + durationGenerator.nextDouble() / 4)
                            * MidiGenerator.GLOBAL_DURATION_MULTIPLIER);

                    if (hasSingleNoteException && gc.isMelodySingleNoteExceptions()) {
                        if (tempChangedDir) {
                            currentDirection = !currentDirection;
                        }
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

                    if (i % 2 == 0 && j == 0 && !gc.isMelodyAvoidChordJumps()) {
                        firstPitchInTwoChords = pitch;
                    }
                    previousNotePitch = pitch;
                    if (hasSingleNoteException && tempChangedDir) {
                        previousNotePitch += (currentDirection) ? 2 : -2;
                    }
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
            List<String> chordStrings = MidiGenerator.getChordsFromMelodyPitches(2, mg.progressionDurations, chordMelodyMap1,
                    MidiUtils.baseFreqMap);
            populateMelodyBasedProgression(chordStrings, 1, chordMelodyMap1.keySet().size() - 1);

        }
        if (genVars && variations != null) {
            sec.setVariation(0, mp.getAbsoluteOrder(), variations);
        }
        return noteList;
    }

    private Note algoGen1GenerateNote(MelodyPart mp, int[] chord, boolean isAscDirection,
                                      List<Integer> chordScale, Note previousNote, Random generator, double durationLeft) {
        // int randPitch = generator.nextInt(8);
        int velMin = mp.getVelocityMin();
        int velSpace = mp.getVelocityMax() - velMin;

        int direction = (isAscDirection) ? 1 : -1;
        double dur = pickDurationWeightedRandom(generator, durationLeft, MidiGenerator.MELODY_DUR_ARRAY,
                MidiGenerator.MELODY_DUR_CHANCE, MidiGenerator.Durations.EIGHTH_NOTE);
        boolean isPause = (generator.nextInt(100) < mp.getPauseChance());
        if (previousNote == null) {
            int[] firstChord = chord;
            int chordNote = (gc.isFirstNoteRandomized()) ? generator.nextInt(firstChord.length) : 0;

            int chosenPitch = 60 + (firstChord[chordNote] % 12);

            previousPitch = chordScale.indexOf(Integer.valueOf(chosenPitch));
            if (previousPitch == -1) {
                LG.d("ERROR PITCH -1 for: " + chosenPitch);
                previousPitch = chordScale.indexOf(Integer.valueOf(chosenPitch + 1));
                if (previousPitch == -1) {
                    LG.i("NOT EVEN +1 pitch exists for " + chosenPitch + "!");
                }
            }

            //LG.d(firstChord[chordNote] + " > from first chord");
            if (isPause) {
                return new Note(Pitches.REST, dur);
            }

            return new Note(chosenPitch, dur, velMin + generator.nextInt(velSpace));
        }

        int change = generator.nextInt(mp.getBlockJump() + 1);
        // weighted against same note
        if (change == 0) {
            change = generator.nextInt((mp.getBlockJump() + 1) / 2);
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
            //LG.d("UNSAMING NOTE!: " + previousPitch + ", BY: " + (-direction * change));
            generatedPitch = maX(previousPitch - direction * change, maxAllowedScaleNotes);
            samePitchCount = 0;
        }
        previousPitch = generatedPitch;
        if (isPause) {
            return new Note(Pitches.REST, dur);
        }
        return new Note(chordScale.get(generatedPitch), dur, velMin + generator.nextInt(velSpace));

    }

    private Note[] algoGen1GenerateMelodyForChord(MelodyPart mp, int[] chord, double maxDuration,
                                                  Random generator, Note previousChordsNote, boolean isAscDirection) {
        List<Integer> scale = transposeScale(MELODY_SCALE, 0, false);

        double currentDuration = 0.0;

        Note previousNote = (gc.isFirstNoteFromChord()) ? null : previousChordsNote;
        List<Note> notes = new ArrayList<>();

        int exceptionsLeft = mp.getMaxNoteExceptions();

        while (currentDuration <= maxDuration - MidiGenerator.Durations.EIGHTH_NOTE) {
            double durationLeft = maxDuration - MidiGenerator.Durations.EIGHTH_NOTE - currentDuration;
            boolean exceptionChangeUsed = false;
            // generate note,
            boolean actualDirection = isAscDirection;
            if ((generator.nextInt(100) < 33) && (exceptionsLeft > 0)) {
                //LG.d("Exception used for chordnote: " + chord[0]);
                exceptionChangeUsed = true;
                actualDirection = !actualDirection;
            }
            Note note = algoGen1GenerateNote(mp, chord, actualDirection, scale, previousNote,
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

    private Vector<Note> algoGen1GenerateMelodySkeletonFromChords(MelodyPart mp, int measures,
                                                                  List<int[]> genRootProg) {
        List<Boolean> directionProgression = MidiGeneratorUtils
                .generateMelodyDirectionsFromChordProgression(genRootProg, true);

        Note previousChordsNote = null;

        Note[] pair024 = null;
        Note[] pair15 = null;
        Random melodyGenerator = new Random();
        if (!mp.isMuted() && mp.getPatternSeedWithPartOffset() != 0) {
            melodyGenerator.setSeed(mp.getPatternSeedWithPartOffset());
        } else {
            melodyGenerator.setSeed(gc.getRandomSeed());
        }
        LG.i("LEGACY ALGORITHM!");
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
                    generatedMelody = algoGen1GenerateMelodyForChord(mp, genRootProg.get(j),
                            mg.progressionDurations.get(j), melodyGenerator, previousChordsNote,
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

    private Note[] deepCopyNotes(MelodyPart mp, Note[] originals, int[] chord,
                                 Random melodyGenerator) {
        Note[] copied = new Note[originals.length];
        for (int i = 0; i < originals.length; i++) {
            Note n = originals[i];
            copied[i] = new Note(n.getPitch(), n.getRhythmValue());
        }
        if (chord != null && melodyGenerator != null && gc.isFirstNoteFromChord()) {
            Note n = algoGen1GenerateNote(mp, chord, true, MELODY_SCALE, null, melodyGenerator,
                    MidiGenerator.Durations.WHOLE_NOTE);
            copied[0] = new Note(n.getPitch(), originals[0].getRhythmValue(),
                    originals[0].getDynamic());
        }
        return copied;
    }



    private Map<Integer, List<PhraseNote>> convertCustomUserDurations(MelodyPart mp, int melodyBlockGeneratorSeed, int chordIndex, int blockOffsetChordIndex) {
        Map<Integer, List<PhraseNote>> customUserDurationsByBlock = new LinkedHashMap<>();
        if (gc.isMelodyUseCustomDurations() && mp.getCustomDurationNotes() != null && mp.getCustomDurationNotes().size() > 1) {
            PartPhraseNotes customDurationNotesMap = createCustomDurationNotesMap(mp.getCustomDurationNotes());
            Integer indexValue = blockOffsetChordIndex >= 0 ? (blockOffsetChordIndex % customDurationNotesMap.size())
                    : (chordIndex % customDurationNotesMap.size());
            Random customDurationsGenerator = new Random(melodyBlockGeneratorSeed + 12 + indexValue);
            if (gc.isMelodyCustomDurationsRandomWeighting()) {
                List<Integer> firstDurationWeights = customDurationNotesMap.stream().map(e -> e.get(0).getDynamic()).collect(Collectors.toList());
                int[] weights = MelodyUtils.normalizedCumulativeWeights(firstDurationWeights.toArray(new Integer[]{}));
                indexValue = OMNI.getWeightedValue(IntStream.range(0, customDurationNotesMap.size()).boxed().toArray(Integer[]::new),
                        customDurationsGenerator.nextInt(100), weights);
            }
            PhraseNotes userCustomDurations = customDurationNotesMap.get(indexValue);
            int pitchValue = userCustomDurations.get(0).getPitch();

            double mult = MidiGenerator.getBeatDurationMult(mg.currentSection);
            if (!MidiUtils.roughlyEqual(mult, 1.0)) {
                userCustomDurations.stretch(mult, false);
            }
            userCustomDurations.remakeNoteStartTimes(true);

            double currentChordDur = mg.progressionDurations.get(chordIndex);


            double startTime = userCustomDurations.getIterationOrder().get(0).getStartTime();
            if (startTime > DBL_ERR) {
                userCustomDurations.add(0, new PhraseNote(pitchValue, 0, 0.0, startTime, 0.0));
            }
            PhraseNote lastNote = userCustomDurations.getIterationOrder().get(userCustomDurations.size()-1);
            if (lastNote.getEndTime() + DBL_ERR < currentChordDur) {
                userCustomDurations.add(userCustomDurations.indexOf(lastNote) + 1, new PhraseNote(pitchValue, 0, 0.0,
                        currentChordDur - lastNote.getEndTime(),
                        lastNote.getOffset() + lastNote.getDuration()));
                userCustomDurations.remakeNoteStartTimes(true);
            }
            // cleanup notes going out of bounds
            for (int i = userCustomDurations.size()-1; i >= 0; i--) {
                PhraseNote currentPn = userCustomDurations.get(i);
                if (currentPn.getStartTime() + DBL_ERR > currentChordDur) {
                    userCustomDurations.remove(currentPn);
                    continue;
                }
                if (currentPn.getEndTime() - DBL_ERR > currentChordDur) {
                    currentPn.setDuration(currentPn.getDuration() - (currentPn.getEndTime() - currentChordDur));
                }
                userCustomDurations.remakeNoteStartTimes(true);
            }

            // cleanup notes overlapping each other
            for (int i = userCustomDurations.size()-1; i >= 1; i--) {
                PhraseNote earlierPn = userCustomDurations.getIterationOrder().get(i-1);
                PhraseNote currentPn = userCustomDurations.getIterationOrder().get(i);
                if (earlierPn.getEndTime() - DBL_ERR > currentPn.getStartTime()) {
                    earlierPn.setDuration(earlierPn.getDuration() - earlierPn.getEndTime() + currentPn.getStartTime());
                    userCustomDurations.remakeNoteStartTimes(true);
                }
            }

            // insert pauses between note gaps
            double target = currentChordDur;
            for (int i = userCustomDurations.size()-1; i >= 0; i--) {
                PhraseNote currentPn = userCustomDurations.getIterationOrder().get(i);
                if (currentPn.getEndTime() + DBL_ERR < target) {
                    int index = userCustomDurations.indexOf(currentPn);
                    userCustomDurations.add(index + 1, new PhraseNote(pitchValue, 0, 0.0,
                            target - currentPn.getEndTime(), currentPn.getOffset() + currentPn.getDuration()));
                }
                target = currentPn.getStartTime();
            }

            userCustomDurations.remakeNoteStartTimes(true);

            // maybe ignore emphasizeKey if cust. durations enabled?

            // TODO: move melody generation to separate class, separate methods

            // ?? apply roughlyEqual filter to weighting, so that only matching sums are considered in the first place
            //  (= support for different settings of different chord durations)
            List<PhraseNote> customDurationsBlock = new ArrayList<>();
            int blockCounter = 0;
            int sizeCounter = 0;
            for (PhraseNote pn : userCustomDurations.getIterationOrder()) {
                customDurationsBlock.add(pn);
                if (pn.getDynamic() > 0) {
                    sizeCounter++;
                }
                if (sizeCounter == 3) {
                    if (customDurationsGenerator.nextBoolean()) {
                        customUserDurationsByBlock.put(blockCounter++, customDurationsBlock);
                        LG.i("Custom durations used: " + StringUtils.join(customDurationsBlock, ","));
                        customDurationsBlock = new ArrayList<>();
                        sizeCounter = 0;
                    }
                } else if (sizeCounter >= 4) {
                    customUserDurationsByBlock.put(blockCounter++, customDurationsBlock);
                    LG.i("Custom durations used: " + StringUtils.join(customDurationsBlock, ","));
                    customDurationsBlock = new ArrayList<>();
                    sizeCounter = 0;
                }
            }
            if (!customDurationsBlock.isEmpty()) {
                if (sizeCounter > 0) {
                    // some valid note exists
                    customUserDurationsByBlock.put(blockCounter, customDurationsBlock);
                } else if (blockCounter > 0 && customUserDurationsByBlock.get(blockCounter-1) != null) {
                    // only pauses exist
                    customUserDurationsByBlock.get(blockCounter-1).addAll(customDurationsBlock);
                }

                LG.i("Custom durations extra in last block: " + StringUtils.join(customDurationsBlock, ","));
            }
        }
        return customUserDurationsByBlock;
    }

    private PartPhraseNotes createCustomDurationNotesMap(PhraseNotes customDurationValues) {
        customDurationValues.remakeNoteStartTimes(true);
        List<PhraseNote> iterOrder = customDurationValues.getIterationOrder();
        Map<Integer, List<PhraseNote>> rawMap = iterOrder.stream().collect(Collectors.groupingBy(e -> e.getPitch()));
        PartPhraseNotes customDurationNotesMap = new PartPhraseNotes();
        for (Integer i : rawMap.keySet().stream().sorted().collect(Collectors.toList())) {
            PhraseNotes pns = PhraseNotes.fromPN(rawMap.get(i));
            Collections.sort(pns, Comparator.comparingInt(iterOrder::indexOf));
            customDurationNotesMap.add(pns);
        }
        return customDurationNotesMap;
    }

    protected Map<Integer, List<Note>> convertMelodySkeletonToFullMelody(MelodyPart mp,
                                                                         List<Double> durations, Section sec, Vector<Note> skeleton, int notesSeedOffset,
                                                                         List<int[]> chords, int measures) {

        int RANDOM_SPLIT_NOTE_PITCH_EXCEPTION_RANGE = 4;

        int seed = mp.getPatternSeedWithPartOffset();
        int orderSeed = seed + mp.getOrder();
        Random splitGenerator = new Random(orderSeed + 4);
        Random pauseGenerator = new Random(orderSeed + 5);
        Random pauseGenerator2 = new Random(orderSeed + 7);
        Random variationGenerator = new Random(mp.getOrder() + gc.getArrangement().getSeed() + 6);
        Random velocityGenerator = new Random(orderSeed + 1 + notesSeedOffset);
        Random splitNoteGenerator = new Random(seed + 8);
        Random splitNoteExceptionGenerator = new Random(seed + 9);
        Random chordLeadingGenerator = new Random(orderSeed + notesSeedOffset + 15);
        Random accentGenerator = new Random(orderSeed + 20);


        int splitChance = mp.getSplitChance();
        Vector<Note> fullMelody = new Vector<>();
        Map<Integer, List<Note>> fullMelodyMap = new HashMap<>();
        for (int i = 0; i < durations.size() * measures; i++) {
            fullMelodyMap.put(i, new Vector<>());
        }
        int chordCounter = 0;
        int measureCounter = 0;
        double durCounter = 0.0;
        double currentChordDur = durations.get(0);

        int volMultiplier = (gc.isScaleMidiVelocityInArrangement()) ? sec.getVol(0) : 100;
        int minVel = MidiGeneratorUtils.multiplyVelocity(mp.getVelocityMin(), volMultiplier, 0, 1);
        int maxVel = MidiGeneratorUtils.multiplyVelocity(mp.getVelocityMax(), volMultiplier, 1, 0);
        LG.d("Sum:" + skeleton.stream().mapToDouble(e -> e.getRhythmValue()).sum());
        for (int i = 0; i < skeleton.size(); i++) {
            Note n1 = skeleton.get(i);
            if (n1.getPitch() >= 0) {
                n1.setPitch(n1.getPitch() + mp.getTranspose());
            }

            //LG.d(" durCounter: " + durCounter);
            if (durCounter > (currentChordDur - DBL_ERR)) {
                chordCounter = (chordCounter + 1) % durations.size();
                if (chordCounter == 0) {
                    measureCounter++;
                    // when measure resets
                    if (variationGenerator.nextInt(100) < gc.getArrangementPartVariationChance()) {
                        splitChance = (int) (splitChance * 1.2);
                    }
                }
                durCounter -= currentChordDur;
                if (durCounter < 0) {
                    durCounter = 0.0;
                }
                currentChordDur = durations.get(chordCounter);
                //splitGenerator.setSeed(seed + 4);
                //pauseGenerator.setSeed(seed + 5);
                //pauseGenerator2.setSeed(seed + 7);
                splitNoteGenerator.setSeed(orderSeed + 8);
                splitNoteExceptionGenerator.setSeed(orderSeed + 9);
                LG.n("Conversion chord#: " + chordCounter + ", duration: " + currentChordDur);
            }

            double adjDur = n1.getRhythmValue();
            //LG.d("Processing dur: " + adjDur + ", durCounter: " + durCounter);
            int velocity = velocityGenerator.nextInt(maxVel - minVel) + minVel;
            double positionInChord = durCounter / durations.get(chordCounter);
            if (positionInChord < DBL_ERR && accentGenerator.nextInt(100) < mp.getAccents()) {
                velocity = MidiGeneratorUtils.addAccent(velocity, accentGenerator, mp.getAccents());
            }

            n1.setDynamic(velocity);


            durCounter += adjDur;

            boolean splitLastNoteInChord = (chordLeadingGenerator.nextInt(100) < mp
                    .getLeadChordsChance()) && (adjDur > MidiGenerator.Durations.DOTTED_SIXTEENTH_NOTE * 1.1)
                    && (i < skeleton.size() - 1)
                    && ((durCounter + skeleton.get(i + 1).getRhythmValue()) > currentChordDur);


            if ((adjDur > MidiGenerator.Durations.EIGHTH_NOTE * 1.4 && splitGenerator.nextInt(100) < splitChance)
                    || splitLastNoteInChord) {

                int pitch1 = n1.getPitch();
                int indexN2 = (i + 1) % skeleton.size();
                Note n2 = skeleton.get(indexN2);
                int pitch2 = n2.getPitch() + (indexN2 > 0 ? mp.getTranspose() : 0);
                if (pitch1 >= pitch2) {
                    int higherNote = pitch1;
                    if (splitNoteExceptionGenerator.nextInt(100) < 33 && !splitLastNoteInChord) {
                        higherNote += RANDOM_SPLIT_NOTE_PITCH_EXCEPTION_RANGE;
                    }
                    pitch2 = MidiGeneratorUtils.getAllowedPitchFromRange(pitch2, higherNote,
                            positionInChord, splitNoteGenerator);
                } else {
                    int lowerNote = pitch1;
                    if (splitNoteExceptionGenerator.nextInt(100) < 33 && !splitLastNoteInChord) {
                        lowerNote -= RANDOM_SPLIT_NOTE_PITCH_EXCEPTION_RANGE;
                    }
                    pitch2 = MidiGeneratorUtils.getAllowedPitchFromRange(lowerNote, pitch2,
                            positionInChord, splitNoteGenerator);
                }

                double multiplier = (MidiGeneratorUtils.isDottedNote(adjDur)
                        && splitGenerator.nextBoolean()) ? (1.0 / 3.0) : 0.5;

                double swingDuration1 = adjDur * multiplier;
                double swingDuration2 = swingDuration1;

                Note n1split1 = new Note(pitch1, swingDuration1, velocity);
                Note n1split2 = new Note(pitch1 >= 0 ? pitch2 : Pitches.REST, swingDuration2, velocity - 10);
                fullMelody.add(n1split1);
                fullMelodyMap.get(chordCounter + chords.size() * measureCounter).add(n1split1);

                fullMelody.add(n1split2);
                fullMelodyMap.get(chordCounter + chords.size() * measureCounter).add(n1split2);

                if (multiplier < 0.4) {
                    int pitch3 = (splitGenerator.nextBoolean()) ? pitch1 : pitch2;
                    double swingDuration3 = swingDuration1;
                    Note n1split3 = new Note(pitch1 >= 0 ? pitch3 : Pitches.REST, swingDuration3, velocity - 20);
                    fullMelody.add(n1split3);
                    fullMelodyMap.get(chordCounter + chords.size() * measureCounter).add(n1split3);
                }

            } else {
                fullMelody.add(n1);
                fullMelodyMap.get(chordCounter + chords.size() * measureCounter).add(n1);
            }
        }
        List<Integer> firstNotePitches = fullMelodyMap.values().stream()
                .map(e -> e.isEmpty() ? Pitches.REST : e.get(0).getPitch())
                .collect(Collectors.toList());

        List<Integer> fillerPattern = mp.getChordSpanFill()
                .getPatternByLength(fullMelodyMap.keySet().size(), mp.isFillFlip());

        int[] pitches = new int[12];
        fullMelody.forEach(e -> {
            int pitch = e.getPitch();
            if (pitch >= 0) {
                pitches[pitch % 12]++;
            }
        });
        applyNoteTargets(fullMelody, fullMelodyMap, pitches, notesSeedOffset, chords, sec, mp);

        if (!ScaleMode.LOCRIAN.equals(gc.getScaleMode())) {
            MidiGeneratorUtils.applyBadIntervalRemoval(fullMelody);
        }

        if (gc.getMelodyReplaceAvoidNotes() > 0) {
            MidiGeneratorUtils.replaceNearChordNotes(fullMelodyMap, chords,
                    mp.getPatternSeedWithPartOffset(), gc.getMelodyReplaceAvoidNotes());
        }

        // pause by %, sort not-paused into pitches
        for (int chordIndex = 0; chordIndex < fullMelodyMap.keySet().size(); chordIndex++) {
            List<Note> notes = MelodyUtils
                    .sortNotesByRhythmicImportance(fullMelodyMap.get(chordIndex));
            //Collections.sort(notes, Comparator.comparing(e -> e.getRhythmValue()));
            pauseGenerator.setSeed(orderSeed + 5);
            int actualPauseChance = MidiGeneratorUtils.adjustChanceParamForTransition(
                    mp.getPauseChance(), sec, chordIndex, durations.size(), 40, 0.25, false, true);
            int pausedNotes = (int) Math.round(notes.size() * actualPauseChance / 100.0);
            int startIndex = (mp.isFillPauses())
                    ? (gc.isMelodyFillPausesPerChord() ? 1 : ((chordIndex == 0) ? 1 : 0))
                    : 0;
            //LG.i("Pausing first # sorted notes: " + pausedNotes);
            for (int j = 0; j < pausedNotes; j++) {
                Note n = notes.get(j);
                if (startIndex == 1) {
                    if (n.equals(fullMelodyMap.get(chordIndex).get(0))) {
                        //pitches[n.getPitch() % 12]++;
                        continue;
                    }
                }
                n.setPitch(Pitches.REST);
            }
            for (int j = pausedNotes; j < notes.size(); j++) {
                Note n = notes.get(j);
                if (fillerPattern.get(chordIndex) < 1) {
                    n.setPitch(Pitches.REST);
                } else {
                    //pitches[n.getPitch() % 12]++;
                }
            }
        }


        // fill pauses toggle
        if (mp.isFillPauses()) {
            Note fillPauseNote = fullMelody.get(0);
            double addedDuration = 0;
            double addedRv = 0;
            List<Note> notesToRemove = new ArrayList<>();

            int currentChordIndex = 0;
            int currentChordCount = 1;

            // 0 1 2 3 | 4 5 6 7 | 8
            // size of 0: 4
            // processing 4:

            for (int i = 1; i < fullMelody.size(); i++) {
                Note n = fullMelody.get(i);
                currentChordCount++;
                if (currentChordCount > fullMelodyMap.get(currentChordIndex).size()) {
                    currentChordIndex++;
                    currentChordCount = 1;
                }

                if (n.getPitch() < 0
                        && !(currentChordCount == 1 && gc.isMelodyFillPausesPerChord())) {
                    addedRv += n.getRhythmValue();
                    if (fillerPattern.get(currentChordIndex) > 0) {
                        addedDuration += n.getRhythmValue();
                    }

                    notesToRemove.add(n);
                } else {
                    fillPauseNote.setDuration(fillPauseNote.getDuration() + addedDuration);
                    fillPauseNote.setRhythmValue(fillPauseNote.getRhythmValue() + addedRv);
                    //LG.d("Filled note duration: " + fillPauseNote.getRhythmValue());
                    addedDuration = 0;
                    addedRv = 0;
                    fillPauseNote = n;
                }
            }
            fullMelody.removeAll(notesToRemove);
            fullMelodyMap.values().forEach(e -> e.removeAll(notesToRemove));

            if (addedDuration > DBL_ERR || addedRv > DBL_ERR) {
                fillPauseNote.setDuration(fillPauseNote.getDuration() + addedDuration);
                fillPauseNote.setRhythmValue(fillPauseNote.getRhythmValue() + addedRv);
            }
        }

        Random startNoteRand = new Random(orderSeed + 25);

        // repair target notes
        for (int chordIndex = 0; chordIndex < firstNotePitches.size(); chordIndex++) {
            if (fullMelodyMap.get(chordIndex).size() > 0) {
                Note n = fullMelodyMap.get(chordIndex).get(0);
                if (fillerPattern.get(chordIndex) > 0
                        && (n.getPitch() >= 0 || gc.isMelodyFillPausesPerChord())) {
                    n.setPitch(firstNotePitches.get(chordIndex));
                }
            }
        }
        // accent lengths of first notes in chord, if not paused and next note has different pitch
        fullMelodyMap.values().forEach(e -> {
            if (e.size() < 3) {
                return;
            }
            Note n = e.get(0);
            int pitch = n.getPitch();
            if (pitch >= 0 && n.getDuration() < MidiGenerator.Durations.QUARTER_NOTE * 1.1
                    && e.get(1).getPitch() != pitch && e.get(2).getPitch() != pitch) {
                n.setDuration(n.getDuration() * (1 + (mp.getAccents() / 200.0)));
            }
        });

        // repair target notes
        for (int i = 0; i < firstNotePitches.size(); i++) {
            if (fullMelodyMap.get(i).size() > 0) {
                Note n = fullMelodyMap.get(i).get(0);
                double preferredDelay = MELODY_SKELETON_DURATIONS[startNoteRand.nextInt(3)];
                if (n.getPitch() >= 0 && startNoteRand.nextInt(100) >= mp.getStartNoteChance()) {
                    int sixteenths = (int) Math.floor(n.getDuration() / MidiGenerator.Durations.SIXTEENTH_NOTE);
                    double usedDelay = -1;
                    switch (sixteenths) {
                        case 0:
                        case 1:
                            n.setPitch(Note.REST);
                            break;
                        case 2:
                            usedDelay = MidiGenerator.Durations.SIXTEENTH_NOTE;
                            break;
                        case 3:
                        case 4:
                            usedDelay = Math.min(preferredDelay, MidiGenerator.Durations.EIGHTH_NOTE);
                            break;
                        default:
                            usedDelay = Math.min(preferredDelay, MidiGenerator.Durations.DOTTED_EIGHTH_NOTE);
                            break;
                    }
                    if (usedDelay > 0) {
                        n.setOffset(n.getOffset() + usedDelay);
                        n.setDuration(n.getDuration() - usedDelay);
                    }
                }
            }
        }

        if (sec.getVariation(0, mp.getAbsoluteOrder()).contains(Integer.valueOf(3))) {
            double currRv = 0;
            for (int chordIndex = 0; chordIndex < fullMelodyMap.keySet().size(); chordIndex++) {
                List<Note> notes = fullMelodyMap.get(chordIndex);
                for (Note n : notes) {
                    if (n.getPitch() >= 0) {
                        double noteStart = currRv + n.getOffset();
                        double noteEnd = noteStart + n.getDuration();

                        double closestEndOnGrid = Math.floor(noteEnd / MidiGenerator.Durations.EIGHTH_NOTE)
                                * MidiGenerator.Durations.EIGHTH_NOTE;
                        if (closestEndOnGrid > (noteStart + MidiGenerator.Durations.SIXTEENTH_NOTE / 2)) {
                            n.setDuration(closestEndOnGrid - noteStart);
                        }
                    }
                }
            }
        }

        return fullMelodyMap;
    }

    protected void applyNoteTargets(List<Note> fullMelody, Map<Integer, List<Note>> fullMelodyMap,
                                    int[] pitches, int notesSeedOffset, List<int[]> chords, Section sec, MelodyPart mp) {
        // --------- NOTE ADJUSTING ---------------
        int[] chordSeparators = new int[fullMelodyMap.keySet().size() + 1];
        chordSeparators[0] = 0;
        for (Integer i : fullMelodyMap.keySet()) {
            int index = i + 1;
            chordSeparators[index] = fullMelodyMap.get(i).size() + chordSeparators[index - 1];
        }
        int surplusTonics = applyTonicNoteTargets(fullMelody, fullMelodyMap, pitches,
                notesSeedOffset, chordSeparators);
        List<Note> modeNoteChanges = applyModeNoteTargets(fullMelody, fullMelodyMap, pitches,
                surplusTonics);
        applyChordNoteTargets(fullMelody, fullMelodyMap, chords, modeNoteChanges, sec, mp);

    }

    private int applyTonicNoteTargets(List<Note> fullMelody, Map<Integer, List<Note>> fullMelodyMap,
                                      int[] pitches, int notesSeedOffset, int[] chordSeparators) {
        double requiredPercentageCs = gc.getMelodyTonicNoteTarget() / 100.0;
        int needed = (int) Math.floor(
                fullMelody.stream().filter(e -> e.getPitch() >= 0).count() * requiredPercentageCs);
        LG.d("Found C's: " + pitches[0] + ", needed: " + needed);
        int surplusTonics = pitches[0] - needed;

        if (gc.getMelodyTonicNoteTarget() > 0 && notesSeedOffset == 0) {
            // for main sections: try to adjust notes towards C if there isn't enough C's
            if (surplusTonics < 0) {
                //LG.d("Correcting melody!");
                int investigatedChordIndex = chordSeparators.length - 1;


                // adjust in pairs starting from last
                while (investigatedChordIndex > 0 && surplusTonics < 0) {
                    int end = chordSeparators[investigatedChordIndex] - 1;
                    // ignore first note in chord - user selectable target note
                    int investigatedChordStart = chordSeparators[investigatedChordIndex - 1] + 1;
                    for (int i = end; i >= investigatedChordStart; i--) {
                        Note n = fullMelody.get(i);
                        int p = n.getPitch();
                        if (p < 0) {
                            continue;
                        }
                        // D
                        if (p % 12 == 2) {
                            n.setPitch(p - 2);
                            surplusTonics++;
                            break;
                        }
                        // B
                        if (p % 12 == 11) {
                            n.setPitch(p + 1);
                            surplusTonics++;
                            break;
                        }
                    }
                    investigatedChordIndex -= 2;
                }

                //LG.d("Remaining difference after last pairs: " + difference);

                // adjust in pairs starting from last-1
                investigatedChordIndex = chordSeparators.length - 2;
                while (investigatedChordIndex > 0 && surplusTonics < 0) {
                    int end = chordSeparators[investigatedChordIndex] - 1;
                    int investigatedChordStart = chordSeparators[investigatedChordIndex - 1] + 1;
                    for (int i = end; i >= investigatedChordStart; i--) {
                        Note n = fullMelody.get(i);
                        int p = n.getPitch();
                        if (p < 0) {
                            continue;
                        }
                        // D
                        if (p % 12 == 2) {
                            n.setPitch(p - 2);
                            surplusTonics++;
                            break;
                        }
                        // B
                        if (p % 12 == 11) {
                            n.setPitch(p + 1);
                            surplusTonics++;
                            break;
                        }
                    }
                    investigatedChordIndex -= 2;
                }

                LG.d("TONIC: Remaining difference after first pairs: " + surplusTonics);

            }
        }
        return surplusTonics;
    }

    private List<Note> applyModeNoteTargets(List<Note> fullMelody,
                                            Map<Integer, List<Note>> fullMelodyMap, int[] pitches, int surplusTonics) {

        ScaleMode scale = (mg.modScale != null) ? mg.modScale : gc.getScaleMode();
        List<Note> modeNoteChanges = new ArrayList<>();
        if (gc.getMelodyModeNoteTarget() > 0 && scale.modeTargetNote >= 0) {
            double requiredPercentage = gc.getMelodyModeNoteTarget() / 100.0;
            int needed = (int) Math.ceil(fullMelody.stream().filter(e -> e.getPitch() >= 0).count()
                    * requiredPercentage);

            int modeNote = MidiUtils.MAJ_SCALE.get(scale.modeTargetNote);
            LG.d("Found Mode notes: " + pitches[modeNote] + ", needed: " + needed);
            if (pitches[modeNote] < needed) {
                int chordSize = fullMelodyMap.keySet().size();
                int difference = needed - pitches[modeNote];


                List<Note> allNotesExceptFirsts = new ArrayList<>();
                for (int chordIndex = 0; chordIndex < chordSize; chordIndex++) {
                    List<Note> chordNotes = fullMelodyMap.get(chordIndex);
                    if (chordNotes.size() > 1) {
                        allNotesExceptFirsts.addAll(chordNotes.subList(1, chordNotes.size()));
                    }
                }
                Collections.sort(allNotesExceptFirsts,
                        (e1, e2) -> MidiUtils.compareNotesByDistanceFromModeNote(e1, e2, modeNote));
                for (Note n : allNotesExceptFirsts) {
                    int pitch = n.getPitch();
                    if (pitch < 0) {
                        continue;
                    }
                    int semitone = pitch % 12;
                    int jump = modeNote - semitone;
                    if (jump > 6) {
                        n.setPitch(MidiUtils.octavePitch(pitch) + modeNote - 12);
                    } else if (jump < -6) {
                        n.setPitch(MidiUtils.octavePitch(pitch) + modeNote + 12);
                    } else {
                        n.setPitch(MidiUtils.octavePitch(pitch) + modeNote);
                    }
                    //LG.i(pitch - n.getPitch());
                    difference--;
                    modeNoteChanges.add(n);
                    if (difference <= 0) {
                        break;
                    }
                }
                LG.d("MODE: Remaining difference after first pairs: " + (-1 * difference));
            }

        }
        return modeNoteChanges;
    }

    private void applyChordNoteTargets(List<Note> fullMelody,
                                       Map<Integer, List<Note>> fullMelodyMap, List<int[]> chords, List<Note> modeNoteChanges,
                                       Section sec, MelodyPart mp) {

        int chordNoteTargetChance = sec.getVariation(0, mp.getAbsoluteOrder())
                .contains(Integer.valueOf(3))
                ? (gc.getMelodyChordNoteTarget()
                + (100 - gc.getMelodyChordNoteTarget()) / 3)
                : gc.getMelodyChordNoteTarget();

        if (chordNoteTargetChance > 0) {
            int chordSize = fullMelodyMap.keySet().size();
            double requiredPercentage = chordNoteTargetChance / 100.0;
            int needed = (int) Math.ceil(fullMelody.stream().filter(e -> e.getPitch() >= 0).count()
                    * requiredPercentage);
            // step 1: get count of how many

            // step 2: get % of how many of the others need to be turned into chord notes

            // step 3: apply %
            int found = 0;
            for (int chordIndex = 0; chordIndex < chordSize; chordIndex++) {
                List<Note> notes = fullMelodyMap.get(chordIndex);
                List<Integer> chordNotes = MidiUtils
                        .chordToPitches(chords.get(chordIndex % chords.size()));
                for (Note n : notes) {
                    if (n.getPitch() >= 0 && chordNotes.contains(n.getPitch() % 12)) {
                        found++;
                    }
                }
            }

            LG.d("Found Chord notes: " + found + ", needed: " + needed);
            if (found < needed) {
                int difference = needed - found;
				/*int chanceToConvertOthers = 100
						- ((100 * (fullMelody.size() - difference)) / fullMelody.size());*/

                for (int chordIndex = 0; chordIndex < chordSize; chordIndex++) {
                    if (difference <= 0) {
                        break;
                    }
                    int maxDifferenceForThisChord = Math.max(1, (difference + 4) / (chordSize));
                    List<Note> notes = fullMelodyMap.get(chordIndex);
                    List<Integer> chordNotes = MidiUtils
                            .chordToPitches(chords.get(chordIndex % chords.size()));

                    List<Note> sortedNotes = new ArrayList<>(notes);
                    Collections.sort(sortedNotes, (e1, e2) -> MidiUtils
                            .compareNotesByDistanceFromChordPitches(e1, e2, chordNotes));

                    // put mode notes at the end, but still sorted - so that chord notes start primarily from corrections of regular notes
                    List<Note> sortedModeNotes = new ArrayList<>();
                    for (int i = 0; i < sortedNotes.size(); i++) {
                        if (modeNoteChanges.contains(sortedNotes.get(i))) {
                            sortedModeNotes.add(sortedNotes.get(i));
                        }
                    }
                    sortedNotes.removeAll(sortedModeNotes);
                    sortedNotes.addAll(sortedModeNotes);

                    for (Note n : notes) {
                        if (n.getPitch() < 0) {
                            continue;
                        }
                        if (!chordNotes.contains(n.getPitch() % 12)) {
                            n.setPitch(n.getPitch() - (n.getPitch() % 12)
                                    + MidiUtils.getClosestFromList(chordNotes, n.getPitch() % 12));
                            difference--;
                            maxDifferenceForThisChord--;
                        }
                        if (difference <= 0 || maxDifferenceForThisChord <= 0) {
                            break;
                        }
                    }
                }

                LG.d("CHORD: Remaining difference: " + (-1 * difference));
            }
        }
    }
    private MelodyBlock convertMelodyBlockWithCustomMap(MelodyBlock mb, Map<Integer, List<PhraseNote>> customBlockDurationsMap, int blockIndex) {
        if (customBlockDurationsMap == null || customBlockDurationsMap.isEmpty() || customBlockDurationsMap.get(blockIndex) == null) {
            LG.i("No block to be converted: " + blockIndex);
            return mb;
        }
        MelodyBlock newMb = new MelodyBlock(customBlockDurationsMap.get(blockIndex).stream().map(e -> e.getDynamic() > 0 ? e.getPitch() : Pitches.REST).collect(Collectors.toList()),
                customBlockDurationsMap.get(blockIndex).stream().map(e -> e.getDuration()).collect(Collectors.toList()), false);
        int counter = 0;
        for (int i = 0; i < mb.notes.size(); i++) {
            for (int j = counter; j < newMb.notes.size(); j++) {
                if (newMb.notes.get(j) != Pitches.REST) {
                    newMb.notes.set(j, mb.notes.get(i));
                    counter = j;
                    break;
                }
            }
        }
        return newMb;
    }

    private List<Note> addEmbellishedNotes(Note n, Random embellishmentGenerator) {
        // pick embellishment rhythm - if note RV is multiple of dotted 8th use length 3, otherwise length 4
        int notesNeeded = MidiUtils.isMultiple(n.getRhythmValue(), MidiGenerator.Durations.DOTTED_EIGHTH_NOTE) ? 3
                : 4;
        Random localEmbGenerator = new Random(embellishmentGenerator.nextInt());
        if (localEmbGenerator.nextInt(100) >= EMBELLISHMENT_CHANCE) {
            return Collections.singletonList(n);
        }
        boolean shortNotes = n.getRhythmValue() > MidiGenerator.Durations.QUARTER_NOTE - DBL_ERR;
        int[] melodySkeletonDurationWeights = shortNotes
                ? MelodyUtils.normalizedCumulativeWeights(100, 100, 300, 100, 300, 100, 100)
                : MelodyUtils.normalizedCumulativeWeights(100, 300, 100, 300, 100, 100);
        Rhythm blockRhythm = new Rhythm(localEmbGenerator.nextInt(), n.getRhythmValue(),
                shortNotes ? MELODY_SKELETON_DURATIONS_SHORT : MELODY_SKELETON_DURATIONS,
                melodySkeletonDurationWeights);
        List<Double> blockDurations = blockRhythm.makeDurations(notesNeeded,
                MidiGenerator.Durations.SIXTEENTH_NOTE);

        // split note according to rhythm, apply pitch change to notes
        // pick embellishment pitch pattern according to rhythm length (3 or 4)
        List<Integer[]> pitchPatterns = BlockType.NEIGHBORY.blocks.stream()
                .filter(e -> e.length == notesNeeded).collect(Collectors.toList());
        if (pitchPatterns.isEmpty()) {
            LG.e("No melody pitch patterns found for: " + notesNeeded);
            return Collections.singletonList(n);
        }
        Integer[] chosenPattern = pitchPatterns
                .get(localEmbGenerator.nextInt(pitchPatterns.size()));
        int pitch = n.getPitch();
        if (pitch == Pitches.REST) {
            return Collections.singletonList(n);
        }
        int semis = pitch % 12;
        int octavePitch = pitch - semis;
        List<Note> embNotes = new ArrayList<>();
        for (int i = 0; i < blockDurations.size(); i++) {
            int pitchIndex = MidiUtils.MAJ_SCALE.indexOf(semis);
            if (pitchIndex < 0) {
                LG.e("------------------------------------------------PITCH FROM MAIN MELODY BLOCK WAS NOT IN MAJOR SCALE!");
            }
            int newPitchIndex = pitchIndex + chosenPattern[i];
            int newPitchSemis = MidiUtils.MAJ_SCALE.get((newPitchIndex + 70) % 7);
            int newPitch = octavePitch + newPitchSemis;
            if (newPitchIndex >= 7) {
                newPitch += 12;
            } else if (newPitchIndex < 0) {
                newPitch -= 12;
            }
            Note embNote = new Note(newPitch, blockDurations.get(i));
            embNote.setDuration(blockDurations.get(i) * MidiGenerator.GLOBAL_DURATION_MULTIPLIER);

            // slightly lower volume on following notes
            embNote.setDynamic(n.getDynamic() - (i * 5));

            // add note
            embNotes.add(embNote);
        }
        return embNotes;
    }

    protected List<MelodyBlock> generateMelodyBlocksForDurations(MelodyPart mp, Section sec,
                                                                 List<Double> durations, List<int[]> roots, int melodyBlockGeneratorSeed, int blockOffset,
                                                                 List<Integer> blockChanges, int maxJump, int startingNote, int chordIndex,
                                                                 List<Integer> forcedLengths, int remainingDirChanges, List<Integer> usedMelodyBlockJumpPreference,
                                                                 Map<Integer, List<PhraseNote>> customUserDurationsByBlock, int numBlocks) {
        List<MelodyBlock> mbs = new ArrayList<>();

        //LG.d(StringUtils.join(melodySkeletonDurationWeights, ','));
        int offsettedMelodyGeneratorSeed = melodyBlockGeneratorSeed + blockOffset;
        Random blockNotesGenerator = new Random(offsettedMelodyGeneratorSeed);

        int prevBlockType = Integer.MIN_VALUE;
        int adjustment = startingNote;

        int remainingVariance = 4;

        for (int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
            if (blockIndex > 0) {
                adjustment += blockChanges.get(blockIndex - 1);
            }
            double blockDuration = !customUserDurationsByBlock.isEmpty()
                    ? customUserDurationsByBlock.get(blockIndex).stream().filter(e -> e.getDynamic() > 0).mapToDouble(e -> e.getDuration()).sum()
                    : durations.get(blockIndex);

            int speed = MidiGeneratorUtils.adjustChanceParamForTransition(mp.getSpeed(), sec,
                    chordIndex, roots.size(), 40, 0.25, false, false);
            speed = OMNI.clamp(speed, -100, 100);
            int addQuick = (speed - 50) * 2;
            int addSlow = addQuick * -1;
            boolean shortNotes = blockDuration < MidiGenerator.Durations.QUARTER_NOTE - DBL_ERR;
            int[] melodySkeletonDurationWeights = shortNotes
                    ? MelodyUtils.normalizedCumulativeWeights(100 + addQuick, 100 + addQuick, 300 + addQuick,
                    100 + addQuick, 300 + addSlow, 100 + addSlow, 100 + addSlow)
                    : MelodyUtils.normalizedCumulativeWeights(100 + addQuick, 300 + addQuick,
                    100 + addQuick, 300 + addSlow, 100 + addSlow, 100 + addSlow);

            Rhythm blockRhythm = new Rhythm(offsettedMelodyGeneratorSeed + blockIndex,
                    blockDuration,
                    shortNotes ? MELODY_SKELETON_DURATIONS_SHORT : MELODY_SKELETON_DURATIONS,
                    melodySkeletonDurationWeights);
            //int length = blockNotesGenerator.nextInt(100) < gc.getMelodyQuickness() ? 4 : 3;

            blockNotesGenerator.setSeed(offsettedMelodyGeneratorSeed + blockIndex);
            Random generateNewBlocksDecider = new Random(offsettedMelodyGeneratorSeed + blockIndex);
            boolean GENERATE_NEW_BLOCKS = generateNewBlocksDecider.nextInt(100) < gc
                    .getMelodyNewBlocksChance();

            Integer forcedBlockLength = (forcedLengths != null) ? forcedLengths.get(blockIndex) : null;
            if (forcedBlockLength == null && !customUserDurationsByBlock.isEmpty()) {
                forcedBlockLength = (int) customUserDurationsByBlock.get(blockIndex).stream().filter(e -> e.getDynamic() > 0).count();
            }
            Pair<Integer, Integer[]> typeBlock = (GENERATE_NEW_BLOCKS)
                    ? MelodyUtils.generateBlockByBlockChangeAndLength(blockChanges.get(blockIndex),
                    maxJump, blockNotesGenerator,
                    forcedBlockLength,
                    remainingVariance, remainingDirChanges)
                    : MelodyUtils.getRandomByApproxBlockChangeAndLength(
                    blockChanges.get(blockIndex), maxJump, blockNotesGenerator,
                    forcedBlockLength,
                    remainingVariance, remainingDirChanges, usedMelodyBlockJumpPreference, gc.getMelodyBlockTypePreference());
            Integer[] blockNotesArray = typeBlock.getRight();
            int blockType = typeBlock.getLeft();

            boolean chordyBlockNotMatchingChord = false;
            if (blockType == 3) {
                int blockStart = (adjustment + 70) % 7;
                chordyBlockNotMatchingChord = !MelodyUtils.cMajorSubstituteNotes
                        .contains(blockStart);
                if (chordyBlockNotMatchingChord) {
                    LG.d("SWAPPING CHORDY BLOCK, blockStart: " + blockStart);
                }
            }

            // try to find a different type for this block change (only for static/non generated blocks)
            if (blockType != Integer.MAX_VALUE && (blockType == prevBlockType || chordyBlockNotMatchingChord)) {
                int length = blockNotesArray.length;
                List<Integer> typesToChoose = new ArrayList<>();
                for (int j = 0; j < BlockType.values().length; j++) {
                    if (j != blockType && BlockType.AVAILABLE_BLOCK_CHANGES_PER_TYPE.get(j)
                            .contains(Math.abs(blockChanges.get(blockIndex)))) {
                        typesToChoose.add(j);
                    }
                }
                if (typesToChoose.size() > 0) {
                    int randomType = BlockType.getWeightedType(typesToChoose, gc.getMelodyBlockTypePreference(), blockNotesGenerator.nextInt(100));
                    Integer[] typedBlock = MelodyUtils.getRandomForTypeAndBlockChangeAndLength(
                            randomType, blockChanges.get(blockIndex), length, blockNotesGenerator,
                            0);
                    if (typedBlock != null) {
                        blockNotesArray = typedBlock;
                        blockType = randomType;
                        LG.d("Found new block!");
                    } else {
                        LG.d("Different block not found in other types!");
                    }
                } else {
                    LG.d("Other types don't have this block!");
                }


            }
            remainingVariance = Math.max(0,
                    remainingVariance - MelodyUtils.variance(blockNotesArray));
            remainingDirChanges = Math.max(0,
                    remainingDirChanges - MelodyUtils.interblockDirectionChange(blockNotesArray));
            List<Integer> blockNotes = Arrays.asList(blockNotesArray);
            List<Double> blockDurations = !customUserDurationsByBlock.isEmpty() && customUserDurationsByBlock.get(blockIndex).size() == blockNotes.size()
                    ? customUserDurationsByBlock.get(blockIndex).stream().filter(e -> e.getDynamic() > 0).map(e -> e.getDuration()).collect(Collectors.toList())
                    : blockRhythm.makeDurations(blockNotes.size(), mp.getSpeed() < 20 ? MidiGenerator.Durations.QUARTER_NOTE : MidiGenerator.Durations.SIXTEENTH_NOTE);


            if (gc.isMelodyArpySurprises() && (blockNotes.size() == 4)
                    && (mp.getSpeed() < 20 || mp.getSpeed() > 80)) {
                double wrongNoteLow = (mp.getSpeed() < 20) ? MidiGenerator.Durations.SIXTEENTH_NOTE * 0.99
                        : MidiGenerator.Durations.DOTTED_QUARTER_NOTE * 0.99;
                double wrongNoteHigh = (mp.getSpeed() < 20) ? MidiGenerator.Durations.SIXTEENTH_NOTE * 1.01
                        : MidiGenerator.Durations.WHOLE_NOTE * 1.01;
                boolean containsWrongNote = blockDurations.stream()
                        .anyMatch(e -> (e > wrongNoteLow && e < wrongNoteHigh));
                if (containsWrongNote) {
                    double arpyDuration = blockDuration / blockNotes.size();
                    for (int j = 0; j < blockDurations.size(); j++) {
                        blockDurations.set(j, arpyDuration);
                    }
                    LG.d("Arpy surprise for block#: " + blockNotes.size() + ", duration: "
                            + durations.get(blockIndex));
                }

            }

            LG.d("Block durations: " + StringUtils.join(blockDurations, ","));
            prevBlockType = blockType;
            //LG.d("Block Durations size: " + blockDurations.size());
            MelodyBlock mb = new MelodyBlock(blockNotes, blockDurations, false);
            mbs.add(mb);
            LG.d("Created block: " + StringUtils.join(blockNotes, ","));
        }
        return mbs;
    }

    void processUserMelody(Phrase userMelody) {
        if (!chordMelodyMap1.isEmpty() || !(MidiGenerator.userChords == null || MidiGenerator.userChords.isEmpty())) {
            return;
        }

        int chordCounter = 0;

        double mult = MidiGenerator.getBeatDurationMult(mg.currentSection);
        double separatorValue = MidiGenerator.Durations.WHOLE_NOTE * mult;
        double chordSeparator = separatorValue;
        Vector<Note> noteList = userMelody.getNoteList();
        if (!chordMelodyMap1.containsKey(Integer.valueOf(0))) {
            chordMelodyMap1.put(Integer.valueOf(0), new ArrayList<>());
        }
        double rhythmCounter = 0;
        List<Double> progDurations = new ArrayList<>();
        progDurations.add(separatorValue);
        for (Note n : noteList) {
            LG.d("Rhythm counter: " + rhythmCounter);
            if (rhythmCounter >= chordSeparator - DBL_ERR) {
                LG.d("NEXT CHORD!");
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
        LG.d("Rhythm counter end: " + rhythmCounter);
        while (rhythmCounter >= chordSeparator + DBL_ERR) {
            LG.d("NEXT CHORD!");
            chordSeparator += separatorValue;
            chordCounter++;
            progDurations.add(separatorValue);
            if (!chordMelodyMap1.containsKey(Integer.valueOf(chordCounter))) {
                chordMelodyMap1.put(Integer.valueOf(chordCounter), new ArrayList<>());
            }
            chordMelodyMap1.get(Integer.valueOf(chordCounter))
                    .add(noteList.get(noteList.size() - 1));
        }
        LG.i("Processed melody, chords: " + (chordCounter + 1));
        List<String> chordStrings = MidiGenerator.getChordsFromMelodyPitches(1, mg.progressionDurations,
                chordMelodyMap1, MidiUtils.freqMap);
		/*List<String> spicyChordStrings = getChordsFromMelodyPitches(1, chordMelodyMap1,
				MidiUtils.freqMap);
		for (int i = 0; i < spicyChordStrings.size(); i++) {
			if (chordStrings.get(i).charAt(0) == spicyChordStrings.get(i).charAt(0)) {
				chordStrings.set(i, spicyChordStrings.get(i));
			}
		}*/

        populateMelodyBasedProgression(chordStrings, 0, chordMelodyMap1.keySet().size());
        mg.progressionDurations = progDurations;
        MidiGenerator.chordInts = chordStrings;
    }

    private void populateMelodyBasedProgression(List<String> chordStrings, int start, int end) {
        List<int[]> altChordProg = new ArrayList<>();

        for (int i = 0; i < start; i++) {
            melodyBasedRootProgression
                    .add(Arrays.copyOf(mg.rootProgression.get(i), mg.rootProgression.get(i).length));
            altChordProg
                    .add(Arrays.copyOf(mg.chordProgression.get(i), mg.chordProgression.get(i).length));
        }
        for (int i = start; i < end; i++) {
            int[] mappedChord = MidiUtils.mappedChord(chordStrings.get(i));
            altChordProg.add(mappedChord);
            melodyBasedRootProgression.add(Arrays.copyOf(mappedChord, mappedChord.length));
        }
        for (int i = end; i < chordStrings.size(); i++) {
            melodyBasedRootProgression
                    .add(Arrays.copyOf(mg.rootProgression.get(i), mg.rootProgression.get(i).length));
            altChordProg
                    .add(Arrays.copyOf(mg.chordProgression.get(i), mg.chordProgression.get(i).length));
        }

        melodyBasedChordProgression = squishChordProgression(altChordProg,
                gc.isSpiceFlattenBigChords(), gc.getRandomSeed(),
                gc.getChordGenSettings().getFlattenVoicingChance(), new ArrayList<>(), null);


        mg.chordProgression = melodyBasedChordProgression;
        mg.rootProgression = melodyBasedRootProgression;
        LG.i(StringUtils.join(chordStrings, ","));
    }
}
