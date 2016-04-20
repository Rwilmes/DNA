package dna.graph.generators.network.tests;

import java.io.IOException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import argList.ArgList;
import argList.types.atomic.EnumArg;
import argList.types.atomic.StringArg;
import dna.graph.generators.network.tests.DatasetUtils.TimestampFormat;
import dna.graph.generators.network.tests.DatasetUtils.ZipMode;
import dna.series.data.SeriesData;
import dna.util.Config;
import dna.util.Log;

public class DatasetPlotting {

	public static void main(String[] args) throws IOException,
			InterruptedException {
		ArgList<DatasetPlotting> argList = new ArgList<DatasetPlotting>(
				DatasetPlotting.class,
				new StringArg("srcDir", "dir of the source data"),
				new StringArg("name", "name of the series to be plotted"),
				new StringArg("dstDir", "destination to be plotted to"),
				new StringArg("gnuplot-path",
						"path of gnuplot executable, set as null for default!"),
				new EnumArg("zip-mode", "zip mode of the data to be plotted",
						ZipMode.values()),
				new EnumArg(
						"timestamp-format",
						"toggles if timestamp (dd-MM-yyyy HH:mm:ss) or DARPA98 week-day (w-d HH:mm:ss) will be used by the timestamp fields",
						TimestampFormat.values()), new StringArg("from",
						"starting timestamp"), new StringArg("to",
						"maximum timestamp"));
		DatasetPlotting d = argList.getInstance(args);

		d.read();
		d.plot();
	}

	/*
	 * CLASS
	 */
	protected SeriesData sd;

	protected String srcDir;
	protected String seriesName;
	protected String dstDir;

	protected String gnuplotPath;

	protected DateTimeFormatter fmt = DateTimeFormat
			.forPattern("dd-MM-yyyy-HH:mm:ss");

	protected DateTime from;
	protected DateTime to;

	public DatasetPlotting(String srcDir, String serieName, String dstDir,
			String gnuplotPath, String zipMode, String timestampFormat,
			String from, String to) {
		this.srcDir = srcDir;
		this.seriesName = serieName;
		this.dstDir = dstDir;
		this.gnuplotPath = gnuplotPath;

		ZipMode zipM = ZipMode.valueOf(zipMode);
		switch (zipM) {
		case batches:
			Config.zipBatches();
			break;
		case runs:
			Config.zipRuns();
			break;
		case none:
			Config.zipNone();
			break;
		}

		// timestamps
		if (timestampFormat.equals("timestamp")) {
			this.from = this.fmt.parseDateTime(from);
			this.to = this.fmt.parseDateTime(to);
		}
		if (timestampFormat.equals("week_day")) {
			String dateFrom = DatasetUtils.getDarpaDate(
					Integer.parseInt("" + from.charAt(0)),
					Integer.parseInt("" + from.charAt(2)));
			this.from = this.fmt.parseDateTime(
					dateFrom + "-" + from.substring(4)).plusSeconds(
					DatasetUtils.gnuplotOffsetSeconds);
			String dateTo = DatasetUtils.getDarpaDate(
					Integer.parseInt("" + to.charAt(0)),
					Integer.parseInt("" + to.charAt(2)));
			this.to = this.fmt.parseDateTime(dateTo + "-" + to.substring(4))
					.plusSeconds((DatasetUtils.gnuplotOffsetSeconds));
		}
	}

	public void read() throws IOException {
		Log.info("reading '" + seriesName + "' from '" + srcDir + "'");
		this.sd = SeriesData.read(srcDir, seriesName, false, false);
	}

	public void plot() throws IOException, InterruptedException {
		// gnuplot path
		if (gnuplotPath != null && gnuplotPath != "null")
			DatasetUtils.setGnuplotPath(gnuplotPath);

		// settings
		DatasetUtils.setGnuplotSettings();

		// set plot interval
		if (this.from != null && this.to != null)
			DatasetUtils.plot(sd, dstDir, from, to);
		else
			DatasetUtils.plot(sd, dstDir);
	}

}
