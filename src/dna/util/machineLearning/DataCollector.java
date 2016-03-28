package dna.util.machineLearning;

import java.io.IOException;
import java.util.ArrayList;

import dna.io.Writer;
import dna.io.filesystem.Dir;
import dna.labels.Label;
import dna.labels.LabelList;
import dna.plot.PlotConfig;
import dna.series.aggdata.AggregatedBatch.BatchReadMode;
import dna.series.data.BatchData;
import dna.series.data.RunData;
import dna.series.data.SeriesData;
import dna.series.lists.BatchDataList;
import dna.util.Config;
import dna.util.Log;

public class DataCollector {

	public static void main(String[] args) throws IOException {
		if (args.length <= 6 || args[0].equals("-h") || args[0].equals("-help")) {
			System.out
					.println("not enough / wrong parameters? displaying help:");
			System.out
					.println("Input: [seriesData-dir], [runId], [batchZipMode], [out-filename], [-l label1:type1 label1:type2 label2:type1 ...] [-m metric1~v1 metric1~v2 metric2~v1 ...]*");
			Log.infoSep();
			System.out.println("seriesData-dir:\t"
					+ "Dir of the series-data. Should contain run.X dir.");
			System.out.println("runId\t\t"
					+ "Id of the run to be used. Usually 0.");
			System.out
					.println("batchZipMode\t"
							+ "Mode in which batches are saved. Valid values: none, batches, runs.");
			System.out.println("out-filename\t"
					+ "Name of the file the output will be written to.");
			System.out
					.println("-l label:type\t"
							+ "Signals the beginning of a list of labels to be included in output data.");
			System.out
					.println("-m metric1~v1\t"
							+ "Signals the beginning of a list of metric-value pairs to be added to the data.");
			return;
		}

		long start = System.currentTimeMillis();

		String dir = args[0];
		int runId = Integer.parseInt(args[1]);
		String readMode = args[2];
		switch (readMode) {
		case "none":
			Config.zipNone();
			break;
		case "batches":
			Config.zipBatches();
			break;
		case "runs":
			Config.zipRuns();
			break;
		}
		String outDir = args[3];

		int labelOffset = 4;
		int metricOffset = 6;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-l"))
				labelOffset = i + 1;
			if (args[i].equals("-m"))
				metricOffset = i + 1;
		}

		String[] inputLabels = new String[metricOffset - labelOffset - 1];
		for (int i = 0; i < inputLabels.length; i++) {
			inputLabels[i] = args[i + labelOffset];
		}

		String[] inputMetrics = new String[args.length - metricOffset];
		for (int i = 0; i < inputMetrics.length; i++) {
			inputMetrics[i] = args[i + metricOffset];
		}

		System.out.println("labels:");
		for (String s : inputLabels)
			System.out.println("\t" + s);

		System.out.println("metrics:");
		for (String s : inputMetrics)
			System.out.println("\t" + s);

		String[] domains = new String[inputMetrics.length];
		String[] values = new String[inputMetrics.length];

		for (int i = 0; i < inputMetrics.length; i++) {
			String[] split = inputMetrics[i].split("~");
			domains[i] = split[0];
			values[i] = split[1];
		}

		System.out.println("read from dir:");
		System.out.println("\t" + dir);

		// String dir = "data/darpa1998_small/w1/monday/1_3600/";
		int run = 0;
		SeriesData sd = SeriesData.read(dir, "1_3600", false, false);

		ArrayList<RunData> rdl = sd.getRuns();

		BatchDataList bdl = sd.getRun(run).getBatches();

		Writer w = new Writer("", outDir);
		String header = "";
		for (int i = 0; i < inputLabels.length; i++) {
			if (i == 0)
				header += inputLabels[i];
			else
				header += "\t" + inputLabels[i];
		}
		for (int i = 0; i < inputMetrics.length; i++) {
			if (i == 0 && header.length() == 0)
				header += inputMetrics[i];
			else
				header += "\t" + inputMetrics[i];
		}
		w.writeln(header);

		System.out.println("gathering data from batches..");
		int amountBatches = bdl.size();
		int counter = 0;
		for (BatchData b : bdl.getList()) {
			long timestamp = b.getTimestamp();

			BatchData bd = BatchData.readIntelligent(
					Dir.getBatchDataDir(dir, run, timestamp), timestamp,
					BatchReadMode.readOnlySingleValues);

			String buff = "";
			boolean first = (buff.length() == 0);
			for (int i = 0; i < inputLabels.length; i++) {
				Label l = parseLabel(inputLabels[i]);
				String v = "0.0";

				LabelList ll = bd.getLabels();
				for (Label label : ll.getList()) {
					if (label.equals(l)) {
						v = "1.0";
					}
				}

				if (!first)
					buff += "\t" + v;
				else {
					buff += v;
					first = false;
				}
			}

			first = (buff.length() == 0);
			for (int i = 0; i < domains.length; i++) {
				String domain = domains[i];
				String value = values[i];
				String v;

				if (bd.contains(domain, value))
					v = getValue(bd, domain, value);
				else
					v = "-";

				if (!first)
					buff += "\t" + v;
				else {
					buff += v;
					first = false;
				}
			}

			w.writeln(buff);

			counter++;
			// System.out.println(counter);

			if (counter % 100 == 0 || counter == amountBatches) {
				System.out.println((int) Math.floor(1.0 * counter
						/ amountBatches * 100)
						+ "%" + "\t" + counter + " of " + amountBatches);
			}
		}

		w.close();
		System.out.println("done in "
				+ ((1.0 * System.currentTimeMillis() - start) / 1000)
				+ " seconds.");
	}

	public static Label parseLabel(String line) {
		String[] split1 = line.split("=");
		String value = null;
		if (split1.length > 1)
			value = split1[1];

		String[] split2 = split1[0].split(":");

		return new Label(split2[0], split2[1], value);
	}

	public static String getValue(BatchData b, String domain, String value) {
		if (domain.equals(PlotConfig.customPlotDomainStatistics)) {
			return "" + b.getValues().get(value).getValue();
		} else if (domain.equals(PlotConfig.customPlotDomainMetricRuntimes)) {
			return "" + b.getMetricRuntimes().get(value).getMilliSec();
		} else if (domain.equals(PlotConfig.customPlotDomainGeneralRuntimes)) {
			return "" + b.getGeneralRuntimes().get(value).getMilliSec();
		} else {
			return ""
					+ b.getMetrics().get(domain).getValues().get(value)
							.getValue();
		}
	}
}
