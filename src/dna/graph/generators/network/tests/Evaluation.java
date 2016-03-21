package dna.graph.generators.network.tests;

import java.io.IOException;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;

import dna.graph.datastructures.GDS;
import dna.graph.generators.GraphGenerator;
import dna.graph.generators.network.EmptyNetwork;
import dna.graph.generators.network.m1.M1Batch;
import dna.graph.weights.TypedWeight;
import dna.graph.weights.Weight.WeightSelection;
import dna.graph.weights.intW.IntWeight;
import dna.labels.labeler.Labeler;
import dna.labels.labeler.LabelerNotApplicableException;
import dna.labels.labeler.darpa.DarpaAttackLabeler;
import dna.labels.labeler.ids.IntrusionDetectionLabeler1;
import dna.metrics.Metric;
import dna.metrics.MetricNotApplicableException;
import dna.metrics.assortativity.AssortativityU;
import dna.metrics.centrality.BetweennessCentralityU;
import dna.metrics.clustering.DirectedClusteringCoefficientU;
import dna.metrics.clustering.local.DirectedLocalClusteringCoefficientR;
import dna.metrics.degree.DegreeDistributionR;
import dna.metrics.degree.DegreeDistributionU;
import dna.metrics.degree.WeightedDegreeDistributionR;
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
import dna.visualization.graph.GraphVisualization;

public class Evaluation {

	public static final String gnuplot_xtics = "GNUPLOT_XTICS";
	public static final String gnuplot_datetime = "GNUPLOT_DATETIME";
	public static final String gnuplot_plotdatetime = "GNUPLOT_PLOTDATETIME";
	public static final String gnuplot_xoffset = "GNUPLOT_XOFFSET";
	public static final long second = 1;
	public static final long minute = 60 * second;
	public static final long hour = 60 * minute;

	public static final String day1 = "monday";
	public static final String day2 = "tuesday";
	public static final String day3 = "wednesday";
	public static final String day4 = "thursday";
	public static final String day5 = "friday";
	public static final String[] entireWeek = new String[] { day1, day2, day3,
			day4, day5 };

	public static void main(String[] args) throws IOException, ParseException,
			AggregationException, MetricNotApplicableException,
			InterruptedException, LabelerNotApplicableException {
		String baseDir = "data/darpa1998/";
		String baseDirSmall = "data/darpa1998_small/";
		String attackList = "attacks.list";

		boolean generate = true;
		boolean plot = true;

		generate(baseDir, attackList, 1, true, metricsDefault, true, day1);
		
//		generate(baseDirSmall, attackList, 1, generate, metricsDefault, plot,
//				entireWeek);
	}

	public static void generate(String baseDir, String attackList, int weekId,
			boolean generate, Metric[] metrics, boolean plot, String... days)
			throws IOException, ParseException, AggregationException,
			MetricNotApplicableException, InterruptedException,
			LabelerNotApplicableException {
		int secondsPerBatch = 1;
		int maxBatches = 100000;
		long lifeTimePerEdgeSeconds = hour;
		long lifeTimePerEdge = lifeTimePerEdgeSeconds * 1000;
		String week = "w" + weekId;

		if (generate) {
			for (int i = 0; i < days.length; i++) {
				String day = days[i];
				String list = week + day + ".list";
				String name = week + day + "_" + secondsPerBatch + "_"
						+ lifeTimePerEdge;

				modell_1_test(baseDir + week + "/", list, name, baseDir,
						attackList, secondsPerBatch, maxBatches, true, true,
						lifeTimePerEdge, metrics);
			}
		}

		if (plot) {
			for (int i = 0; i < days.length; i++) {
				String day = days[i];
				String list = week + day + ".list";
				String name = week + day + "_" + secondsPerBatch + "_"
						+ lifeTimePerEdge;

				Log.info("reading " + week + day + "data!");
				SeriesData sd = SeriesData.read(baseDir + week + "/" + name
						+ "/series/", week + day, false, false);
				Log.info("plotting " + week + day + "data!");
				plot(sd, baseDir + week + "/" + name + "/plots/");
			}
		}
	}

	public static SeriesData modell_1_test(String dir, String filename,
			String name, String attackListDir, String attackListFilename,
			int batchLength, int maxBatches, long edgeLifeTime, Metric[] metrics)
			throws IOException, ParseException, AggregationException,
			MetricNotApplicableException, InterruptedException,
			LabelerNotApplicableException {
		return modell_1_test(dir, filename, name, attackListDir,
				attackListFilename, batchLength, maxBatches, true, true,
				edgeLifeTime, metrics);
	}

	public static SeriesData modell_1_test(String dir, String filename,
			String name, String attackListDir, String attackListFilename,
			int batchLength, int maxBatches, boolean removeInactiveEdges,
			boolean removeZeroDegreeNodes, long edgeLifeTime, Metric[] metrics)
			throws IOException, ParseException, AggregationException,
			MetricNotApplicableException, InterruptedException,
			LabelerNotApplicableException {
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
		GraphGenerator gg = new EmptyNetwork(GDS.directedVE(TypedWeight.class,
				WeightSelection.None, IntWeight.class, WeightSelection.Zero),
				timestampSeconds);

		// init batch generator
		BatchGenerator bg = new M1Batch(reader);

		// init labeler
		Labeler[] labeler = new Labeler[] { new IntrusionDetectionLabeler1(),
				new DarpaAttackLabeler(attackListDir, attackListFilename) };

		// init series
		Series s = new Series(gg, bg, metrics, labeler,
				dir + name + "/series/", "s1");

		// generate
		SeriesData sd = s.generate(1, maxBatches, false);
		GraphVisualization.setText("Finished generation");
		Log.infoSep();
		return sd;
	}

	/*
	 * PLOTTING
	 */
	public static void plot(SeriesData sd, String dir) throws IOException,
			InterruptedException {
		String defXTics = Config.get(gnuplot_xtics);
		String defDateTime = Config.get(gnuplot_datetime);
		String defPlotDateTime = Config.get(gnuplot_plotdatetime);
		// Config.overwrite(gnuplot_xtics, "7200");
		Config.overwrite(gnuplot_datetime, "%H:%M");
		// Config.overwrite(gnuplot_datetime, "%M:%S");
		Config.overwrite(gnuplot_plotdatetime, "true");
		GraphVisualization.setText("Generating single scalar plots for "
				+ sd.getName());
		Plotting.plot(sd, dir, new PlottingConfig(
				PlotFlag.plotSingleScalarValues));
		Config.overwrite(gnuplot_xtics, defXTics);
		Config.overwrite(gnuplot_datetime, defDateTime);
		Config.overwrite(gnuplot_plotdatetime, defPlotDateTime);
		GraphVisualization
				.setText("Plotting of " + sd.getName() + " finished!");
	}

	/*
	 * METRICS
	 */

	public static final String[] metricHostFilter = new String[] { "HOST" };
	public static final String[] metricPortFilter = new String[] { "PORT" };

	public static final Metric[] metricsAll = new Metric[] {
			new AssortativityU(), new BetweennessCentralityU(),
			new DirectedClusteringCoefficientU(),
			new DirectedLocalClusteringCoefficientR(),
			new UnweightedAllPairsShortestPathsU(),
			new IntWeightedAllPairsShortestPathsU(),
			new RichClubConnectivityByDegreeU(), new MatchingU(),
			new OverlapU(), new DegreeDistributionU(), new EdgeWeightsR(1.0),
			new DirectedMotifsU() };

	public static final Metric[] metricsDefault = new Metric[] {
			new DegreeDistributionU(), new DirectedMotifsU(),
			new EdgeWeightsR(1.0), new WeightedDegreeDistributionR() };

	public static final Metric[] metricsDefaultHostOnly = new Metric[] {
			new DegreeDistributionR(Evaluation.metricHostFilter),
			new DirectedMotifsU(), new EdgeWeightsR(1.0),
			new WeightedDegreeDistributionR(Evaluation.metricHostFilter) };

	public static final Metric[] metricsDefaultPortOnly = new Metric[] {
			new DegreeDistributionR(Evaluation.metricPortFilter),
			new DirectedMotifsU(), new EdgeWeightsR(1.0),
			new WeightedDegreeDistributionR(Evaluation.metricPortFilter) };

	public static final Metric[] metricsDefaultAll = new Metric[] {
			new DegreeDistributionR(Evaluation.metricHostFilter),
			new DegreeDistributionR(Evaluation.metricPortFilter),
			new DegreeDistributionR(), new DirectedMotifsU(),
			new EdgeWeightsR(1.0),
			new WeightedDegreeDistributionR(Evaluation.metricHostFilter),
			new WeightedDegreeDistributionR(Evaluation.metricPortFilter),
			new WeightedDegreeDistributionR() };

}
