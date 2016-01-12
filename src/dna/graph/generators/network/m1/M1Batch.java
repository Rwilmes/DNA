package dna.graph.generators.network.m1;

import java.io.IOException;
import java.util.HashMap;

import dna.graph.edges.Edge;
import dna.graph.generators.network.NetworkBatch;
import dna.graph.generators.network.NetworkGraph;
import dna.graph.nodes.Node;
import dna.updates.batch.Batch;
import dna.updates.update.EdgeAddition;
import dna.updates.update.NodeAddition;
import dna.util.network.NetworkEvent;
import dna.visualization.graph.GraphVisualization;

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
	public void onEvent(NetworkGraph g, Batch b, NetworkEvent entry,
			HashMap<Integer, Node> portMap, HashMap<String, Node> ipMap) {
		if (GraphVisualization.isEnabled()) {
			GraphVisualization.getGraphPanel(g).setText(
					"Network Time: " + entry.getTimeReadable());
		}

		// if port == 0, return
		if (entry.getDstPort() == 0)
			return;

		// print entry for debugging
		entry.print();

		// get srcIp, dstIp and dstPort
		String srcIp = entry.getSrcIp();
		String dstIp = entry.getDstIp();
		int dstPort = entry.getDstPort();

		Node srcNode;
		Node dstNode;
		Node portNode;

		// if port node not present yet, add it and buffer in port
		if (g.hasPort(dstPort)) {
			portNode = g.getNode(g.getPortMapping(dstPort));
			if (portNode == null)
				portNode = portMap.get(dstPort);
		} else {
			portNode = g.getGraphDatastructures().newNodeInstance(
					g.addPort(dstPort));
			b.add(new NodeAddition(portNode));
			portMap.put(dstPort, portNode);
		}

		// if ip node not present yet, add it and buffer in ipMap
		if (g.hasIp(srcIp)) {
			srcNode = g.getNode(g.getIpMapping(srcIp));
			if (srcNode == null)
				srcNode = ipMap.get(srcIp);
		} else {
			srcNode = g.getGraphDatastructures()
					.newNodeInstance(g.addIp(srcIp));
			b.add(new NodeAddition(srcNode));
			ipMap.put(srcIp, srcNode);
		}

		// if ip node not present yet, add it and buffer in ipMap
		if (g.hasIp(dstIp)) {
			dstNode = g.getNode(g.getIpMapping(dstIp));
			if (dstNode == null)
				dstNode = ipMap.get(dstIp);
		} else {
			dstNode = g.getGraphDatastructures()
					.newNodeInstance(g.addIp(dstIp));
			b.add(new NodeAddition(dstNode));
			ipMap.put(dstIp, dstNode);
		}

		// add edges
		Edge e1 = g.getGraphDatastructures().newEdgeInstance(srcNode, portNode);
		Edge e2 = g.getGraphDatastructures().newEdgeInstance(portNode, dstNode);

		if (!g.containsEdge(e1))
			b.add(new EdgeAddition(e1));
		if (!g.containsEdge(e2))
			b.add(new EdgeAddition(e2));
	}
}
