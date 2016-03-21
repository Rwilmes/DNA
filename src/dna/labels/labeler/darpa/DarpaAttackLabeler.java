package dna.labels.labeler.darpa;

import dna.labels.labeler.test.IntervalLabeler;
import dna.labels.labeler.test.ListLabeler;

/**
 * A labeler used to parse darpa-attack-lists and create labels.
 * 
 * @author Rwilmes
 * 
 */
public class DarpaAttackLabeler extends ListLabeler {

	public DarpaAttackLabeler(String dir, String filename, String... classes) {
		this("DarpaAttackLabeler", dir, filename, classes);
	}

	public DarpaAttackLabeler(String name, String dir, String filename,
			String... classes) {
		super(name, dir, filename);
	}

	/**
	 * Format should be:<br>
	 * <br>
	 * 
	 * $attack-name$ \t $class$ \t $week$ \t $day_of_week$ \t $from_date$ \t
	 * $from_time$ \t $from_unix_timestamp$ \t $to_date$ \t $to_time$ \t
	 * $to_unix_timestamp$
	 */
	@Override
	protected IntervalLabeler parseLabelerFromString(String name, String line) {
		String[] splits = line.split("\t");

		String type = splits[0];
		String classification = splits[1];
		int week = Integer.parseInt(splits[2]);
		int day = Integer.parseInt(splits[3]);

		long fromTimestamp = Long.parseLong(splits[6]);
		long toTimestamp = Long.parseLong(splits[9]);

		return new IntervalLabeler("attack", type, fromTimestamp, toTimestamp);
	}

}
