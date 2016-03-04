package dna.parallel.partition;

import java.util.List;

import dna.graph.Graph;
import dna.graph.nodes.Node;
import dna.updates.batch.Batch;

public abstract class Partition {

	public static enum PartitionType {
		NodeCut, Separated, Overlapping
	}

	public Graph g;

	public Partition(Graph g) {
		this.g = g;
	}

	protected static Graph getInitialGraph(Graph g, List<Node> nodes,
			String name) {
		Graph g_ = g.getGraphDatastructures().newGraphInstance(name,
				g.getTimestamp(), nodes.size(), 0);
		for (Node n : nodes) {
			g_.addNode(g_.getGraphDatastructures()
					.newNodeInstance(n.asString()));
		}
		return g_;
	}

	protected static Graph[] getInitialGraphs(Graph g, List<Node>[] nodess) {
		Graph[] graphs = new Graph[nodess.length];
		for (int i = 0; i < graphs.length; i++) {
			graphs[i] = getInitialGraph(g, nodess[i], "partition" + i);
		}
		return graphs;
	}

	protected static Batch[] getEmptyBatches(Batch b, int partitionCount) {
		Batch[] batches = new Batch[partitionCount];
		for (int i = 0; i < batches.length; i++) {
			batches[i] = new Batch(b.getGraphDatastructures(), b.getFrom(),
					b.getTo());
		}
		return batches;
	}
}