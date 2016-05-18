package dna.graph.generators.network.tests;

import java.io.IOException;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;

import dna.graph.datastructures.GDS;
import dna.graph.generators.GraphGenerator;
import dna.graph.generators.network.EmptyNetwork;
import dna.graph.generators.network.NetflowBatch;
import dna.graph.weights.TypedWeight;
import dna.graph.weights.Weight.WeightSelection;
import dna.graph.weights.intW.IntWeight;
import dna.labels.labeler.Labeler;
import dna.labels.labeler.LabelerNotApplicableException;
import dna.metrics.Metric;
import dna.metrics.MetricNotApplicableException;
import dna.metrics.degree.DegreeDistributionR;
import dna.metrics.degree.WeightedDegreeDistributionR;
import dna.metrics.motifs.DirectedMotifsU;
import dna.metrics.weights.EdgeWeightsR;
import dna.plot.Plotting;
import dna.plot.PlottingConfig.PlotFlag;
import dna.series.AggregationException;
import dna.series.Series;
import dna.series.data.SeriesData;
import dna.updates.generators.BatchGenerator;
import dna.util.Config;
import dna.util.Log;
import dna.util.network.netflow.DefaultNetflowReader;
import dna.util.network.netflow.NetflowEvent.NetflowEventField;
import dna.util.network.netflow.NetflowEventReader;
import dna.visualization.graph.GraphVisualization;
import dna.visualization.graph.toolTips.infoLabel.NetworkNodeKeyLabel;

public class NetflowTest2 {

	public static void main(String[] args) throws IOException, ParseException,
			AggregationException, MetricNotApplicableException,
			InterruptedException, LabelerNotApplicableException {
		Config.overwrite("GNUPLOT_PATH",
				"C://Program Files (x86)//gnuplot//bin//gnuplot.exe");

		Config.zipBatches();
		setGnuplotSettings();

		String dir = "data/models/";
		String srcFile = "data_small.netflow";

		srcFile = "data.netflow";

		String name = "mB";
		String dstDir = dir + name + "/";

		String plotDir = dir + name + "_plots" + "/";

		int edgeLifeTime = 60 * 10;

		// GraphVisualization.enable();
		// setGraphVisSettings();

		SeriesData sd = test(dir, srcFile, name, dstDir, edgeLifeTime);

		Plotting.plot(sd, plotDir, PlotFlag.plotSingleScalarValues);

	}

	public static SeriesData test(String dir, String filename, String name,
			String dstDir, int edgeLifeTimeSeconds) throws IOException,
			ParseException, AggregationException, MetricNotApplicableException,
			InterruptedException, LabelerNotApplicableException {
		Config.overwrite("GRAPH_VIS_SHOW_NODE_INDEX", "true");

		NetflowEventReader reader = new DefaultNetflowReader(dir, filename);
		reader.setBatchIntervalSeconds(1);
		reader.setEdgeLifeTimeSeconds(edgeLifeTimeSeconds);

		// init graph generator
		long timestampMillis = reader.getInitTimestamp().getMillis();
		long timestampSeconds = TimeUnit.MILLISECONDS
				.toSeconds(timestampMillis);
		GraphGenerator gg = new EmptyNetwork(GDS.directedVE(TypedWeight.class,
				WeightSelection.None, IntWeight.class, WeightSelection.Zero),
				timestampSeconds);

		NetflowEventField source = NetflowEventField.SrcAddress;
		NetflowEventField destination = NetflowEventField.DstAddress;
		NetflowEventField intermediateNodes[] = new NetflowEventField[] { NetflowEventField.DstPort };
		NetflowEventField edgeWeights[] = new NetflowEventField[0];
		NetflowEventField nodeWeights[] = new NetflowEventField[0];

		// init batch generator
		BatchGenerator bg = new NetflowBatch(name, reader, source,
				intermediateNodes, destination, edgeWeights, nodeWeights);

		NetworkNodeKeyLabel.netflowBatchGenerator = (NetflowBatch) bg;
		// ((NetflowBatch) bg).setDebug(true);

		// init metrics
		// Metric[] metrics = TEST.metrics_m1;
		Metric[] metrics = metricsDefaultAll;

		// init series
		Series s = new Series(gg, bg, metrics, new Labeler[0], dstDir, name);

		// generate
		SeriesData sd = s.generate(1, 1000, false);

		GraphVisualization.setText("Finished");
		Log.infoSep();

		return sd;
	}

	public static final Metric[] metricsTesting = new Metric[] {
			new DegreeDistributionR(), new EdgeWeightsR(1.0) };

	public static final Metric[] metricsDefaultAll = new Metric[] {
			new DegreeDistributionR(Evaluation.metricHostFilter),
			new DegreeDistributionR(Evaluation.metricPortFilter),
			new DegreeDistributionR(), new DirectedMotifsU(),
			new EdgeWeightsR(1.0),
			new WeightedDegreeDistributionR(Evaluation.metricHostFilter),
			new WeightedDegreeDistributionR(Evaluation.metricPortFilter),
			new WeightedDegreeDistributionR() };

	public static void setGraphVisSettings() {
		Config.overwrite("GRAPH_VIS_NETWORK_NODE_SHAPE", "true");
		Config.overwrite("GRAPH_VIS_TIMESTAMP_IN_SECONDS", "true");
		Config.overwrite("GRAPH_VIS_DATETIME_FORMAT", "HH:mm:ss");
		Config.overwrite("GRAPH_VIS_TIMESTAMP_OFFSET", "-" + (int) (6 * hour));
	}

	public static void setGnuplotSettings() {
		Config.overwrite("GNUPLOT_DEFAULT_PLOT_LABELS", "true");
		Config.overwrite("GNUPLOT_LABEL_BIG_TIMESTAMPS", "true");
		Config.overwrite("GNUPLOT_LABEL_COLOR_OFFSET", "12");
		Config.overwrite("GNUPLOT_DATETIME", "%H:%M");
		Config.overwrite("GNUPLOT_PLOTDATETIME", "true");
	}

	public static final long second = 1;
	public static final long minute = 60 * second;
	public static final long hour = 60 * minute;
}
