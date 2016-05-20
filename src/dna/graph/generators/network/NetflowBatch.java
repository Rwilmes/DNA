package dna.graph.generators.network;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;

import dna.graph.Graph;
import dna.graph.IElement;
import dna.graph.generators.network.weights.NetworkWeight.ElementType;
import dna.graph.nodes.Node;
import dna.graph.weights.IWeightedEdge;
import dna.graph.weights.IWeightedNode;
import dna.graph.weights.TypedWeight;
import dna.graph.weights.doubleW.DoubleWeight;
import dna.updates.batch.Batch;
import dna.updates.update.EdgeAddition;
import dna.updates.update.EdgeRemoval;
import dna.updates.update.EdgeWeight;
import dna.updates.update.NodeAddition;
import dna.updates.update.NodeRemoval;
import dna.util.Log;
import dna.util.network.NetworkEvent;
import dna.util.network.netflow.NetflowEvent;
import dna.util.network.netflow.NetflowEvent.NetflowDirection;
import dna.util.network.netflow.NetflowEvent.NetflowEventField;
import dna.util.network.netflow.NetflowEventReader;

public class NetflowBatch extends NetworkBatch2 {

	protected NetflowEventField source;
	protected NetflowEventField destination;
	protected NetflowEventField[] intermediateNodes;
	protected NetflowEventField[] nodeWeights;
	protected NetflowEventField[] edgeWeights;

	protected NetflowEventField[] forward;
	protected NetflowEventField[] backward;

	public NetflowBatch(String name, NetflowEventReader reader,
			NetflowEventField[] forward, NetflowEventField[] backward,
			NetflowEventField[] edgeWeights, NetflowEventField[] nodeWeights)
			throws FileNotFoundException {
		super(name, reader, reader.getBatchIntervalSeconds());
		this.forward = forward;
		this.backward = backward;
		this.edgeWeights = edgeWeights;
		this.nodeWeights = nodeWeights;
		this.map = new HashMap<String, Integer>();
		this.intermediateMap = new HashMap<String, Integer>();
		this.counter = 0;
	}

	@Override
	public Batch craftBatch(Graph g, DateTime timestamp,
			ArrayList<NetworkEvent> events,
			ArrayList<NetworkEdge> decrementEdges,
			HashMap<String, Integer> edgeWeightChanges) {
		// init batch
		Batch b = new Batch(g.getGraphDatastructures(), g.getTimestamp(),
				TimeUnit.MILLISECONDS.toSeconds(timestamp.getMillis()), 0, 0,
				0, 0, 0, 0);

		HashMap<Integer, Integer> nodesDegreeMap = new HashMap<Integer, Integer>();

		HashMap<Integer, Node> addedNodes = new HashMap<Integer, Node>();

		ArrayList<NetworkEdge> addedEdges = new ArrayList<NetworkEdge>();

		for (NetworkEvent networkEvent : events) {
			NetflowEvent event = (NetflowEvent) networkEvent;

			if (event.getSrcAddress().equals(event.getDstAddress())
					|| event.getDirection() == null)
				continue;

			NetflowDirection direction = event.getDirection();

			switch (direction) {
			case backward:
				Log.warn("BACKWARD FLOW??");
				break;
			case bidirectional:
				processEvents(event, this.forward, addedNodes, addedEdges, b, g);
				processEvents(event, this.backward, addedNodes, addedEdges, b,
						g);
				break;
			case forward:
				processEvents(event, this.forward, addedNodes, addedEdges, b, g);
				break;
			}
		}

		for (Integer nodeId : addedNodes.keySet()) {
			nodesDegreeMap.put(nodeId, 0);
		}

		for (NetworkEdge dne : decrementEdges) {
			boolean present = false;
			for (NetworkEdge ne : addedEdges) {
				if (ne.getSrc() == dne.getSrc() && ne.getDst() == dne.getDst()) {
					present = true;
					break;
				}
			}

			if (!present)
				addedEdges.add(new NetworkEdge(dne.getSrc(), dne.getDst(), 0,
						0.0));
		}

		for (NetworkEdge ne : addedEdges) {
			NetworkEdge decrementNe = new NetworkEdge(ne.getSrc(), ne.getDst(),
					0, 0.0);
			for (NetworkEdge dne : decrementEdges) {
				if (ne.getSrc() == dne.getSrc() && ne.getDst() == dne.getDst()) {
					decrementNe = dne;
					break;
				}
			}

			addEdgeToBatch(b, g, ne, decrementNe, addedNodes, nodesDegreeMap);
		}

		// compute node degrees and delete zero degree nodes
		computeNodeDegrees(nodesDegreeMap, g, b);

		return b;
	}

	protected void computeNodeDegrees(HashMap<Integer, Integer> nodeDegreeMap,
			Graph g, Batch b) {
		Iterator<IElement> ite = g.getNodes().iterator();
		while (ite.hasNext()) {
			Node n = (Node) ite.next();
			int index = n.getIndex();

			incrementNodeDegree(nodeDegreeMap, index, n.getDegree());

			int degree = nodeDegreeMap.get(index);

			if (degree == 0) {
				if (reader instanceof NetflowEventReader
						&& ((NetflowEventReader) reader)
								.isRemoveZeroDegreeNodes())
					b.add(new NodeRemoval(n));
			}
		}
	}

	protected void processEvents(NetflowEvent event,
			NetflowEventField[] eventFields, HashMap<Integer, Node> addedNodes,
			ArrayList<NetworkEdge> addedEdges, Batch b, Graph g) {
		if (eventFields == null || eventFields.length < 2)
			return;

		for (int i = 0; i < eventFields.length - 1; i++) {
			String string0 = event.get(eventFields[i]);
			String string1 = event.get(eventFields[i + 1]);

			if (string0 == null || string0.equals("null")) {
				if (eventFields[i].equals(NetflowEventField.DstPort)
						|| eventFields[i].equals(NetflowEventField.SrcPort)) {
					string0 = event.get(NetflowEventField.Protocol);
				}
			}
			if (string1 == null || string1.equals("null")) {
				if (eventFields[i + 1].equals(NetflowEventField.DstPort)
						|| eventFields[i + 1].equals(NetflowEventField.SrcPort)) {
					string1 = event.get(NetflowEventField.Protocol);
				}
			}

			int mapping0 = map(string0);
			int mapping1 = map(string1);

			// add node i and i+1
			addNode(addedNodes, b, g, mapping0, eventFields[i]);
			addNode(addedNodes, b, g, mapping1, eventFields[i + 1]);

			// add edge node i --> node i+1
			addEdge(addedEdges, mapping0, mapping1, b.getTo() * 1000, 1.0);
		}
	}

	protected void addEdgeToBatch(Batch b, Graph g, NetworkEdge ne,
			NetworkEdge dne, HashMap<Integer, Node> addedNodes,
			HashMap<Integer, Integer> nodeDegreeMap) {
		Node srcNode = g.getNode(ne.getSrc());
		if (srcNode == null)
			srcNode = addedNodes.get(ne.getSrc());

		Node dstNode = g.getNode(ne.getDst());
		if (dstNode == null)
			dstNode = addedNodes.get(ne.getDst());

		IWeightedEdge e = (IWeightedEdge) g.getEdge(srcNode, dstNode);
		DoubleWeight w = new DoubleWeight(ne.getWeight() + dne.getWeight());

		if (e == null) {
			e = (IWeightedEdge) g.getGraphDatastructures().newEdgeInstance(
					srcNode, dstNode);
			e.setWeight(w);
			b.add(new EdgeAddition(e));
			incrementNodeDegree(nodeDegreeMap, ne.getSrc());
			incrementNodeDegree(nodeDegreeMap, ne.getDst());

			if (reader instanceof NetflowEventReader)
				addEdgeWeightDecrementalToQueue((NetflowEventReader) reader, ne);
		} else {
			w = (DoubleWeight) e.getWeight();

			DoubleWeight wNew = new DoubleWeight(w.getWeight() + ne.getWeight()
					+ dne.getWeight());

			if (wNew.getWeight() == 0) {
				b.add(new EdgeRemoval(e));
				decrementNodeDegree(nodeDegreeMap, ne.getSrc());
				decrementNodeDegree(nodeDegreeMap, ne.getDst());
			} else {
				b.add(new EdgeWeight(e, wNew));
			}

			if (ne.getWeight() > 0)
				addEdgeWeightDecrementalToQueue((NetflowEventReader) reader, ne);
		}
	}

	protected void decrementNodeDegree(HashMap<Integer, Integer> nodeDegreeMap,
			int nodeId) {
		incrementNodeDegree(nodeDegreeMap, nodeId, -1);
	}

	protected void incrementNodeDegree(HashMap<Integer, Integer> nodeDegreeMap,
			int nodeId) {
		incrementNodeDegree(nodeDegreeMap, nodeId, 1);
	}

	protected void incrementNodeDegree(HashMap<Integer, Integer> nodeDegreeMap,
			int nodeId, int count) {
		if (nodeDegreeMap.containsKey(nodeId)) {
			nodeDegreeMap.put(nodeId, nodeDegreeMap.get(nodeId) + count);
		} else {
			nodeDegreeMap.put(nodeId, count);
		}
	}

	protected void addEdgeWeightDecrementalToQueue(NetflowEventReader r,
			NetworkEdge e) {
		r.addEdgeToQueue(new NetworkEdge(e.getSrc(), e.getDst(),
				(e.getTime() + r.getEdgeLifeTimeSeconds() * 1000), e
						.getWeight() * -1));
	}

	protected void addEdge(ArrayList<NetworkEdge> addedEdges, NetworkEdge edge) {
		addEdge(addedEdges, edge.getSrc(), edge.getDst(), edge.getTime(),
				edge.getWeight());
	}

	protected void addEdge(ArrayList<NetworkEdge> addedEdges, int src, int dst,
			long time, double weight) {
		boolean alreadyAdded = false;
		for (NetworkEdge ne : addedEdges) {
			if (ne.getSrc() == src && ne.getDst() == dst) {
				alreadyAdded = true;
				ne.setWeight(ne.getWeight() + weight);
			}
		}

		if (!alreadyAdded) {
			addedEdges.add(new NetworkEdge(src, dst, time, weight));
		}
	}

	protected Node addNode(HashMap<Integer, Node> addedNodes, Batch b, Graph g,
			int nodeToAdd, NetflowEventField type) {
		ElementType eType = ElementType.UNKNOWN;

		switch (type) {
		case DstAddress:
			eType = ElementType.HOST;
			break;
		case SrcAddress:
			eType = ElementType.HOST;
			break;
		case DstPort:
			eType = ElementType.PORT;
			break;
		case SrcPort:
			eType = ElementType.PORT;
			break;
		case Protocol:
			eType = ElementType.PROT;
			break;
		}

		return addNode(addedNodes, b, g, nodeToAdd, eType);
	}

	protected Node addNode(HashMap<Integer, Node> addedNodes, Batch b, Graph g,
			int nodeToAdd, ElementType type) {
		if (addedNodes.containsKey(nodeToAdd)) {
			return addedNodes.get(nodeToAdd);
		} else {
			Node n = g.getNode(nodeToAdd);
			if (n != null) {
				return n;
			} else {
				// init node
				n = g.getGraphDatastructures().newNodeInstance(nodeToAdd);

				// set type-weight
				if (g.getGraphDatastructures().getNodeWeightType()
						.equals(TypedWeight.class)) {
					((IWeightedNode) n).setWeight(new TypedWeight(type
							.toString()));
				}

				addedNodes.put(nodeToAdd, n);
				b.add(new NodeAddition(n));

				return n;
			}
		}
	}

	protected int map(String key) {
		if (this.map.keySet().contains(key))
			return this.map.get(key);
		else {
			this.map.put(key, this.counter);
			this.counter++;
			return (this.counter - 1);
		}
	}

	protected int mapIntermediate(String key) {
		if (this.intermediateMap.keySet().contains(key))
			return this.intermediateMap.get(key);
		else {
			this.intermediateMap.put(key, this.counter);
			this.counter++;
			return (this.counter - 1);
		}
	}

	protected int counter;

	protected HashMap<String, Integer> map;

	protected HashMap<String, Integer> intermediateMap;

	public String getKey(Integer mapping) {
		Set<String> keys = map.keySet();

		for (String key : keys) {
			if (map.get(key) == mapping)
				return key;
		}

		keys = intermediateMap.keySet();

		for (String key : keys) {
			if (map.get(key) == mapping)
				return key;
		}

		return "unknown";
	}
}
