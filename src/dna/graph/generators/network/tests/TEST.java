package dna.graph.generators.network.tests;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import dna.graph.datastructures.GDS;
import dna.graph.generators.GraphGenerator;
import dna.graph.generators.network.EmptyNetwork;
import dna.graph.generators.network.NetworkEdge;
import dna.graph.generators.network.m1.M1Batch;
import dna.graph.generators.network.m1.M1Graph;
import dna.graph.generators.network.weights.NetworkWeight;
import dna.graph.weights.IntWeight;
import dna.graph.weights.LongWeight;
import dna.graph.weights.Weight.WeightSelection;
import dna.labels.labeler.Labeler;
import dna.labels.labeler.LabelerNotApplicableException;
import dna.labels.labeler.ids.IntrusionDetectionLabeler1;
import dna.metrics.Metric;
import dna.metrics.MetricNotApplicableException;
import dna.metrics.assortativity.AssortativityU;
import dna.metrics.centrality.BetweennessCentralityU;
import dna.metrics.clustering.DirectedClusteringCoefficientU;
import dna.metrics.clustering.local.DirectedLocalClusteringCoefficientR;
import dna.metrics.degree.DegreeDistributionR;
import dna.metrics.degree.DegreeDistributionU;
import dna.metrics.motifs.DirectedMotifsU;
import dna.metrics.paths.IntWeightedAllPairsShortestPathsU;
import dna.metrics.paths.UnweightedAllPairsShortestPathsU;
import dna.metrics.richClub.RichClubConnectivityByDegreeU;
import dna.metrics.similarityMeasures.matching.MatchingU;
import dna.metrics.similarityMeasures.overlap.OverlapU;
import dna.metrics.weights.EdgeWeightsR;
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
import dna.util.network.tcp.TCPEventReader;
import dna.visualization.graph.GraphVisualization;

public class TEST {

	public static final String gnuplot_xtics = "GNUPLOT_XTICS";
	public static final String gnuplot_datetime = "GNUPLOT_DATETIME";
	public static final String gnuplot_plotdatetime = "GNUPLOT_PLOTDATETIME";
	public static final String gnuplot_xoffset = "GNUPLOT_XOFFSET";

	public static final long w2mon_start = 897285347;
	public static final long w2tue_start = 897371832;
	public static final long w5thu_start = 899359214;

	public static final long second = 1;
	public static final long minute = 60 * second;
	public static final long hour = 60 * minute;

	// public static final Metric[] metrics = new Metric[] {
	// new DegreeDistributionU(), new EdgeWeightsR(1.0),
	// new DirectedMotifsU() };

	public static final Metric[] metrics = new Metric[] {
			new AssortativityU(),
			new BetweennessCentralityU(),
			new DirectedClusteringCoefficientU(),
			new DirectedLocalClusteringCoefficientR(),
			// new StrongConnectivityU(),
			// new WeakConnectivityU(),
			new UnweightedAllPairsShortestPathsU(),
			new IntWeightedAllPairsShortestPathsU(),
			new RichClubConnectivityByDegreeU(), new MatchingU(),
			new OverlapU(), new DegreeDistributionU(), new EdgeWeightsR(1.0),
			new DirectedMotifsU() };

	public static final Metric[] metrics_m1 = new Metric[] {
			new DegreeDistributionU(), new DirectedMotifsU() };

	public static void main(String[] args) throws IOException,
			InterruptedException, AggregationException,
			MetricNotApplicableException, ClassNotFoundException,
			ParseException, LabelerNotApplicableException {
		Config.overwrite("GNUPLOT_PATH",
				"C://Program Files (x86)//gnuplot//bin//gnuplot.exe");
		Config.overwrite("GRAPH_VIS_NETWORK_NODE_SHAPE", "true");
		Config.overwrite("GRAPH_VIS_NODE_DEFAULT_SIZE", "14");
		Config.overwrite("GRAPH_VIS_TIMESTAMP_IN_SECONDS", "true");
		Config.overwrite("GRAPH_VIS_DATETIME_FORMAT", "hh:mm:ss");
		Config.overwrite("GNUPLOT_DEFAULT_PLOT_LABELS", "true");
		Config.zipBatches();
		// GraphVisualization.enable();

		boolean plot = true;
		boolean debug = false;

		boolean removeInactiveEdges = false;
		boolean removeZeroDegreeNodes = false;

		boolean normalTest = false;
		boolean timedTest = false;
		boolean timedTest2 = false;
		boolean nodeTypeTest = false;

		boolean w2mondayGen = true;
		boolean w2mondayPlot = true;
		boolean w2mondayStepPlot = false;

		boolean w2tuesdayGen = false;
		boolean w2tuesdayPlot = false;
		boolean w2tuesdayStepPlot = false;

		boolean w5thursdayGen = false;
		boolean w5thursdayPlot = false;
		boolean w5thursdayStepPlot = false;

		boolean w5thursday11Gen = false;
		boolean w5thursday11Plot = false;

		int secondsPerBatch = 1;
		int maxBatches = 100000;

		long plotInterval = 6 * hour;
		int plotIntervalSteps = 8;
		double plotOverlapPercent = 0.5;

		long lifeTimePerEdgeSeconds = hour;
		long lifeTimePerEdge = lifeTimePerEdgeSeconds * 1000;

		String dir = "data/tcp_test/10/";
		String file = "out_10_3.list";

		String name = secondsPerBatch + "_" + lifeTimePerEdge;

		Log.infoSep();
		Log.info("NETWORK-TESTS");
		Log.info("dir:\t\t" + dir);
		Log.info("file:\t\t" + file);
		Log.info("seconds:\t" + secondsPerBatch);
		Log.info("maxBatches:\t" + maxBatches);
		Log.infoSep();

		if (normalTest) {
			// TCPTEST1("data/tcp_test/10/", "out_10_3.list", secondsPerBatch,
			// maxBatches, plot, debug);
			modell_1_test("data/tcp_test/10/", "out_10_3.list", "normal",
					secondsPerBatch, maxBatches, false, false, lifeTimePerEdge,
					plot, debug);
		}
		if (timedTest)
			TCPTEST1TIMED("data/tcp_test/10/", "out_10_3.list",
					secondsPerBatch, lifeTimePerEdge, maxBatches, plot, debug);
		if (timedTest2) {
			TCPTEST1TIMED2("data/tcp_test/10/", "out_10_3.list",
					secondsPerBatch, lifeTimePerEdge, maxBatches, plot, debug);

			// blub();
		}

		if (nodeTypeTest)
			NodeTypeTest("data/tcp_test/10/", "out_10_3.list", secondsPerBatch,
					lifeTimePerEdge, maxBatches, plot, debug);

		if (w2mondayGen) {
			modell_1_test("data/tcp_test/w2monday/", "w2monday.list", name,
					secondsPerBatch, maxBatches, true, true, lifeTimePerEdge,
					false, debug);

			// modell_1_test("data/tcp_test/w2mon-00-19/", "out00-19.list",
			// name,
			// secondsPerBatch, maxBatches, true, true, lifeTimePerEdge,
			// false, debug);
		}
		if (w2mondayPlot) {
			Log.info("reading w2 monday data");
			SeriesData sd = SeriesData.read("data/tcp_test/w2monday/" + name
					+ "/series/", "w2-monday", false, false);
			Log.info("plotting w2 monday data");
			plotW2Single(sd, "data/tcp_test/w2monday/" + name
					+ "/series/plots/");

			// Log.info("reading w2 monday data");
			// SeriesData sd = SeriesData.read("data/tcp_test/w2mon-00-19/" +
			// name
			// + "/series/", "w2-monday-00-19", false, false);
			// Log.info("plotting w2 monday data");
			// plotW2(sd, "data/tcp_test/w2mon-00-19/" + name +
			// "/series/plots/");
		}

		if (w2mondayStepPlot) {
			Log.info("reading w2 monday data");
			SeriesData sd = SeriesData.read("data/tcp_test/w2monday/" + name
					+ "/series/", "w2-monday", false, false);
			Log.info("plotting w2 monday data");
			plotIntervals(sd, "data/tcp_test/w2monday/" + name + "/plots/",
					w2mon_start, plotInterval, plotOverlapPercent,
					plotIntervalSteps, true, false);
		}

		if (w2tuesdayGen) {
			modell_1_test("data/tcp_test/w2tuesday/", "w2tuesday.list", name,
					secondsPerBatch, maxBatches, true, true, lifeTimePerEdge,
					false, debug);
		}
		if (w2tuesdayPlot) {
			Log.info("reading w2 tuesday data");
			SeriesData sd = SeriesData.read("data/tcp_test/w2tuesday/" + name
					+ "/series/", "w2-tuesday", false, false);
			Log.info("plotting w2 tuesday data");
			plotW2Single(sd, "data/tcp_test/w2tuesday/" + name
					+ "/series/plots/");
			// plotW2Multi(sd, "data/tcp_test/w2tuesday/" + name
			// + "/series/plots/");
		}
		if (w2tuesdayStepPlot) {
			Log.info("reading w2 tuesday data");
			SeriesData sd = SeriesData.read("data/tcp_test/w2tuesday/" + name
					+ "/series/", "w2-tuesday", false, false);
			Log.info("plotting w2 tuesday data");
			plotIntervals(sd, "data/tcp_test/w2tuesday/" + name + "/plots/",
					w2tue_start, plotInterval, plotOverlapPercent,
					plotIntervalSteps, true, false);
		}

		if (w5thursdayGen) {
			modell_1_test("data/tcp_test/w5thursday/", "w5thursday.list", name,
					secondsPerBatch, maxBatches, true, true, lifeTimePerEdge,
					false, debug);
		}
		if (w5thursdayPlot) {
			Log.info("reading w5 thursday data");
			SeriesData sd = SeriesData.read("data/tcp_test/w5thursday/" + name
					+ "/series/", "w5-thursday", false, false);
			Log.info("plotting w5 thursday data");
			plotW5(sd, "data/tcp_test/w5thursday/" + name + "/series/plots/");
		}
		if (w5thursdayStepPlot) {
			Log.info("reading w5 thursday data");
			SeriesData sd = SeriesData.read("data/tcp_test/w5thursday/" + name
					+ "/series/", "w5-thursday", false, false);
			Log.info("plotting w5 thursday data");
			plotIntervals(sd, "data/tcp_test/w5thursday/" + name + "/plots/",
					w5thu_start, plotInterval, plotOverlapPercent,
					plotIntervalSteps, true, false);
		}

		if (w5thursday11Gen) {
			modell_1_test("data/tcp_test/w5thursday-11/", "w5thursday-11.list",
					name, secondsPerBatch, maxBatches, true, true,
					lifeTimePerEdge, false, debug);
		}

		if (w5thursday11Plot) {
			Log.info("reading w5 thursday data");
			SeriesData sd = SeriesData.read("data/tcp_test/w5thursday-11/"
					+ name + "/series/", "w5-thursday-11", false, false);
			Log.info("plotting w5 thursday data");
			plotW2Single(sd, "data/tcp_test/w5thursday-11/" + name
					+ "/series/plots/");
		}

	}

	public static void blub2(long start, long end) throws FileNotFoundException {
		TCPEventReader r = new DefaultTCPEventReader("data/tcp_test/10/",
				"out_10_3.list");

		NetworkEdge n1 = new NetworkEdge(1, 5, 10L);
		NetworkEdge n2 = new NetworkEdge(2, 3, 15L);
		NetworkEdge n3 = new NetworkEdge(1, 3, 17L);

		r.addEdgeToQueue(n1);
		r.addEdgeToQueue(n2);
		r.addEdgeToQueue(n3);

		Log.infoSep();
		Log.info("events between: " + start + " - " + end);
		for (NetworkEdge e : r.getDecrementEdges(end))
			System.out.println("\t" + e.toString());
		Log.infoSep();
	}

	public static void blub() throws FileNotFoundException {

		for (long i = 0; i < 21; i++) {
			blub2(0, i);
		}

	}

	public static void plotW5(SeriesData sd, String dir) throws IOException,
			InterruptedException {
		String defXTics = Config.get(gnuplot_xtics);
		String defDateTime = Config.get(gnuplot_datetime);
		String defPlotDateTime = Config.get(gnuplot_plotdatetime);
		String defXOffset = Config.get(gnuplot_xoffset);
		Config.overwrite(gnuplot_xtics, "7200");
		Config.overwrite(gnuplot_datetime, "%H:%M");
		Config.overwrite(gnuplot_plotdatetime, "true");
		GraphVisualization.setText("Generating single scalar plots");
		Plotting.plot(sd, dir, new PlottingConfig(
				PlotFlag.plotSingleScalarValues));
		Config.overwrite(gnuplot_xtics, defXTics);
		Config.overwrite(gnuplot_datetime, defDateTime);
		Config.overwrite(gnuplot_plotdatetime, defPlotDateTime);
		Config.overwrite(gnuplot_xoffset, defXOffset);
	}

	public static void plotIntervals(SeriesData sd, String dir, long from,
			long interval, double overlapPercent, int steps,
			boolean plotSingle, boolean plotMulti) throws IOException,
			InterruptedException {

		long inset = (long) Math.floor((overlapPercent * interval));

		for (int i = 0; i < steps; i++) {
			long begin = from + i
					* (interval - (long) Math.floor(overlapPercent * interval));
			long end = begin + interval;

			if (plotSingle)
				plotFromToSingle(sd, dir + i + "/", begin, end,
						PlotFlag.plotSingleScalarValues);

			if (plotMulti)
				plotFromToSingle(sd, dir + i + "/", begin, end,
						PlotFlag.plotNodeValueLists);
		}
	}

	public static void plotFromToSingle(SeriesData sd, String dir, long from,
			long to, PlotFlag... flags) throws IOException,
			InterruptedException {
		String defXTics = Config.get(gnuplot_xtics);
		String defDateTime = Config.get(gnuplot_datetime);
		String defPlotDateTime = Config.get(gnuplot_plotdatetime);
		// Config.overwrite(gnuplot_xtics, "7200");
		Config.overwrite(gnuplot_datetime, "%H:%M");
		Config.overwrite(gnuplot_plotdatetime, "true");
		PlottingConfig pcfg = new PlottingConfig(flags);
		pcfg.setPlotInterval(from, to, 1);
		Log.info("Generating plots from " + from + " to " + to + ". -> '" + dir
				+ "'");
		GraphVisualization.setText("Generating plots from " + from + " to "
				+ to + ".");
		Plotting.plotRun(sd, 0, dir, pcfg);
		Config.overwrite(gnuplot_xtics, defXTics);
		Config.overwrite(gnuplot_datetime, defDateTime);
		Config.overwrite(gnuplot_plotdatetime, defPlotDateTime);
	}

	public static void plotFromToMulti(SeriesData sd, String dir, long from,
			long to, PlotFlag... flags) throws IOException,
			InterruptedException {
		PlottingConfig pcfg = new PlottingConfig(flags);
		pcfg.setPlotInterval(from, to, 1);
		Log.info("Generating plots from " + from + " to " + to + ". -> '" + dir
				+ "'");
		GraphVisualization.setText("Generating plots from " + from + " to "
				+ to + ".");
		Plotting.plotRun(sd, 0, dir, pcfg);
	}

	public static void plotW2Single(SeriesData sd, String dir)
			throws IOException, InterruptedException {
		String defXTics = Config.get(gnuplot_xtics);
		String defDateTime = Config.get(gnuplot_datetime);
		String defPlotDateTime = Config.get(gnuplot_plotdatetime);
		Config.overwrite(gnuplot_xtics, "7200");
		Config.overwrite(gnuplot_datetime, "%H:%M");
		Config.overwrite(gnuplot_plotdatetime, "true");
		GraphVisualization.setText("Generating single scalar plots");
		Plotting.plot(sd, dir, new PlottingConfig(
				PlotFlag.plotSingleScalarValues));
		Config.overwrite(gnuplot_xtics, defXTics);
		Config.overwrite(gnuplot_datetime, defDateTime);
		Config.overwrite(gnuplot_plotdatetime, defPlotDateTime);
	}

	public static void plotW2Multi(SeriesData sd, String dir)
			throws IOException, InterruptedException {
		GraphVisualization.setText("Generating multi scalar plots");
		Plotting.plot(sd, dir, new PlottingConfig(PlotFlag.plotNodeValueLists));
	}

	public static void NodeTypeTest(String dir, String filename, int seconds,
			long millis, int maxBatches, boolean plot, boolean debug)
			throws IOException, ParseException, AggregationException,
			MetricNotApplicableException, InterruptedException,
			LabelerNotApplicableException {
		Log.info("M1-Batch test with node-types as weights!");
		Log.info("Lifetime:\t" + millis + "ms");
		Config.overwrite("GRAPH_VIS_SHOW_NODE_INDEX", "true");

		DefaultTCPEventReader reader = new DefaultTCPEventReader(dir, filename);
		reader.setBatchInterval(seconds);
		reader.setEdgeLifeTime(millis);

		GraphGenerator gg = new M1Graph(GDS.directed(), reader);

		long timestampMillis = reader.getInitTimestamp().getMillis();
		long timestampSeconds = TimeUnit.MILLISECONDS
				.toSeconds(timestampMillis);

		gg = new EmptyNetwork(GDS.directedVE(NetworkWeight.class,
				WeightSelection.None, LongWeight.class, WeightSelection.Zero),
				timestampSeconds);

		BatchGenerator bg = new M1Batch(reader);
		((M1Batch) bg).setDebug(debug);

		Metric[] metrics = new Metric[] { new DegreeDistributionU(),
				new DirectedMotifsU() };

		Series s = new Series(gg, bg, metrics, dir + seconds + "_ntt/series/",
				"s1");

		SeriesData sd = s.generate(1, maxBatches, false);

		if (plot) {
			plot(sd, dir + seconds + "_ntt/plots/", true, true);
		}

		GraphVisualization.setText("Finished");
		Log.infoSep();
	}

	public static void modell_1_test(String dir, String filename, String name,
			int batchLength, int maxBatches, boolean removeInactiveEdges,
			boolean removeZeroDegreeNodes, long edgeLifeTime, boolean plot,
			boolean debug) throws IOException, ParseException,
			AggregationException, MetricNotApplicableException,
			InterruptedException, LabelerNotApplicableException {
		Log.info("Modell 1 test!");
		Config.overwrite("GRAPH_VIS_SHOW_NODE_INDEX", "true");

		DefaultTCPEventReader reader = new DefaultTCPEventReader(dir, filename);
		reader.setBatchInterval(batchLength);
		reader.setEdgeLifeTime(edgeLifeTime);
		reader.setRemoveInactiveEdges(removeInactiveEdges);
		reader.setRemoveZeroDegreeNodes(removeZeroDegreeNodes);

		// init graph generator
		long timestampMillis = reader.getInitTimestamp().getMillis();
		long timestampSeconds = TimeUnit.MILLISECONDS
				.toSeconds(timestampMillis);
		GraphGenerator gg = new EmptyNetwork(GDS.directedVE(
				NetworkWeight.class, WeightSelection.None, IntWeight.class,
				WeightSelection.Zero), timestampSeconds);

		// gg = new RandomGraph(GDS.directedVE(NetworkWeight.class,
		// WeightSelection.None, IntWeight.class, WeightSelection.Zero),
		// 0, 0, timestampSeconds);

		// init batch generator
		BatchGenerator bg = new M1Batch(reader);
		((M1Batch) bg).setDebug(debug);

		// init metrics
		Metric[] metrics = TEST.metrics_m1;

		// init labeler
		Labeler[] labeler = new Labeler[] { new IntrusionDetectionLabeler1() };

		// init series
		Series s = new Series(gg, bg, metrics, labeler,
				dir + name + "/series/", "s1");

		// generate
		SeriesData sd = s.generate(1, maxBatches, false);

		// plot
		if (plot) {
			plot(sd, dir + batchLength + name + "/plots/", true, false);
		}

		GraphVisualization.setText("Finished");
		Log.infoSep();
	}

	public static void TCPTEST1(String dir, String filename, int seconds,
			int maxBatches, boolean plot, boolean debug) throws IOException,
			ParseException, AggregationException, MetricNotApplicableException,
			InterruptedException, LabelerNotApplicableException {
		Log.info("M1-Batch test with permanent edges!");
		Config.overwrite("GRAPH_VIS_SHOW_NODE_INDEX", "true");

		DefaultTCPEventReader reader = new DefaultTCPEventReader(dir, filename);
		reader.setBatchInterval(seconds);

		GraphGenerator gg = new M1Graph(GDS.directed(), reader);

		long timestampMillis = reader.getInitTimestamp().getMillis();
		long timestampSeconds = TimeUnit.MILLISECONDS
				.toSeconds(timestampMillis);
		gg = new EmptyNetwork(GDS.directedV(NetworkWeight.class,
				WeightSelection.None), timestampSeconds);

		BatchGenerator bg = new M1Batch(reader);
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
			MetricNotApplicableException, InterruptedException,
			LabelerNotApplicableException {
		Log.info("M1-Batch test with timed edges!");
		Log.info("Lifetime:\t" + millis + "ms");
		Config.overwrite("GRAPH_VIS_SHOW_NODE_INDEX", "true");

		DefaultTCPEventReader reader = new DefaultTCPEventReader(dir, filename);
		reader.setBatchInterval(seconds);
		reader.setEdgeLifeTime(millis);

		GraphGenerator gg = new M1Graph(GDS.directed(), reader);

		long timestampMillis = reader.getInitTimestamp().getMillis();
		long timestampSeconds = TimeUnit.MILLISECONDS
				.toSeconds(timestampMillis);
		gg = new EmptyNetwork(GDS.directedVE(NetworkWeight.class,
				WeightSelection.None, IntWeight.class, WeightSelection.Zero),
				timestampSeconds);
		BatchGenerator bg = new M1Batch(reader);
		((M1Batch) bg).setDebug(debug);

		Metric[] metrics = new Metric[] { new DegreeDistributionU(),
				new EdgeWeightsR(1.0), new DirectedMotifsU() };

		Series s = new Series(gg, bg, metrics,
				dir + seconds + "_timed/series/", "s1");

		SeriesData sd = s.generate(1, maxBatches, false);

		if (plot) {
			plot(sd, dir + seconds + "_timed/plots/", true, false);
		}

		GraphVisualization.setText("Finished");
		Log.infoSep();
	}

	public static void TCPTEST1TIMED2(String dir, String filename, int seconds,
			long millis, int maxBatches, boolean plot, boolean debug)
			throws IOException, ParseException, AggregationException,
			MetricNotApplicableException, InterruptedException,
			LabelerNotApplicableException {
		Log.info("M1-Batch test with timed edges!");
		Log.info("Lifetime:\t" + millis + "ms");
		Config.overwrite("GRAPH_VIS_SHOW_NODE_INDEX", "true");

		DefaultTCPEventReader reader = new DefaultTCPEventReader(dir, filename);
		reader.setRemoveInactiveEdges(true);
		reader.setRemoveZeroDegreeNodes(true);
		reader.setBatchInterval(seconds);
		reader.setEdgeLifeTime(millis);
		GraphGenerator gg = new M1Graph(GDS.directed(), reader);

		long timestampMillis = reader.getInitTimestamp().getMillis();
		long timestampSeconds = TimeUnit.MILLISECONDS
				.toSeconds(timestampMillis);
		gg = new EmptyNetwork(GDS.directedVE(NetworkWeight.class,
				WeightSelection.None, IntWeight.class, WeightSelection.Zero),
				timestampSeconds);
		//
		// gg = new RandomGraph(GDS.directedVE(NetworkWeight.class,
		// WeightSelection.None, LongWeight.class, WeightSelection.Zero), 0,0);
		//
		BatchGenerator bg = new M1Batch(reader);
		((M1Batch) bg).setDebug(debug);

		Metric[] metrics = new Metric[] { new EdgeWeightsR(1.0),
				new DegreeDistributionR(), new DirectedMotifsU() };

		Series s = new Series(gg, bg, metrics, dir + seconds
				+ "_timed2/series/", "s1");

		SeriesData sd = s.generate(1, maxBatches, false);

		Log.info("SET");
		Set<NetworkEdge> set = reader.getEdgeWeightMap().keySet();
		for (NetworkEdge netEdge : set) {
			System.out.println(netEdge.getSrc() + "  =>  " + netEdge.getDst()
					+ "\tw= "
					+ reader.getEdgeWeightMap().get(netEdge).getWeight());
		}

		// -1

		Log.info("SET2");

		if (plot) {
			plot(sd, dir + seconds + "_timed2/plots/", true, false);
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
