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
import dna.graph.generators.network.NetworkBatch;
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
import dna.updates.update.NodeRemoval;
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
public class M1BatchTimed2 extends NetworkBatch {

	protected long edgeLifeTimeMillis;

	protected boolean debug;

	public enum TCPNodeType {
		NOTHING, REMOVE_ON_ZERO_DEGREE
	};

	public M1BatchTimed2(TCPEventReader reader) throws IOException {
		super("M1-BatchGenerator", reader, reader.getBatchInterval());
		this.edgeLifeTimeMillis = reader.getEdgeLifeTimeMillis();
		if (reader.getEdgeLifeTimeMillis() * 1000 <= reader.getBatchInterval())
			Log.warn("interval < batch-interval!");
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
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

	protected void incrementDegreeChanges(int key, HashMap<Integer, Integer> map) {
		if (map.containsKey(key))
			map.put(key, map.get(key) + 1);
		else
			map.put(key, 1);
	}

	protected void decrementDegreeChanges(int key, HashMap<Integer, Integer> map) {
		if (map.containsKey(key))
			map.put(key, map.get(key) - 1);
		else
			map.put(key, -1);
	}

	@Override
	public Batch craftBatch(Graph g, DateTime timestamp,
			ArrayList<TCPEvent> events, HashMap<String, Integer> edgeWeightMap) {
		// init batch
		Batch b = new Batch(g.getGraphDatastructures(), g.getTimestamp(),
				TimeUnit.MILLISECONDS.toSeconds(timestamp.getMillis()), 0, 0,
				0, 0, 0, 0);

		ArrayList<Node> addedNodes = new ArrayList<Node>();
		ArrayList<NetworkEdge> addedEdges = new ArrayList<NetworkEdge>();

		// map: node-mapping -> node-degree
		HashMap<Integer, Integer> nodeDegreeMap = new HashMap<Integer, Integer>();

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
					(t + this.edgeLifeTimeMillis)));
			reader.addEdgeToQueue(new NetworkEdge(portMapping, dstIpMapping,
					(t + this.edgeLifeTimeMillis)));

			// account weight changes
			incrementWeightChanges(srcIpMapping, portMapping, edgeWeightMap);
			incrementWeightChanges(portMapping, dstIpMapping, edgeWeightMap);

			/*
			 * NODES
			 */

			Node srcNode;
			Node dstNode;
			Node portNode;

			if (!reader.isNodeActive("" + srcIpMapping)) {
				srcNode = addHostNode(g, srcIpMapping);
				reader.addNode("" + srcIpMapping);
				addedNodes.add(srcNode);
				b.add(new NodeAddition(srcNode));
				nodeDegreeMap.put(srcIpMapping, 0);
			}
			if (!reader.isNodeActive("" + dstIpMapping)) {
				dstNode = addHostNode(g, dstIpMapping);
				reader.addNode("" + dstIpMapping);
				addedNodes.add(dstNode);
				b.add(new NodeAddition(dstNode));
				nodeDegreeMap.put(dstIpMapping, 0);
			}
			if (!reader.isNodeActive("" + portMapping)) {
				portNode = addPortNode(g, portMapping);
				reader.addNode("" + portMapping);
				addedNodes.add(portNode);
				b.add(new NodeAddition(portNode));
				nodeDegreeMap.put(portMapping, 0);
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
			addEdgeToBatch(b, g, addedNodes, addedEdges.get(j), nodeDegreeMap);
		}

		// change edge weights, possibly delete nodes
		if (reader.isRemoveInactiveEdges())
			handleEdgeWeights(b, g, edgeWeightMap, nodeDegreeMap);

		// remove nodes with degree == 0
		if (reader.isRemoveZeroDegreeNodes())
			removeEmptyNodes(b, g, nodeDegreeMap);

		// debug
		if (this.debug) {
			Log.infoSep();
			Log.info("RETURNING BATCH: " + b.getTo());
			b.print();

			Log.info("");
			Log.info("degree-map:");
			for (Integer i : nodeDegreeMap.keySet()) {
				Log.info(":  " + i + "\t->\t" + nodeDegreeMap.get(i));
			}
			Log.infoSep();
		}

		return b;
	}

	protected void removeEmptyNodes(Batch b, Graph g,
			HashMap<Integer, Integer> nodeDegreeMap) {
		Iterator<IElement> iterator = g.getNodes().iterator();
		while (iterator.hasNext()) {
			Node n = (Node) iterator.next();
			int index = n.getIndex();
			int degree = n.getDegree();

			if (nodeDegreeMap.containsKey(index))
				degree += nodeDegreeMap.get(index);

			if (degree <= 0) {
				reader.removeNode("" + index);
				b.add(new NodeRemoval(n));
			}
		}
	}

	protected void handleEdgeWeights(Batch b, Graph g,
			HashMap<String, Integer> edgeWeightMap,
			HashMap<Integer, Integer> nodeDegreeMap) {
		for (String s : edgeWeightMap.keySet()) {
			String[] splits = s.split("->");
			changeEdgeWeights(b, g, Integer.parseInt(splits[0]),
					Integer.parseInt(splits[1]), edgeWeightMap.get(s),
					nodeDegreeMap);
		}
	}

	protected void addEdgeToBatch(Batch b, Graph g, ArrayList<Node> addedNodes,
			NetworkEdge ne, HashMap<Integer, Integer> nodeDegreeMap) {
		int src = ne.getSrc();
		int dst = ne.getDst();

		Node sNode = getNode(g, addedNodes, src);
		Node dNode = getNode(g, addedNodes, dst);

		IWeightedEdge e = (IWeightedEdge) g.getGraphDatastructures()
				.newEdgeInstance(sNode, dNode);
		e.setWeight(new IntWeight(1));

		b.add(new EdgeAddition(e));
		incrementDegreeChanges(src, nodeDegreeMap);
		incrementDegreeChanges(dst, nodeDegreeMap);
	}

	protected void changeEdgeWeights(Batch b, Graph g, int src, int dst,
			int weight, HashMap<Integer, Integer> nodeDegreeMap) {
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
				else {
					decrementDegreeChanges(src, nodeDegreeMap);
					decrementDegreeChanges(dst, nodeDegreeMap);
					b.add(new EdgeRemoval(edge));
				}
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
					(t + this.edgeLifeTimeMillis)));
			reader.addEdgeToQueue(new NetworkEdge(portMapping, dstIpMapping,
					(t + this.edgeLifeTimeMillis)));

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
				if (lastTime > w.getWeight() + edgeLifeTimeMillis)
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
