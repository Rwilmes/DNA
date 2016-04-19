package dna.graph.generators.network.tests;

import argList.ArgList;
import argList.types.atomic.EnumArg;
import argList.types.atomic.StringArg;

public class DatasetAnalysis {

	public enum DatasetType {
		packet, netflow, sessions
	}

	public enum ModelType {
		modelA, modelA_noWeights
	}

	public static void main(String[] args) {
		ArgList<DatasetAnalysis> argList = new ArgList<DatasetAnalysis>(
				DatasetAnalysis.class, new StringArg("srcDir",
						"dir of the source data"), new StringArg("srcFilename",
						"filename of the dataset sourcefile"), new EnumArg(
						"DatasetType", "type of input dataset",
						DatasetType.values()), new EnumArg("ModelType",
						"model to use", ModelType.values())
		// new EnumArg("graphType", "type of graph to generate", GraphType
		// .values()), new StringArrayArg("graphArguments",
		// "arguments for the graph generator", ","), new EnumArg(
		// "batchType", "type of batches to generate", BatchType
		// .values()), new StringArrayArg(
		// "batchArguments", "arguments for the batch generator",
		// ","), new LongArg("seed",
		// "seed to initialize the PRNG with"), new IntArg(
		// "batches", "number of batches to generate"),
		// new StringArg("destDir", "dir where to store the dataset"),
		// new StringArg("graphFilename", "e.g., 0.dnag"), new StringArg(
		// "batchSuffix", "e.g., .dnab")
		);

		// args = new String[] { "Undirected", "", "Random", "20,30", "Random",
		// "0,0,2,1", "0", "10", "../examples/random/", "0.dnag", ".dnab" };

		DatasetAnalysis d = argList.getInstance(args);
		d.generate();
	}

	public DatasetAnalysis(String srcDir, String srcFilename, String dType,
			String mType) {
		DatasetType datasetType = DatasetType.valueOf(dType);
		ModelType modelType = ModelType.valueOf(mType);
		System.out.println("init!");
	}

	public void generate() {
		System.out.println("generate!");
	}
}
