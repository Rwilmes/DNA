package dna.graph.generators.network.m1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import dna.graph.Graph;
import dna.graph.IElement;
import dna.graph.edges.Edge;
import dna.graph.generators.network.NetworkEdge;
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
import dna.util.network.tcp.TCPEvent;
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

		System.out.println(srcIp + "\t" + dstIp + "\t" + dstPort);

		if (ipMap.containsKey(srcIp)) {
			System.out.println("1");
			srcNode = ipMap.get(srcIp);
		} else {
			System.out.println("2");
			if (!reader.containsIpNode(srcIp)) {
				System.out.println("3");
				srcNode = g.getGraphDatastructures().newNodeInstance(
						reader.mapp(srcIp));
				b.add(new NodeAddition(srcNode));
				reader.addIpNode(srcIp, srcNode);
			} else {
				System.out.println("4");
				srcNode = reader.getIpNodeMap().get(srcIp);
			}
			ipMap.put(srcIp, srcNode);
		}

		if (ipMap.containsKey(dstIp)) {
			System.out.println("10");
			dstNode = ipMap.get(dstIp);
		} else {
			System.out.println("11");
			if (!reader.containsIpNode(dstIp)) {
				System.out.println("12");
				dstNode = g.getGraphDatastructures().newNodeInstance(
						reader.mapp(dstIp));
				b.add(new NodeAddition(dstNode));
				reader.addIpNode(dstIp, dstNode);
			} else {
				System.out.println("13");
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

				// changeMapEntry(nm, edge.getN1().getIndex(), -1);
				// changeMapEntry(nm, edge.getN2().getIndex(), -1);
			}
		}

		// add edges
		if (!g.containsEdge((Edge) e1) && !addedEdges.contains((Edge) e1)) {
			b.add(new EdgeAddition(e1));
			addedEdges.add((Edge) e1);

			changeMapEntry(nm, e1.getN1().getIndex(), 1);
			changeMapEntry(nm, e1.getN2().getIndex(), 1);
		}
		if (!g.containsEdge((Edge) e2) && !addedEdges.contains((Edge) e2)) {
			b.add(new EdgeAddition(e2));
			addedEdges.add((Edge) e2);

			changeMapEntry(nm, e2.getN1().getIndex(), 1);
			changeMapEntry(nm, e2.getN2().getIndex(), 1);
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

			Edge edge1 = null;
			Edge edge2 = null;

			Iterator<IElement> iterator = g.getEdges().iterator();
			while (iterator.hasNext() && !(edge1Exists && edge2Exists)) {
				Edge edge = (Edge) iterator.next();
				int n1 = edge.getN1Index();
				int n2 = edge.getN2Index();

				if (n1 == srcIpMapping && n2 == portMapping) {
					edge1 = edge;
					edge1Exists = true;
				}
				if (n1 == portMapping && n2 == dstIpMapping) {
					edge2 = edge;
					edge2Exists = true;
				}
			}

			if (edge1Exists) {
				// update timestamp
				b.add(new EdgeWeight((IWeightedEdge) edge1, new LongWeight(t)));
			} else {
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

			if (edge2Exists) {
				// update timestamp
				b.add(new EdgeWeight((IWeightedEdge) edge2, new LongWeight(t)));
			} else {
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

		// check if edges to old
		Iterator<IElement> iterator = g.getEdges().iterator();
		while (iterator.hasNext()) {
			Edge edge = (Edge) iterator.next();
			LongWeight w = (LongWeight) ((IWeightedEdge) edge).getWeight();

			NetworkEdge nEdge = new NetworkEdge(edge.getN1Index(),
					edge.getN2Index(), w.getWeight());

			boolean added = false;
			for (int j = 0; j < addedEdges.size() && !added; j++) {
				NetworkEdge ne = addedEdges.get(j);
				if (nEdge.sameEdge(ne))
					added = true;
			}

			if (!added) {
				if (lastTime > w.getWeight() + interval)
					b.add(new EdgeRemoval(edge));
			}
		}

		// add edges
		for (int j = 0; j < addedEdges.size(); j++) {
			NetworkEdge ne = addedEdges.get(j);
			int src = ne.getSrc();
			int dst = ne.getDst();

			Node sNode = getNode(g, addedNodesNodes, src);
			Node dNode = getNode(g, addedNodesNodes, dst);

			IWeightedEdge e = (IWeightedEdge) g.getGraphDatastructures()
					.newEdgeInstance(sNode, dNode);
			b.add(new EdgeAddition(e));
			b.add(new EdgeWeight(e, new LongWeight(ne.getTime())));
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
