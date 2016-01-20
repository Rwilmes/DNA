package dna.graph.generators.network.m1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import dna.graph.Graph;
import dna.graph.IElement;
import dna.graph.edges.Edge;
import dna.graph.nodes.Node;
import dna.graph.weights.IWeightedEdge;
import dna.graph.weights.LongWeight;
import dna.updates.batch.Batch;
import dna.updates.update.EdgeAddition;
import dna.updates.update.EdgeRemoval;
import dna.updates.update.EdgeWeight;
import dna.updates.update.NodeAddition;
import dna.util.Log;
import dna.util.network.NetworkEvent;
import dna.util.network.tcp.TCPEventReader;
import dna.visualization.graph.GraphVisualization;

/**
 * M1-BatchGenerator with timed edges.
 * 
 * @author Rwilmes
 * 
 */
public class M1BatchTimed extends M1Batch {

	protected long interval;

	protected TCPNodeType nodeAction;

	public enum TCPNodeType {
		NOTHING, REMOVE_ON_ZERO_DEGREE
	};

	public M1BatchTimed(TCPEventReader reader, int batchIntervalInSeconds,
			long edgeLifetimeInMillis) throws IOException {
		this(reader, batchIntervalInSeconds, edgeLifetimeInMillis,
				TCPNodeType.NOTHING);
	}

	public M1BatchTimed(TCPEventReader reader, int batchIntervalInSeconds,
			long edgeLifetimeInMillis, TCPNodeType nodeAction)
			throws IOException {
		super(reader, batchIntervalInSeconds);
		this.interval = edgeLifetimeInMillis;
		this.nodeAction = nodeAction;

		if (edgeLifetimeInMillis * 1000 <= batchIntervalInSeconds)
			Log.warn("interval < batch-interval!");
	}

	@Override
	public void onEvent(Graph g, Batch b, NetworkEvent e,
			HashMap<Integer, Node> portMap, HashMap<String, Node> ipMap,
			HashMap<Integer, Integer> nm, ArrayList<Edge> addedEdges,
			ArrayList<Edge> removedEdges) {
		if (GraphVisualization.isEnabled()) {
			GraphVisualization.getGraphPanel(g).setText(
					"Network Time: " + e.getTimeReadable());
		}

		// if port == 0, return
		if (e.getDstPort() == 0)
			return;

		// print entry for debugging
		if (debug)
			e.print();

		// get srcIp, dstIp and dstPort
		String srcIp = e.getSrcIp();
		String dstIp = e.getDstIp();
		int dstPort = e.getDstPort();

		// add nodes
		Node srcNode;
		Node dstNode;
		Node portNode;

		if (ipMap.containsKey(srcIp)) {
			srcNode = ipMap.get(srcIp);
		} else {
			if (!reader.containsIpNode(srcIp)) {
				srcNode = g.getGraphDatastructures().newNodeInstance(
						reader.mapp(srcIp));
				b.add(new NodeAddition(srcNode));
				reader.addIpNode(srcIp, srcNode);
			} else {
				srcNode = reader.getIpNodeMap().get(srcIp);
			}
			ipMap.put(srcIp, srcNode);
		}

		if (ipMap.containsKey(dstIp)) {
			dstNode = ipMap.get(dstIp);
		} else {
			if (!reader.containsIpNode(dstIp)) {
				dstNode = g.getGraphDatastructures().newNodeInstance(
						reader.mapp(dstIp));
				b.add(new NodeAddition(dstNode));
				reader.addIpNode(dstIp, dstNode);
			} else {
				dstNode = reader.getIpNodeMap().get(dstIp);
			}
			ipMap.put(dstIp, dstNode);
		}

		if (portMap.containsKey(dstPort)) {
			portNode = portMap.get(dstPort);
		} else {
			if (!reader.containsPortNode(dstPort)) {
				portNode = g.getGraphDatastructures().newNodeInstance(
						reader.mapp(dstPort));
				b.add(new NodeAddition(portNode));
				reader.addPortNode(dstPort, portNode);
			} else {
				portNode = reader.getPortNodeMap().get(dstPort);
			}
			portMap.put(dstPort, portNode);
		}

		// get timestamp
		long t = e.getTime().getMillis();

		// add edges
		IWeightedEdge e1 = (IWeightedEdge) g.getGraphDatastructures()
				.newEdgeInstance(srcNode, portNode);
		IWeightedEdge e2 = (IWeightedEdge) g.getGraphDatastructures()
				.newEdgeInstance(portNode, dstNode);

		// check which edges are timed out and need to be removed
		Iterator<IElement> i = g.getEdges().iterator();
		while (i.hasNext()) {
			Edge edge = (Edge) i.next();
			LongWeight w = (LongWeight) ((IWeightedEdge) edge).getWeight();

			if (edge == e1 || edge == e2)
				continue;

			if (removedEdges.contains(edge))
				continue;

			if (debug) {
				System.out.println("comparing edge: " + edge.getN1Index()
						+ "  =>  " + edge.getN2Index());
				System.out.println("\tcurrent time: " + t + "\tedgeTime: "
						+ (w.getWeight() + interval) + "\tremove: "
						+ (t > w.getWeight() + interval));
			}

			if (t > w.getWeight() + interval) {
				b.add(new EdgeRemoval(edge));
				removedEdges.add(edge);
			}
		}

		// add edges
		if (!g.containsEdge((Edge) e1) && !addedEdges.contains((Edge) e1)) {
			b.add(new EdgeAddition(e1));
			addedEdges.add((Edge) e1);
		}
		if (!g.containsEdge((Edge) e2) && !addedEdges.contains((Edge) e2)) {
			b.add(new EdgeAddition(e2));
			addedEdges.add((Edge) e2);
		}

		// add edgeweights
		b.add(new EdgeWeight(e1, new LongWeight(t)));
		b.add(new EdgeWeight(e2, new LongWeight(t)));
	}

	protected void changeMapEntry(HashMap<Integer, Integer> map, int key,
			int change) {

		if (!map.containsKey(key)) {
			Log.info("changing entry: " + key + "  from  " + 0 + "  to  "
					+ change);
			map.put(key, change);
		} else {
			Log.info("changing entry: " + key + "  from  " + map.get(key)
					+ "  to  " + (map.get(key) + change));
			map.put(key, map.get(key) + change);
		}
	}

}
