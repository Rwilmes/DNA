package dna.parallel.partitioning;

import java.util.ArrayList;
import java.util.List;

import dna.graph.Graph;
import dna.graph.IElement;
import dna.graph.nodes.Node;

public class EqualSizePartitioning extends Partitioning {

	public EqualSizePartitioning() {
		super("EqualSizePartitioning");
	}

	@Override
	protected List<Node>[] partition(Graph g, int partitionCount) {
		List<Node> sorted = new ArrayList<Node>(g.getNodeCount());
		for (IElement n : g.getNodes()) {
			sorted.add((Node) n);
		}
		return this.split(g, sorted, partitionCount);
	}

}
