package dna.metrics.degree;

import dna.metrics.algorithms.IRecomputation;
import dna.series.data.distr.BinnedIntDistr;

public class WeightedDegreeDistributionR extends WeightedDegreeDistribution
		implements IRecomputation {

	protected BinnedIntDistr inOutDegree;

	public WeightedDegreeDistributionR() {
		super("WeightedDegreeDistributionR");
	}

	public WeightedDegreeDistributionR(String[] nodeTypes) {
		super("WeightedDegreeDistributionR", nodeTypes);
	}

	@Override
	public boolean recompute() {
		return this.compute();
	}

}
