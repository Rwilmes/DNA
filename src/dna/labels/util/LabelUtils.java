package dna.labels.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import dna.io.Reader;
import dna.io.Writer;
import dna.io.filesystem.Dir;
import dna.labels.Label;
import dna.labels.LabelList;
import dna.series.data.BatchData;
import dna.util.Config;

/**
 * Utility class for DNA labeling.
 * 
 * @author Rwilmes
 * 
 */
public class LabelUtils {

	/**
	 * Analyzes a label-list and compares the key-label with all other labels.
	 * Then returns a HashMap mapping label-identifiers -> LabelStat objects.<br>
	 * <br>
	 * 
	 * <b>Note:</b> KeyLabel of format: $label_name$:$label_type$
	 */
	public static HashMap<String, LabelStat> analyzeLabelList(String dir,
			String filename, long conditionLifeTime,
			boolean countTrueNegatives, boolean considerConditionedNegatives,
			boolean considerConditionedPositives, Label keyLabel)
			throws IOException {
		ArrayList<BatchData> list = readBatchLabelsFromList(dir, filename);
		String keyId = keyLabel.getIdentifier();

		// mapping label-identifiers to labelstat objects
		HashMap<String, LabelStat> map = new HashMap<String, LabelStat>();
		// mapping label-identifiers to their last occurence timestamp
		HashMap<String, Long> lastTimeLabelOccuredMap = new HashMap<String, Long>();

		// iterate over all batches and gather labels
		for (BatchData batch : list) {
			for (Label l : batch.getLabels().getList()) {
				if (!l.equals(keyLabel)) {
					String identifier = l.getIdentifier();
					if (!map.containsKey(identifier))
						map.put(identifier, new LabelStat(keyId, identifier));
				}
			}
		}

		for (BatchData batch : list) {
			long timestamp = batch.getTimestamp();
			LabelList ll = batch.getLabels();
			boolean keyLabelContained = false;

			// check if key-label present and refresh timestamps
			for (Label l : ll.getList()) {
				if (l.equals(keyLabel))
					keyLabelContained = true;
				else
					lastTimeLabelOccuredMap.put(l.getIdentifier(), timestamp);
			}

			// iterate over labels
			for (String identifier : map.keySet()) {
				LabelStat ls = map.get(identifier);
				boolean timeConditionMet = false;

				// check if label is contained in batch
				boolean labelContained = false;
				for (Label l : ll.getList()) {
					if (l.getIdentifier().equals(identifier)) {
						labelContained = true;
						break;
					}
				}

				// check if time condition met
				if (!labelContained) {
					if (lastTimeLabelOccuredMap.containsKey(identifier)) {
						if (timestamp <= (lastTimeLabelOccuredMap
								.get(identifier) + conditionLifeTime))
							timeConditionMet = true;
					}
				}

				// only do something when 1 of the 3 conditions is met
				if (labelContained || keyLabelContained || timeConditionMet) {
					// true-positive
					if (labelContained && keyLabelContained)
						ls.incrTruePositives();

					// false-negative
					if (labelContained && !keyLabelContained)
						ls.incrFalseNegatives();

					// false-positive / cond-positive
					if (!labelContained && keyLabelContained) {
						if (timeConditionMet && considerConditionedPositives)
							ls.incrCondPositives();
						else
							ls.incrFalsePositives();
					}

					// true-negative / cond-negative
					if (!labelContained && !keyLabelContained) {
						if (timeConditionMet && considerConditionedNegatives)
							ls.incrCondNegatives();
						else if (countTrueNegatives)
							ls.incrTrueNegatives();
					}
				}

				map.put(identifier, ls);
			}

		}

		return map;
	}

	/**
	 * Reads a batch-list and returns a list of BatchData objects containing
	 * labels.
	 **/
	public static ArrayList<BatchData> readBatchLabelsFromList(String dir,
			String filename) throws IOException {
		String delimiter = Config.get("DATA_DELIMITER");
		String valueSeparator = Config.get("LABEL_VALUE_SEPARATOR");
		String nameTypeSeparator = Config.get("LABEL_NAME_TYPE_SEPARATOR");

		ArrayList<BatchData> batchList = new ArrayList<BatchData>();
		ArrayList<Long> timestampList = new ArrayList<Long>();

		// read all lines
		Reader r = new Reader(dir, filename);
		String line;
		while ((line = r.readString()) != null) {
			String[] splits = line.split(delimiter);
			long timestamp = Dir.getTimestamp(splits[0]);

			BatchData batch;

			if (!timestampList.contains(timestamp)) {
				batch = new BatchData(timestamp);
				batchList.add(batch);
				timestampList.add(timestamp);
			} else {
				batch = batchList.get(timestampList.indexOf(timestamp));
			}

			String[] valueSplit = splits[1].split("\\" + valueSeparator);
			String[] nameTypeSplit = valueSplit[0].split("\\"
					+ nameTypeSeparator);

			batch.getLabels()
					.add(new Label(nameTypeSplit[0], nameTypeSplit[1],
							valueSplit[1]));
		}

		r.close();
		return batchList;
	}

	public static HashMap<String, LabelStat> analyzeAndPrint(String dir,
			String filename, long conditionTime, boolean countTrueNegatives,
			boolean considerConditionedNegatives,
			boolean considerConditionedPositives, Label keyLabel)
			throws IOException {
		HashMap<String, LabelStat> map = LabelUtils.analyzeLabelList(dir,
				filename, conditionTime, countTrueNegatives,
				considerConditionedNegatives, considerConditionedPositives,
				keyLabel);

		for (String label : map.keySet()) {
			LabelStat ls = map.get(label);
			ls.printAll();
			System.out.println("");

		}

		return map;
	}

	/** Writes all label-stats contained in the map to the specified file. **/
	public static void writeLabelStats(HashMap<String, LabelStat> stats,
			String dir, String filename, boolean writeShort) throws IOException {
		Writer w = new Writer(dir, filename);

		boolean headerWritten = false;
		for (String id : stats.keySet()) {
			LabelStat ls = stats.get(id);
			if (!headerWritten) {
				w.writeln(ls.getHeader(writeShort));
				headerWritten = true;
			}
			w.writeln(ls.getDataLine(writeShort));
		}
		w.close();
	}

}
