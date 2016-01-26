package dna.graph.generators.network.weights;

import dna.graph.weights.Weight;

/**
 * Represents the different types a node can have inside a network-graph.
 * 
 * @author Rwilmes
 * 
 */
public class NetworkNodeWeight extends Weight {

	public enum NodeType {
		HOST, PORT, UNKNOWN
	};

	private NodeType weight;

	// constructor
	public NetworkNodeWeight(NodeType weight) {
		this.weight = weight;
	}

	public NetworkNodeWeight(String str) {
		if (str.equals("host"))
			this.weight = NodeType.HOST;
		if (str.equals("port"))
			this.weight = NodeType.PORT;
	}

	public NetworkNodeWeight(WeightSelection ws) {
		this.weight = NodeType.UNKNOWN;
	}

	public NodeType getWeight() {
		return weight;
	}

	public void setWeight(NodeType weight) {
		this.weight = weight;
	}

	@Override
	public String asString() {
		return this.weight.toString();
	}

}
