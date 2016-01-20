package dna.graph.generators.network.m1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import dna.graph.Graph;
import dna.graph.edges.Edge;
import dna.graph.generators.network.NetworkBatch;
import dna.graph.nodes.Node;
import dna.updates.batch.Batch;
import dna.updates.update.EdgeAddition;
import dna.updates.update.NodeAddition;
import dna.util.network.NetworkEvent;
import dna.util.network.tcp.TCPEventReader;
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

	protected boolean debug = false;

	public M1Batch(String dir, String filename, int batchIntervalInSeconds)
			throws IOException {
		super("M1-BatchGenerator", dir, filename, batchIntervalInSeconds);
	}

	public M1Batch(TCPEventReader reader, int batchIntervalInSeconds)
			throws IOException {
		super("M1-BatchGenerator", reader, batchIntervalInSeconds);
	}

	@Override
	public void onEvent(Graph g, Batch b, NetworkEvent entry,
			HashMap<Integer, Node> portMap, HashMap<String, Node> ipMap,
			HashMap<Integer, Integer> nodeDegreeChangeMap, ArrayList<Edge> addedEdges, ArrayList<Edge> removedEdges) {
		if (GraphVisualization.isEnabled()) {
			GraphVisualization.getGraphPanel(g).setText(
					"Network Time: " + entry.getTimeReadable());
		}

		// if port == 0, return
		if (entry.getDstPort() == 0)
			return;

		// print entry for debugging
		if (debug)
			entry.print();

		// get srcIp, dstIp and dstPort
		String srcIp = entry.getSrcIp();
		String dstIp = entry.getDstIp();
		int dstPort = entry.getDstPort();

		Node srcNode;
		Node dstNode;
		Node portNode;

		// if port node not present yet, add it and buffer in port
		if (reader.containsPort(dstPort)) {
			portNode = g.getNode(reader.getPortMapping(dstPort));
			if (portNode == null)
				portNode = portMap.get(dstPort);
		} else {
			portNode = g.getGraphDatastructures().newNodeInstance(
					reader.addPort(dstPort));
			b.add(new NodeAddition(portNode));
			portMap.put(dstPort, portNode);
		}

		// if ip node not present yet, add it and buffer in ipMap
		if (reader.containsIp(srcIp)) {
			srcNode = g.getNode(reader.getIpMapping(srcIp));
			if (srcNode == null)
				srcNode = ipMap.get(srcIp);
		} else {
			srcNode = g.getGraphDatastructures().newNodeInstance(
					reader.addIp(srcIp));
			b.add(new NodeAddition(srcNode));
			ipMap.put(srcIp, srcNode);
		}

		// if ip node not present yet, add it and buffer in ipMap
		if (reader.containsIp(dstIp)) {
			dstNode = g.getNode(reader.getIpMapping(dstIp));
			if (dstNode == null)
				dstNode = ipMap.get(dstIp);
		} else {
			dstNode = g.getGraphDatastructures().newNodeInstance(
					reader.addIp(dstIp));
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

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

}
