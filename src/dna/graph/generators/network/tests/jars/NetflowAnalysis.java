package dna.graph.generators.network.tests.jars;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import argList.ArgList;
import argList.types.array.StringArrayArg;
import argList.types.atomic.BooleanArg;
import argList.types.atomic.EnumArg;
import argList.types.atomic.IntArg;
import argList.types.atomic.StringArg;
import dna.graph.datastructures.GDS;
import dna.graph.generators.GraphGenerator;
import dna.graph.generators.network.EmptyNetwork;
import dna.graph.generators.network.NetflowBatch;
import dna.graph.generators.network.tests.DatasetUtils;
import dna.graph.generators.network.tests.DatasetUtils.TimestampFormat;
import dna.graph.generators.network.tests.DatasetUtils.ZipMode;
import dna.graph.generators.network.tests.Evaluation;
import dna.graph.weights.TypedWeight;
import dna.graph.weights.Weight.WeightSelection;
import dna.graph.weights.intW.IntWeight;
import dna.labels.labeler.Labeler;
import dna.labels.labeler.LabelerNotApplicableException;
import dna.labels.labeler.darpa.DarpaAttackLabeler;
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
import dna.series.AggregationException;
import dna.series.Series;
import dna.series.data.SeriesData;
import dna.updates.generators.BatchGenerator;
import dna.util.Config;
import dna.util.Log;
import dna.util.network.netflow.DarpaNetflowReader;
import dna.util.network.netflow.NetflowEvent.NetflowDirection;
import dna.util.network.netflow.NetflowEvent.NetflowEventField;
import dna.util.network.netflow.NetflowEventReader;
import dna.visualization.graph.GraphVisualization;
import dna.visualization.graph.toolTips.infoLabel.NetworkNodeKeyLabel;

public class NetflowAnalysis {

	/*
	 * MAIN
	 */
	public static void main(String[] args) throws IOException, ParseException,
			AggregationException, MetricNotApplicableException,
			LabelerNotApplicableException {
		ArgList<NetflowAnalysis> argList = new ArgList<NetflowAnalysis>(
				NetflowAnalysis.class,
				new StringArg("srcDir", "dir of the source data"),
				new StringArg("srcFilename",
						"filename of the dataset sourcefile"),
				new StringArg("dstDir", "dir of the destination data"),
				new StringArg("name", "name of the destination data, set "
						+ '"' + "null" + '"'
						+ " for automatic name-setting based on model"),
				new StringArg(
						"descr",
						"description/version of the generated run, will be added as suffix to the destination name"),
				new IntArg("dataOffset",
						"offset to be added to the data timestamps in seconds"),
				new IntArg("batchWindow",
						"length of a batch timewindow in seconds"),
				new IntArg("edgeLifeTime", "lifetime of an edge in seconds"),
				new StringArrayArg(
						"edges",
						"edges to be added to the graph of format: NetflowEventField-NetflowEventField-...-NetflowEventField. Possible NetflowEventFields are: Bytes, BytesToDestination, BytesToSrc, ConnectionState, Date, Direction, DstAddress, DstPort, Duration, Flags, Label, None, numberOfNetflows, Packets, PacketsToDestination, PacketToSrc, Protocol, SrcAddress, SrcPort, Time",
						";"),
				new StringArrayArg(
						"edgeDirections",
						"directions of the edges to be added, possible values: bw, fw",
						";"),
				new StringArrayArg(
						"edgeWeights",
						"edgeWeights to be added to the graph, same format as 'edges'",
						";"),
				new StringArrayArg(
						"nodeWeights",
						"edgeWeights to be added to the graph, same format as 'edges'",
						";"),
				new EnumArg(
						"timestamp-format",
						"toggles if timestamp (dd-MM-yyyy HH:mm:ss) or DARPA98 week-day (w-d HH:mm:ss) will be used by the timestamp fields",
						TimestampFormat.values()),
				new StringArg("from", "starting timestamp"),
				new StringArg("to", "maximum timestamp"),
				new StringArg("attackList",
						"path to the attack-list file to be used"),
				new BooleanArg("enableVis",
						"true to enable graph-visualization"),
				new EnumArg("zip-mode", "zip mode of the data to be plotted",
						ZipMode.values()),
				new StringArrayArg(
						"metrics",
						"list of metrics to be computed with format: [class-path]+[_host/_port/_all (optional)]. Instead of metrics one may also add the following flags : metricsAll, metricsDefaultAll, metricsDefaultHosts, metricsDefaultPorts to add predefined metrics.",
						";"));
		NetflowAnalysis d = argList.getInstance(args);
		d.generate();
	}

	/*
	 * GENERATION CLASS
	 */
	// general
	protected String srcDir;
	protected String srcFilename;
	protected String dstDir;
	protected String name;
	protected String descr;

	// modeling
	protected int batchLengthSeconds;
	protected int edgeLifeTimeSeconds;

	protected NetflowEventField[][] edges;
	protected NetflowDirection[] edgeDirections;
	protected NetflowEventField[][] edgeWeights;
	protected NetflowEventField[][] nodeWeights;

	// timestamp
	protected DateTimeFormatter fmt = DateTimeFormat
			.forPattern("dd-MM-yyyy-HH:mm:ss");
	protected DateTime from;
	protected DateTime to;

	protected int dataOffsetSeconds;

	protected String attackListPath;

	protected boolean enableVis;

	protected Metric[] metrics;

	/** Constructor **/
	public NetflowAnalysis(String srcDir, String srcFilename, String dstDir,
			String name, String descr, Integer dataOffsetSeconds,
			Integer batchLengthSeconds, Integer edgeLifeTimeSeconds,
			String[] edges, String[] edgeDirections, String[] edgeWeights,
			String[] nodeWeights, String timestampFormat, String from,
			String to, String attackListPath, Boolean enableVis,
			String zipMode, String[] metrics) {
		this.srcDir = srcDir;
		this.srcFilename = srcFilename;
		this.dstDir = dstDir;
		if (name == null || name.equals("null")) {
			this.name = generateName(edges, edgeDirections, edgeWeights,
					nodeWeights, batchLengthSeconds, edgeLifeTimeSeconds);
		} else {
			this.name = name;
		}

		this.descr = descr;
		this.dataOffsetSeconds = dataOffsetSeconds;
		this.batchLengthSeconds = batchLengthSeconds;
		this.edgeLifeTimeSeconds = edgeLifeTimeSeconds;

		this.edges = parseNetflowEventFields(edges);
		this.edgeDirections = parseEdgeDirections(edgeDirections);
		this.edgeWeights = parseNetflowEventFields(edgeWeights);
		this.nodeWeights = parseNetflowEventFields(nodeWeights);

		// timestamps
		if (timestampFormat.equals("timestamp")) {
			this.from = this.fmt.parseDateTime(from);
			this.to = this.fmt.parseDateTime(to);
		}
		if (timestampFormat.equals("week_day")) {
			String dateFrom = DatasetUtils.getDarpaDate(
					Integer.parseInt("" + from.charAt(0)),
					Integer.parseInt("" + from.charAt(2)));
			this.from = this.fmt.parseDateTime(dateFrom + "-"
					+ from.substring(4));
			String dateTo = DatasetUtils.getDarpaDate(
					Integer.parseInt("" + to.charAt(0)),
					Integer.parseInt("" + to.charAt(2)));
			this.to = this.fmt.parseDateTime(dateTo + "-" + to.substring(4));
		}

		this.attackListPath = attackListPath;
		this.enableVis = enableVis;

		ZipMode zipM = ZipMode.valueOf(zipMode);
		switch (zipM) {
		case batches:
			Config.zipBatches();
			break;
		case runs:
			Config.zipRuns();
			break;
		case none:
			Config.zipNone();
			break;
		}

		// metrics
		ArrayList<Metric> metricList = new ArrayList<Metric>();

		for (int i = 0; i < metrics.length; i++) {
			String classPath = metrics[i];

			// metric-flag cases
			if (classPath.equals("metricsAll")) {
				addMetricsToList(metricList, NetflowAnalysis.metricsAll);
			} else if (classPath.equals("metricsDefaultAll")) {
				addMetricsToList(metricList, NetflowAnalysis.metricsDefaultAll);
			} else if (classPath.equals("metricsDefaultHosts")) {
				addMetricsToList(metricList,
						NetflowAnalysis.metricsDefaultHostOnly);
			} else if (classPath.equals("metricsDefaultPorts")) {
				addMetricsToList(metricList,
						NetflowAnalysis.metricsDefaultPortOnly);

				// normal metric-cases
			} else if (classPath.endsWith("_host")) {
				metricList.add(instantiateMetric(
						classPath.replaceAll("_host", ""), "HOST"));
			} else if (classPath.endsWith("_port")) {
				metricList.add(instantiateMetric(
						classPath.replaceAll("_port", ""), "PORT"));
			} else if (classPath.endsWith("_all")) {
				metricList.add(instantiateMetric(
						classPath.replaceAll("_all", ""), null));
				metricList.add(instantiateMetric(
						classPath.replaceAll("_all", ""), "HOST"));
				metricList.add(instantiateMetric(
						classPath.replaceAll("_all", ""), "PORT"));
			} else {
				metricList.add(instantiateMetric(classPath, null));
			}
		}

		this.metrics = metricList.toArray(new Metric[metricList.size()]);
	}

	/*
	 * GENERATION
	 */
	/** Generation method. **/
	public void generate() throws IOException, ParseException,
			AggregationException, MetricNotApplicableException,
			LabelerNotApplicableException {
		Log.info("generating data from '" + srcDir + srcFilename + "'");
		Log.info("to:\t" + dstDir + name);
		Log.info("batch window:\t" + batchLengthSeconds + "s");
		Log.info("edgeLifeTime:\t" + edgeLifeTimeSeconds + "s");
		Log.info("descr:\t" + descr);
		if (from != null)
			Log.info("from:\t\t" + from.toString());
		if (to != null)
			Log.info("to:\t\t" + to.toString());
		Log.info("offset:\t" + dataOffsetSeconds);
		Log.info("attack-list:\t" + attackListPath);
		Log.info("enable-vis:\t" + enableVis);

		Log.infoSep();
		Log.info("edges:");
		for (int i = 0; i < edges.length; i++) {
			String buff = "\t";
			switch (edgeDirections[i]) {
			case backward:
				buff += "<--\t";
				break;
			case forward:
				buff += "-->\t";
				break;
			}

			for (int j = 0; j < edges[i].length; j++) {
				if (j > 0)
					buff += "-";
				buff += edges[i][j].toString();
			}
			Log.info(buff);
			buff = "\t\t";

			buff += "\t\tEW: ";
			for (int j = 0; j < edgeWeights[i].length; j++) {
				if (j > 0)
					buff += ", ";
				buff += edgeWeights[i][j].toString();
			}
			Log.info(buff);
			buff = "\t\tNW: ";
			for (int j = 0; j < nodeWeights[i].length; j++) {
				if (j > 0)
					buff += ", ";
				buff += nodeWeights[i][j].toString();
			}
			Log.info(buff);
		}
		Log.infoSep();
		Log.info("metrics:");
		for (Metric m : this.metrics)
			Log.info("\t" + m.getName());
		Log.infoSep();

		generate(srcDir, srcFilename, dstDir, name + "_" + descr,
				dataOffsetSeconds, batchLengthSeconds, edgeLifeTimeSeconds,
				from, to, attackListPath, enableVis, metrics, edges,
				edgeDirections, edgeWeights, nodeWeights);
	}

	public static SeriesData generate(String srcDir, String srcFilename,
			String dstDir, String name, int dataOffsetSeconds,
			int batchLengthSeconds, int edgeLifeTimeSeconds, DateTime from,
			DateTime to, String attackListPath, boolean enableVis,
			Metric[] metrics, NetflowEventField[][] edges,
			NetflowDirection[] edgeDirections,
			NetflowEventField[][] edgeWeights, NetflowEventField[][] nodeWeights)
			throws IOException, ParseException, AggregationException,
			MetricNotApplicableException, LabelerNotApplicableException {
		Config.overwrite("GRAPH_VIS_SHOW_NODE_INDEX", "true");

		// init reader
		NetflowEventReader reader = new DarpaNetflowReader(srcDir, srcFilename);
		reader.setBatchIntervalSeconds(batchLengthSeconds);
		reader.setEdgeLifeTimeSeconds(edgeLifeTimeSeconds);
		reader.setDataOffset(reader.getDataOffset() + dataOffsetSeconds);

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
		BatchGenerator bg = new NetflowBatch(name, reader, edges,
				edgeDirections, edgeWeights, nodeWeights);

		// for graph representation
		NetworkNodeKeyLabel.netflowBatchGenerator = (NetflowBatch) bg;

		// init Labeler
		Labeler[] labeler = new Labeler[0];
		if (attackListPath != null && !attackListPath.equals("null"))
			labeler = new Labeler[] { new DarpaAttackLabeler(attackListPath, "") };

		// init series
		Series s = new Series(gg, bg, metrics, labeler, dstDir, name);

		// generate
		SeriesData sd = s.generate(1, Integer.MAX_VALUE, false, false, true, 0);

		GraphVisualization.setText("Finished");
		Log.infoSep();

		return sd;
	}

	/** Model A generation method. **/
	// public static SeriesData generate2(String srcDir, String srcFilename,
	// DatasetType datasetType, ModelType modelType,
	// String attackListPath, int batchLengthSeconds,
	// long edgeLifeTimeSeconds, DateTime from, DateTime to, String descr,
	// int dataOffset, boolean enableVis, Metric[] metrics)
	// throws IOException, ParseException, AggregationException,
	// MetricNotApplicableException, LabelerNotApplicableException {
	// int maxBatches = 1000000;
	//
	// // vis
	// Config.overwrite("GRAPH_VIS_SHOW_NODE_INDEX", "true");
	// if (enableVis) {
	// DatasetUtils.setGraphVisSettings();
	// GraphVisualization.enable();
	// } else
	// GraphVisualization.disable();
	//
	// // set offset
	// TCPEventReader.timestampOffset = dataOffset;
	// TCPEventReader reader = null;
	//
	// // init labeler
	// Labeler[] labeler = new Labeler[0];
	//
	// switch (datasetType) {
	// case netflow:
	// if (attackListPath != null && !attackListPath.equals("null"))
	// labeler = new Labeler[] { new DarpaAttackLabeler(
	// attackListPath, "") };
	// reader = new NetFlowReader2(srcDir, srcFilename);
	// break;
	// case packet:
	// if (attackListPath != null && !attackListPath.equals("null"))
	// labeler = new Labeler[] { new DarpaAttackLabeler(
	// attackListPath, "") };
	// reader = new TCPPacketReader(srcDir, srcFilename, null);
	// break;
	// case session:
	// reader = new DefaultTCPEventReader(srcDir, srcFilename);
	// EntryBasedAttackLabeler ebal = new EntryBasedAttackLabeler();
	// labeler = new Labeler[] { ebal };
	// reader.setDarpaLabeler(ebal);
	// break;
	// case botnet:
	// reader = new NetFlowReader(srcDir, srcFilename, null);
	// EntryBasedAttackLabeler ebal2 = new EntryBasedAttackLabeler();
	// labeler = new Labeler[] { ebal2 };
	// reader.setDarpaLabeler(ebal2);
	// break;
	// }
	//
	// // additional reader settings
	// reader.setBatchInterval(batchLengthSeconds);
	// reader.setEdgeLifeTime(edgeLifeTimeSeconds * 1000);
	// reader.setRemoveInactiveEdges(true);
	// reader.setRemoveZeroDegreeNodes(true);
	//
	// // if (from != null)
	// // reader.setMinimumTimestamp(from);
	// // if (to != null)
	// // reader.setMaximumTimestamp(to);
	//
	// // init graph generator
	// long timestampMillis = reader.getInitTimestamp().getMillis();
	// long timestampSeconds = TimeUnit.MILLISECONDS
	// .toSeconds(timestampMillis);
	// GraphGenerator gg = new EmptyNetwork(GDS.directedVE(TypedWeight.class,
	// WeightSelection.None, IntWeight.class, WeightSelection.Zero),
	// timestampSeconds);
	//
	// // init batch generator
	// BatchGenerator bg = null;
	//
	// switch (modelType) {
	// case modelA:
	// bg = new M1Batch(reader);
	// break;
	// }
	//
	// // init series
	// Series s = new Series(gg, bg, metrics, labeler, srcDir
	// + DatasetUtils.getName(batchLengthSeconds, edgeLifeTimeSeconds,
	// descr) + "/", "s1");
	//
	// // generate
	// SeriesData sd = s.generate(1, maxBatches, false);
	// GraphVisualization.setText("Finished generation");
	// Log.infoSep();
	// return sd;
	// }

	/*
	 * UTLITY & STATICS
	 */
	/**
	 * Instantiates a metric by the given classPath and nodeType. nodeType may
	 * be null to instantiate without.
	 **/
	public static Metric instantiateMetric(String classPath, String nodeType) {
		Metric m = null;

		try {
			Class<?> cl = Class.forName(classPath);
			Constructor<?> cons;
			if (nodeType == null) {
				cons = cl.getConstructor();
				m = (Metric) cons.newInstance();
			} else {
				cons = cl.getConstructor(String[].class);
				m = (Metric) cons
						.newInstance((Object) new String[] { nodeType });
			}
		} catch (ClassNotFoundException | NoSuchMethodException
				| SecurityException | InstantiationException
				| IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			Log.error("problem when instantiating metric: " + classPath
					+ " with nodeType: " + nodeType);
			e.printStackTrace();
		}

		return m;
	}

	/** Adds metrics from array to list. Checks and prevents duplicates. **/
	public static ArrayList<Metric> addMetricsToList(
			ArrayList<Metric> metricList, Metric[] metrics) {
		for (Metric m : metrics) {
			boolean contained = false;
			for (Metric m2 : metricList) {
				if (m.getName().equals(m2.getName()))
					contained = true;
			}
			if (!contained)
				metricList.add(m);
		}
		return metricList;
	}

	/*
	 * STATIC METRICS
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
			new OverlapU(), new DegreeDistributionU(),
			new DegreeDistributionR(metricHostFilter),
			new DegreeDistributionR(metricPortFilter), new EdgeWeightsR(1.0),
			new DirectedMotifsU(), new WeightedDegreeDistributionR(),
			new WeightedDegreeDistributionR(metricHostFilter),
			new WeightedDegreeDistributionR(metricPortFilter) };

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

	/**
	 * Parses a String array where each String has the format:<br>
	 * <br>
	 * 
	 * NetflowEventField-NetflowEventField-...-NetflowEventField
	 */
	protected NetflowEventField[][] parseNetflowEventFields(String[] input) {
		NetflowEventField[][] edges = new NetflowEventField[input.length][];
		for (int i = 0; i < input.length; i++) {
			String[] splits = input[i].split("-");
			NetflowEventField[] fields = new NetflowEventField[splits.length];

			for (int j = 0; j < splits.length; j++) {
				fields[j] = NetflowEventField.valueOf(splits[j]);
			}

			edges[i] = fields;
		}

		return edges;
	}

	protected NetflowDirection[] parseEdgeDirections(String[] inputDirections) {
		NetflowDirection[] directions = new NetflowDirection[inputDirections.length];
		for (int i = 0; i < inputDirections.length; i++) {
			if (inputDirections[i].toLowerCase().equals("fw"))
				directions[i] = NetflowDirection.forward;
			if (inputDirections[i].toLowerCase().equals("bw"))
				directions[i] = NetflowDirection.backward;
		}
		return directions;
	}

	protected String map(NetflowEventField key) {
		switch (key) {
		case Bytes:
			return "b";
		case BytesToDestination:
			return "bd";
		case BytesToSrc:
			return "bs";
		case ConnectionState:
			return "cs";
		case Date:
			return "date";
		case Direction:
			return "dir";
		case DstAddress:
			return "d";
		case DstPort:
			return "dp";
		case Duration:
			return "dur";
		case Flags:
			return "f";
		case Label:
			return "l";
		case None:
			return "";
		case numberOfNetflows:
			return "n";
		case Packets:
			return "p";
		case PacketsToDestination:
			return "pd";
		case PacketToSrc:
			return "ps";
		case Protocol:
			return "pr";
		case SrcAddress:
			return "s";
		case SrcPort:
			return "sp";
		case Time:
			return "time";
		default:
			return "unknown";
		}
	}

	protected String generateName(String[] edges, String[] edgeDirections,
			String[] edgeWeights, String[] nodeWeights,
			Integer batchLengthSeconds, Integer edgeLifeTimeSeconds) {
		String name = "directed.";

		for (int i = 0; i < edges.length; i++) {
			String temp = edgeDirections[i] + "";

			String[] splits = edges[i].split("-");

			for (int j = 0; j < splits.length; j++) {
				NetflowEventField f = NetflowEventField.valueOf(splits[j]);
				temp += "" + map(f);
			}

			if (i > 0)
				name += ".";
			name += temp;
		}
		name += "/" + batchLengthSeconds + "_" + edgeLifeTimeSeconds + "/";

		if (edgeWeights.length == 0) {
			name += "none";
		}

		for (int i = 0; i < edgeWeights.length; i++) {
			if (i > 0)
				name += ".";
			name += map(NetflowEventField.valueOf(edgeWeights[i]));
		}

		name += "_";

		if (nodeWeights.length == 0) {
			name += "none";
		} else {
			for (int i = 0; i < nodeWeights.length; i++) {
				if (i > 0)
					name += ".";
				name += map(NetflowEventField.valueOf(nodeWeights[i]));
			}
		}

		return name;
	}
}