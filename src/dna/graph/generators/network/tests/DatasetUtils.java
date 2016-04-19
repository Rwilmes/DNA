package dna.graph.generators.network.tests;

public class DatasetUtils {

	/**
	 * Returns the absolute date of a DARPA 1998 week and day pair in
	 * dd-MM-yyyy.
	 **/
	public static String getDarpaDate(int week, int day) {
		int d = 1;
		int m = 6;

		int days = (week - 1) * 7 + (day - 1);

		if (days > 31) {
			d += (days - 30);
			m++;
		} else {
			d += days;
		}

		String ds = (d > 9) ? "" + d : "0" + d;
		String ms = (m > 9) ? "" + m : "0" + m;
		String ys = "1998";

		return ds + "-" + ms + "-" + ys;
	}

	/** Returns a conventional series-name. **/
	public static String getName(int secondsPerBatch,
			long lifeTimePerEdgeSeconds) {
		return secondsPerBatch + "_" + lifeTimePerEdgeSeconds;
	}

	/** Returns a conventional series-name. **/
	public static String getName(int secondsPerBatch,
			long lifeTimePerEdgeSeconds, String descr) {
		if (descr == null || descr.equals(""))
			return getName(secondsPerBatch, lifeTimePerEdgeSeconds);
		else
			return secondsPerBatch + "_" + lifeTimePerEdgeSeconds + "_" + descr;
	}
}
