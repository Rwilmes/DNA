package dna.labels.util;

/**
 * An object which contains statistical information about the relation between
 * two labels.
 * 
 * @author Rwilmes
 * 
 */
public class LabelStat {

	private String identifier;
	private String identifier2;
	private int total;

	private int negatives;
	private int positives;

	private int trueNegatives;
	private int falseNegatives;
	private int condNegatives;

	private int truePositives;
	private int falsePositives;
	private int condPositives;

	public LabelStat(String identifier, String identifier2) {
		this.identifier = identifier;
		this.identifier2 = identifier2;
		this.total = 0;

		this.negatives = 0;
		this.positives = 0;

		this.trueNegatives = 0;
		this.falseNegatives = 0;
		this.condNegatives = 0;

		this.truePositives = 0;
		this.falsePositives = 0;
		this.condPositives = 0;
	}

	public void incrTrueNegatives() {
		this.trueNegatives++;
		this.negatives++;
		this.total++;
	}

	public void incrFalseNegatives() {
		this.falseNegatives++;
		this.positives++;
		this.total++;
	}

	public void incrCondNegatives() {
		this.condNegatives++;
		this.negatives++;
		this.total++;
	}

	public void incrTruePositives() {
		this.truePositives++;
		this.positives++;
		this.total++;
	}

	public void incrFalsePositives() {
		this.falsePositives++;
		this.negatives++;
		this.total++;
	}

	public void incrCondPositives() {
		this.condPositives++;
		this.positives++;
		this.total++;
	}

	public String getIdentifier() {
		return identifier;
	}

	public String getIdentifier2() {
		return identifier2;
	}

	public int getTotal() {
		return total;
	}

	public int getNegatives() {
		return negatives;
	}

	public int getPositives() {
		return positives;
	}

	public int getTrueNegatives() {
		return trueNegatives;
	}

	public int getFalseNegatives() {
		return falseNegatives;
	}

	public int getCondNegatives() {
		return condNegatives;
	}

	public int getTruePositives() {
		return truePositives;
	}

	public int getFalsePositives() {
		return falsePositives;
	}

	public int getCondPositives() {
		return condPositives;
	}

	public double getPositivesPercent() {
		return 1.0 * positives / total;
	}

	public double getNegativesPercent() {
		return 1.0 * negatives / total;
	}

	public double getTruePositiveRate() {
		return 1.0 * truePositives / positives;
	}

	public double getTrueNegativeRate() {
		return 1.0 * trueNegatives / negatives;
	}

	public double getPositivePredictiveValue() {
		return 1.0 * truePositives
				/ (truePositives + falsePositives + condPositives);
	}

	public double getNegativePredictiveValue() {
		return 1.0 * trueNegatives
				/ (trueNegatives + falseNegatives + condNegatives);
	}

	public double getFalsePositiveRate() {
		return 1.0 * falsePositives / negatives;
	}

	public double getFalseNegativeRate() {
		return 1.0 * falseNegatives / positives;
	}

	public double getFalseDiscoveryRate() {
		return 1.0 * falsePositives
				/ (truePositives + falsePositives + condPositives);
	}

	public double getCondPositiveRate() {
		return 1.0 * condPositives / positives;
	}

	public double getCondNegativeRate() {
		return 1.0 * condNegatives / negatives;
	}

	public double getAccuracy() {
		return (1.0 * truePositives + trueNegatives)
				/ (truePositives + falsePositives + trueNegatives + falseNegatives);
	}

	/** Returns a line of data. **/
	public String getDataLine(boolean shortVersion) {
		if (shortVersion)
			return getDataLineShort();
		else
			return getDataLine();
	}

	/**
	 * Returns a line of data.<br>
	 * <br>
	 * 
	 * Each field will be separated by a tab-character and contains the
	 * following fields:<br>
	 * <br>
	 * 
	 * identifier1, identifier2, total, negatives, true-negatives,
	 * false-positives, cond-negatives,<br>
	 * positives, true-positives, false-negatives, cond-positives,<br>
	 * negatives-percent, true-negative-rate, false-positive-rate,
	 * cond-negative-rate, negative-predictive-value,<br>
	 * positives-percent, true-positive-rate, false-negative-rate,
	 * cond-positive-rate, positiv-predictive-value,<br>
	 * accuracy, false-discovery-rate.
	 **/
	public String getDataLine() {
		String buff = "";

		// add identifier
		buff += getIdentifier() + "\t" + getIdentifier2();

		// add total
		buff += "\t" + getTotal();

		// add negatives
		buff += "\t" + getNegatives() + "\t" + getTrueNegatives() + "\t"
				+ getFalsePositives() + "\t" + getCondNegatives();

		// add positives
		buff += "\t" + getPositives() + "\t" + getTruePositives() + "\t"
				+ getFalseNegatives() + "\t" + getCondPositives();

		// add negative-rates
		buff += "\t" + getNegativesPercent() + "\t" + getTrueNegativeRate()
				+ "\t" + getFalsePositiveRate() + "\t" + getCondNegativeRate()
				+ "\t" + getNegativePredictiveValue();

		// add positive-rates
		buff += "\t" + getPositivesPercent() + "\t" + getTruePositiveRate()
				+ "\t" + getFalseNegativeRate() + "\t" + getCondPositiveRate()
				+ "\t" + getPositivePredictiveValue();

		// add additional rates
		buff += "\t" + getAccuracy() + "\t" + getFalseDiscoveryRate();

		return buff;
	}

	/** Returns a short data line. **/
	public String getDataLineShort() {
		String buff = "";

		// add identifier
		buff += getIdentifier() + "\t" + getIdentifier2();

		// add TPR
		buff += "\t" + getTruePositiveRate();

		// add FNR
		buff += "\t" + getFalseNegativeRate();

		// add CPR
		buff += "\t" + getCondPositiveRate();

		return buff;
	}

	/** Returns a header-line for the data returned by getDataLine(..). **/
	public String getHeader(boolean shortVersion) {
		if (shortVersion)
			return getHeaderShort();
		else
			return getHeader();
	}

	/** Returns a header-line for the data returned by getDataLine(). **/
	public String getHeader() {
		String buff = "";

		// add identifier
		buff += "ID1" + "\t" + "ID2";

		// add total
		buff += "\t" + "TOTAL";

		// add negatives
		buff += "\t" + "N" + "\t" + "TN" + "\t" + "FP" + "\t" + "CN";

		// add positives
		buff += "\t" + "P" + "\t" + "TP" + "\t" + "FN" + "\t" + "CP";

		// add negative-rates
		buff += "\t" + "n-p" + "\t" + "TNR" + "\t" + "FPR" + "\t" + "CNR"
				+ "\t" + "NPV";

		// add positive-rates
		buff += "\t" + "p-p" + "\t" + "TPR" + "\t" + "FNR" + "\t" + "CPR"
				+ "\t" + "PPV";

		// add additional rates
		buff += "\t" + "ACC" + "\t" + "FDR";

		return buff;
	}

	/** Returns a short header for getDataLineShort(). **/
	public String getHeaderShort() {
		String buff = "";

		// add identifier
		buff += "ID1" + "\t" + "ID2";

		// add TPR
		buff += "\t" + "TPR";

		// add FNR
		buff += "\t" + "FNR";

		// add CPR
		buff += "\t" + "CPR";

		return buff;
	}

	public void printAll() {
		System.out.println(getIdentifier() + " vs " + getIdentifier2());
		System.out.println("-----------------");
		System.out.println("--- absolutes ---");
		printAbsolutes();
		System.out.println("-------------");
		System.out.println("--- rates ---");
		printRates();
	}

	public void printAbsolutes() {
		System.out.println("total:\t" + getTotal());
		System.out.println("---");
		System.out.println("N:\t" + getNegatives());
		System.out.println("TN:\t" + getTrueNegatives());
		System.out.println("FP:\t" + getFalsePositives());
		System.out.println("CN:\t" + getCondNegatives());
		System.out.println("---");
		System.out.println("P:\t" + getPositives());
		System.out.println("TP:\t" + getTruePositives());
		System.out.println("FN:\t" + getFalseNegatives());
		System.out.println("CP:\t" + getCondPositives());
	}

	public void printRates() {
		System.out.println("N:\t" + getNegativesPercent());
		System.out.println("TNR:\t" + getTrueNegativeRate());
		System.out.println("FPR:\t" + getFalsePositiveRate());
		System.out.println("CNR:\t" + getCondNegativeRate());
		System.out.println("NPV:\t" + getNegativePredictiveValue());
		System.out.println("---");
		System.out.println("P:\t" + getPositivesPercent());
		System.out.println("TPR:\t" + getTruePositiveRate());
		System.out.println("FNR:\t" + getFalseNegativeRate());
		System.out.println("CPR:\t" + getCondPositiveRate());
		System.out.println("PPV:\t" + getPositivePredictiveValue());
		System.out.println("---");
		System.out.println("ACC:\t" + getAccuracy());
		System.out.println("FDR:\t" + getFalseDiscoveryRate());
	}

}
