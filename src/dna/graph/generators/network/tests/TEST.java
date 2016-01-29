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
import dna.graph.generators.network.m1.M1BatchTimed;
import dna.graph.generators.network.m1.M1BatchTimed2;
import dna.graph.generators.network.m1.M1Graph;
import dna.graph.generators.network.weights.NetworkWeight;
import dna.graph.generators.random.RandomGraph;
import dna.graph.weights.LongWeight;
import dna.graph.weights.Weight.WeightSelection;
import dna.metrics.Metric;
import dna.metrics.MetricNotApplicableException;
import dna.metrics.degree.DegreeDistributionR;
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
import dna.util.network.tcp.TCPEventReader;
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
		Config.overwrite("GRAPH_VIS_NETWORK_NODE_SHAPE", "true");
		Config.overwrite("GRAPH_VIS_NODE_DEFAULT_SIZE", "14");

		Config.zipBatches();
		GraphVisualization.enable();

		boolean plot = true;
		boolean debug = true;

		boolean normalTest = false;
		boolean timedTest = false;
		boolean timedTest2 = true;
		boolean nodeTypeTest = false;
		boolean w2tuesdayGen = false;
		boolean w2tuesdayPlot = false;

		int secondsPerBatch = 1;
		int maxBatches = 1;
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
					maxBatches, plot, debug);

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

		if (w2tuesdayGen)
			TCPTEST1TIMED("data/tcp_test/w2tuesday/", "w2tuesday.list",
					secondsPerBatch, lifeTimePerEdge, maxBatches, false, debug);

		if (w2tuesdayPlot) {
			Log.info("reading w2 tuesday data");
			SeriesData sd = SeriesData.read(
					"data/tcp_test/w2tuesday/1_timed/series/", "w2-tuesday",
					false, false);
			Log.info("plotting w2 tuesday data");
			plotW2(sd, "data/tcp_test/w2tuesday/1_timed/series/plots/");
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

	public static void plotW2(SeriesData sd, String dir) throws IOException,
			InterruptedException {
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

		gg = new EmptyNetwork(GDS.directedVE(NetworkWeight.class,
				WeightSelection.None, LongWeight.class, WeightSelection.Zero),
				timestampSeconds);

		BatchGenerator bg = new M1BatchTimed(reader, seconds, millis);
		((M1BatchTimed) bg).setDebug(debug);

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
		gg = new EmptyNetwork(GDS.directedV(NetworkWeight.class,
				WeightSelection.None), timestampSeconds);

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
		gg = new EmptyNetwork(GDS.directedVE(NetworkWeight.class,
				WeightSelection.None, LongWeight.class, WeightSelection.Zero),
				timestampSeconds);
		BatchGenerator bg = new M1BatchTimed(reader, seconds, millis);
		((M1BatchTimed) bg).setDebug(debug);

		Metric[] metrics = new Metric[] { new DegreeDistributionU(),
				new DirectedMotifsU() };

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
			MetricNotApplicableException, InterruptedException {
		Log.info("M1-Batch test with timed edges!");
		Log.info("Lifetime:\t" + millis + "ms");
		Config.overwrite("GRAPH_VIS_SHOW_NODE_INDEX", "true");

		DefaultTCPEventReader reader = new DefaultTCPEventReader(dir, filename);
		GraphGenerator gg = new M1Graph(GDS.directed(), reader);

		long timestampMillis = reader.getInitTimestamp().getMillis();
		long timestampSeconds = TimeUnit.MILLISECONDS
				.toSeconds(timestampMillis);
		gg = new EmptyNetwork(GDS.directedVE(NetworkWeight.class,
				WeightSelection.None, LongWeight.class, WeightSelection.Zero),
				timestampSeconds);
//		
//		gg = new RandomGraph(GDS.directedVE(NetworkWeight.class,
//				WeightSelection.None, LongWeight.class, WeightSelection.Zero), 0,0);
//		
		BatchGenerator bg = new M1BatchTimed2(reader, seconds, millis);
		((M1BatchTimed2) bg).setDebug(debug);

		Metric[] metrics = new Metric[] { new DegreeDistributionU(), new DegreeDistributionR(),
				new DirectedMotifsU() };

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
