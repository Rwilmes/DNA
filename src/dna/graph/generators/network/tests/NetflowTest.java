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
import dna.labels.labeler.darpa.EntryBasedAttackLabeler;
import dna.metrics.Metric;
import dna.metrics.MetricNotApplicableException;
import dna.metrics.degree.DegreeDistributionR;
import dna.metrics.degree.WeightedDegreeDistributionR;
import dna.metrics.motifs.DirectedMotifsU;
import dna.metrics.weights.EdgeWeightsR;
import dna.series.AggregationException;
import dna.series.Series;
import dna.series.data.SeriesData;
import dna.updates.generators.BatchGenerator;
import dna.util.Config;
import dna.util.Log;
import dna.util.network.NetFlowReader2;
import dna.util.network.tcp.TCPEventReader;
import dna.visualization.graph.GraphVisualization;

public class NetflowTest {

	public static void test(String dataPath, int edgeLifeTime)
			throws IOException, ParseException, AggregationException,
			MetricNotApplicableException, LabelerNotApplicableException,
			InterruptedException {
		NetflowTest.test(dataPath, 1, edgeLifeTime);
	}

	public static void test(String dataPath, int batchLengthInSeconds,
			int edgeLifeTime) throws IOException, ParseException,
			AggregationException, MetricNotApplicableException,
			LabelerNotApplicableException, InterruptedException {
		NetflowTest.main(new String[] { dataPath, "" + batchLengthInSeconds,
				"" + edgeLifeTime, null });
	}

	public static void main(String[] args) throws IOException, ParseException,
			AggregationException, MetricNotApplicableException,
			LabelerNotApplicableException, InterruptedException {
		if (args.length < 3) {
			System.out.println("wrong parameters!");
			System.out
					.println("java -jar netflow.jar [data-path] [batch-length-in-seconds] [edgeLifeTime] [optional: descr/version]");
		} else {
			// GraphVisualization.enable();
			Config.zipBatches();
			BotnetTest.setGraphVisSettings();
			BotnetTest.setGnuplotSettings();

			String[] splits = args[0].split("/");
			String dir = "";
			for (int i = 0; i < splits.length - 1; i++) {
				dir += splits[i] + "/";
			}
			String filename = splits[splits.length - 1];

			int batchLength = Integer.parseInt(args[1]);
			long edgeLifeTime = Long.parseLong(args[2]);
			String descr = null;
			if (args.length >= 4)
				descr = args[3];

			String from = null;
			String to = null;

			from = getDate(3, 4) + " 13:00:00.000000";
			to = getDate(3, 4) + " 16:00:00.000000";

			System.out.println("* " + dir + "\t" + filename + "\tname: "
					+ Evaluation.getName(batchLength, edgeLifeTime, descr));

			from = null;
			to = null;

			SeriesData sd = modelB(dir, filename, batchLength, edgeLifeTime,
					from, to, descr);

			Evaluation.plot(sd,
					dir + Evaluation.getName(batchLength, edgeLifeTime, descr)
							+ "/" + "plots/");
		}

	}

	public static String getDate(int week, int day) {
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

		return ms + "-" + ds + "-" + ys;
	}

	public static SeriesData modelB(String srcDir, String datasetFilename,
			int batchLength, long edgeLifeTimeSeconds, String from, String to,
			String descr) throws IOException, ParseException,
			AggregationException, MetricNotApplicableException,
			LabelerNotApplicableException {
		int maxBatches = 100000;
		Log.info("Modell 1 test!");
		Config.overwrite("GRAPH_VIS_SHOW_NODE_INDEX", "true");
		TCPEventReader.timestampOffset = (int) (-4 * Evaluation.hour);

		// DefaultTCPEventReader reader = new DefaultTCPEventReader(srcDir,
		// datasetFilename);

		TCPEventReader reader = new NetFlowReader2(srcDir, datasetFilename);

		reader.setBatchInterval(batchLength);
		reader.setEdgeLifeTime(edgeLifeTimeSeconds * 1000);
		reader.setRemoveInactiveEdges(true);
		reader.setRemoveZeroDegreeNodes(true);
		EntryBasedAttackLabeler ebal = new EntryBasedAttackLabeler();
		reader.setDarpaLabeler(ebal);

		if (from != null)
			reader.setMinimumTimestamp(from);
		if (to != null)
			reader.setMaximumTimestamp(to);

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
		Labeler[] labeler = new Labeler[] { new DarpaAttackLabeler(
				"data/darpa1998/", "attacks.list")
		// new IntrusionDetectionLabeler1(),
		// new WeightBasedIDSLabeler(),
		// new DarpaAttackLabeler(attackListDir, attackListFilename)
		// ebal
		};

		// init series
		Series s = new Series(gg, bg, metricsDefaultAll, labeler, srcDir
				+ Evaluation.getName(batchLength, edgeLifeTimeSeconds, descr)
				+ "/", "s1");

		// generate
		SeriesData sd = s.generate(1, maxBatches, false);
		GraphVisualization.setText("Finished generation");
		Log.infoSep();
		return sd;
	}

	public static final Metric[] metricsDefaultAll = new Metric[] {
			new DegreeDistributionR(Evaluation.metricHostFilter),
			new DegreeDistributionR(Evaluation.metricPortFilter),
			new DegreeDistributionR(), new DirectedMotifsU(),
			new EdgeWeightsR(1.0),
			new WeightedDegreeDistributionR(Evaluation.metricHostFilter),
			new WeightedDegreeDistributionR(Evaluation.metricPortFilter),
			new WeightedDegreeDistributionR() };

}