package dna.graph.generators.network;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
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
	}

	@Override
	public Batch craftBatch(Graph g, DateTime timestamp,
			ArrayList<NetworkEvent> events,
			HashMap<String, Integer> edgeWeightChanges) {
		// init batch
		Batch b = new Batch(g.getGraphDatastructures(), g.getTimestamp(),
				TimeUnit.MILLISECONDS.toSeconds(timestamp.getMillis()), 0, 0,
				0, 0, 0, 0);
		Log.infoSep();
		Log.info(g.getTimestamp() + "\t->\t"
				+ TimeUnit.MILLISECONDS.toSeconds(timestamp.getMillis()));

		HashMap<Integer, Node> addedNodes = new HashMap<Integer, Node>();

		ArrayList<NetworkEdge> addedEdges = new ArrayList<NetworkEdge>();

		for (NetworkEvent networkEvent : events) {
			NetflowEvent event = (NetflowEvent) networkEvent;

			String sourceString = event.get(this.source);
			String destinationString = event.get(this.destination);

			String protString = event.getProtocol();

			if (sourceString.equals(destinationString)) {
				continue;
			}

			int sourceId = map(sourceString);
			int destinationId = map(destinationString);
			System.out.println(sourceId + "  <--  " + sourceString);
			String[] intermediateStrings = new String[this.intermediateNodes.length];
			int[] intermediateIds = new int[this.intermediateNodes.length];
			for (int i = 0; i < intermediateStrings.length; i++) {
				String intermediateString = event.get(intermediateNodes[i]);
				intermediateStrings[i] = intermediateString;
				intermediateIds[i] = map(intermediateString);
				System.out.println(intermediateIds[i] + "  <--  "
						+ intermediateString);
			}
			System.out.println(destinationId + "  <--  " + destinationString);

			Node srcNode = addNode(addedNodes, b, g, sourceId, ElementType.HOST);
			// b.add(new NodeAddition(srcNode));
			// Node[] intermediateNodes = new Node[intermediateIds.length];
			Node dstNode = addNode(addedNodes, b, g, destinationId,
					ElementType.HOST);
			// b.add(new NodeAddition(dstNode));

			if (intermediateIds.length >= 1) {
				addNode(addedNodes, b, g, intermediateIds[0], ElementType.PORT);
				addEdge(addedEdges, sourceId, intermediateIds[0], event
						.getTime().getMillis(), 1.0);

				addNode(addedNodes, b, g,
						intermediateIds[intermediateIds.length - 1],
						ElementType.PORT);
				addEdge(addedEdges,
						intermediateIds[intermediateIds.length - 1],
						destinationId, event.getTime().getMillis(), 1.0);

				for (int i = 0; i < intermediateIds.length - 1; i++) {
					addNode(addedNodes, b, g, intermediateIds[i],
							ElementType.PORT);
					addEdge(addedEdges, intermediateIds[i],
							intermediateIds[i + 1],
							event.getTime().getMillis(), 1.0);
				}
			} else {
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

			System.out.println("ADDING EDGE:   " + ne.toString());

		} else {
			w = (DoubleWeight) e.getWeight();

			b.add(new EdgeWeight(e, new DoubleWeight(w.getWeight()
					+ ne.getWeight())));

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
			int nodeToAdd, ElementType type) {
		System.out.println("attempt to add: " + nodeToAdd + "   contained? : "
				+ addedNodes.containsKey(nodeToAdd));

		if (addedNodes.containsKey(nodeToAdd)) {
			return addedNodes.get(nodeToAdd);
		} else {
			Node n = g.getNode(nodeToAdd);
			if (n != null) {
				System.out.println("!= NULL");
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

				System.out.println("putting " + nodeToAdd);
				addedNodes.put(nodeToAdd, n);
				b.add(new NodeAddition(n));

				return n;
			}
		}
	}

	public Node addNode(Graph g, int mapping, ElementType type) {
		Node n = g.getNode(mapping);
		if (n != null)
			return n;

		// init node
		n = g.getGraphDatastructures().newNodeInstance(mapping);

		// set type-weight
		if (g.getGraphDatastructures().getNodeWeightType()
				.equals(TypedWeight.class)) {
			((IWeightedNode) n).setWeight(new TypedWeight(type.toString()));
		}

		return n;
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

}
