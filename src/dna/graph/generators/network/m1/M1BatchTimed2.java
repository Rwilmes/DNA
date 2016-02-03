package dna.graph.generators.network.m1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;

import dna.graph.Graph;
import dna.graph.IElement;
import dna.graph.edges.Edge;
import dna.graph.generators.network.NetworkEdge;
import dna.graph.generators.network.weights.NetworkWeight;
import dna.graph.generators.network.weights.NetworkWeight.ElementType;
import dna.graph.nodes.Node;
import dna.graph.weights.IWeightedEdge;
import dna.graph.weights.IWeightedNode;
import dna.graph.weights.IntWeight;
import dna.graph.weights.LongWeight;
import dna.updates.batch.Batch;
import dna.updates.update.EdgeAddition;
import dna.updates.update.EdgeRemoval;
import dna.updates.update.EdgeWeight;
import dna.updates.update.NodeAddition;
import dna.updates.update.NodeWeight;
import dna.util.Log;
import dna.util.network.tcp.TCPEvent;
import dna.util.network.tcp.TCPEventReader;

/**
 * M1-BatchGenerator with timed edges.
 * 
 * @author Rwilmes
 * 
 */
public class M1BatchTimed2 extends M1Batch {

	protected long interval;

	public enum TCPNodeType {
		NOTHING, REMOVE_ON_ZERO_DEGREE
	};

	public M1BatchTimed2(TCPEventReader reader, int batchIntervalInSeconds,
			long edgeLifetimeInMillis) throws IOException {
		super(reader, batchIntervalInSeconds);
		this.interval = edgeLifetimeInMillis;
		if (edgeLifetimeInMillis * 1000 <= batchIntervalInSeconds)
			Log.warn("interval < batch-interval!");
	}

	/** Returns a map with the sum of all weight decrementals per edge. **/
	protected HashMap<String, Integer> getWeightDecrementals(
			ArrayList<NetworkEdge> allEdges) {
		HashMap<String, Integer> wcMap = new HashMap<String, Integer>();
		for (int i = 0; i < allEdges.size(); i++) {
			NetworkEdge e = allEdges.get(i);
			decrementWeightChanges(e.getSrc(), e.getDst(), wcMap);
		}

		return wcMap;
	}

	protected String getIdentifier(int src, int dst) {
		return src + "->" + dst;
	}

	protected void incrementWeightChanges(int src, int dst,
			HashMap<String, Integer> map) {
		// add incrementals to map
		String identifier = getIdentifier(src, dst);
		if (map.containsKey(identifier)) {
			map.put(identifier, map.get(identifier) + 1);
		} else {
			map.put(identifier, 1);
		}
	}

	protected void decrementWeightChanges(int src, int dst,
			HashMap<String, Integer> map) {
		// add incrementals to map
		String identifier = getIdentifier(src, dst);
		if (map.containsKey(identifier)) {
			map.put(identifier, map.get(identifier) - 1);
		} else {
			map.put(identifier, -1);
		}
	}

	@Override
	public Batch craftBatch(Graph g, DateTime timestamp,
			ArrayList<TCPEvent> events, HashMap<String, Integer> map) {
		// init batch
		Batch b = new Batch(g.getGraphDatastructures(), g.getTimestamp(),
				TimeUnit.MILLISECONDS.toSeconds(timestamp.getMillis()), 0, 0,
				0, 0, 0, 0);

		ArrayList<Node> addedNodes = new ArrayList<Node>();
		ArrayList<NetworkEdge> addedEdges = new ArrayList<NetworkEdge>();

		// iterate over events
		for (int i = 0; i < events.size(); i++) {
			TCPEvent e = events.get(i);
			String srcIp = e.getSrcIp();
			String dstIp = e.getDstIp();
			int port = e.getDstPort();
			long t = e.getTime().getMillis();

			// get mappings
			int srcIpMapping = reader.mapp(srcIp);
			int dstIpMapping = reader.mapp(dstIp);
			int portMapping = reader.mapp(port);

			// add events to queue when to decrement edges
			reader.addEdgeToQueue(new NetworkEdge(srcIpMapping, portMapping,
					(t + this.interval)));
			reader.addEdgeToQueue(new NetworkEdge(portMapping, dstIpMapping,
					(t + this.interval)));

			// account weight changes
			incrementWeightChanges(srcIpMapping, portMapping, map);
			incrementWeightChanges(portMapping, dstIpMapping, map);

			/*
			 * NODES
			 */

			Node srcNode;
			Node dstNode;
			Node portNode;

			if (!reader.isNodeActive(srcIp)) {
				srcNode = addHostNode(g, srcIpMapping);
				reader.addNode(srcIp);
				addedNodes.add(srcNode);
				b.add(new NodeAddition(srcNode));
			}
			if (!reader.isNodeActive(dstIp)) {
				dstNode = addHostNode(g, dstIpMapping);
				reader.addNode(dstIp);
				addedNodes.add(dstNode);
				b.add(new NodeAddition(dstNode));
			}
			if (!reader.isNodeActive("" + port)) {
				portNode = addPortNode(g, portMapping);
				reader.addNode("" + port);
				addedNodes.add(portNode);
				b.add(new NodeAddition(portNode));
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

				if (n1 == srcIpMapping && n2 == portMapping)
					edge1Exists = true;

				if (n1 == portMapping && n2 == dstIpMapping)
					edge2Exists = true;
			}

			if (!edge1Exists) {
				NetworkEdge ne = new NetworkEdge(srcIpMapping, portMapping, t);
				if (!ne.containedIn(addedEdges))
					addedEdges.add(ne);
			}
			if (!edge2Exists) {
				NetworkEdge ne = new NetworkEdge(portMapping, dstIpMapping, t);
				if (!ne.containedIn(addedEdges))
					addedEdges.add(ne);
			}
		}

		// add edges
		for (int j = 0; j < addedEdges.size(); j++) {
			addEdgeToBatch(b, g, addedNodes, addedEdges.get(j));
		}

		// change edge weights, possibly delete nodes
		for (String s : map.keySet()) {
			String[] splits = s.split("->");
			changeEdgeWeights(b, g, Integer.parseInt(splits[0]),
					Integer.parseInt(splits[1]), map.get(s));
		}

		Log.infoSep();
		Log.info("RETURNING BATCH: " + b.getTo());
		b.print();
		Log.infoSep();

		return b;
	}

	protected void addEdgeToBatch(Batch b, Graph g, ArrayList<Node> addedNodes,
			NetworkEdge ne) {

		int src = ne.getSrc();
		int dst = ne.getDst();

		Node sNode = getNode(g, addedNodes, src);
		Node dNode = getNode(g, addedNodes, dst);

		IWeightedEdge e = (IWeightedEdge) g.getGraphDatastructures()
				.newEdgeInstance(sNode, dNode);
		e.setWeight(new IntWeight(1));

		b.add(new EdgeAddition(e));
		// b.add(new EdgeWeight(e, new LongWeight(ne.getTime())));
	}

	protected void changeEdgeWeights(Batch b, Graph g, int src, int dst,
			int weight) {
		Iterator<IElement> ite = g.getEdges().iterator();

		boolean fin = false;
		while (ite.hasNext() && !fin) {
			IWeightedEdge edge = (IWeightedEdge) ite.next();
			if (edge.getN1().getIndex() == src
					&& edge.getN2().getIndex() == dst) {
				IntWeight w = (IntWeight) edge.getWeight();
				weight += w.getWeight();

				// change weight or remove
				if (weight > 0)
					b.add(new EdgeWeight(edge, new IntWeight(weight)));
				else
					b.add(new EdgeRemoval(edge));

				fin = true;
			}
		}
	}

	public Node addNode(Graph g, int mapping, ElementType type) {
		Node n = g.getNode(mapping);
		if (n != null)
			return n;

		// init node
		n = g.getGraphDatastructures().newNodeInstance(mapping);

		// set networkweight
		if (g.getGraphDatastructures().getNodeWeightType()
				.equals(NetworkWeight.class))
			((IWeightedNode) n).setWeight(new NetworkWeight(type));

		return n;
	}

	public Node addHostNode(Graph g, int mapping) {
		return addNode(g, mapping, ElementType.HOST);
	}

	public Node addPortNode(Graph g, int mapping) {
		return addNode(g, mapping, ElementType.PORT);
	}

	// @Override
	public Batch craftBatch2(Graph g, ArrayList<TCPEvent> events,
			HashMap<String, Integer> weightChangesMap2) {
		ArrayList<String> addedNodes = new ArrayList<String>();
		ArrayList<Node> addedNodesNodes = new ArrayList<Node>();
		ArrayList<NetworkEdge> addedEdges = new ArrayList<NetworkEdge>();

		// time of last event
		long lastTime = events.get(events.size() - 1).getTime().getMillis();

		// init batch
		Batch b = new Batch(g.getGraphDatastructures(), g.getTimestamp(),
				TimeUnit.MILLISECONDS.toSeconds(lastTime), 0, 0, 0, 0, 0, 0);

		// get all edges to decrement
		ArrayList<NetworkEdge> decrementEdges = reader
				.getDecrementEdges(lastTime);

		// get the amount of weight-decrements per edge
		HashMap<String, Integer> weightChangesMap = getWeightDecrementals(decrementEdges);

		// gather changes inside events
		for (int i = 0; i < events.size(); i++) {
			TCPEvent e = events.get(i);
			String srcIp = e.getSrcIp();
			String dstIp = e.getDstIp();
			int port = e.getDstPort();
			long t = e.getTime().getMillis();

			// get mappings
			int srcIpMapping = reader.mapp(srcIp);
			int dstIpMapping = reader.mapp(dstIp);
			int portMapping = reader.mapp(port);

			// add events to queue when to decrement edges
			reader.addEdgeToQueue(new NetworkEdge(srcIpMapping, portMapping,
					(t + this.interval)));
			reader.addEdgeToQueue(new NetworkEdge(portMapping, dstIpMapping,
					(t + this.interval)));

			// account weight changes
			incrementWeightChanges(srcIpMapping, portMapping, weightChangesMap);
			incrementWeightChanges(portMapping, dstIpMapping, weightChangesMap);

			/*
			 * NODES
			 */

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

					// check if nodeweights
					if (g.getGraphDatastructures().getNodeWeightType()
							.equals(NetworkWeight.class))
						b.add(new NodeWeight((IWeightedNode) srcNode,
								new NetworkWeight(ElementType.HOST)));
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

					// check if nodeweights
					if (g.getGraphDatastructures().getNodeWeightType()
							.equals(NetworkWeight.class))
						b.add(new NodeWeight((IWeightedNode) dstNode,
								new NetworkWeight(ElementType.HOST)));
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

					// check if nodeweights
					if (g.getGraphDatastructures().getNodeWeightType()
							.equals(NetworkWeight.class))
						b.add(new NodeWeight((IWeightedNode) portNode,
								new NetworkWeight(ElementType.PORT)));
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
					reader.incrementEdgeWeight(ne);
				} else {
					// add edge
					NetworkEdge netEdge = new NetworkEdge(srcIpMapping,
							portMapping, t);
					addedEdges.add(netEdge);
					reader.incrementEdgeWeight(netEdge);
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
					reader.incrementEdgeWeight(ne);
				} else {
					// add edge
					NetworkEdge netEdge = new NetworkEdge(portMapping,
							dstIpMapping, t);
					addedEdges.add(netEdge);
					reader.incrementEdgeWeight(netEdge);
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

}
