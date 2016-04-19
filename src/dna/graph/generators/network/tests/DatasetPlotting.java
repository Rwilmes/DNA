package dna.graph.generators.network.tests;

import java.io.IOException;

import argList.ArgList;
import argList.types.atomic.StringArg;
import dna.series.data.SeriesData;
import dna.util.Log;

public class DatasetPlotting {

	public static void main(String[] args) throws IOException,
			InterruptedException {
		ArgList<DatasetPlotting> argList = new ArgList<DatasetPlotting>(
				DatasetPlotting.class, new StringArg("srcDir",
						"dir of the source data"), new StringArg("name",
						"name of the series to be plotted"), new StringArg(
						"dstDir", "destination to be plotted to"),
				new StringArg("gnuplot-path",
						"path of gnuplot executable, set as null for default!"));
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

	public DatasetPlotting(String srcDir, String serieName, String dstDir,
			String gnuplotPath) {
		this.srcDir = srcDir;
		this.seriesName = serieName;
		this.dstDir = dstDir;
		this.gnuplotPath = gnuplotPath;

		System.out.println("init!");
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

		// plot
		DatasetUtils.plot(sd, dstDir);
	}

}
