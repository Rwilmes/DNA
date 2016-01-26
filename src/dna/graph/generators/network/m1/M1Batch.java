package dna.graph.generators.network.m1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import dna.graph.Graph;
import dna.graph.IElement;
import dna.graph.edges.Edge;
import dna.graph.generators.network.NetworkBatch;
import dna.graph.generators.network.NetworkEdge;
import dna.graph.nodes.Node;
import dna.updates.batch.Batch;
import dna.updates.update.EdgeAddition;
import dna.updates.update.NodeAddition;
import dna.util.network.NetworkEvent;
import dna.util.network.tcp.TCPEvent;
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

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	@Override
	public Batch craftBatch(Graph g, ArrayList<TCPEvent> events) {
		ArrayList<String> addedNodes = new ArrayList<String>();
		ArrayList<Node> addedNodesNodes = new ArrayList<Node>();
		ArrayList<NetworkEdge> addedEdges = new ArrayList<NetworkEdge>();

		// time of last event
		long lastTime = events.get(events.size() - 1).getTime().getMillis();

		// init batch
		Batch b = new Batch(g.getGraphDatastructures(), g.getTimestamp(),
				TimeUnit.MILLISECONDS.toSeconds(lastTime), 0, 0, 0, 0, 0, 0);

		// gather changes inside events
		for (int i = 0; i < events.size(); i++) {
			TCPEvent e = events.get(i);
			String srcIp = e.getSrcIp();
			String dstIp = e.getDstIp();
			int port = e.getDstPort();
			long t = e.getTime().getMillis();

			/*
			 * NODES
			 */

			int srcIpMapping = reader.mapp(srcIp);
			int dstIpMapping = reader.mapp(dstIp);
			int portMapping = reader.mapp(port);

			boolean srcNodeExists = reader.isNodeActive(srcIp);
			boolean dstNodeExists = reader.isNodeActive(dstIp);
			boolean portNodeExists = reader.isNodeActive("" + port);

			Node srcNode;
			Node dstNode;
			Node portNode;

			// if node doesnt exist -> add it
			if (!srcNodeExists) {
				if (!addedNodes.contains(srcIp)) {
					srcNode = g.getGraphDatastructures().newNodeInstance(
							srcIpMapping);
					b.add(new NodeAddition(srcNode));
					reader.addNode(srcIp);
					addedNodes.add(srcIp);
					addedNodesNodes.add(srcNode);
				} else {
					srcNode = addedNodesNodes.get(addedNodes.indexOf(srcIp));
				}
			} else {
				srcNode = g.getNode(srcIpMapping);
			}
			if (!dstNodeExists) {
				if (!addedNodes.contains(dstIp)) {
					dstNode = g.getGraphDatastructures().newNodeInstance(
							dstIpMapping);
					b.add(new NodeAddition(dstNode));
					reader.addNode(dstIp);
					addedNodes.add(dstIp);
					addedNodesNodes.add(dstNode);
				} else {
					dstNode = addedNodesNodes.get(addedNodes.indexOf(dstIp));
				}
			} else {
				dstNode = g.getNode(dstIpMapping);
			}
			if (!portNodeExists) {
				if (!addedNodes.contains("" + port)) {
					portNode = g.getGraphDatastructures().newNodeInstance(
							portMapping);
					b.add(new NodeAddition(portNode));
					reader.addNode("" + port);
					addedNodes.add("" + port);
					addedNodesNodes.add(portNode);
				} else {
					portNode = addedNodesNodes.get(addedNodes
							.indexOf("" + port));
				}
			} else {
				portNode = g.getNode(portMapping);
			}

			/*
			 * EDGES
			 */

			// check if edges already exist in graph
			boolean edge1Exists = false;
			boolean edge2Exists = false;

			Iterator<IElement> iterator = g.getEdges().iterator();
			while (iterator.hasNext() && !(edge1Exists && edge2Exists)) {
				Edge edge = (Edge) iterator.next();
				int n1 = edge.getN1Index();
				int n2 = edge.getN2Index();

				if (n1 == srcIpMapping && n2 == portMapping) {
					edge1Exists = true;
				}
				if (n1 == portMapping && n2 == dstIpMapping) {
					edge2Exists = true;
				}
			}

			if (!edge1Exists) {
				boolean edge1added = false;
				NetworkEdge ne = null;
				for (int j = 0; j < addedEdges.size(); j++) {
					ne = addedEdges.get(j);
					if (ne.getSrc() == srcIpMapping
							&& ne.getDst() == portMapping) {
						edge1added = true;
						break;
					}
				}

				if (edge1added) {
					// update ne timestamp
					ne.setTime(t);
				} else {
					// add edge
					addedEdges
							.add(new NetworkEdge(srcIpMapping, portMapping, t));
				}
			}

			if (!edge2Exists) {
				boolean edge2added = false;
				NetworkEdge ne = null;
				for (int j = 0; j < addedEdges.size(); j++) {
					ne = addedEdges.get(j);
					if (ne.getSrc() == portMapping
							&& ne.getDst() == dstIpMapping) {
						edge2added = true;
						break;
					}
				}

				if (edge2added) {
					// update ne timestamp
					ne.setTime(t);
				} else {
					// add edge
					addedEdges
							.add(new NetworkEdge(portMapping, dstIpMapping, t));
				}
			}
		}

		// add edges
		for (int j = 0; j < addedEdges.size(); j++) {
			NetworkEdge ne = addedEdges.get(j);
			int src = ne.getSrc();
			int dst = ne.getDst();

			Node sNode = getNode(g, addedNodesNodes, src);
			Node dNode = getNode(g, addedNodesNodes, dst);

			Edge e = g.getGraphDatastructures().newEdgeInstance(sNode, dNode);
			b.add(new EdgeAddition(e));
		}

		return b;
	}

	protected Node getNode(Graph g, ArrayList<Node> addedNodesNodes, int index) {
		Node n = g.getNode(index);
		if (n != null)
			return n;

		for (Node node : addedNodesNodes) {
			if (node.getIndex() == index)
				return node;
		}
		return null;
	}

}
