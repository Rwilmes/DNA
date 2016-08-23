package dna.graph.generators.network.tests.jars;

import java.io.IOException;

import argList.ArgList;
import argList.types.atomic.EnumArg;
import argList.types.atomic.StringArg;
import dna.series.data.SeriesData;
import dna.util.Config;
import dna.util.Log;

public class Plotting {

	public enum ZipMode {
		none, batches, runs
	};

	/*
	 * MAIN
	 */
	public static void main(String[] args) throws IOException,
			InterruptedException {
		ArgList<Plotting> argList = new ArgList<Plotting>(Plotting.class,
				new StringArg("srcDir", "dir of the source data"),
				new StringArg("dstDir", "dir of the destination data"),
				new EnumArg("zip-mode", "zip-mode of the given data",
						ZipMode.values()), new StringArg("gnuplot-path",
						"dir of the gnuplot binary files"));
		Plotting d = argList.getInstance(args);
		d.generate();
	}

	protected String srcDir;
	protected String dstDir;
	protected ZipMode zipMode;
	protected String gnuplotPath;

	public Plotting(String srcDir, String dstDir, String zipMode,
			String gnuplotPath) {
		this.srcDir = srcDir;
		this.dstDir = dstDir;
		this.zipMode = ZipMode.valueOf(zipMode);
		this.gnuplotPath = gnuplotPath;
	}

	public void generate() throws IOException, InterruptedException {
		switch (this.zipMode) {
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

		Config.overwrite("GNUPLOT_PATH", this.gnuplotPath);

		Log.info("zipmode: " + this.zipMode.toString());
		Log.info("plotting " + "'" + this.srcDir + "'");
		Log.info("\t--> " + "'" + this.dstDir + "'");
		SeriesData sd = SeriesData.read(this.srcDir, "series0", false, false);

		dna.plot.Plotting.plot(sd, this.dstDir);
	}
}
