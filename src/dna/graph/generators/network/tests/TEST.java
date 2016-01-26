package dna.graph.generators.network.tests;

import java.io.IOException;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;

import dna.graph.datastructures.GDS;
import dna.graph.generators.GraphGenerator;
import dna.graph.generators.network.EmptyNetwork;
import dna.graph.generators.network.m1.M1Batch;
import dna.graph.generators.network.m1.M1BatchTimed;
import dna.graph.generators.network.m1.M1Graph;
import dna.graph.weights.LongWeight;
import dna.graph.weights.Weight.WeightSelection;
import dna.metrics.Metric;
import dna.metrics.MetricNotApplicableException;
import dna.metrics.degree.DegreeDistributionU;
import dna.metrics.motifs.DirectedMotifsU;
import dna.plot.Plotting;
import dna.plot.PlottingConfig;
import dna.plot.PlottingConfig.PlotFlag;
import dna.series.AggregationException;
import dna.series.Series;
import dna.series.data.SeriesData;
import dna.updates.generators.BatchGenerator;
import dna.util.Config;
import dna.util.Log;
import dna.util.network.tcp.DefaultTCPEventReader;
import dna.visualization.graph.GraphVisualization;

public class TEST {

	public static final String gnuplot_xtics = "GNUPLOT_XTICS";
	public static final String gnuplot_datetime = "GNUPLOT_DATETIME";
	public static final String gnuplot_plotdatetime = "GNUPLOT_PLOTDATETIME";

	public static void main(String[] args) throws IOException,
			InterruptedException, AggregationException,
			MetricNotApplicableException, ClassNotFoundException,
			ParseException {

		Config.overwrite("GNUPLOT_PATH",
				"C://Program Files (x86)//gnuplot//bin//gnuplot.exe");

		Config.zipRuns();
		GraphVisualization.enable();

		boolean normalTest = false;
		boolean timedTest = false;

		boolean nodeTypeTest = true;

		int secondsPerBatch = 1;
		int maxBatches = 1000;
		long lifeTimePerEdge = 60000;

		String dir = "data/tcp_test/10/";
		String file = "out_10_3.list";

		Log.infoSep();
		Log.info("NETWORK-TESTS");
		Log.info("dir:\t\t" + dir);
		Log.info("file:\t\t" + file);
		Log.info("seconds:\t" + secondsPerBatch);
		Log.info("maxBatches:\t" + maxBatches);
		Log.infoSep();

		if (normalTest)
			TCPTEST1("data/tcp_test/10/", "out_10_3.list", secondsPerBatch,
					maxBatches, true, true);

		if (timedTest)
			TCPTEST1TIMED("data/tcp_test/10/", "out_10_3.list",
					secondsPerBatch, lifeTimePerEdge, maxBatches, true, false);

		if (nodeTypeTest)
			NodeTypeTest("data/tcp_test/10/", "out_10_3.list", secondsPerBatch,
					lifeTimePerEdge, maxBatches, true, true);

	}

	public static void NodeTypeTest(String dir, String filename, int seconds,
			long millis, int maxBatches, boolean plot, boolean debug)
			throws IOException, ParseException, AggregationException,
			MetricNotApplicableException, InterruptedException {
		Log.info("M1-Batch test with node-types as weights!");
		Log.info("Lifetime:\t" + millis + "ms");
		Config.overwrite("GRAPH_VIS_SHOW_NODE_INDEX", "true");

		DefaultTCPEventReader reader = new DefaultTCPEventReader(dir, filename);
		GraphGenerator gg = new M1Graph(GDS.directed(), reader);

		long timestampMillis = reader.getInitTimestamp().getMillis();
		long timestampSeconds = TimeUnit.MILLISECONDS
				.toSeconds(timestampMillis);
		gg = new EmptyNetwork(GDS.directedE(LongWeight.class,
				WeightSelection.Zero), timestampSeconds);
		BatchGenerator bg = new M1BatchTimed(reader, seconds, millis);
		((M1BatchTimed) bg).setDebug(debug);

		Metric[] metrics = new Metric[] { new DegreeDistributionU(),
				new DirectedMotifsU() };

		Series s = new Series(gg, bg, metrics,
				dir + seconds + "_timed/series/", "s1");

		System.out.println("millis: " + timestampMillis + "\tseconds: "
				+ timestampSeconds);

		SeriesData sd = s.generate(1, maxBatches, false);

		if (plot) {
			plot(sd, dir + seconds + "_timed/plots/", true, true);
		}

		GraphVisualization.setText("Finished");
		Log.infoSep();
	}

	public static void TCPTEST1(String dir, String filename, int seconds,
			int maxBatches, boolean plot, boolean debug) throws IOException,
			ParseException, AggregationException, MetricNotApplicableException,
			InterruptedException {
		Log.info("M1-Batch test with permanent edges!");
		Config.overwrite("GRAPH_VIS_SHOW_NODE_INDEX", "true");

		DefaultTCPEventReader reader = new DefaultTCPEventReader(dir, filename);
		GraphGenerator gg = new M1Graph(GDS.directed(), reader);

		long timestampMillis = reader.getInitTimestamp().getMillis();
		long timestampSeconds = TimeUnit.MILLISECONDS
				.toSeconds(timestampMillis);
		gg = new EmptyNetwork(GDS.directed(), timestampSeconds);

		BatchGenerator bg = new M1Batch(reader, seconds);
		((M1Batch) bg).setDebug(debug);

		Metric[] metrics = new Metric[] { new DegreeDistributionU(),
				new DirectedMotifsU() };

		Series s = new Series(gg, bg, metrics, dir + seconds + "/series/", "s1");

		SeriesData sd = s.generate(1, maxBatches, false);

		// plot
		if (plot) {
			plot(sd, dir + seconds + "/plots/", true, true);
		}

		GraphVisualization.setText("Finished");
		Log.infoSep();
	}

	public static void TCPTEST1TIMED(String dir, String filename, int seconds,
			long millis, int maxBatches, boolean plot, boolean debug)
			throws IOException, ParseException, AggregationException,
			MetricNotApplicableException, InterruptedException {
		Log.info("M1-Batch test with timed edges!");
		Log.info("Lifetime:\t" + millis + "ms");
		Config.overwrite("GRAPH_VIS_SHOW_NODE_INDEX", "true");

		DefaultTCPEventReader reader = new DefaultTCPEventReader(dir, filename);
		GraphGenerator gg = new M1Graph(GDS.directed(), reader);

		long timestampMillis = reader.getInitTimestamp().getMillis();
		long timestampSeconds = TimeUnit.MILLISECONDS
				.toSeconds(timestampMillis);
		gg = new EmptyNetwork(GDS.directedE(LongWeight.class,
				WeightSelection.Zero), timestampSeconds);
		BatchGenerator bg = new M1BatchTimed(reader, seconds, millis);
		((M1BatchTimed) bg).setDebug(debug);

		Metric[] metrics = new Metric[] { new DegreeDistributionU(),
				new DirectedMotifsU() };

		Series s = new Series(gg, bg, metrics,
				dir + seconds + "_timed/series/", "s1");

		SeriesData sd = s.generate(1, maxBatches, false);

		if (plot) {
			plot(sd, dir + seconds + "_timed/plots/", true, true);
		}

		GraphVisualization.setText("Finished");
		Log.infoSep();
	}

	public static void plot(SeriesData sd, String dir, boolean plotSingle,
			boolean plotMulti) throws IOException, InterruptedException {
		if (plotMulti) {
			GraphVisualization.setText("Generating multi scalar plots");
			Plotting.plot(sd, dir, new PlottingConfig(
					PlotFlag.plotMultiScalarValues));
		}
		if (plotSingle) {
			String defXTics = Config.get(gnuplot_xtics);
			String defDateTime = Config.get(gnuplot_datetime);
			String defPlotDateTime = Config.get(gnuplot_plotdatetime);
			Config.overwrite(gnuplot_xtics, "300");
			Config.overwrite(gnuplot_datetime, "%H:%M:%S");
			Config.overwrite(gnuplot_plotdatetime, "true");
			GraphVisualization.setText("Generating single scalar plots");
			Plotting.plot(sd, dir, new PlottingConfig(
					PlotFlag.plotSingleScalarValues));
			Config.overwrite(gnuplot_xtics, defXTics);
			Config.overwrite(gnuplot_datetime, defDateTime);
			Config.overwrite(gnuplot_plotdatetime, defPlotDateTime);
		}
	}

}
