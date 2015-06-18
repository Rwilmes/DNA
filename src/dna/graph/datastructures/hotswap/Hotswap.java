package dna.graph.datastructures.hotswap;

import java.io.IOException;

import dna.graph.Graph;
import dna.graph.datastructures.DArray;
import dna.graph.datastructures.DArrayList;
import dna.graph.datastructures.DHashMap;
import dna.graph.datastructures.DHashSet;
import dna.graph.datastructures.DHashTable;
import dna.graph.datastructures.DataStructure;
import dna.graph.datastructures.GraphDataStructure;
import dna.graph.datastructures.IDataStructure;
import dna.graph.datastructures.config.DSConfig;
import dna.graph.datastructures.config.DSConfigDirected;
import dna.graph.datastructures.config.DSConfigUndirected;
import dna.graph.datastructures.cost.CostEstimation;
import dna.graph.datastructures.cost.CostFunctionsS;
import dna.graph.datastructures.cost.CostFunctionsSMap;
import dna.graph.datastructures.count.Counting;
import dna.graph.datastructures.count.OperationCount.AggregationType;
import dna.graph.datastructures.count.OperationCounts;
import dna.graph.datastructures.recommendation.Recommendation;
import dna.graph.edges.DirectedEdge;
import dna.graph.nodes.DirectedNode;

public class Hotswap {

	protected static boolean enabled = false;

	public static void enable() {
		enabled = true;
		initRecommendation();
	}

	public static void disable() {
		enabled = false;
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static int amortization = 10000000;
	public static int batches = 4;

	public static Recommendation r = null;

	public static void initRecommendation() {
		if (r != null) {
			return;
		}
		final String measurements = "measurements_thinner/";
		CostFunctionsSMap map = new CostFunctionsSMap();
		try {
			map.add(CostFunctionsS.read(measurements, DArray.class,
					DirectedNode.class, DirectedEdge.class));
			map.add(CostFunctionsS.read(measurements, DArrayList.class,
					DirectedNode.class, DirectedEdge.class));
			map.add(CostFunctionsS.read(measurements, DHashSet.class,
					DirectedNode.class, DirectedEdge.class));
			map.add(CostFunctionsS.read(measurements, DHashMap.class,
					DirectedNode.class, DirectedEdge.class));
			map.add(CostFunctionsS.read(measurements, DHashTable.class,
					DirectedNode.class, DirectedEdge.class));
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		r = new Recommendation(map);
	}

	public static DSConfig check(GraphDataStructure gds) {
		// System.out.println("HOTSWAP CHECK");
		if (Counting.batchApplication.size() < batches) {
			// System.out.println("still "
			// + (batches - Counting.batchApplication.size())
			// + " rounds to go...");
			return null;
		}
		if (r == null) {
			initRecommendation();
		}

		DSConfig current = DSConfig.convert(gds);
		// System.out.println("current:     " + current);

		// int[] batches_ = new int[] { 1, 2, 3, 4 };
		// int[] amortization_ = new int[] { 1, 10, 100, 1000 };
		// for (int batches : batches_) {
		// System.out.println();
		// testStatic(batches, current);
		// for (int amortization : amortization_) {
		// testDynamic(batches, amortization, current);
		// }
		// }

		OperationCounts ocs = Counting.addLastRounds(batches,
				AggregationType.LAST);
		DSConfig cfg = r.recommendFastestConfig(ocs, batches, amortization,
				current);
		// System.out.println("recommended: " + cfg);
		return cfg;
	}

	public static void testDynamic(int batches, int amortization,
			DSConfig current) {
		OperationCounts ocs = Counting.addLastRounds(batches,
				AggregationType.LAST);
		DSConfig cfgD = r.recommendFastestConfig(ocs, batches, amortization,
				current);
		double cost = CostEstimation.estimateCosts(cfgD, ocs, r.map);
		double costSwap = CostEstimation.estimateSwapCosts(current, cfgD, ocs,
				r.map);
		double costCurrent = CostEstimation.estimateCosts(current, ocs, r.map);
		System.out.println("dynamic-" + batches + "-" + amortization + ": "
				+ cfgD + "\n  @ " + (cost / costCurrent) + " (swap: "
				+ (costSwap / costCurrent) + ")");
	}

	public static void testStatic(int batches, DSConfig current) {
		OperationCounts ocs = Counting.addLastRounds(batches,
				AggregationType.LAST);
		DSConfig cfgD = r.recommendFastestConfig(ocs);
		double cost = CostEstimation.estimateCosts(cfgD, ocs, r.map);
		double costCurrent = CostEstimation.estimateCosts(current, ocs, r.map);
		System.out.println("static-" + batches + ":    " + cfgD + "\n  @ "
				+ (cost / costCurrent));
	}

	public static void execute(Graph g, DSConfig cfg) {
		// System.out.println("HOTSWAP - EXECUTE");
		DSConfig current = DSConfig.convert(g.getGraphDatastructures());
		if (cfg.equals(current)) {
			// System.out.println("noting to to...");
			return;
		}

		System.out.println("HOT SWAP: " + current);
		System.out.println("       => " + cfg);

		Counting.disable();
		DataStructure.disableContainsOnAddition();
		g.getGraphDatastructures().switchDatastructures(cfg.getGDS(), g);
		DataStructure.enableContainsOnAddition();
		Counting.enable();

		// if (current instanceof DSConfigDirected) {
		// execute(g, (DSConfigDirected) current, (DSConfigDirected) cfg);
		// } else {
		// execute(g, (DSConfigUndirected) current, (DSConfigUndirected) cfg);
		// }
	}

	public static void execute(Graph g, DSConfigDirected current,
			DSConfigDirected cfg) {
		System.out.println("HOTSWAP EXECUTE directed");
		if (!current.V.equals(cfg.V)) {

		}
		if (!current.E.equals(cfg.E)) {

		}
		if (!current.in.equals(cfg.in)) {

		}
		if (!current.out.equals(cfg.out)) {

		}
		if (!current.neighbors.equals(cfg.neighbors)) {

		}
	}

	public static void execute(Graph g, DSConfigUndirected current,
			DSConfigUndirected cfg) {
		System.out.println("HOTSWAP EXECUTE undirected");
		if (!current.V.equals(cfg.V)) {

		}
		if (!current.E.equals(cfg.E)) {

		}
		if (!current.adj.equals(cfg.adj)) {

		}
	}

	protected static void swapV(Graph g, Class<IDataStructure> ds) {

	}

	protected static void swapE(Graph g, Class<IDataStructure> ds) {

	}

	protected static void swapAdj(Graph g, Class<IDataStructure> ds) {

	}

	protected static void swapIn(Graph g, Class<IDataStructure> ds) {

	}

	protected static void swapOut(Graph g, Class<IDataStructure> ds) {

	}

	protected static void swapNeighbors(Graph g, Class<IDataStructure> ds) {

	}

}
