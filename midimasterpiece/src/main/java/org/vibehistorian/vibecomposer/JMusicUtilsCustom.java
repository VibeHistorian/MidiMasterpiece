package org.vibehistorian.vibecomposer;

import jm.JMC;
import jm.constants.Pitches;
import jm.midi.SMF;
import jm.midi.Track;
import jm.midi.event.CChange;
import jm.midi.event.EndTrack;
import jm.midi.event.Event;
import jm.midi.event.KeySig;
import jm.midi.event.NoteOn;
import jm.midi.event.PChange;
import jm.midi.event.TempoEvent;
import jm.midi.event.TimeSig;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Score;
import jm.music.tools.Mod;
import org.apache.commons.lang3.tuple.Pair;
import org.vibehistorian.vibecomposer.Helpers.PartExt;
import org.vibehistorian.vibecomposer.Helpers.PhraseExt;
import org.vibehistorian.vibecomposer.Helpers.PhraseNotes;
import org.vibehistorian.vibecomposer.MidiGenerator.Durations;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import static org.vibehistorian.vibecomposer.MidiGenerator.DBL_ERR;

public class JMusicUtilsCustom implements JMC {

	private static double tickRemainder = 0.0;
	private static final short DEFAULT_PPQN = 960;

	public static class EventPair {
		public double time;
		public Event ev;

		public EventPair(double t, Event e) {
			time = t;
			ev = e;
		}
	}

	public static class CompareKey implements Comparator {
		public int compare(Object a, Object b) {
			EventPair ae = (EventPair) a;
			EventPair be = (EventPair) b;
			if (ae.time - be.time < 0)
				return -1;
			else if (ae.time - be.time > 0)
				return 1;
			else
				return 0;
		}
	}

	public static void consolidate(Part p) {

		Phrase[] phr = p.getPhraseArray();
		if (phr.length < 2)
			return;
		// the new phrase has the start time of the earliest one
		Phrase firstPhrase = phr[0];
		Phrase nphr = (firstPhrase instanceof PhraseExt)
				? new PhraseExt((PhraseExt) firstPhrase, firstPhrase.getStartTime())
				: new Phrase(firstPhrase.getStartTime());

		Note n;
		boolean finished = false;
		double prevsst = phr[0].getStartTime();
		while (!finished) {
			double sst = Double.POSITIVE_INFINITY; // smallest start time
			// a temporary phrase (pointer to the one in the array
			Phrase tphr = null;

			//find the phrase with the smallest start time
			for (int i = 0; i < phr.length; i++) {

				if (phr[i].getSize() > 0 && phr[i].getStartTime() < sst) {
					sst = phr[i].getStartTime();
					tphr = phr[i];
				}
			}
			if (tphr == null) {
				finished = true;
				break;
			}
			//get a note out of that phrase and, if it is not a rest,
			// put it into the new phrase, adjusting the rhythmValue
			// of the previous note accordingly
			n = tphr.getNote(0);

			//if (!n.isRest()) {
			if (nphr.getSize() > 0) { // if it is not the first note
				nphr.getNote(nphr.getSize() - 1)
						.setRhythmValue(((int) ((sst - prevsst) * 100000 + 0.5)) / 100000.0);
			} else {// if it is the first note to go in, set the startime
				nphr.setStartTime(sst);
			}

			//}
			nphr.addNote(n);
			// adjust the start time and remove the note
			tphr.setStartTime(((int) ((sst + n.getRhythmValue()) * 100000 + 0.5)) / 100000.0);
			tphr.removeNote(0);

			prevsst = sst;

		}
		p.empty();
		p.addPhrase(nphr);
	}

	public static void addRestsToRhythmValues(Phrase phr) {
		Vector<Note> noteList = phr.getNoteList();
		Vector<Note> newNoteList = new Vector<>();

		Note prev = null;
		for (Note n : noteList) {
			if (n.isRest()) {
				if (prev != null) {
					prev.setRhythmValue(prev.getRhythmValue() + n.getRhythmValue());
				}
			} else {
				prev = n;
				newNoteList.add(n);
			}
		}
		phr.setNoteList(newNoteList);

	}

	public static Phrase doublePhrase(Phrase phr, int doubleTranspose, boolean pauseSquish,
			int dynamicChange) {
		Phrase phr2 = phr.copy();
		Mod.transpose(phr2, doubleTranspose);
		Mod.increaseDynamic(phr2, dynamicChange);
		Part phrPart = new Part();
		phrPart.add(phr2);
		phrPart.add(phr);
		if (pauseSquish) {
			Mod.consolidate(phrPart);
		} else {
			JMusicUtilsCustom.consolidate(phrPart);
		}

		phr = phrPart.getPhrase(0);
		return phr;
	}

	public static Score scoreCopy(Score score) {
		Score scrCopy = new Score();
		Enumeration<?> enum1 = score.getPartList().elements();
		while (enum1.hasMoreElements()) {
			PartExt part = (PartExt) enum1.nextElement();
			if (part.isFillerPart()) {
				continue;
			}
			scrCopy.addPart(part.copy());
		}

		return scrCopy;

	}

	public static void humanize(PartExt part, Random generator, double rhythmVariation,
			boolean isDrum) {
		if (part == null) {
			return;
		}
		Enumeration enum1 = part.getPhraseList().elements();
		while (enum1.hasMoreElements()) {
			Phrase phr = (Phrase) enum1.nextElement();
			humanize(phr, 0, rhythmVariation, 0, generator, isDrum);
		}
	}

	public static void humanize(Phrase phrase, int pitchVariation, double rhythmVariation,
			int dynamicVariation, Random generator, boolean isDrum) {
		if (phrase == null) {
			return;
		}
		Vector<Note> notes = phrase.getNoteList();
		PhraseNotes phraseNotes = new PhraseNotes(notes);
		phraseNotes.remakeNoteStartTimes();

		List<Double> offsets = MidiGeneratorUtils.generateRhythmOffsets(notes.size() - 1, rhythmVariation, generator.nextLong());
		List<Double> durations = MidiGeneratorUtils.generateRhythmOffsets(notes.size() - 1, rhythmVariation, generator.nextLong());

		for (int i = 0; i < notes.size(); i++) {
			Note n = notes.get(i);
			if (i == 0) {
				continue;
			}

			// create new pitch value
			if (pitchVariation > 0 && n.getPitch() != Pitches.REST) {
				n.setPitch(n.getPitch()
						+ (int) (generator.nextDouble() * (pitchVariation * 2) - pitchVariation));
			}
			// create new rhythm and duration values

			if (rhythmVariation > 0.0) {
				double varOffset = offsets.get(i - 1);
				double varDur = durations.get(i - 1);
				double dur = n.getDuration();
				if (!isDrum && dur < Durations.SIXTEENTH_NOTE + DBL_ERR) {
					varDur /= 5;
				}


				n.setOffset(n.getOffset() + varOffset);
				//LG.i("Note start time: " + phraseNotes.get(i).getAbsoluteStartTime() + ", rv: " + n.getRhythmValue() + "; new off: " + n.getOffset());

				n.setDuration(dur + varDur);

			}
			// create new dynamic value
			if (dynamicVariation > 0) {
				n.setDynamic(n.getDynamic() + (int) (generator.nextDouble() * (dynamicVariation * 2)
						- dynamicVariation));
			}
		}
		if (!isDrum) {
			MidiGeneratorUtils.applySamePitchCollisionAvoidance(notes);
		}
	}

	public static void midi(Score scr, String fileName) {
		//Score s = adjustTempo(scr);
		SMF smf = new SMF((short) 1, DEFAULT_PPQN);
		try {
			double time1 = System.currentTimeMillis();
			LG.d("----------------------------- Writing MIDI File ------------------------------");
			smf.clearTracks();
			scoreToSMF(scr, smf);
			OutputStream os = new FileOutputStream(fileName);
			smf.write(os);
			double time2 = System.currentTimeMillis();
			LG.d("MIDI file '" + fileName + "' written from score '" + scr.getTitle() + "' in "
					+ ((time2 - time1) / 1000) + " seconds.");
			LG.d("------------------------------------------------------------------------------");
		} catch (IOException e) {
			LG.e(e);
		}
	}

	public static void scoreToSMF(Score score, SMF smf) {
		if (VERBOSE)
			System.out.println("Converting to SMF data structure...");

		double scoreTempo = score.getTempo();
		double partTempoMultiplier = 1.0;
		double phraseTempoMultiplier = 1.0;
		int phraseNumb;
		Phrase phrase1, phrase2;

		//Add a tempo track at the start of top of the list
		//Add time sig to the tempo track
		Track smfT = new Track();
		smfT.addEvent(new TempoEvent(0, score.getTempo()));
		smfT.addEvent(new TimeSig(0, score.getNumerator(), score.getDenominator()));
		smfT.addEvent(new KeySig(0, score.getKeySignature()));
		smfT.addEvent(new EndTrack());
		smf.getTrackList().addElement(smfT);
		//---------------------------------------------------
		int partCount = 0;
		Enumeration aEnum = score.getPartList().elements();
		while (aEnum.hasMoreElements()) {
			Track smfTrack = new Track();
			PartExt inst = (PartExt) aEnum.nextElement();
			System.out.print("    Part " + partCount + " '" + inst.getTitle()
					+ "' to SMF Track on Ch. " + inst.getChannel() + ": ");
			partCount++;

			// set up tempo difference between score and track - if any
			if (inst.getTempo() != Part.DEFAULT_TEMPO)
				partTempoMultiplier = scoreTempo / inst.getTempo();
			else
				partTempoMultiplier = 1.0;
			//System.out.println("partTempoMultiplier = " + partTempoMultiplier);

			//order phrases based on their startTimes
			Vector<Phrase> phrList = inst.getPhraseList();
			Collections.sort(phrList, Comparator.comparingDouble(Phrase::getStartTime));

			//break Note objects into NoteStart's and NoteEnd's
			//as well as combining all phrases into one list
			//			HashMap midiEvents = new HashMap();
			;
			LinkedList<EventPair> midiEvents = new LinkedList<>();

			/*if(inst.getTempo() != Part.DEFAULT_TEMPO){
				//System.out.println("Adding part tempo");
				midiEvents.add(new EventPair(0, new TempoEvent(inst.getTempo())));
			} */
			//if this part has a Program Change value then set it
			if (inst.getInstrument() != NO_INSTRUMENT) {
				//System.out.println("Instrument change no. " + inst.getInstrument());
				midiEvents.add(new EventPair(0,
						new PChange((short) inst.getInstrument(), (short) inst.getChannel(), 0)));
			}

			if (inst.getNumerator() != NO_NUMERATOR) {
				midiEvents.add(
						new EventPair(0, new TimeSig(inst.getNumerator(), inst.getDenominator())));
			}

			if (inst.getKeySignature() != NO_KEY_SIGNATURE) {
				midiEvents.add(
						new EventPair(0, new KeySig(inst.getKeySignature(), inst.getKeyQuality())));
			}

			Enumeration partEnum = phrList.elements();
            double startTime = 0.0;
			double offsetValue = 0.0;
			int phraseCounter = 0;
			while (partEnum.hasMoreElements()) {
				Phrase phrase = (Phrase) partEnum.nextElement();
				Enumeration phraseEnum = phrase.getNoteList().elements();
				startTime = phrase.getStartTime() * partTempoMultiplier;
				//LG.i("Phr start time: " + startTime);
				if (phrase.getInstrument() != NO_INSTRUMENT) {
					midiEvents.add(new EventPair(startTime, new PChange(
							(short) phrase.getInstrument(), (short) inst.getChannel(), 0)));
				} else {
					midiEvents
							.add(new EventPair(startTime, new PChange((short) inst.getInstrument(),
									(short) inst.getChannel(), 0)));
				}
				if (phrase.getTempo() != Phrase.DEFAULT_TEMPO) {
					phraseTempoMultiplier = scoreTempo / phrase.getTempo(); //(scoreTempo * partTempoMultiplier) / phrase.getTempo();
				} else {
					phraseTempoMultiplier = partTempoMultiplier;
				}

				////////////////////////////////////////////////
				//System.out.println();
				System.out.print(" Phrase " + phraseCounter++ + ":");
				// set a silly starting value to force and initial pan cc event
				double pan = -1.0;
				resetTicker(); // zero the ppqn error calculator
				while (phraseEnum.hasMoreElements()) {
					Note note = (Note) phraseEnum.nextElement();
					offsetValue = note.getOffset();
					// add a pan control change if required
					if (note.getPan() != pan) {
						pan = note.getPan();
						midiEvents
								.add(new EventPair(startTime + offsetValue, new CChange((short) 10,
										(short) (pan * 127), (short) inst.getChannel(), 0)));
					}
					//check for frequency rather than MIDI notes
					int pitch = 0;
					if (note.getPitchType() == Note.FREQUENCY) {
						System.err.println(
								"jMusic warning: converting note frequency to the closest MIDI pitch for SMF.");
						//System.exit(1);
						pitch = Note.freqToMidiPitch(note.getFrequency());
					} else
						pitch = note.getPitch();
					if (pitch >= 0) {
						midiEvents.add(new EventPair(new Double(startTime + offsetValue),
								new NoteOn((short) pitch, (short) note.getDynamic(),
										(short) inst.getChannel(), 0)));

						// Add a NoteOn for the END of the note with 0 dynamic, as recommended.
						//create a timing event at the end of the notes duration
						double endTime = startTime + (note.getDuration() * phraseTempoMultiplier);
						// Add the note-off time to the list
						midiEvents.add(new EventPair(new Double(endTime + offsetValue), new NoteOn(
								(short) pitch, (short) 0, (short) inst.getChannel(), 0)));
					}
					// move the note-on time forward by the rhythmic value
					startTime += tickRounder(note.getRhythmValue() * phraseTempoMultiplier); //time between start times
					System.out.print("."); // completed a note
				}
			}

			//Sort the hashmap by starttime (key value)
			Collections.sort(midiEvents, new CompareKey());
			//Add times to events, now that things are sorted
			double st = 0.0; //start time
			double sortStart = 0.0; // start time from list of notes ons and offs.
			int time; // the start time as ppqn value
			resetTicker();

			int timeCounter = 0;
			for (int index = 0; index < midiEvents.size(); index++) {
				EventPair ep = midiEvents.get(index);
				Event event = ep.ev;
				sortStart = ep.time;
				int newValue = (int) Math.round(sortStart * smf.getPPQN());
				time = newValue - timeCounter;
				timeCounter = newValue;
				event.setTime(time);
				smfTrack.addEvent(event);
			}
			int actualEndTime = (int) ((score.getEndTime() - sortStart) * (double)smf.getPPQN() + 0.5);
			Event endTrack = new EndTrack();
			endTrack.setTime(actualEndTime);
			smfTrack.addEvent(endTrack);
			//add this track to the SMF
			smf.getTrackList().addElement(smfTrack);
			System.out.println();
		}
	}

	private static void resetTicker() {
		tickRemainder = 0.0;

			/* OLD CODE ---------------
			//Sort lists so start times are in the right order
			Enumeration start = midiNoteEvents.elements();
			Enumeration timeing = timeingList.elements();
			Vector sortedStarts = new Vector();
			Vector sortedEvents = new Vector();
			while(start.hasMoreElements()){
				double smallest = ((Double)start.nextElement()).doubleValue();
				Event anevent = (Event) timeing.nextElement();
				int index = 0, count = 0;
				while(start.hasMoreElements()){
					count++;
					double d1 = ((Double)start.nextElement()).doubleValue();
					Event event1 = (Event) timeing.nextElement();
					if(smallest == d1){ //if note time is equal
						if(zeroVelEventQ(event1)) {
							index = count;
						}
					}
					if(smallest > d1){
						smallest = d1;
						index = count;
					}
				}
				sortedStarts.addElement(midiNoteEvents.elementAt(index));
				sortedEvents.addElement(timeingList.elementAt(index));
				midiNoteEvents.removeElementAt(index);
				timeingList.removeElementAt(index);
				//reset lists for next run
				start = midiNoteEvents.elements();
				timeing = timeingList.elements();
			}
			*/
	}

	/**
	 * We need to call this any time we calculate unusual time values,
	 * to prevent time creep due to the MIDI tick roundoff error.
	 * This method wriiten by Bob Lee.
	 */
	private static double tickRounder(double timeValue) {
		final double tick = 1. / (double) DEFAULT_PPQN;
		final double halfTick = 1. / (DEFAULT_PPQN * 2.);
		int ticks = (int) (timeValue * (double) DEFAULT_PPQN);
		double rounded = ((double) ticks) * tick;
		tickRemainder += timeValue - rounded;
		if (tickRemainder > halfTick) {
			rounded += tick;
			tickRemainder -= tick;
		}
		return rounded;
	}

	public static List<Pair<Double, Note>> makeNoteStartTimes(List<Note> notes) {
		double current = 0.0;
		List<Pair<Double, Note>> startTimes = new ArrayList<>();
		for (Note n : notes) {
			startTimes.add(Pair.of(current + n.getOffset(), n));
			current += n.getRhythmValue();
		}
		Collections.sort(startTimes, Map.Entry.comparingByKey());
		return startTimes;
	}
}
