package dna.graph.generators.network.tests;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

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
import dna.util.Log;

public class DatasetAnalysis {

	public enum DatasetType {
		packet, netflow, sessions
	}

	public enum ModelType {
		modelA, modelA_noWeights
	}

	public enum TimestampFormat {
		timestamp, week_day
	}

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
						"list of metrics to be computed. Format: [class-path]+[_host/_port (optional)]",
						";"));
		DatasetAnalysis d = argList.getInstance(args);
		d.generate();
	}

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

		this.metrics = new Metric[metrics.length];
		for (int i = 0; i < metrics.length; i++) {
			String classPath = metrics[i];
			if (classPath.endsWith("_host")) {
				this.metrics[i] = instantiateMetric(
						classPath.replaceAll("_host", ""), "HOST");
			} else if (classPath.endsWith("_port")) {
				this.metrics[i] = instantiateMetric(
						classPath.replaceAll("_port", ""), "PORT");
			} else {
				this.metrics[i] = instantiateMetric(classPath, null);
			}
		}
	}

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

	protected Metric instantiateMetric(String classPath, String nodeType) {
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

}
