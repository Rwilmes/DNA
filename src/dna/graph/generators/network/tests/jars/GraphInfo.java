package dna.graph.generators.network.tests.jars;

import java.io.IOException;
import java.util.ArrayList;

import argList.ArgList;
import argList.types.atomic.EnumArg;
import argList.types.atomic.StringArg;
import dna.io.filesystem.Dir;
import dna.series.aggdata.AggregatedBatch.BatchReadMode;
import dna.series.data.BatchData;
import dna.series.data.Value;
import dna.series.lists.ValueList;
import dna.util.Config;

public class GraphInfo {

	public enum ZipMode {
		none, batches, runs
	};

	/*
	 * MAIN
	 */
	public static void main(String[] args) throws IOException {
		ArgList<GraphInfo> argList = new ArgList<GraphInfo>(GraphInfo.class,
				new StringArg("srcDir", "dir of the source data"), new EnumArg(
						"zip-mode", "zip-mode of the given data",
						ZipMode.values()));
		GraphInfo d = argList.getInstance(args);
		d.generate();
	}

	protected String srcDir;
	protected ZipMode zipMode;

	public GraphInfo(String srcDir, String zipMode) {
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

		String[] values = new String[] { "nodes", "addedNodes", "removedNodes",
				"edges", "addedEdges", "removedEdges" };

		// get batches to read
		String[] batches = Dir.getBatchesIntelligent(srcDir);

		ArrayList<double[]> valueList = new ArrayList<double[]>();

		/** read all batches and sum up runtimes **/
		for (int i = 0; i < batches.length; i++) {
			// System.out.println(batches[i]);
			BatchData bd = BatchData.readIntelligent(srcDir + batches[i] + "/",
					Dir.getTimestamp(batches[i]),
					BatchReadMode.readOnlySingleValues);

			double[] vs = new double[values.length + 1];
			vs[0] = bd.getTimestamp();
			ValueList vl = bd.getValues();

			for (int j = 0; j < values.length; j++) {
				Value v = vl.get(values[j]);
				vs[j + 1] = v.getValue();
			}

			valueList.add(vs);
		}

		printHeader(values);

		for (double[] doubles : valueList) {
			print(doubles);
		}

	}

	public static void printHeader(String[] values) {
		String buff = "timestamp";

		for (int i = 0; i < values.length; i++) {
			buff += "\t" + values[i];
		}

		System.out.println(buff);
	}

	public static void print(double[] values) {
		String buff = "" + values[0];

		for (int i = 1; i < values.length; i++) {
			buff += "\t" + values[i];
		}

		System.out.println(buff);
	}
}
