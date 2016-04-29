package dna.util.machineLearning;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import argList.ArgList;
import argList.types.array.IntArrayArg;
import argList.types.array.StringArrayArg;
import argList.types.atomic.BooleanArg;
import argList.types.atomic.EnumArg;
import argList.types.atomic.StringArg;
import dna.io.Writer;
import dna.io.filesystem.Dir;
import dna.labels.Label;
import dna.plot.PlotConfig;
import dna.series.aggdata.AggregatedBatch.BatchReadMode;
import dna.series.data.BatchData;
import dna.util.Config;
import dna.util.Log;

public class DataCollector {

	public static void main(String[] args) throws IOException {
		ArgList<DataCollector> argList = new ArgList<DataCollector>(
				DataCollector.class,
				new StringArrayArg("seriesDirs", "list of source-series-dirs",
						","),
				new IntArrayArg("runIds", "ids of the respective runs", ","),
				new EnumArg("zip-mode",
						"zip-mode of the series. Possible values",
						new String[] { "none, batches, runs" }),
				new BooleanArg(
						"includeUnavailable",
						"if true, even batches that are not contained in all series and runs will be included"),
				new StringArg("unavailable-char",
						"character used for values that are not available."),
				new EnumArg(
						"timestamp-format",
						"toggles if timestamp (dd-MM-yyyy HH:mm:ss) or DARPA98 week-day (w-d HH:mm:ss) will be used by the timestamp fields",
						new String[] { "timestamp", "week_day" }),
				new StringArg("from", "starting timestamp"),
				new StringArg("to", "maximum timestamp"),
				new StringArg("output", "path of the output file"),
				new StringArrayArg(
						"labels",
						"list of labels to be included of format: <label>:<type>",
						","),
				new StringArrayArg(
						"metrics",
						"list of metrics to be included of format: <metric>~<value>. Use <seriesId>:<metric>~<value> to only include the metric-value pair of a specific series.",
						","));
		DataCollector d = argList.getInstance(args);
		d.collect();
	}

	protected String[] seriesDirs;
	protected Integer[] runIds;
	protected String zipMode;
	protected String collectionMode;
	protected boolean includeUnavailable;
	protected String timestampFormat;
	protected DateTimeFormatter fmt = DateTimeFormat
			.forPattern("dd-MM-yyyy-HH:mm:ss");
	protected DateTime from;
	protected DateTime to;
	protected String outputPath;
	protected String notAvailableChar;
	protected String[] labels;
	protected String[] metrics;

	public DataCollector(String[] seriesDirs, Integer[] runIds, String zipMode,
			Boolean includeUnavailable, String notAvailableChar,
			String timestampFormat, String from, String to, String outputPath,
			String[] labels, String[] metrics) {
		this.seriesDirs = seriesDirs;
		this.runIds = runIds;
		this.zipMode = zipMode;
		this.includeUnavailable = includeUnavailable;
		this.timestampFormat = timestampFormat;
		this.outputPath = outputPath;
		this.notAvailableChar = notAvailableChar;
		this.labels = labels;
		this.metrics = metrics;

		// zip mode
		switch (this.zipMode) {
		case "batches":
			Config.zipBatches();
			break;
		case "runs":
			Config.zipRuns();
			break;
		case "none":
			Config.zipNone();
			break;
		}

		// timestamps
		if (timestampFormat.equals("timestamp")) {
			this.from = this.fmt.parseDateTime(from);
			this.to = this.fmt.parseDateTime(to);
		}
		if (timestampFormat.equals("week_day")) {
			String dateFrom = getDarpaDate(
					Integer.parseInt("" + from.charAt(0)),
					Integer.parseInt("" + from.charAt(2)));
			this.from = this.fmt.parseDateTime(dateFrom + "-"
					+ from.substring(4));
			String dateTo = getDarpaDate(Integer.parseInt("" + to.charAt(0)),
					Integer.parseInt("" + to.charAt(2)));
			this.to = this.fmt.parseDateTime(dateTo + "-" + to.substring(4));
		}
	}

	/**
	 * Collects the data and writes the results
	 * 
	 * @throws IOException
	 **/
	public void collect() throws IOException {
		// start time
		long start = System.currentTimeMillis();

		Log.infoSep();

		String[] runDirs = new String[seriesDirs.length];
		for (int i = 0; i < seriesDirs.length; i++) {
			runDirs[i] = Dir.getRunDataDir(seriesDirs[i], runIds[i]);
		}

		String[] batches = getBatches(runDirs, from, to, includeUnavailable);

		Log.info(batches.length + " batches:");
		for (int i = 0; i < batches.length; i++) {
			if (i > 2) {
				Log.info("\t...");
				i = batches.length - 1;
			}
			Log.info("\t"
					+ batches[i]
					+ "\t\t"
					+ new DateTime(Dir.getTimestamp(batches[i]) * 1000)
							.toString());
		}
		Log.infoSep();

		// gather metrics
		@SuppressWarnings("unchecked")
		ArrayList<String>[] metricsList = new ArrayList[seriesDirs.length];
		for (int i = 0; i < metricsList.length; i++) {
			metricsList[i] = new ArrayList<String>();
		}

		for (String m : this.metrics) {
			// check if only for one series
			if (m.contains(":")) {
				String[] splits = m.split(":");
				int index = Integer.parseInt(splits[0]);
				if (!metricsList[index].contains(splits[1]))
					metricsList[index].add(splits[1]);
			} else {
				// add for all series
				for (int i = 0; i < seriesDirs.length; i++) {
					if (!metricsList[i].contains(m))
						metricsList[i].add(m);
				}
			}
		}

		String header = "Labels";

		for (int i = 0; i < metricsList.length; i++) {
			for (String s : metricsList[i]) {
				header += "\t" + "s" + i + ":" + s;
			}
		}

		Writer w = new Writer("", outputPath);
		w.writeln(header);

		// iterate over batches
		for (int i = 0; i < batches.length; i++) {
			long timestamp = Dir.getTimestamp(batches[i]);

			String line = "blub";

			// iterate over series
			for (int j = 0; j < seriesDirs.length; j++) {
				BatchData batchData = null;

				try {
					batchData = BatchData.readIntelligent(Dir.getBatchDataDir(
							seriesDirs[j], runIds[j], timestamp), timestamp,
							BatchReadMode.readAllValues);
				} catch (FileNotFoundException e) {
				}

				ArrayList<String> metrics = metricsList[j];
				for (int k = 0; k < metrics.size(); k++) {
					String metric = metrics.get(k);
					String v;

					String[] splits = metric.split("~");
					String domain = splits[0];
					String value = splits[1];

					if (batchData == null || !batchData.contains(domain, value)) {
						v = this.notAvailableChar;
					} else {
						v = getValue(batchData, domain, value);
					}

					line += "\t" + v;
				}

			}

			w.writeln(line);
		}

		w.close();
		Log.info("done in "
				+ ((1.0 * System.currentTimeMillis() - start) / 1000)
				+ " seconds.");
	}

	/** Returns a list of available batches in respect to the parameters. **/
	public String[] getBatches(String[] dirs, DateTime fromDateTime,
			DateTime toDateTime, boolean includeUnavailable) throws IOException {
		long from = 0;
		long to = Long.MAX_VALUE;
		if (fromDateTime != null) {
			from = (long) Math.floor(fromDateTime.getMillis() / 1000);
			Log.info("from: " + "\t" + from + "\t" + fromDateTime.toString());
		}
		if (toDateTime != null) {
			to = (long) Math.floor(toDateTime.getMillis() / 1000);
			Log.info("to: " + "\t\t" + to + "\t" + toDateTime.toString());
		}

		ArrayList<String> batchesList = new ArrayList<String>();
		ArrayList<Integer> batchesValue = new ArrayList<Integer>();

		int maxValue = 0;

		for (int i = 0; i < dirs.length; i++) {
			int value = 2 ^ (i);
			maxValue += value;

			String[] batches = Dir.getBatchesIntelligent(dirs[i]);

			for (int j = 0; j < batches.length; j++) {
				String batch = batches[j];
				long timestamp = Dir.getTimestamp(batch);
				if (timestamp < from || to < timestamp)
					continue;
				if (!batchesList.contains(batch)) {
					batchesList.add(batch);
					batchesValue.add(value);
				} else {
					int index = batchesList.indexOf(batch);
					batchesValue.set(index, batchesValue.get(index) + value);
				}
			}
		}

		Log.infoSep();

		ArrayList<String> selectedBatches = new ArrayList<String>();

		if (includeUnavailable) {
			selectedBatches = batchesList;
		} else {
			for (int i = 0; i < batchesList.size(); i++) {
				if (batchesValue.get(i) == maxValue)
					selectedBatches.add(batchesList.get(i));
			}
		}

		batchesList = null;
		batchesValue = null;

		Collections.sort(selectedBatches);
		return selectedBatches.toArray(new String[selectedBatches.size()]);
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
}
