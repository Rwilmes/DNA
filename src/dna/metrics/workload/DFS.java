package dna.metrics.workload;

import java.util.HashSet;
import java.util.LinkedList;

import dna.graph.Graph;
import dna.graph.IElement;
import dna.graph.edges.DirectedEdge;
import dna.graph.edges.UndirectedEdge;
import dna.graph.nodes.DirectedNode;
import dna.graph.nodes.Node;
import dna.graph.nodes.UndirectedNode;
import dna.util.Log;

/**
 * 
 * Workload operation that executes a DFS from a random node until all nodes are
 * seen (of this node's connected component).
 * 
 * @author benni
 *
 */
public class DFS extends RandomElementWorkload {

	public DFS(int times, int samples) {
		super("DFS", ListType.V, times, samples, 0);
	}

	@Override
	protected void createWorkloadE(Graph g) {
		Log.error("DFSWorkload is not implemented for list type E");
	}

	@Override
	protected void createWorkloadV(Graph g) {
		HashSet<Node> seen = new HashSet<Node>();
		Node start = this.getSampleNode();
		seen.add(start);
		LinkedList<Node> list = new LinkedList<Node>();
		list.add(start);
		while (!list.isEmpty()) {
			Node current = list.removeLast();
			if (current instanceof DirectedNode) {
				for (IElement e : ((DirectedNode) current).getOutgoingEdges()) {
					Node n = ((DirectedEdge) e).getDst();
					if (seen.contains(n)) {
						continue;
					}
					seen.add(n);
					list.add(n);
				}
			} else if (current instanceof UndirectedNode) {
				for (IElement e : ((UndirectedNode) current).getEdges()) {
					Node n = ((UndirectedEdge) e).getDifferingNode(current);
					if (seen.contains(n)) {
						continue;
					}
					seen.add(n);
					list.add(n);
				}
			} else {
				Log.error("unsupported node type: " + current.getClass());
			}
		}
	}

}
