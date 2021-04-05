package org.vibehistorian.midimasterpiece.midigenerator;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "GUIConfig")
@XmlType(propOrder = {})
public class GUIConfig {
	/*
	 * soundbank, piece length, fixed duration CB, minor CB,
	 * melody/c1/c2/arp/bass/drums CB and inst choice, arp#, random#, 
	 * random pattern, repeatable, pauses CBs
	 * transpose, bpm
	 * max note jump, note#1 from chord, randomized, pause%, spice%, dim/aug
	 * first/last c, CUSTOM CHORDS, chord durations
	 * random seed
	 */
	
	public GUIConfig() {
		
	}
	
	public String getSoundbankName() {
		return soundbankName;
	}
	
	public void setSoundbankName(String soundbankName) {
		this.soundbankName = soundbankName;
	}
	
	public int getPieceLength() {
		return pieceLength;
	}
	
	public void setPieceLength(int pieceLength) {
		this.pieceLength = pieceLength;
	}
	
	public boolean isFixedDuration() {
		return fixedDuration;
	}
	
	public void setFixedDuration(boolean fixedDuration) {
		this.fixedDuration = fixedDuration;
	}
	
	public boolean isMinor() {
		return minor;
	}
	
	public void setMinor(boolean minor) {
		this.minor = minor;
	}
	
	public boolean isMelodyEnable() {
		return melodyEnable;
	}
	
	public void setMelodyEnable(boolean melodyEnable) {
		this.melodyEnable = melodyEnable;
	}
	
	public boolean isChords1Enable() {
		return chords1Enable;
	}
	
	public void setChords1Enable(boolean chords1Enable) {
		this.chords1Enable = chords1Enable;
	}
	
	public boolean isChords2Enable() {
		return chords2Enable;
	}
	
	public void setChords2Enable(boolean chords2Enable) {
		this.chords2Enable = chords2Enable;
	}
	
	public boolean isArp1ArpEnable() {
		return arp1ArpEnable;
	}
	
	public void setArp1ArpEnable(boolean arp1ArpEnable) {
		this.arp1ArpEnable = arp1ArpEnable;
	}
	
	public boolean isBassRootsEnable() {
		return bassRootsEnable;
	}
	
	public void setBassRootsEnable(boolean bassRootsEnable) {
		this.bassRootsEnable = bassRootsEnable;
	}
	
	public int getMelodyInst() {
		return melodyInst;
	}
	
	public void setMelodyInst(int melodyInst) {
		this.melodyInst = melodyInst;
	}
	
	public int getChords1Inst() {
		return chords1Inst;
	}
	
	public void setChords1Inst(int chords1Inst) {
		this.chords1Inst = chords1Inst;
	}
	
	public int getChords2Inst() {
		return chords2Inst;
	}
	
	public void setChords2Inst(int chords2Inst) {
		this.chords2Inst = chords2Inst;
	}
	
	public int getArp1ArpInst() {
		return arp1ArpInst;
	}
	
	public void setArp1ArpInst(int arp1ArpInst) {
		this.arp1ArpInst = arp1ArpInst;
	}
	
	public int getBassRootsInst() {
		return bassRootsInst;
	}
	
	public void setBassRootsInst(int bassRootsInst) {
		this.bassRootsInst = bassRootsInst;
	}
	
	public boolean isDrumsEnable() {
		return drumsEnable;
	}
	
	public void setDrumsEnable(boolean drumsEnable) {
		this.drumsEnable = drumsEnable;
	}
	
	public boolean isArpRandomCount() {
		return arpRandomCount;
	}
	
	public void setArpRandomCount(boolean arpRandomCount) {
		this.arpRandomCount = arpRandomCount;
	}
	
	public boolean isArpRandomPattern() {
		return arpRandomPattern;
	}
	
	public void setArpRandomPattern(boolean arpRandomPattern) {
		this.arpRandomPattern = arpRandomPattern;
	}
	
	public boolean isArpRandomRepeats() {
		return arpRandomRepeats;
	}
	
	public void setArpRandomRepeats(boolean arpRandomRepeats) {
		this.arpRandomRepeats = arpRandomRepeats;
	}
	
	public boolean isArpRandomPauses() {
		return arpRandomPauses;
	}
	
	public void setArpRandomPauses(boolean arpRandomPauses) {
		this.arpRandomPauses = arpRandomPauses;
	}
	
	public int getTranspose() {
		return transpose;
	}
	
	public void setTranspose(int transpose) {
		this.transpose = transpose;
	}
	
	public double getBpm() {
		return bpm;
	}
	
	public void setBpm(double bpm) {
		this.bpm = bpm;
	}
	
	public int getMaxNoteJump() {
		return maxNoteJump;
	}
	
	public void setMaxNoteJump(int maxNoteJump) {
		this.maxNoteJump = maxNoteJump;
	}
	
	public int getPauseChance() {
		return pauseChance;
	}
	
	public void setPauseChance(int pauseChance) {
		this.pauseChance = pauseChance;
	}
	
	public int getSpiceChance() {
		return spiceChance;
	}
	
	public void setSpiceChance(int spiceChance) {
		this.spiceChance = spiceChance;
	}
	
	public boolean isFirstNoteFromChord() {
		return firstNoteFromChord;
	}
	
	public void setFirstNoteFromChord(boolean firstNoteFromChord) {
		this.firstNoteFromChord = firstNoteFromChord;
	}
	
	public boolean isFirstNoteRandomized() {
		return firstNoteRandomized;
	}
	
	public void setFirstNoteRandomized(boolean firstNoteRandomized) {
		this.firstNoteRandomized = firstNoteRandomized;
	}
	
	public boolean isDimAugEnabled() {
		return dimAugEnabled;
	}
	
	public void setDimAugEnabled(boolean dimAugEnabled) {
		this.dimAugEnabled = dimAugEnabled;
	}
	
	public String getFirstChord() {
		return firstChord;
	}
	
	public void setFirstChord(String firstChord) {
		this.firstChord = firstChord;
	}
	
	public String getLastChord() {
		return lastChord;
	}
	
	public void setLastChord(String lastChord) {
		this.lastChord = lastChord;
	}
	
	public String getCustomChords() {
		return customChords;
	}
	
	public void setCustomChords(String customChords) {
		this.customChords = customChords;
	}
	
	public long getRandomSeed() {
		return randomSeed;
	}
	
	@XmlAttribute
	public void setRandomSeed(long randomSeed) {
		this.randomSeed = randomSeed;
	}
	
	private String soundbankName = "MuseScore_General.sf2";
	
	private int pieceLength = 4;
	
	private boolean fixedDuration = true;
	
	private boolean minor = false;
	
	private boolean melodyEnable = true;
	private boolean chords1Enable = false;
	private boolean chords2Enable = false;
	private boolean arp1ArpEnable = true;
	private boolean arp2ArpEnable = true;
	private boolean bassRootsEnable = true;
	
	private int melodyInst = 46;
	private int chords1Inst = 107;
	private int chords2Inst = 95;
	private int arp1ArpInst = 46;
	private int arp2ArpInst = 4;
	private int bassRootsInst = 33;
	
	private boolean drumsEnable = false;
	
	private int secondArpMultiplier = 4;
	private int secondArpOctaveAdjust = 0;
	private int arpCustomCount = 3;
	private boolean arpRandomCount = true;
	private boolean arpRandomPattern = true;
	private boolean arpRandomRepeats = true;
	private boolean arpRandomPauses = true;
	
	private int transpose = 0;
	private double bpm = 80;
	private boolean arpAffectsBpm = true;
	
	private int maxNoteJump = 4;
	private int maxExceptions = 1;
	
	private int pauseChance = 25;
	private int melodyPauseChance = 20;
	private int secondArpPauseChance = 50;
	private int spiceChance = 8;
	private int chordTransitionChance = 0;
	private int chordSlashChance = 0;
	private int chordStrum = 0;
	private int secondChordStrum = 0;
	
	private List<DrumPart> drumParts = new ArrayList<>();
	private boolean drumPatternAffectsVelocity = true;
	
	private boolean firstNoteFromChord = true;
	private boolean firstNoteRandomized = true;
	private boolean dimAugEnabled = false;
	
	private String firstChord = "R";
	private String lastChord = "R";
	
	private boolean customChordsEnabled = true;
	private String customChords = "R";
	private String customChordDurations = "2,2,2,2";
	
	
	private long randomSeed = 0;
	private long userMelodySeed = 0;
	
	//start-transients
	private transient boolean lockMelody = false;
	private transient boolean lockChords1 = true;
	private transient boolean lockChords2 = true;
	private transient boolean lockArp1Arp = false;
	private transient boolean lockArp2Arp = false;
	private transient boolean lockBassRoots = false;
	
	
	public boolean isLockArp2Arp() {
		return lockArp2Arp;
	}
	
	public void setLockArp2Arp(boolean lockArp2Arp) {
		this.lockArp2Arp = lockArp2Arp;
	}
	
	
	public boolean isLockMelody() {
		return lockMelody;
	}
	
	@XmlTransient
	public void setLockMelody(boolean lockMelody) {
		this.lockMelody = lockMelody;
	}
	
	public boolean isLockChords1() {
		return lockChords1;
	}
	
	@XmlTransient
	public void setLockChords1(boolean lockChords1) {
		this.lockChords1 = lockChords1;
	}
	
	public boolean isLockChords2() {
		return lockChords2;
	}
	
	@XmlTransient
	public void setLockChords2(boolean lockChords2) {
		this.lockChords2 = lockChords2;
	}
	
	public boolean isLockArp1Arp() {
		return lockArp1Arp;
	}
	
	@XmlTransient
	public void setLockArp1Arp(boolean lockArp1Arp) {
		this.lockArp1Arp = lockArp1Arp;
	}
	
	public boolean isLockBassRoots() {
		return lockBassRoots;
	}
	
	@XmlTransient
	public void setLockBassRoots(boolean lockBassRoots) {
		this.lockBassRoots = lockBassRoots;
	}
	//end-transients
	
	public boolean isArp2ArpEnable() {
		return arp2ArpEnable;
	}
	
	public void setArp2ArpEnable(boolean arp2ArpEnable) {
		this.arp2ArpEnable = arp2ArpEnable;
	}
	
	public int getArp2ArpInst() {
		return arp2ArpInst;
	}
	
	public void setArp2ArpInst(int arp2ArpInst) {
		this.arp2ArpInst = arp2ArpInst;
	}
	
	public String getCustomChordDurations() {
		return customChordDurations;
	}
	
	public void setCustomChordDurations(String customChordDurations) {
		this.customChordDurations = customChordDurations;
	}
	
	public boolean isCustomChordsEnabled() {
		return customChordsEnabled;
	}
	
	public void setCustomChordsEnabled(boolean customChordsEnabled) {
		this.customChordsEnabled = customChordsEnabled;
	}
	
	public int getArpCustomCount() {
		return arpCustomCount;
	}
	
	public void setArpCustomCount(int arpCustomCount) {
		this.arpCustomCount = arpCustomCount;
	}
	
	public long getUserMelodySeed() {
		return userMelodySeed;
	}
	
	public void setUserMelodySeed(long userMelodySeed) {
		this.userMelodySeed = userMelodySeed;
	}
	
	public int getMelodyPauseChance() {
		return melodyPauseChance;
	}
	
	public void setMelodyPauseChance(int melodyPauseChance) {
		this.melodyPauseChance = melodyPauseChance;
	}
	
	public int getSecondArpMultiplier() {
		return secondArpMultiplier;
	}
	
	public void setSecondArpMultiplier(int secondArpMultiplier) {
		this.secondArpMultiplier = secondArpMultiplier;
	}
	
	public int getSecondArpOctaveAdjust() {
		return secondArpOctaveAdjust;
	}
	
	public void setSecondArpOctaveAdjust(int secondArpOctaveAdjust) {
		this.secondArpOctaveAdjust = secondArpOctaveAdjust;
	}
	
	public int getSecondArpPauseChance() {
		return secondArpPauseChance;
	}
	
	public void setSecondArpPauseChance(int secondArpPauseChance) {
		this.secondArpPauseChance = secondArpPauseChance;
	}
	
	public boolean isArpAffectsBpm() {
		return arpAffectsBpm;
	}
	
	public void setArpAffectsBpm(boolean arpAffectBpm) {
		this.arpAffectsBpm = arpAffectBpm;
	}
	
	public int getMaxExceptions() {
		return maxExceptions;
	}
	
	public void setMaxExceptions(int maxExceptions) {
		this.maxExceptions = maxExceptions;
	}
	
	public List<DrumPart> getDrumParts() {
		return drumParts;
	}
	
	public void setDrumParts(List<DrumPart> drumParts) {
		this.drumParts = drumParts;
	}
	
	public boolean isDrumPatternAffectsVelocity() {
		return drumPatternAffectsVelocity;
	}
	
	public void setDrumPatternAffectsVelocity(boolean drumPatternAffectsVelocity) {
		this.drumPatternAffectsVelocity = drumPatternAffectsVelocity;
	}
	
	public int getChordTransitionChance() {
		return chordTransitionChance;
	}
	
	public void setChordTransitionChance(int chordTransitionChance) {
		this.chordTransitionChance = chordTransitionChance;
	}
	
	public int getChordStrum() {
		return chordStrum;
	}
	
	public void setChordStrum(int chordStrum) {
		this.chordStrum = chordStrum;
	}
	
	public int getSecondChordStrum() {
		return secondChordStrum;
	}
	
	public void setSecondChordStrum(int secondChordStrum) {
		this.secondChordStrum = secondChordStrum;
	}
	
	public int getChordSlashChance() {
		return chordSlashChance;
	}
	
	public void setChordSlashChance(int chordSlashChance) {
		this.chordSlashChance = chordSlashChance;
	}
	
}
