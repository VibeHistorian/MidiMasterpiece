package org.vibehistorian.midimasterpiece.midigenerator;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "chordPart")
@XmlType(propOrder = {})
public class ChordPart extends InstPart {
	private int transitionChance = 0;
	private int transitionSplit = 625;
	
	private int strum = 0;
	
	
	private MidiUtils.POOL instPool = MidiUtils.POOL.PLUCK;
	
	public ChordPart() {
		
	}
	
	public ChordPart(int instrument, int transitionChance, int transitionSplit, int strum,
			int delay, int transpose, int patternSeed, RhythmPattern pattern, int patternShift,
			int order, boolean muted, int midiChannel) {
		super();
		this.setInstrument(instrument);
		this.transitionChance = transitionChance;
		this.transitionSplit = transitionSplit;
		this.strum = strum;
		this.delay = delay;
		this.transpose = transpose;
		this.patternSeed = patternSeed;
		this.pattern = pattern;
		this.patternShift = patternShift;
		this.order = order;
		this.muted = muted;
		this.midiChannel = midiChannel;
	}
	
	
	public int getTransitionChance() {
		return transitionChance;
	}
	
	public void setTransitionChance(int transitionChance) {
		this.transitionChance = transitionChance;
	}
	
	public int getTransitionSplit() {
		return transitionSplit;
	}
	
	public void setTransitionSplit(int transitionSplit) {
		this.transitionSplit = transitionSplit;
	}
	
	public int getStrum() {
		return strum;
	}
	
	public void setStrum(int strum) {
		this.strum = strum;
	}
	
	
	@XmlAttribute
	public int getOrder() {
		return order;
	}
	
	public void setOrder(int order) {
		this.order = order;
	}
	
	
	public MidiUtils.POOL getInstPool() {
		return instPool;
	}
	
	public void setInstPool(MidiUtils.POOL instPool) {
		this.instPool = instPool;
	}
	
}
