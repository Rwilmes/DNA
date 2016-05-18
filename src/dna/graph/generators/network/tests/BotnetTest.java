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
import dna.io.Reader;
import dna.io.Writer;
import dna.labels.labeler.Labeler;
import dna.labels.labeler.LabelerNotApplicableException;
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
import dna.util.network.NetFlowReader;
import dna.util.network.tcp.TCPEventReader;
import dna.visualization.graph.GraphVisualization;

public class BotnetTest {

	public static final long second = 1;
	public static final long minute = 60 * second;
	public static final long hour = 60 * minute;

	public static void main(String[] args) throws IOException, ParseException,
			AggregationException, MetricNotApplicableException,
			LabelerNotApplicableException, InterruptedException {
		// GraphVisualization.enable();
		Config.zipBatches();
		setGraphVisSettings();
		setGnuplotSettings();

		String dir = "data/botnet/";
		String filename = "botnet10.netflow";
		filename = "botnet10_transformed.netflow";
		int batchLength = 1;
		long edgeLifeTime = second * 15 * 1000;

		// SeriesData sd = modelB(dir, filename, batchLength, edgeLifeTime);

		// SeriesData sd = SeriesData.read(dir + "s1/", "s1", false, false);
		// Evaluation.plot(sd, dir + "s1/" + "plots/");

		// transform(dir, filename, "out.txt");

		// removeStuff("data/darpa1998_packets/w2/monday/", "packets.txt",
		// "new_all.txt", "" + '"');

		removeStuff2("data/darpa1998_netflow/w3/thursday/", "data.netflow",
				"data_fixed2.netflow");

		Log.info("finished!");
	}

	public static SeriesData modelB(String srcDir, String datasetFilename,
			int batchLength, long edgeLifeTime) throws IOException,
			ParseException, AggregationException, MetricNotApplicableException,
			LabelerNotApplicableException {
		int maxBatches = 100000;
		Log.info("Modell 1 test!");
		Config.overwrite("GRAPH_VIS_SHOW_NODE_INDEX", "true");

		// DefaultTCPEventReader reader = new DefaultTCPEventReader(srcDir,
		// datasetFilename);

		TCPEventReader reader = new NetFlowReader(srcDir, datasetFilename, null);

		reader.setBatchInterval(batchLength);
		reader.setEdgeLifeTime(edgeLifeTime);
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
		Labeler[] labeler = new Labeler[] {
		// new IntrusionDetectionLabeler1(),
		// new WeightBasedIDSLabeler(),
		// new DarpaAttackLabeler(attackListDir, attackListFilename)
		ebal };

		// init series
		Series s = new Series(gg, bg, metricsDefaultAll, labeler, srcDir
				+ "s1/", "s1");

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
		// Config.overwrite("GRAPH_VIS_SIZE_NODES_BY_DEGREE", "false");
	}

	public static void transform(String dir, String filename, String dstFilename)
			throws IOException {
		Reader r = new Reader(dir, filename);
		Writer w = new Writer(dir, dstFilename);

		String line = r.readString();

		int counter = 0;
		while (line != null) {
			// System.out.println(line);
			String[] splits = line.split(":");
			// for (String s : splits)
			// System.out.println("\t" + s);
			//

			String temp;

			if (splits.length != 5) {
				System.out.println(counter);
				temp = line;
			} else {
				temp = splits[0] + ":" + splits[1] + ":" + splits[2] + "\t"
						+ splits[3] + "\t" + splits[4];
			}

			counter++;
			if (counter % 10000 == 0)
				System.out.println("lines: " + counter);
			w.writeln(temp);
			line = r.readString();
		}

		w.close();
		r.close();
	}

	public static void removeStuff(String dir, String filename,
			String dstFilename, String substringToRemove) throws IOException {
		Reader r = new Reader(dir, filename);
		Writer w = new Writer(dir, dstFilename);

		String line = r.readString();
		int counter = 0;
		while (line != null) {
			String temp = line.replaceAll(substringToRemove, "");

			w.writeln(temp);

			if (counter % 10000 == 0)
				System.out.println(counter);
			counter++;
			line = r.readString();
		}

		r.close();
		w.close();
	}

	public static void removeStuff2(String dir, String filename,
			String dstFilename) throws IOException {
		Reader r = new Reader(dir, filename);
		Writer w = new Writer(dir, dstFilename);

		String line = r.readString();
		int counter = 0;
		while (line != null) {
			line = line.trim();
			// line = line.replaceAll("  ", " ");
			String[] splits = line.split(" ");

			String temp = splits[0]
//					.replaceAll("-98", "-1998")
					+ " "
					+ splits[1];
			for (int i = 2; i < splits.length; i++) {
				if (splits[i].length() > 0)
					temp += "\t" + splits[i];
			}
			w.writeln(temp);

			if (counter % 10000 == 0)
				System.out.println(counter);
			counter++;
			line = r.readString();
		}

		r.close();
		w.close();
	}
}