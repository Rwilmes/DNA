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
import dna.util.network.tcp.TCPEventReader;
import dna.util.network.tcp.TCPPacketReader;
import dna.visualization.graph.GraphVisualization;

public class PacketTest {

	public static void main(String[] args) throws IOException, ParseException,
			AggregationException, MetricNotApplicableException,
			LabelerNotApplicableException, InterruptedException {
		// GraphVisualization.enable();
		Config.zipBatches();
		BotnetTest.setGraphVisSettings();
		BotnetTest.setGnuplotSettings();

		String dir = "data/darpa1998_packets/w2/tuesday/";
		String filename = "new2.txt";

		int batchLength = 1;
		long edgeLifeTime = 60 * 60;

		SeriesData sd = modelB(dir, filename, batchLength, edgeLifeTime);
		Evaluation.plot(sd, dir + Evaluation.getName(batchLength, edgeLifeTime)
				+ "/" + "plots/");

	}

	public static SeriesData modelB(String srcDir, String datasetFilename,
			int batchLength, long edgeLifeTimeSeconds) throws IOException,
			ParseException, AggregationException, MetricNotApplicableException,
			LabelerNotApplicableException {
		int maxBatches = 100000;
		Log.info("Modell 1 test!");
		Config.overwrite("GRAPH_VIS_SHOW_NODE_INDEX", "true");
		TCPEventReader.timestampOffset = (int) (2 * Evaluation.hour);

		// DefaultTCPEventReader reader = new DefaultTCPEventReader(srcDir,
		// datasetFilename);

		TCPEventReader reader = new TCPPacketReader(srcDir, datasetFilename,
				null);

		reader.setBatchInterval(batchLength);
		reader.setEdgeLifeTime(edgeLifeTimeSeconds * 1000);
		reader.setRemoveInactiveEdges(true);
		reader.setRemoveZeroDegreeNodes(true);
		EntryBasedAttackLabeler ebal = new EntryBasedAttackLabeler();
		reader.setDarpaLabeler(ebal);

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
				+ Evaluation.getName(batchLength, edgeLifeTimeSeconds) + "/",
				"s1");

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
