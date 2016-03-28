package dna.util.machineLearning;

import java.io.IOException;

public class DataCollectorExec {

	public static String day1 = "monday";
	public static String day2 = "tuesday";
	public static String day3 = "wednesday";
	public static String day4 = "thursday";
	public static String day5 = "friday";

	public static String[] days = { day1, day2, day3, day4, day5 };

	public static void main(String[] args) throws IOException {
		String baseDir = "data/darpa1998/";

		int week = 2;

		String[] data = m1AllW2;

		for (String day : days) {
			data[0] = baseDir + "w" + week + "/" + day + "/1_3600/";
			data[3] = baseDir + "w" + week + "/" + day + ".data";
			DataCollector.main(data);
		}

	}

	public static String[] m1All = new String[] {
			"data/darpa1998/w1/wednesday/1_3600/", "0", "batches",
			"data/darpa1998/w1/wednesday.ml", "-l", "attack:smurf",
			"attack:neptune", "attack:pod", "attack:teardrop", "-m",
			"DegreeDistributionU~DegreeMin", "DegreeDistributionU~DegreeMax",
			"DegreeDistributionU~InDegreeMin",
			"DegreeDistributionU~InDegreeMax",
			"DegreeDistributionU~InDegreeMin",
			"DegreeDistributionU~OutDegreeMax", "DirectedMotifsU~DM01",
			"DirectedMotifsU~DM02", "DirectedMotifsU~DM03",
			"DirectedMotifsU~DM04", "DirectedMotifsU~DM05",
			"DirectedMotifsU~DM06", "DirectedMotifsU~DM07",
			"DirectedMotifsU~DM08", "DirectedMotifsU~DM09",
			"DirectedMotifsU~DM10", "DirectedMotifsU~DM11",
			"DirectedMotifsU~DM12", "DirectedMotifsU~DM13",
			"DirectedMotifsU~TOTAL", "EdgeWeightsR-1.0~MinWeight",
			"EdgeWeightsR-1.0~MaxWeight", "EdgeWeightsR-1.0~AverageWeight",
			"WeightedDegreeDistributionR~WeightedDegreeMin",
			"WeightedDegreeDistributionR~WeightedDegreeMax",
			"WeightedDegreeDistributionR~WeightedDegreeAvg",
			"WeightedDegreeDistributionR~WeightedInDegreeMin",
			"WeightedDegreeDistributionR~WeightedInDegreeMax",
			"WeightedDegreeDistributionR~WeightedInDegreeAvg",
			"WeightedDegreeDistributionR~WeightedOutDegreeMin",
			"WeightedDegreeDistributionR~WeightedOutDegreeMax",
			"WeightedDegreeDistributionR~WeightedOutDegreeAvg" };

	public static String[] m1AllW2 = new String[] {
			"data/darpa1998/w1/wednesday/1_3600/", "0", "batches",
			"data/darpa1998/w1/wednesday.ml", "-l", "attack:ipsweep",
			"attack:portsweep", "-m", "DegreeDistributionU~DegreeMin",
			"DegreeDistributionU~DegreeMax", "DegreeDistributionU~InDegreeMin",
			"DegreeDistributionU~InDegreeMax",
			"DegreeDistributionU~InDegreeMin",
			"DegreeDistributionU~OutDegreeMax", "DirectedMotifsU~DM01",
			"DirectedMotifsU~DM02", "DirectedMotifsU~DM03",
			"DirectedMotifsU~DM04", "DirectedMotifsU~DM05",
			"DirectedMotifsU~DM06", "DirectedMotifsU~DM07",
			"DirectedMotifsU~DM08", "DirectedMotifsU~DM09",
			"DirectedMotifsU~DM10", "DirectedMotifsU~DM11",
			"DirectedMotifsU~DM12", "DirectedMotifsU~DM13",
			"DirectedMotifsU~TOTAL", "EdgeWeightsR-1.0~MinWeight",
			"EdgeWeightsR-1.0~MaxWeight", "EdgeWeightsR-1.0~AverageWeight",
			"WeightedDegreeDistributionR~WeightedDegreeMin",
			"WeightedDegreeDistributionR~WeightedDegreeMax",
			"WeightedDegreeDistributionR~WeightedDegreeAvg",
			"WeightedDegreeDistributionR~WeightedInDegreeMin",
			"WeightedDegreeDistributionR~WeightedInDegreeMax",
			"WeightedDegreeDistributionR~WeightedInDegreeAvg",
			"WeightedDegreeDistributionR~WeightedOutDegreeMin",
			"WeightedDegreeDistributionR~WeightedOutDegreeMax",
			"WeightedDegreeDistributionR~WeightedOutDegreeAvg" };
}
