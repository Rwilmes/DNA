package dna.graph.weights.multi;

import dna.graph.weights.Weight;
import dna.graph.weights.doubleW.DoubleWeight;

public class NetworkEdgeWeight extends Weight {

	protected DoubleWeight[] weights;

	public NetworkEdgeWeight(int numberOfWeights) {
		this(new DoubleWeight[numberOfWeights]);
	}

	public NetworkEdgeWeight(DoubleWeight[] weights) {
		this.weights = weights;
	}

	public DoubleWeight[] getWeights() {
		return weights;
	}

	public DoubleWeight getWeight(int index) {
		return weights[index];
	}

	@Override
	public String asString() {
		return "NetworkEdgeWeight-" + this.weights.length;
	}

}
