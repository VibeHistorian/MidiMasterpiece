package org.vibehistorian.vibecomposer.Parts;

import org.vibehistorian.vibecomposer.Helpers.PhraseNotes;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlRootElement(name = "melodyPart")
@XmlType(propOrder = {})
public class MelodyPart extends InstPart {

	private boolean fillPauses = false;
	private List<Integer> chordNoteChoices = null;
	private List<Integer> melodyPatternOffsets = null;
	private int maxBlockChange = 7;
	private int leadChordsChance = 0;
	private int blockJump = 0;
	private int maxNoteExceptions = 2;
	private int alternatingRhythmChance = 50;
	private int doubledRhythmChance = 50;
	private int splitChance = 0;
	private int noteExceptionChance = 33;
	private int speed = 0;
	private int startNoteChance = 100;
	private boolean patternFlexible = false;

	private PhraseNotes customDurationNotes = null;

	public MelodyPart() {
		partNum = 0;
	}

	public boolean isFillPauses() {
		return fillPauses;
	}

	public void setFillPauses(boolean fillPauses) {
		this.fillPauses = fillPauses;
	}

	public List<Integer> getChordNoteChoices() {
		return chordNoteChoices;
	}

	public void setChordNoteChoices(List<Integer> chordNoteChoices) {
		this.chordNoteChoices = chordNoteChoices;
	}

	public List<Integer> getMelodyPatternOffsets() {
		return melodyPatternOffsets;
	}

	public void setMelodyPatternOffsets(List<Integer> melodyPatternOffsets) {
		this.melodyPatternOffsets = melodyPatternOffsets;
	}

	public int getMaxBlockChange() {
		return maxBlockChange;
	}

	public void setMaxBlockChange(int maxBlockChange) {
		this.maxBlockChange = maxBlockChange;
	}

	public int getLeadChordsChance() {
		return leadChordsChance;
	}

	public void setLeadChordsChance(int leadChordsChance) {
		this.leadChordsChance = leadChordsChance;
	}

	public int getBlockJump() {
		return blockJump;
	}

	public void setBlockJump(int blockJump) {
		this.blockJump = blockJump;
	}

	public int getMaxNoteExceptions() {
		return maxNoteExceptions;
	}

	public void setMaxNoteExceptions(int maxNoteExceptions) {
		this.maxNoteExceptions = maxNoteExceptions;
	}

	public int getAlternatingRhythmChance() {
		return alternatingRhythmChance;
	}

	public void setAlternatingRhythmChance(int alternatingRhythmChance) {
		this.alternatingRhythmChance = alternatingRhythmChance;
	}

	public int getDoubledRhythmChance() {
		return doubledRhythmChance;
	}

	public void setDoubledRhythmChance(int doubledRhythmChance) {
		this.doubledRhythmChance = doubledRhythmChance;
	}

	public int getSplitChance() {
		return splitChance;
	}

	public void setSplitChance(int splitChance) {
		this.splitChance = splitChance;
	}

	public int getNoteExceptionChance() {
		return noteExceptionChance;
	}

	public void setNoteExceptionChance(int noteExceptionChance) {
		this.noteExceptionChance = noteExceptionChance;
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public int getPartNum() {
		return 0;
	}

	public int getStartNoteChance() {
		return startNoteChance;
	}

	public void setStartNoteChance(int startNoteChance) {
		this.startNoteChance = startNoteChance;
	}

	public boolean isPatternFlexible() {
		return patternFlexible;
	}

	public void setPatternFlexible(boolean patternFlexible) {
		this.patternFlexible = patternFlexible;
	};

	public PhraseNotes getCustomDurationNotes() {
		return customDurationNotes;
	}

	public void setCustomDurationNotes(PhraseNotes customDurationNotes) {
		this.customDurationNotes = customDurationNotes;
	}
}
