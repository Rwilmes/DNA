package dna.graph.generators.network.tests.jars;

import java.io.IOException;
import java.util.ArrayList;

import org.joda.time.DateTime;

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

		String dir = "data/naive-netflow/";
		String filename = "2_2.netflow";
		// filename = "2_2_small.netflow";

		String dstPath = "data/naive-netflow/test1.features";

		int windowSize = 10;

		Integer[] windowSizes = new Integer[] { 5, 10, 15, 30, 60, 120, 300,
				600 };

		Integer[] indizes = new Integer[] { 2, 4, 6 };
		String[] features = new String[] { "portsIn", "portsIn", "portsIn" };

		NetflowNaiveFeatures n = new NetflowNaiveFeatures(dir, filename,
				dstPath, windowSizes, indizes, features);
		n.generate();
	}

	protected String dir;
	protected String filename;
	protected Integer[] windowSizes;

	protected String dstPath;

	protected ArrayList<HostList> hostLists;
	protected ArrayList<Integer> expireList;

	protected Integer[] featureHostListIndizes;
	protected NetflowFeature[] features;

	public NetflowNaiveFeatures(String dir, String filename, String dstPath,
			Integer[] windowSizes, Integer[] featureListIndizes,
			String[] features) {
		this.dir = dir;
		this.filename = filename;
		this.windowSizes = windowSizes;

		this.dstPath = dstPath;

		this.featureHostListIndizes = featureListIndizes;
		this.features = new NetflowFeature[features.length];
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
					+ getHeader(this.features[i],
							this.expireList.get(this.featureHostListIndizes[i]));
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

	public String getHeader(NetflowFeature feature, Integer time) {
		return feature.toString() + "_" + time + "_MIN" + "\t"
				+ feature.toString() + "_" + time + "_MAX" + "\t"
				+ feature.toString() + "_" + time + "_AVG";
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
					+ getAvg(values);
			break;
		case bytesOut:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add(h.getBytesOut());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values);
			break;
		case bytesTotal:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add(h.getBytesOut() + h.getBytesIn());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values);
			break;
		case flowsIn:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add(h.getFlowsIn());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values);
			break;
		case flowsInPerPort:
			valuesD = new ArrayList<Double>();
			for (Host h : list.getList()) {
				valuesD.add(1.0 * h.getFlowsIn() / h.getPortsIn().size());
			}
			val += getMinD(valuesD) + "\t" + getMaxD(valuesD) + "\t"
					+ getAvgD(valuesD);
			break;
		case flowsOut:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add(h.getFlowsOut());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values);
			break;
		case flowsOutPerPort:
			valuesD = new ArrayList<Double>();
			for (Host h : list.getList()) {
				valuesD.add(1.0 * h.getFlowsOut() / h.getPortsOut().size());
			}
			val += getMinD(valuesD) + "\t" + getMaxD(valuesD) + "\t"
					+ getAvgD(valuesD);
			break;
		case flowsTotal:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add(h.getFlowsOut() + h.getFlowsIn());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values);
			break;
		case packetsIn:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add(h.getPacketsIn());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values);
			break;
		case packetsOut:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add(h.getPacketsOut());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values);
			break;
		case packetsTotal:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add(h.getPacketsIn() + h.getPacketsOut());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values);
			break;
		case portsIn:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add((long) h.getPortsIn().size());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values);
			break;
		case portsOut:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add((long) h.getPortsOut().size());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values);
			break;
		case portsTotal:
			values = new ArrayList<Long>();
			for (Host h : list.getList()) {
				values.add((long) h.getPortsIn().size()
						+ h.getPortsOut().size());
			}
			val += getMin(values) + "\t" + getMax(values) + "\t"
					+ getAvg(values);
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
}
