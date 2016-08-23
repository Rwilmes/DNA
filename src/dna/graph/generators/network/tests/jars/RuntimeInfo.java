package dna.graph.generators.network.tests.jars;

import java.io.IOException;
import java.util.HashMap;

import argList.ArgList;
import argList.types.atomic.EnumArg;
import argList.types.atomic.StringArg;
import dna.io.filesystem.Dir;
import dna.series.aggdata.AggregatedBatch.BatchReadMode;
import dna.series.data.BatchData;
import dna.series.data.RunTime;
import dna.series.lists.RunTimeList;
import dna.util.Config;

public class RuntimeInfo {

	public enum ZipMode {
		none, batches, runs
	};

	/*
	 * MAIN
	 */
	public static void main(String[] args) throws IOException {
		ArgList<RuntimeInfo> argList = new ArgList<RuntimeInfo>(
				RuntimeInfo.class, new StringArg("srcDir",
						"dir of the source data"), new EnumArg("zip-mode",
						"zip-mode of the given data", ZipMode.values()));
		RuntimeInfo d = argList.getInstance(args);
		d.generate();
	}

	protected String srcDir;
	protected ZipMode zipMode;

	public RuntimeInfo(String srcDir, String zipMode) {
		this.srcDir = srcDir;
		this.zipMode = ZipMode.valueOf(zipMode);
	}

	public void generate() throws IOException {
		switch (this.zipMode) {
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

		// get batches to read
		String[] batches = Dir.getBatchesIntelligent(srcDir);

		// runtime map
		HashMap<String, Double> map = new HashMap<String, Double>();
		String total = "total";
		map.put(total, 0.0);

		/** read all batches and sum up runtimes **/
		for (int i = 0; i < batches.length; i++) {
			// System.out.println(batches[i]);
			BatchData bd = BatchData.readIntelligent(srcDir + batches[i] + "/",
					Dir.getTimestamp(batches[i]),
					BatchReadMode.readOnlySingleValues);

			map.put(total, map.get(total)
					+ bd.getGeneralRuntimes().get(total).getRuntime());

			RunTimeList rl = bd.getMetricRuntimes();

			for (RunTime metric : rl.getList()) {
				String m = metric.getName();
				double t = metric.getRuntime();
				if (map.containsKey(m))
					map.put(m, map.get(m) + t);
				else
					map.put(m, t);
			}
		}

		double totalTime = map.get(total);

		double totalMetricTime = 0.0;

		for (String rt : map.keySet()) {
			if (rt == total)
				continue;

			totalMetricTime += map.get(rt);
			// print(rt, map.get(rt), totalTime);
		}

		print(total, totalTime, totalTime, totalMetricTime);
		print("metrics", totalMetricTime, totalTime, totalMetricTime);

		for (String rt : map.keySet()) {
			if (rt == total)
				continue;
			print(rt, map.get(rt), totalTime, totalMetricTime);
		}

	}

	public void print(String name, double nanoSeconds, double totalNanoSeconds,
			double totalMetricTime) {
		double secs = Math.floor(nanoSeconds / 1000.0 / 1000.0 * 100000) / 100000;
		double perc = Math.floor(nanoSeconds / totalNanoSeconds * 100000) / 100000;
		double percMetrics = Math.floor(nanoSeconds / totalMetricTime * 100000) / 100000;
		System.out.println(perc + "\t" + percMetrics + "\t" + secs + "\t"
				+ name);
	}

}
