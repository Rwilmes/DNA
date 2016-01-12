package dna.graph.generators.network.m1;

import java.io.IOException;
import java.util.HashMap;

import dna.graph.Graph;
import dna.graph.edges.Edge;
import dna.graph.generators.network.NetworkBatch;
import dna.graph.generators.network.NetworkEvent;
import dna.graph.nodes.Node;
import dna.updates.batch.Batch;
import dna.updates.update.EdgeAddition;

/**
 * NetworkBatch-Generator for Model1.<br>
 * 
 * onEvent: addEdge(srcIp, dstPort), addEde(dstPort, dstIp)
 * 
 * @author Rwilmes
 * 
 */
public class M1Batch extends NetworkBatch {

	public M1Batch(String dir, String filename, int batchIntervalInSeconds)
			throws IOException {
		super("M1-BatchGenerator", dir, filename, batchIntervalInSeconds);
	}

	@Override
	public void onEvent(Graph g, Batch b, NetworkEvent entry,
			HashMap<Integer, Integer> portMap, HashMap<String, Integer> ipMap) {
		// if port == 0, return
		if (entry.getDstPort() == 0)
			return;

		// print entry for debugging
		entry.print();

		// get srcIp, dstIp and dstPort
		String srcIp = entry.getSrcIp();
		String dstIp = entry.getDstIp();
		int dstPort = entry.getDstPort();

		// get mapping
		int srcId = ipMap.get(srcIp);
		int dstId = ipMap.get(dstIp);
		int portId = portMap.get(dstPort);

		// get nodes
		Node srcNode = g.getNode(srcId);
		Node dstNode = g.getNode(dstId);
		Node portNode = g.getNode(portId);

		// add edges
		Edge e1 = g.getGraphDatastructures().newEdgeInstance(srcNode, portNode);
		Edge e2 = g.getGraphDatastructures().newEdgeInstance(portNode, dstNode);

		if (!g.containsEdge(e1))
			b.add(new EdgeAddition(e1));
		if (!g.containsEdge(e2))
			b.add(new EdgeAddition(e2));
	}

}
