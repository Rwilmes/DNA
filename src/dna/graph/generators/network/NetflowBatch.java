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
import dna.graph.weights.multi.NetworkMultiWeight;
import dna.updates.batch.Batch;
import dna.updates.update.EdgeAddition;
import dna.updates.update.EdgeRemoval;
import dna.updates.update.EdgeWeight;
import dna.updates.update.NodeAddition;
import dna.updates.update.NodeRemoval;
import dna.updates.update.NodeWeight;
import dna.util.network.NetworkEvent;
import dna.util.network.netflow.NetflowEvent;
import dna.util.network.netflow.NetflowEvent.NetflowDirection;
import dna.util.network.netflow.NetflowEvent.NetflowEventField;
import dna.util.network.netflow.NetflowEventReader;

public class NetflowBatch extends NetworkBatch2 {

	public enum NodeWeightMode {
		none, both, srcOnly, dstOnly
	};

	// protected NetflowEventField source;
	// protected NetflowEventField destination;
	// protected NetflowEventField[] intermediateNodes;

	protected NetflowEventField[][] edges;
	protected NetflowDirection[] edgeDirections;
	protected NetflowEventField[][] edgeWeights;
	protected NetflowEventField[][] nodeWeights;

	// protected NetflowEventField[] forwardEdgeWeights;
	// protected NetflowEventField[] backwardEdgeWeights;

	// protected NetflowEventField[] forward;
	// protected NetflowEventField[] backward;

	public NetflowBatch(String name, NetflowEventReader reader,
			NetflowEventField[][] edges, NetflowDirection[] edgeDirections,
			NetflowEventField[][] edgeWeights, NetflowEventField[][] nodeWeights)
			throws FileNotFoundException {
		super(name, reader, reader.getBatchIntervalSeconds());
		this.edges = edges;
		this.edgeDirections = edgeDirections;
		this.edgeWeights = edgeWeights;
		// this.forward = forward;
		// this.backward = backward;
		// this.forwardEdgeWeights = forwardEdgeWeights;
		// this.backwardEdgeWeights = backwardEdgeWeights;
		this.nodeWeights = nodeWeights;
		this.map = new HashMap<String, Integer>();
		this.intermediateMap = new HashMap<String, Integer>();
		this.counter = 0;
	}

	@Override
	public Batch craftBatch(Graph g, DateTime timestamp,
			ArrayList<NetworkEvent> events,
			ArrayList<UpdateEvent> decrementEvents,
			HashMap<String, Integer> edgeWeightChanges) {
		// init batch
		Batch b = new Batch(g.getGraphDatastructures(), g.getTimestamp(),
				TimeUnit.MILLISECONDS.toSeconds(timestamp.getMillis()), 0, 0,
				0, 0, 0, 0);

		HashMap<Integer, Integer> nodesDegreeMap = new HashMap<Integer, Integer>();

		HashMap<Integer, Node> addedNodes = new HashMap<Integer, Node>();

		HashMap<Integer, double[]> nodesWeightMap = new HashMap<Integer, double[]>();

		ArrayList<NetworkEdge> addedEdges = new ArrayList<NetworkEdge>();

		ArrayList<NetworkEdge> decrementEdges = new ArrayList<NetworkEdge>();
		ArrayList<NodeUpdate> decrementNodes = new ArrayList<NodeUpdate>();
		for (UpdateEvent ue : decrementEvents) {
			if (ue instanceof NetworkEdge)
				decrementEdges.add((NetworkEdge) ue);
			if (ue instanceof NodeUpdate)
				decrementNodes.add((NodeUpdate) ue);
		}

		for (NetworkEvent networkEvent : events) {
			NetflowEvent event = (NetflowEvent) networkEvent;

			if (event.getSrcAddress().equals(event.getDstAddress())
					|| event.getDirection() == null)
				continue;

			NetflowDirection direction = event.getDirection();

			for (int i = 0; i < this.edgeDirections.length; i++) {
				NetflowDirection edgeDir = this.edgeDirections[i];

				if (edgeDir.equals(direction)
						|| direction.equals(NetflowDirection.bidirectional)) {
					processEvents(event, this.edges[i], this.edgeWeights[i],
							this.nodeWeights[i], addedNodes, nodesWeightMap,
							addedEdges, b, g);
				}
			}
			//
			// switch (direction) {
			// case backward:
			// processBackward(event, addedNodes, addedEdges, b, g);
			// // processEvents(event, this.backward, this.backwardEdgeWeights,
			// // addedNodes, addedEdges, b, g);
			// break;
			// case bidirectional:
			// processBidirectional(event, addedNodes, addedEdges, b, g);
			// // processEvents(event, this.forward, this.forwardEdgeWeights,
			// // addedNodes, addedEdges, b, g);
			// // processEvents(event, this.backward, this.backwardEdgeWeights,
			// // addedNodes, addedEdges, b, g);
			// break;
			// case forward:
			// processForward(event, addedNodes, addedEdges, b, g);
			// // processEvents(event, this.forward, this.forwardEdgeWeights,
			// // addedNodes, addedEdges, b, g);
			// break;
			// }
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
						new double[0]));
		}

		for (NetworkEdge ne : addedEdges) {
			NetworkEdge decrementNe = new NetworkEdge(ne.getSrc(), ne.getDst(),
					0, new double[0]);
			for (NetworkEdge dne : decrementEdges) {
				if (ne.getSrc() == dne.getSrc() && ne.getDst() == dne.getDst()) {
					decrementNe = dne;
					break;
				}
			}

			addEdgeToBatch(b, g, ne, decrementNe, addedNodes, nodesDegreeMap);
		}

		// compute node degrees and weights and delete zero degree nodes
		computeNodeDegreesAndWeights(addedNodes, nodesDegreeMap,
				nodesWeightMap, decrementNodes, g, b);

		return b;
	}

	protected double[] getZeroArray(int length) {
		double[] array = new double[length];
		for (int i = 0; i < array.length; i++)
			array[i] = 0.0;
		return array;
	}

	protected void computeNodeDegreesAndWeights(
			HashMap<Integer, Node> addedNodes,
			HashMap<Integer, Integer> nodeDegreeMap,
			HashMap<Integer, double[]> nodesWeightMap,
			ArrayList<NodeUpdate> decrementNodeUpdates, Graph g, Batch b) {
		// lit of all nodes to be updated
		ArrayList<Integer> nodes = new ArrayList<Integer>();

		for (Integer index : addedNodes.keySet()) {
			if (!nodes.contains(index))
				nodes.add(index);
		}

		for (Integer index : nodesWeightMap.keySet()) {
			if (!nodes.contains(index))
				nodes.add(index);
		}

		for (NodeUpdate nu : decrementNodeUpdates) {
			if (!nodes.contains(nu.getIndex()))
				nodes.add(nu.getIndex());
		}

		Iterator<IElement> ite = g.getNodes().iterator();
		while (ite.hasNext()) {
			Node n = (Node) ite.next();
			int index = n.getIndex();
			incrementNodeDegree(nodeDegreeMap, index, n.getDegree());

			int degree = nodeDegreeMap.get(index);

			if (degree == 0) {
				if (reader instanceof NetflowEventReader
						&& ((NetflowEventReader) reader)
								.isRemoveZeroDegreeNodes()) {
					// remove node from graph
					b.add(new NodeRemoval(n));

					// remove node index from considered nodes
					if (nodes.contains(index))
						nodes.remove(new Integer(index));
				}
			} else {
				if (!nodes.contains(index))
					nodes.add(index);
			}
		}

		for (Integer index : nodes) {
			boolean added = addedNodes.containsKey(index);
			boolean weightChanged = nodesWeightMap.containsKey(index);
			boolean updated = false;
			for (NodeUpdate nu : decrementNodeUpdates) {
				if (nu.getIndex() == index)
					updated = true;
			}

			// get node from graph
			Node n = g.getNode(index);

			// if not in graph it must be newly added
			if (n == null)
				n = addedNodes.get(index);

			IWeightedNode wn = (IWeightedNode) n;
			NetworkMultiWeight oldW = (NetworkMultiWeight) wn.getWeight();

			if (added) {
				double[] weightChanges = nodesWeightMap.get(index);
				addNodeWeightDecrementalToQueue((NetflowEventReader) reader,
						new NodeUpdate(index, b.getTo() * 1000, weightChanges));
			} else {
				if (weightChanged) {
					// case: node not newly added, weight changed during batch
					// and updated
					double[] weightChanges = nodesWeightMap.get(index);
					addNodeWeightDecrementalToQueue(
							(NetflowEventReader) reader, new NodeUpdate(index,
									b.getTo() * 1000, weightChanges));

					double[] decrement = new double[weightChanges.length];
					for (NodeUpdate nu : decrementNodeUpdates) {
						if (nu.getIndex() == index) {
							decrement = nu.getUpdates();
							break;
						}
					}

					weightChanges = addition(weightChanges, decrement);

					NetworkMultiWeight newW = NetworkMultiWeight.addition(oldW,
							weightChanges);

					b.add(new NodeWeight(wn, newW));
				} else {
					if (updated) {
						// case: node not newly added, update
						double[] decrement = null;

						for (NodeUpdate nu : decrementNodeUpdates) {
							if (nu.getIndex() == index) {
								decrement = nu.getUpdates();
								break;
							}
						}

						NetworkMultiWeight newW = NetworkMultiWeight.addition(
								oldW, decrement);
						b.add(new NodeWeight(wn, newW));
					}
				}
			}
		}
	}

	protected void processEvents(NetflowEvent event,
			NetflowEventField[] eventFields, NetflowEventField[] edgeWeights,
			NetflowEventField[] nodeWeights, HashMap<Integer, Node> addedNodes,
			HashMap<Integer, double[]> nodeWeightMap,
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

			// get node weights
			double[] nw = new double[nodeWeights.length];
			for (int j = 0; j < nw.length; j++) {
				nw[j] = Double.parseDouble(event.get(nodeWeights[j]));
			}

			double[] srcWeights;
			double[] dstWeights;

			if (eventFields.length % 2 == 0) {
				if (i % 2 == 0) {
					srcWeights = nw;
					dstWeights = nw;
				} else {
					srcWeights = new double[nw.length];
					dstWeights = new double[nw.length];
				}
			} else {
				if (i % 2 == 0) {
					srcWeights = nw;
					dstWeights = nw;
				} else if (i == eventFields.length - 2) {
					srcWeights = new double[nw.length];
					dstWeights = nw;
				} else {
					srcWeights = new double[nw.length];
					dstWeights = new double[nw.length];
				}
			}

			// add node i and i+1
			addNode(addedNodes, nodeWeightMap, b, g, mapping0, eventFields[i],
					srcWeights);
			addNode(addedNodes, nodeWeightMap, b, g, mapping1,
					eventFields[i + 1], dstWeights);

			// get edge weights
			double[] ew = new double[edgeWeights.length];
			for (int j = 0; j < ew.length; j++) {
				ew[j] = Double.parseDouble(event.get(edgeWeights[j]));
			}

			// add edge node i --> node i+1
			addEdge(addedEdges, mapping0, mapping1, b.getTo() * 1000, ew);
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

		DoubleWeight[] w = new DoubleWeight[Math.max(
				ne.getEdgeWeights().length, dne.getEdgeWeights().length)];
		for (int i = 0; i < w.length; i++) {
			double neWeight = (i < ne.getEdgeWeights().length) ? ne
					.getEdgeWeights()[i] : 0;
			double dneWeight = (i < dne.getEdgeWeights().length) ? dne
					.getEdgeWeights()[i] : 0;

			w[i] = new DoubleWeight(neWeight + dneWeight);
		}

		// check if edge exists, if not create new edge instance
		if (e == null) {
			e = (IWeightedEdge) g.getGraphDatastructures().newEdgeInstance(
					srcNode, dstNode);
			e.setWeight(w[0]);
			b.add(new EdgeAddition(e));
			incrementNodeDegree(nodeDegreeMap, ne.getSrc());
			incrementNodeDegree(nodeDegreeMap, ne.getDst());

			if (reader instanceof NetflowEventReader)
				addEdgeWeightDecrementalToQueue((NetflowEventReader) reader, ne);
		} else {
			w[0] = (DoubleWeight) e.getWeight();

			DoubleWeight[] wNew = new DoubleWeight[Math.max(
					ne.getEdgeWeights().length, dne.getEdgeWeights().length)];
			for (int i = 0; i < wNew.length; i++) {
				double neWeight = (i < ne.getEdgeWeights().length) ? ne
						.getEdgeWeights()[i] : 0;
				double dneWeight = (i < dne.getEdgeWeights().length) ? dne
						.getEdgeWeights()[i] : 0;

				wNew[i] = new DoubleWeight(w[i].getWeight() + neWeight
						+ dneWeight);
			}

			if (wNew[0].getWeight() == 0) {
				b.add(new EdgeRemoval(e));
				decrementNodeDegree(nodeDegreeMap, ne.getSrc());
				decrementNodeDegree(nodeDegreeMap, ne.getDst());
			} else {
				b.add(new EdgeWeight(e, wNew[0]));
			}

			if (ne.getEdgeWeights().length > 0 && ne.getEdgeWeights()[0] > 0)
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

	protected void addNodeWeightDecrementalToQueue(NetflowEventReader r,
			NodeUpdate update) {
		double[] nodeWeightsArray = new double[update.getUpdates().length];
		for (int i = 0; i < nodeWeightsArray.length; i++) {
			nodeWeightsArray[i] = (-1) * update.getUpdates()[i];
		}

		r.addUpdateEventToQueue(new NodeUpdate(update.getIndex(), update
				.getTime() + r.getEdgeLifeTimeSeconds() * 1000,
				nodeWeightsArray));
	}

	protected void addEdgeWeightDecrementalToQueue(NetflowEventReader r,
			NetworkEdge e) {
		double[] edgeWeightsArray = new double[e.getEdgeWeights().length];
		for (int i = 0; i < edgeWeightsArray.length; i++) {
			edgeWeightsArray[i] = (-1) * e.getEdgeWeights()[i];
		}

		r.addUpdateEventToQueue(new NetworkEdge(e.getSrc(), e.getDst(), (e
				.getTime() + r.getEdgeLifeTimeSeconds() * 1000),
				edgeWeightsArray));
	}

	protected void addEdge(ArrayList<NetworkEdge> addedEdges, int src, int dst,
			long time, double[] edgeWeights) {
		boolean alreadyAdded = false;
		for (NetworkEdge ne : addedEdges) {
			if (ne.getSrc() == src && ne.getDst() == dst) {
				alreadyAdded = true;

				// compute new edge weights
				double[] edgeWeightsNew = ne.getEdgeWeights();
				for (int i = 0; i < edgeWeightsNew.length; i++)
					edgeWeightsNew[i] += edgeWeights[i];
				ne.setEdgeWeights(edgeWeightsNew);
			}
		}

		if (!alreadyAdded) {
			addedEdges.add(new NetworkEdge(src, dst, time, edgeWeights));
		}
	}

	protected Node addNode(HashMap<Integer, Node> addedNodes,
			HashMap<Integer, double[]> nodeWeightsMap, Batch b, Graph g,
			int nodeToAdd, NetflowEventField type, double[] nodeWeights) {
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

		return addNode(addedNodes, nodeWeightsMap, b, g, nodeToAdd, eType,
				nodeWeights);
	}

	protected Node addNode(HashMap<Integer, Node> addedNodes,
			HashMap<Integer, double[]> nodeWeightsMap, Batch b, Graph g,
			int nodeToAdd, ElementType type, double[] nodeWeights) {
		if (addedNodes.containsKey(nodeToAdd)) {
			IWeightedNode n = (IWeightedNode) addedNodes.get(nodeToAdd);
			NetworkMultiWeight nmw = NetworkMultiWeight.addition(
					(NetworkMultiWeight) n.getWeight(), nodeWeights);
			n.setWeight(nmw);
			nodeWeightsMap.put(nodeToAdd, nmw.getDoubles());
			return (Node) n;
		} else {
			Node n = g.getNode(nodeToAdd);
			if (n != null) {
				if (nodeWeightsMap.containsKey(nodeToAdd))
					nodeWeightsMap
							.put(nodeToAdd,
									addition(nodeWeightsMap.get(nodeToAdd),
											nodeWeights));
				else
					nodeWeightsMap.put(nodeToAdd, nodeWeights);
				return n;
			} else {
				// init node
				n = g.getGraphDatastructures().newNodeInstance(nodeToAdd);

				// set type-weight
				TypedWeight typeW = new TypedWeight(type.toString());
				((IWeightedNode) n).setWeight(new NetworkMultiWeight(typeW,
						nodeWeights));

				nodeWeightsMap.put(nodeToAdd, nodeWeights);
				addedNodes.put(nodeToAdd, n);
				b.add(new NodeAddition(n));
				return n;
			}
		}
	}

	public double[] addition(double[] d1, double[] d2) {
		double[] d3 = new double[Math.max(d1.length, d2.length)];
		for (int i = 0; i < d3.length; i++) {
			double do1 = (i < d1.length ? d1[i] : 0.0);
			double do2 = (i < d2.length ? d2[i] : 0.0);
			d3[i] = do1 + do2;
		}
		return d3;
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
