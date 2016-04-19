package dna.graph.generators.network.tests;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import argList.ArgList;
import argList.types.array.StringArrayArg;
import argList.types.atomic.EnumArg;
import argList.types.atomic.IntArg;
import argList.types.atomic.LongArg;
import argList.types.atomic.StringArg;
import dna.metrics.Metric;
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
import dna.util.Log;

public class DatasetAnalysis {

	/*
	 * ENUMERATIONS
	 */
	public enum DatasetType {
		packet, netflow, sessions
	}

	public enum ModelType {
		modelA, modelA_noWeights
	}

	public enum TimestampFormat {
		timestamp, week_day
	}

	/*
	 * MAIN
	 */
	public static void main(String[] args) {
		ArgList<DatasetAnalysis> argList = new ArgList<DatasetAnalysis>(
				DatasetAnalysis.class,
				new StringArg("srcDir", "dir of the source data"),
				new StringArg("srcFilename",
						"filename of the dataset sourcefile"),
				new EnumArg("DatasetType", "type of input dataset", DatasetType
						.values()),
				new EnumArg("ModelType", "model to use", ModelType.values()),
				new IntArg("batchWindow",
						"length of a batch timewindow in seconds"),
				new LongArg("edgeLifeTime", "lifetime of an edge in seconds"),
				new StringArg("descr",
						"description/version of the generated run"),
				new EnumArg(
						"timestamp-format",
						"toggles if timestamp (dd-MM-yyyy HH:mm:ss) or DARPA98 week-day (w-d HH:mm:ss) will be used by the timestamp fields",
						TimestampFormat.values()),
				new StringArg("from", "starting timestamp"),
				new StringArg("to", "maximum timestamp"),
				new LongArg("dataOffset",
						"offset to be added to the data in seconds"),
				new StringArrayArg(
						"metrics",
						"list of metrics to be computed with format: [class-path]+[_host/_port/_all (optional)]. Instead of metrics one may also add the following flags : metricsAll, metricsDefaultAll, metricsDefaultHosts, metricsDefaultPorts to add predefined metrics.",
						";"));
		DatasetAnalysis d = argList.getInstance(args);
		d.generate();
	}

	/*
	 * GENERATION CLASS
	 */
	protected String srcDir;
	protected String srcFilename;
	protected DatasetType datasetType;
	protected ModelType modelType;
	protected int batchWindow;
	protected long edgeLifeTime;

	protected String descr;

	protected DateTime from;
	protected DateTime to;

	protected long dataOffset;

	protected DateTimeFormatter fmt = DateTimeFormat
			.forPattern("dd-MM-yyyy-HH:mm:ss");

	protected Metric[] metrics;

	/** Constructor **/
	public DatasetAnalysis(String srcDir, String srcFilename,
			String datasetType, String modelType, Integer batchWindow,
			Long edgeLifeTime, String descr, String timestampFormat,
			String from, String to, Long dataOffset, String[] metrics) {
		this.srcDir = srcDir;
		this.srcFilename = srcFilename;
		this.datasetType = DatasetType.valueOf(datasetType);
		this.modelType = ModelType.valueOf(modelType);
		this.batchWindow = batchWindow;
		this.edgeLifeTime = edgeLifeTime;
		this.descr = descr;
		this.dataOffset = dataOffset;

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

		// metrics
		ArrayList<Metric> metricsList = new ArrayList<Metric>();

		for (int i = 0; i < metrics.length; i++) {
			String classPath = metrics[i];

			// metric-flag cases
			if (classPath.equals("metricsAll")) {
				addMetricsToList(metricsList, DatasetAnalysis.metricsAll);
			} else if (classPath.equals("metricsDefaultAll")) {
				addMetricsToList(metricsList, DatasetAnalysis.metricsDefaultAll);
			} else if (classPath.equals("metricsDefaultHosts")) {
				addMetricsToList(metricsList,
						DatasetAnalysis.metricsDefaultHostOnly);
			} else if (classPath.equals("metricsDefaultPorts")) {
				addMetricsToList(metricsList,
						DatasetAnalysis.metricsDefaultPortOnly);

				// normal metric-cases
			} else if (classPath.endsWith("_host")) {
				metricsList.add(instantiateMetric(
						classPath.replaceAll("_host", ""), "HOST"));
			} else if (classPath.endsWith("_port")) {
				metricsList.add(instantiateMetric(
						classPath.replaceAll("_port", ""), "PORT"));
			} else if (classPath.endsWith("_all")) {
				metricsList.add(instantiateMetric(
						classPath.replaceAll("_all", ""), null));
				metricsList.add(instantiateMetric(
						classPath.replaceAll("_all", ""), "HOST"));
				metricsList.add(instantiateMetric(
						classPath.replaceAll("_all", ""), "PORT"));
			} else {
				metricsList.add(instantiateMetric(classPath, null));
			}
		}

		this.metrics = metricsList.toArray(new Metric[metricsList.size()]);
	}

	/** Generation method. **/
	public void generate() {
		Log.info("generating " + datasetType.toString() + " data from '"
				+ srcDir + srcFilename + "'");
		Log.info("model:\t" + modelType.toString());
		Log.info("batch window:\t" + batchWindow + " s");
		Log.info("edgeLifeTime:\t" + edgeLifeTime + " s");
		Log.info("descr:\t" + descr);
		if (from != null)
			Log.info("from:\t\t" + from.toString());
		if (to != null)
			Log.info("to:\t\t" + to.toString());
		Log.info("offset:\t" + dataOffset);

		Log.infoSep();
		Log.info("metrics:");
		for (Metric m : this.metrics)
			Log.info("\t" + m.getName());
		Log.infoSep();
	}

	/*
	 * STATICS
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

}
