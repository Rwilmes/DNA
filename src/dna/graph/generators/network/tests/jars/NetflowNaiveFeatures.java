package dna.graph.generators.network.tests.jars;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.joda.time.DateTime;

import argList.ArgList;
import argList.types.array.IntArrayArg;
import argList.types.array.StringArrayArg;
import argList.types.atomic.StringArg;
import dna.io.Writer;
import dna.util.Log;
import dna.util.network.hosts.Host;
import dna.util.network.hosts.HostList;
import dna.util.network.hosts.HostUpdate;
import dna.util.network.netflow.DarpaNetflowReader;
import dna.util.network.netflow.NetflowEvent;
import dna.util.network.netflow.NetflowEventReader;

public class NetflowNaiveFeatures {

	public enum NetflowFeature {
		flowsIn, flowsOut, flowsTotal, packetsIn, packetsOut, packetsTotal, bytesIn, bytesOut, bytesTotal, portsIn, portsOut, portsTotal, flowsInPerPort, flowsOutPerPort
	};

	public static void main(String[] args) throws IOException {
		ArgList<NetflowNaiveFeatures> argList = new ArgList<NetflowNaiveFeatures>(
				NetflowNaiveFeatures.class,
				new StringArg("srcDir", "dir of the source data"),
				new StringArg("srcFilename",
						"filename of the netflow sourcefile"),
				new StringArg("dstPath", "path to the destination data"),
				new IntArrayArg("windowSizes",
						"sizes of the time windows to be monitored", ","),
				new StringArrayArg(
						"features",
						"features to be computed for each window size. Possibel values: flowsIn, flowsOut, flowsTotal, packetsIn, packetsOut, packetsTotal, bytesIn, bytesOut, bytesTotal, portsIn, portsOut, portsTotal, flowsInPerPort, flowsOutPerPort.",
						","),
				new StringArrayArg(
						"extraPercentValues",
						"extra percent values to be computed for each feature. Example: 99;95;50 will compute top 99 and 95 percent boundary and median.",
						","));
		NetflowNaiveFeatures d = argList.getInstance(args);

		long start = System.currentTimeMillis();

		d.generate();

		long end = System.currentTimeMillis();
		System.out.println("seconds passed:\t" + (end - start * 1.0) / 1000);
	}

	// public static void main(String[] args) throws IOException {
	// long start = System.currentTimeMillis();
	// String dir = "data/naive-netflow/";
	// String filename = "2_2.netflow";
	// // filename = "2_2_small.netflow";
	//
	// String dstPath = "data/naive-netflow/test2.features";
	//
	// int windowSize = 10;
	//
	// String input = "flowsIn_5;flowsOut_5";
	//
	// // flowsIn, flowsOut, flowsTotal, packetsIn, packetsOut, packetsTotal,
	// // bytesIn, bytesOut, bytesTotal, portsIn, portsOut, portsTotal,
	// // flowsInPerPort, flowsOutPerPort
	//
	// Integer[] windowSizes = new Integer[] { 5, 10, 15, 30, 60, 120, 300,
	// 600, 900, 1800, 3600 };
	//
	// String[] extraPercentValues = new String[] { "99", "98", "97", "96",
	// "95", "90", "85", "80", "70", "50" };
	// Integer[] indizes = new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7 };
	// String[] features = new String[] { "flowsIn", "flowsIn", "flowsIn",
	// "flowsIn", "flowsIn", "flowsIn", "flowsOut", "flowsTotal" };
	//
	// indizes = new Integer[] { 6 };
	// features = new String[] { "packetsTotal" };
	//
	// NetflowNaiveFeatures n = new NetflowNaiveFeatures(dir, filename,
	// dstPath, windowSizes, indizes, features, extraPercentValues);
	// n.generate();
	// long end = System.currentTimeMillis();
	// System.out.println("seconds passed:\t" + (end - start * 1.0) / 1000);
	//
	// }

	protected String dir;
	protected String filename;
	protected Integer[] windowSizes;

	protected String dstPath;

	protected ArrayList<HostList> hostLists;
	protected ArrayList<Integer> expireList;

	protected Integer[] featureHostListIndizes;
	protected NetflowFeature[] features;

	protected String[] extraPercentValues;

	// new StringArg("dstPath", "path to the destination data"),
	// new IntArrayArg("windowSizes",
	// "sizes of the time windows to be monitored", ";"),
	// new StringArrayArg(
	// "features",
	// "features to be computed for each window size. Possibel values: flowsIn, flowsOut, flowsTotal, packetsIn, packetsOut, packetsTotal, bytesIn, bytesOut, bytesTotal, portsIn, portsOut, portsTotal, flowsInPerPort, flowsOutPerPort.",
	// ";"),
	// new StringArrayArg(

	public NetflowNaiveFeatures(String dir, String filename, String dstPath,
			Integer[] windowSizes, String[] features,
			String[] extraPercentValues) {
		this.dir = dir;
		this.filename = filename;
		this.windowSizes = windowSizes;
		this.extraPercentValues = extraPercentValues;

		this.dstPath = dstPath;

		// this array holds which features to be computed
		this.features = new NetflowFeature[windowSizes.length * features.length];

		// this array will hold the window-size index for the given feature
		this.featureHostListIndizes = new Integer[windowSizes.length
				* features.length];

		// this.featureHostListIndizes = featureListIndizes;

		for (int i = 0; i < features.length; i++) {
			for (int j = 0; j < windowSizes.length; j++) {
				int index = i * windowSizes.length + j;
				// System.out.println(i +"\t" + j + "\t" + index);
				this.features[index] = NetflowFeature.valueOf(features[i]);
				this.featureHostListIndizes[index] = j;
			}
		}

		for (int i = 0; i < features.length; i++) {
			this.features[i] = NetflowFeature.valueOf(features[i]);
		}

		// init host lists
		hostLists = new ArrayList<HostList>();
		expireList = new ArrayList<Integer>();

		for (Integer i : windowSizes) {
			hostLists.add(new HostList(i + "s"));
			expireList.add(i);
		}

		Log.infoSep();
		Log.info("Naive netflow feature extraction");
		Log.info("");
		Log.info("Dir:\t" + dir);
		Log.info("Filename:\t" + filename);
		Log.info("");
		for (Integer i : windowSizes)
			Log.info("\t" + i + " s");
		Log.info("");
		Log.info("Percentages:");
		for (String s : extraPercentValues)
			Log.info("\t" + s);
		Log.info("");
	}

	public void generate() throws IOException {

		NetflowEventReader reader = new DarpaNetflowReader(dir, filename);

		DateTime init = reader.getInitTimestamp();

		Log.info("Init:\t" + init);
		Log.info("");

		DateTime currentTime = init;

		Writer w = new Writer("", this.dstPath);

		String header = "Timestamp";
		for (int i = 0; i < this.features.length; i++) {
			header += "\t"
					+ getHeader(
							this.features[i],
							this.expireList.get(this.featureHostListIndizes[i]),
							this.extraPercentValues);
		}

		w.writeln(header);

		while (reader.isNextEventPossible()) {
			NetflowEvent event = reader.getNextEvent();
			DateTime time = event.getTime();

			long millisNew = time.getMillis();
			long millisOld = currentTime.getMillis();

			long millisDiff = millisNew - millisOld;
			long secondsDiff = (long) Math.floor(millisDiff / 1000);

			DateTime t = currentTime;

			for (int i = 1; i <= secondsDiff; i++) {
				DateTime t0 = currentTime.plusSeconds(i);

				// update hosts based on time
				boolean somethingHappened = false;
				for (HostList hl : hostLists) {
					somethingHappened = somethingHappened | hl.update(t0);
				}

				if (somethingHappened) {
					String line = getLine(t0);
					w.writeln(line);
				}

			}

			// init new hosts based on event
			for (int i = 0; i < hostLists.size(); i++) {
				HostList list = hostLists.get(i);
				int seconds = expireList.get(i);

				Host hSrc = list.refreshHost(event.getSrcAddress(), time, null);
				Host hDst = list.refreshHost(event.getDstAddress(), time, null);
				hSrc.applyUpdate(HostUpdate.getSrcUpdateFromEvent(event,
						seconds));
				hDst.applyUpdate(HostUpdate.getDstUpdateFromEvent(event,
						seconds));
			}

			currentTime = time;
		}

		w.close();

		Log.info("End:  " + currentTime);
		Log.infoSep();
	}

	public String getHeader(NetflowFeature feature, Integer time,
			String[] extraPercentValues) {
		String buff = feature.toString() + "_" + time + "_MIN" + "\t"
				+ feature.toString() + "_" + time + "_MAX" + "\t"
				+ feature.toString() + "_" + time + "_AVG";
		for (String p : extraPercentValues)
			buff += "\t" + feature.toString() + "_" + time + "_p" + p;
		return buff;
	}

	public String getLine(DateTime time) {
		String line = "" + (long) Math.floor(time.getMillis() / 1000);
		for (int i = 0; i < this.featureHostListIndizes.length; i++) {
			line += "\t"
					+ getValue(
							this.hostLists.get(this.featureHostListIndizes[i]),
							this.features[i]);
		}

		return line;
	}

	public String getValue(HostList list, NetflowFeature feature) {
		String val = "";
		ArrayList<Long> values;
		ArrayList<Double> valuesD;
		switch (feature) {
		case bytesIn:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add(h.getBytesIn());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values)
					+ getPercentValues(values, extraPercentValues);
			break;
		case bytesOut:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add(h.getBytesOut());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values)
					+ getPercentValues(values, extraPercentValues);
			break;
		case bytesTotal:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add(h.getBytesOut() + h.getBytesIn());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values)
					+ getPercentValues(values, extraPercentValues);
			break;
		case flowsIn:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add(h.getFlowsIn());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values)
					+ getPercentValues(values, extraPercentValues);
			break;
		case flowsInPerPort:
			valuesD = new ArrayList<Double>();
			for (Host h : list.getList()) {
				valuesD.add(1.0 * h.getFlowsIn() / h.getPortsIn().size());
			}
			val += getMinD(valuesD) + "\t" + getMaxD(valuesD) + "\t"
					+ getAvgD(valuesD)
					+ getPercentValuesD(valuesD, extraPercentValues);
			break;
		case flowsOut:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add(h.getFlowsOut());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values)
					+ getPercentValues(values, extraPercentValues);
			break;
		case flowsOutPerPort:
			valuesD = new ArrayList<Double>();
			for (Host h : list.getList()) {
				valuesD.add(1.0 * h.getFlowsOut() / h.getPortsOut().size());
			}
			val += getMinD(valuesD) + "\t" + getMaxD(valuesD) + "\t"
					+ getAvgD(valuesD)
					+ getPercentValuesD(valuesD, extraPercentValues);
			break;
		case flowsTotal:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add(h.getFlowsOut() + h.getFlowsIn());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values)
					+ getPercentValues(values, extraPercentValues);
			break;
		case packetsIn:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add(h.getPacketsIn());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values)
					+ getPercentValues(values, extraPercentValues);
			break;
		case packetsOut:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add(h.getPacketsOut());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values)
					+ getPercentValues(values, extraPercentValues);
			break;
		case packetsTotal:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add(h.getPacketsIn() + h.getPacketsOut());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values)
					+ getPercentValues(values, extraPercentValues);
			break;
		case portsIn:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add((long) h.getPortsIn().size());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values)
					+ getPercentValues(values, extraPercentValues);
			break;
		case portsOut:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add((long) h.getPortsOut().size());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values)
					+ getPercentValues(values, extraPercentValues);
			break;
		case portsTotal:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add((long) h.getPortsIn().size()
						+ h.getPortsOut().size());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values)
					+ getPercentValues(values, extraPercentValues);
			break;
		default:
			break;
		}
		return val;
	}

	public long getMin(ArrayList<Long> values) {
		long min = 0;
		for (Long v : values) {
			if (v < min)
				min = v;
		}
		return min;
	}

	public long getMax(ArrayList<Long> values) {
		long max = 0;
		for (Long v : values) {
			if (v > max)
				max = v;
		}
		return max;
	}

	public double getAvg(ArrayList<Long> values) {
		long sum = 0;
		for (Long v : values) {
			sum += v;
		}
		return 1.0 * sum / values.size();
	}

	public double getMinD(ArrayList<Double> values) {
		double min = 0;
		for (Double v : values) {
			if (v < min)
				min = v;
		}
		return min;
	}

	public double getMaxD(ArrayList<Double> values) {
		double max = 0;
		for (Double v : values) {
			if (v > max)
				max = v;
		}
		return max;
	}

	public double getAvgD(ArrayList<Double> values) {
		double sum = 0;
		for (Double v : values) {
			sum += v;
		}
		return 1.0 * sum / values.size();
	}

	public String getPercentValues(ArrayList<Long> values,
			String[] extraPercentValues) {
		long[] v = new long[values.size()];
		for (int i = 0; i < v.length; i++) {
			v[i] = values.get(i);
		}
		Arrays.sort(v);

		String buff = "";

		// for each percent value calculate upper bound
		for (String p : extraPercentValues) {
			double percent = Double.parseDouble(p) / 100;
			int index = (int) Math.ceil(v.length * percent);
			// System.out.println("perc: " + p);
			// System.out.println("percD: " + percent);
			// System.out.println("index: " + index + "\tfrom: " + v.length);
			// System.out.println("");
			if (index >= v.length)
				index = v.length - 1;
			buff += "\t" + v[index];
		}
		return buff;
	}

	public String getPercentValuesD(ArrayList<Double> values,
			String[] extraPercentValues) {
		double[] v = new double[values.size()];
		for (int i = 0; i < v.length; i++) {
			v[i] = values.get(i);
		}
		Arrays.sort(v);

		String buff = "";

		// for each percent value calculate upper bound
		for (String p : extraPercentValues) {
			double percent = Double.parseDouble(p) / 100;
			int index = (int) Math.ceil(v.length * percent);
			if (index >= v.length)
				index = v.length - 1;
			buff += "\t" + v[index];
		}
		return buff;
	}
}
