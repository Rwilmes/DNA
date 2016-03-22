package dna.graph.generators.network.tests;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import dna.graph.datastructures.GDS;
import dna.graph.generators.GraphGenerator;
import dna.graph.generators.network.EmptyNetwork;
import dna.graph.generators.network.m1.M1Batch;
import dna.graph.weights.TypedWeight;
import dna.graph.weights.Weight.WeightSelection;
import dna.graph.weights.intW.IntWeight;
import dna.labels.Label;
import dna.labels.labeler.Labeler;
import dna.labels.labeler.LabelerNotApplicableException;
import dna.labels.labeler.darpa.DarpaAttackLabeler;
import dna.labels.labeler.ids.IntrusionDetectionLabeler1;
import dna.labels.util.LabelStat;
import dna.labels.util.LabelUtils;
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
import dna.util.network.tcp.TCPEventReader;
import dna.visualization.graph.GraphVisualization;

public class Evaluation {

	public static final long second = 1;
	public static final long minute = 60 * second;
	public static final long hour = 60 * minute;

	public static final int week1 = 1;
	public static final int week2 = 2;
	public static final int week3 = 3;
	public static final int week4 = 4;
	public static final int week5 = 5;
	public static final int week6 = 6;
	public static final int week7 = 7;

	public static final String day1 = "monday";
	public static final String day2 = "tuesday";
	public static final String day3 = "wednesday";
	public static final String day4 = "thursday";
	public static final String day5 = "friday";
	public static final String[] entireWeek = new String[] { day1, day2, day3,
			day4, day5 };

	public static Label keyLabel = new Label("IDS", "M1", "1");

	public static void main(String[] args) throws IOException, ParseException,
			AggregationException, MetricNotApplicableException,
			InterruptedException, LabelerNotApplicableException {
		// config
		Config.zipBatches();
		setGraphVisSettings();
		setGnuplotSettings();
		TCPEventReader.timestampOffset = (int) (2 * hour);

		// dirs
		String baseDir = "data/darpa1998/";
		String baseDirSmall = "data/darpa1998_small/";
		String attackList = "attacks.list";

		// flags
		boolean generate = !true;
		boolean plot = !true;
		boolean analyze = true;

		// generate(baseDir, attackList, 1, generate, metricsDefault, plot,
		// day3);

		if (generate || plot || analyze)
			generate(baseDirSmall, attackList, week1, generate, metricsDefault,
					plot, analyze, entireWeek);
	}

	/*
	 * CONTROL
	 */
	public static void generate(String baseDir, String attackList, int weekId,
			boolean generate, Metric[] metrics, boolean plot, boolean analyze,
			String... days) throws IOException, ParseException,
			AggregationException, MetricNotApplicableException,
			InterruptedException, LabelerNotApplicableException {
		int secondsPerBatch = 1;
		int maxBatches = 100000;
		long lifeTimePerEdgeSeconds = hour;
		long lifeTimePerEdge = lifeTimePerEdgeSeconds * 1000;
		String week = "w" + weekId;

		if (generate) {
			for (int i = 0; i < days.length; i++) {
				String day = days[i];
				String list = getList(week, day);
				String name = getName(secondsPerBatch, lifeTimePerEdgeSeconds);

				modell_1_test(baseDir + week + "/", baseDir + week + "/" + day
						+ "/", list, name, baseDir, attackList,
						secondsPerBatch, maxBatches, true, true,
						lifeTimePerEdge, metrics);
			}
		}

		if (plot) {
			for (int i = 0; i < days.length; i++) {
				String day = days[i];
				String name = getName(secondsPerBatch, lifeTimePerEdgeSeconds);
				String dir = getDir(baseDir, week, day);

				// read
				logRead(week, day, name);
				SeriesData sd = SeriesData.read(dir + name + "/", week + day,
						false, false);

				// plot
				logPlot(week, day, name);
				plot(sd, dir + name + "/plots/");
			}
		}

		if (analyze) {
			// array of daily stats
			HashMap<String, LabelStat>[] dailyStats = new HashMap[days.length];

			for (int i = 0; i < days.length; i++) {
				String day = days[i];
				String name = getName(secondsPerBatch, lifeTimePerEdgeSeconds);
				String labelListDir = getLabelListDir(baseDir, week, day, name);

				logAnalyze(week, day, name);
				HashMap<String, LabelStat> stats = analyze(labelListDir,
						"___labels.run.0.labels", keyLabel);

				// write stats
				LabelUtils.writeLabelStats(stats, labelListDir,
						"___labels.run.0.results", true);
				dailyStats[i] = stats;
			}

			// gather weekly stats
			HashMap<String, LabelStat> weeklyStats = new HashMap<String, LabelStat>();
			for (int i = 0; i < dailyStats.length; i++) {
				HashMap<String, LabelStat> stats = dailyStats[i];

				for (String id : stats.keySet()) {
					LabelStat stat = stats.get(id);

					// if not present in weekly stats -> init
					if (!weeklyStats.containsKey(id)) {
						weeklyStats.put(id, new LabelStat(stats.get(id)
								.getIdentifier(), stats.get(id)
								.getIdentifier2()));
					}

					// add to weekly-stats
					weeklyStats.get(id).addStats(stat);
				}
			}

			// write weekly stats
			LabelUtils.writeLabelStats(weeklyStats, baseDir + week + "/", week
					+ ".results", true);
		}
	}

	public static HashMap<String, LabelStat> analyze(String dir,
			String filename, Label keyLabel) throws IOException {
		long conditionTime = hour;
		boolean countTrueNegatives = false;
		boolean considerConditionedNegatives = false;
		boolean considerConditionedPositives = true;

		return LabelUtils.analyzeLabelList(dir, filename, conditionTime, false,
				considerConditionedNegatives, considerConditionedPositives,
				keyLabel);
	}

	/*
	 * GENERATION
	 */
	public static SeriesData modell_1_test(String srcDir, String dstDir,
			String datasetFilename, String name, String attackListDir,
			String attackListFilename, int batchLength, int maxBatches,
			long edgeLifeTime, Metric[] metrics) throws IOException,
			ParseException, AggregationException, MetricNotApplicableException,
			InterruptedException, LabelerNotApplicableException {
		return modell_1_test(srcDir, dstDir, datasetFilename, name,
				attackListDir, attackListFilename, batchLength, maxBatches,
				true, true, edgeLifeTime, metrics);
	}

	public static SeriesData modell_1_test(String srcDir, String dstDir,
			String datasetFilename, String name, String attackListDir,
			String attackListFilename, int batchLength, int maxBatches,
			boolean removeInactiveEdges, boolean removeZeroDegreeNodes,
			long edgeLifeTime, Metric[] metrics) throws IOException,
			ParseException, AggregationException, MetricNotApplicableException,
			InterruptedException, LabelerNotApplicableException {
		Log.info("Modell 1 test!");
		Config.overwrite("GRAPH_VIS_SHOW_NODE_INDEX", "true");

		DefaultTCPEventReader reader = new DefaultTCPEventReader(srcDir,
				datasetFilename);
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
		Series s = new Series(gg, bg, metrics, labeler, dstDir + name + "/",
				"s1");

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

	/*
	 * UTILITY
	 */
	public static void logRead(String week, String day, String name) {
		Log.info("reading " + week + day + " " + name + " data!");
	}

	public static void logPlot(String week, String day, String name) {
		Log.info("plotting " + week + day + " " + name + " data!");
	}

	public static void logAnalyze(String week, String day, String name) {
		Log.info("analyzing labels of " + week + day + " " + name + " data!");
	}

	public static String getList(String week, String day) {
		return week + day + ".list";
	}

	public static String getName(int secondsPerBatch,
			long lifeTimePerEdgeSeconds) {
		return secondsPerBatch + "_" + lifeTimePerEdgeSeconds;
	}

	public static String getDir(String baseDir, String week, String day) {
		return baseDir + week + "/" + day + "/";
	}

	public static String getLabelListDir(String baseDir, String week,
			String day, String name) {
		return getDir(baseDir, week, day) + name + "/";
	}

	/*
	 * CONFIGURATION
	 */
	public static final String gnuplot_xtics = "GNUPLOT_XTICS";
	public static final String gnuplot_datetime = "GNUPLOT_DATETIME";
	public static final String gnuplot_plotdatetime = "GNUPLOT_PLOTDATETIME";
	public static final String gnuplot_xoffset = "GNUPLOT_XOFFSET";

	public static void setGnuplotSettings() {
		Config.overwrite("GNUPLOT_PATH", "C://files//gnuplot//bin//gnuplot.exe");
		Config.overwrite("GNUPLOT_DEFAULT_PLOT_LABELS", "true");
		Config.overwrite("GNUPLOT_LABEL_BIG_TIMESTAMPS", "true");
		Config.overwrite("GNUPLOT_LABEL_FILTER_LIST", "DoS1:max, DoS2:product");
		Config.overwrite("GNUPLOT_LABEL_COLOR_OFFSET", "12");
	}

	public static void setGraphVisSettings() {
		Config.overwrite("GRAPH_VIS_NETWORK_NODE_SHAPE", "true");
		Config.overwrite("GRAPH_VIS_TIMESTAMP_IN_SECONDS", "true");
		Config.overwrite("GRAPH_VIS_DATETIME_FORMAT", "HH:mm:ss");
		Config.overwrite("GRAPH_VIS_TIMESTAMP_OFFSET", "-" + (int) (2 * hour));
	}

}
