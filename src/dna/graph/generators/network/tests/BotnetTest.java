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

		String dir = "data/botnet4/";
		String filename = "botnet4_raw.netflow";
		// filename = "botnet10_transformed.netflow";
		int batchLength = 1;
		long edgeLifeTime = second * 15 * 1000;

		int skipFirst = 2;

		// SeriesData sd = modelB(dir, filename, batchLength, edgeLifeTime);

		// SeriesData sd = SeriesData.read(dir + "s1/", "s1", false, false);
		// Evaluation.plot(sd, dir + "s1/" + "plots/");

		// transform(dir, filename, "botnet4_formatted.netflow");
		transform2("data/transform/", "outside.netflow",
				"outside_formatted.netflow", skipFirst);

		// parsInfo(dir, "out4.netflow", "out4.stats");

		// removeStuff("data/darpa1998_packets/w2/monday/", "packets.txt",
		// "new_all.txt", "" + '"');

		// removeStuff2("data/darpa1998_netflow/w2/2/", "outside_small.netflow",
		// "outside_small_fixed.netflow");

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

	public static void parsInfo(String dir, String filename, String dstFilename)
			throws IOException {
		System.out.println("BLUB");
		Reader r = new Reader(dir, filename);
		Writer w = new Writer(dir, dstFilename);

		String line = r.readString();

		int counter = 0;

		HashMap<String, Integer> srcIps = new HashMap<String, Integer>();
		HashMap<String, Integer> srcPorts = new HashMap<String, Integer>();
		HashMap<String, Integer> dstIps = new HashMap<String, Integer>();
		HashMap<String, Integer> dstPorts = new HashMap<String, Integer>();

		HashMap<String, Integer> prots = new HashMap<String, Integer>();

		while (line != null) {

			String[] splits = line.split("\t");
			// System.out.println(line);

			// for (int i = 0; i < splits.length; i++) {
			// System.out.println("\t" + i + "\t" + splits[i]);
			// }

			String prot = splits[2];
			String srcIp = splits[3];
			String srcPort = splits[4];
			String dstIp = splits[6];
			String dstPort = splits[7];

			if (prot.equals("0.000"))
				System.out.println(counter);

			if (prots.containsKey(prot))
				prots.put(prot, prots.get(prot) + 1);
			else
				prots.put(prot, 1);
			if (srcIps.containsKey(srcIp))
				srcIps.put(srcIp, srcIps.get(srcIp) + 1);
			else
				srcIps.put(srcIp, 1);
			if (srcPorts.containsKey(srcPort)) {
				// System.out.println(srcPort);
				srcPorts.put(srcPort, srcPorts.get(srcPort) + 1);
			} else
				srcPorts.put(srcPort, 1);
			if (dstIps.containsKey(dstIp))
				dstIps.put(dstIp, dstIps.get(dstIp) + 1);
			else
				dstIps.put(dstIp, 1);
			if (dstPorts.containsKey(dstPort))
				dstPorts.put(dstPort, dstPorts.get(dstPort) + 1);
			else
				dstPorts.put(dstPort, 1);

			// if (counter >= 10)
			// break;
			counter++;
			if (counter % 10000 == 0)
				System.out.println("lines: " + counter);

			// w.writeln(temp);
			line = r.readString();
		}

		w.writeln("prots");
		for (String s : prots.keySet()) {
			w.writeln("\t" + s + "\t\t" + prots.get(s));
		}
		w.writeln("srcIps");
		for (String s : srcIps.keySet()) {
			w.writeln("\t" + s + "\t\t" + srcIps.get(s));
		}
		w.writeln("srcPorts");
		for (String s : srcPorts.keySet()) {
			w.writeln("\t" + s + "\t\t" + srcPorts.get(s));
		}
		w.writeln("dstIps");
		for (String s : dstIps.keySet()) {
			w.writeln("\t" + s + "\t\t" + dstIps.get(s));
		}
		w.writeln("dstPorts");
		for (String s : dstPorts.keySet()) {
			w.writeln("\t" + s + "\t\t" + dstPorts.get(s));
		}

		w.close();
		r.close();
	}

	public static void transform2(String dir, String filename,
			String dstFilename, int skipFirst) throws IOException {
		System.out.println("BLUB");
		Reader r = new Reader(dir, filename);
		Writer w = new Writer(dir, dstFilename);

		String line = r.readString();
		int counter = 0;
		while (line != null) {
			String[] splits = line.split("\t");

			if (counter < skipFirst || splits[0].endsWith("-16")) {
				line = r.readString();
				counter++;
				continue;
			}

			// System.out.println(line);
			// System.out.println("splits: " + splits.length);
			//

			String temp = splits[0].replace("-98", "-1998") + " ";
			for (int i = 1; i < splits.length - 1; i++) {
				// System.out.println("\t" + i + "\t" + splits[i]);
				temp += splits[i] + "\t";
			}

			temp += splits[splits.length - 1];
			w.writeln(temp);
			line = r.readString();
			counter++;

			if (counter % 1000 == 0)
				System.out.println(counter);
		}

		w.close();
		r.close();
	}

	public static void transform(String dir, String filename, String dstFilename)
			throws IOException {
		System.out.println("BLUB");
		Reader r = new Reader(dir, filename);
		Writer w = new Writer(dir, dstFilename);

		String line = r.readString();

		int counter = 0;
		while (line != null) {

			String[] splits = line.split(":");

			String temp;
			if (splits.length != 5) {
				if (splits.length != 3) {
					counter++;
					line = r.readString();
					continue;
				}

				splits = line.split("\t");
				temp = "";

				if (splits.length == 1) {
					splits = line.split(" ");

					temp += splits[0] + " ";
					for (int i = 1; i < 5; i++)
						temp += splits[i] + "\t";

					temp += "0" + "\t";

					for (int i = 5; i < 7; i++)
						temp += splits[i] + "\t";

					temp += "0" + "\t";

					for (int i = 7; i < 12; i++) {
						temp += splits[i] + "\t";
					}

					temp += splits[12];
				} else if (splits.length == 14) {
					for (int i = 0; i < 4; i++)
						temp += splits[i] + "\t";

					temp += "0" + "\t";

					for (int i = 5; i < 7; i++)
						temp += splits[i] + "\t";

					temp += "0" + "\t";

					for (int i = 8; i < 13; i++) {
						temp += splits[i] + "\t";
					}

					temp += splits[13];
				} else if (splits.length == 15) {
					for (int i = 0; i < 5; i++)
						temp += splits[i] + "\t";

					for (int i = 6; i < 14; i++)
						temp += splits[i] + "\t";

					temp += splits[14];
				} else if (splits.length == 16) {
					for (int i = 0; i < 5; i++)
						temp += splits[i] + "\t";

					for (int i = 6; i < 9; i++)
						temp += splits[i] + "\t";

					for (int i = 10; i < 15; i++)
						temp += splits[i] + "\t";

					temp += splits[15];

				} else {
					System.out.println(counter + "\t" + splits.length + "\t"
							+ line);
					counter++;
					line = r.readString();
					continue;
				}
			} else {
				temp = splits[0] + ":" + splits[1] + ":" + splits[2] + "\t"
						+ splits[3] + "\t" + splits[4];
			}

			if (temp.endsWith("Botnet"))
				temp += "\t" + 1;
			else
				temp += "\t" + 0;

			temp = temp.replaceAll("\t\t", "\t");

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

			String temp = splits[0].replaceAll("-98", "-1998") + " "
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
