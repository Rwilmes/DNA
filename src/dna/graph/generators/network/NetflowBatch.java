package dna.graph.generators.network;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;

import dna.graph.Graph;
import dna.graph.generators.network.weights.NetworkWeight.ElementType;
import dna.graph.nodes.Node;
import dna.graph.weights.IWeightedEdge;
import dna.graph.weights.IWeightedNode;
import dna.graph.weights.TypedWeight;
import dna.graph.weights.doubleW.DoubleWeight;
import dna.updates.batch.Batch;
import dna.updates.update.EdgeAddition;
import dna.updates.update.EdgeWeight;
import dna.updates.update.NodeAddition;
import dna.util.Log;
import dna.util.network.NetworkEvent;
import dna.util.network.netflow.NetflowEvent;
import dna.util.network.netflow.NetflowEvent.NetflowEventField;
import dna.util.network.netflow.NetflowEventReader;

public class NetflowBatch extends NetworkBatch2 {

	protected NetflowEventField source;
	protected NetflowEventField destination;
	protected NetflowEventField[] intermediateNodes;
	protected NetflowEventField[] nodeWeights;
	protected NetflowEventField[] edgeWeights;

	public NetflowBatch(String name, NetflowEventReader reader,
			NetflowEventField source, NetflowEventField[] intermediateNodes,
			NetflowEventField destination, NetflowEventField[] edgeWeights,
			NetflowEventField[] nodeWeights) throws FileNotFoundException {
		super(name, reader, reader.getBatchIntervalSeconds());
		this.source = source;
		this.intermediateNodes = intermediateNodes;
		this.destination = destination;
		this.edgeWeights = edgeWeights;
		this.nodeWeights = nodeWeights;
		this.map = new HashMap<String, Integer>();
		this.intermediateMap = new HashMap<String, Integer>();
		this.counter = 0;
		this.debug = false;
	}

	@Override
	public Batch craftBatch(Graph g, DateTime timestamp,
			ArrayList<NetworkEvent> events,
			HashMap<String, Integer> edgeWeightChanges) {
		// init batch
		Batch b = new Batch(g.getGraphDatastructures(), g.getTimestamp(),
				TimeUnit.MILLISECONDS.toSeconds(timestamp.getMillis()), 0, 0,
				0, 0, 0, 0);
		if (debug) {
			Log.infoSep();
			Log.info(g.getTimestamp() + "\t->\t"
					+ TimeUnit.MILLISECONDS.toSeconds(timestamp.getMillis()));
		}

		HashMap<Integer, Node> addedNodes = new HashMap<Integer, Node>();

		ArrayList<NetworkEdge> addedEdges = new ArrayList<NetworkEdge>();

		for (NetworkEvent networkEvent : events) {
			NetflowEvent event = (NetflowEvent) networkEvent;

			String sourceString = event.get(this.source);
			String destinationString = event.get(this.destination);

			if (sourceString.equals(destinationString)) {
				continue;
			}

			int sourceId = map(sourceString);
			int destinationId = map(destinationString);
			if (debug)
				System.out.println(sourceId + "  <--  " + sourceString);
			String[] intermediateStrings = new String[this.intermediateNodes.length];
			int[] intermediateIds = new int[this.intermediateNodes.length];
			for (int i = 0; i < intermediateStrings.length; i++) {
				String intermediateString = event.get(intermediateNodes[i]);

				if (intermediateString == null
						|| intermediateString.equals("null"))
					intermediateString = event.getProtocol();

				intermediateStrings[i] = intermediateString;
				intermediateIds[i] = map(intermediateString);
				if (debug)
					System.out.println(intermediateIds[i] + "  <-i-  "
							+ intermediateString);
			}

			if (debug)
				System.out.println(destinationId + "  <--  "
						+ destinationString);

			// add source & destination node
			addNode(addedNodes, b, g, sourceId, ElementType.HOST);
			addNode(addedNodes, b, g, destinationId, ElementType.HOST);

			// add nodes
			if (intermediateIds.length >= 1) {
				// source --> intermediate 1
				addNode(addedNodes, b, g, intermediateIds[0],
						intermediateNodes[0]);
				addEdge(addedEdges, sourceId, intermediateIds[0], event
						.getTime().getMillis(), 1.0);

				// last intermediate --> destination
				addNode(addedNodes, b, g,
						intermediateIds[intermediateIds.length - 1],
						intermediateNodes[intermediateIds.length - 1]);
				addEdge(addedEdges,
						intermediateIds[intermediateIds.length - 1],
						destinationId, event.getTime().getMillis(), 1.0);

				// intermediate nodes
				for (int i = 0; i < intermediateIds.length - 1; i++) {
					addNode(addedNodes, b, g, intermediateIds[i],
							intermediateNodes[i]);
					addEdge(addedEdges, intermediateIds[i],
							intermediateIds[i + 1],
							event.getTime().getMillis(), 1.0);
				}
			} else {
				// add source --> destination
				addEdge(addedEdges, sourceId, destinationId, event.getTime()
						.getMillis(), 1.0);
			}
		}

		for (NetworkEdge ne : addedEdges) {
			addEdgeToBatch(b, g, ne, addedNodes);
		}

		return b;
	}

	protected void addEdgeToBatch(Batch b, Graph g, NetworkEdge ne,
			HashMap<Integer, Node> addedNodes) {
		if (debug)
			System.out.println("adding edge: " + ne.toString());
		Node srcNode = g.getNode(ne.getSrc());
		if (srcNode == null)
			srcNode = addedNodes.get(ne.getSrc());

		Node dstNode = g.getNode(ne.getDst());
		if (dstNode == null)
			dstNode = addedNodes.get(ne.getDst());

		IWeightedEdge e = (IWeightedEdge) g.getEdge(srcNode, dstNode);

		DoubleWeight w = new DoubleWeight(ne.getWeight());

		if (e == null) {
			e = (IWeightedEdge) g.getGraphDatastructures().newEdgeInstance(
					srcNode, dstNode);
			e.setWeight(w);
			b.add(new EdgeAddition(e));

			if (debug)
				System.out.println("ADDING EDGE:   " + ne.toString());

		} else {
			w = (DoubleWeight) e.getWeight();

			b.add(new EdgeWeight(e, new DoubleWeight(w.getWeight()
					+ ne.getWeight())));

			if (debug)
				System.out.println("INCREMENTING WEIGHT ON EDGE:    "
						+ ne.toString());
		}
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

		if (!alreadyAdded)
			addedEdges.add(new NetworkEdge(src, dst, time, weight));
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

	protected boolean debug;

	public void setDebug(boolean debug) {
		this.debug = debug;
	}
}
