package dna.graph.weights.multi;

import dna.graph.weights.TypedWeight;
import dna.graph.weights.doubleW.DoubleWeight;

public class NetworkNodeWeight extends TypedWeight {

	DoubleWeight[] weights;

	public NetworkNodeWeight(String type, DoubleWeight[] weights) {
		super(type);
		this.weights = weights;
	}

	public DoubleWeight[] getWeights() {
		return weights;
	}

	public DoubleWeight getWeight(int index) {
		return weights[index];
	}

}
