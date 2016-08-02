package dna.metrics.degree;

import dna.metrics.algorithms.IRecomputation;
import dna.series.data.distr.BinnedIntDistr;

public class WeightedDegreeDistributionMultiR extends
		WeightedDegreeDistribution implements IRecomputation {

	protected BinnedIntDistr inOutDegree;

	protected int index;
	protected String name;

	public WeightedDegreeDistributionMultiR(int index, double binsize) {
		this("WeightedDegreeDistributionR-" + index, index, null, (int) Math
				.ceil(binsize));
	}

	public WeightedDegreeDistributionMultiR(String name, int index,
			String[] nodeTypes, double binsize) {
		super(name + (int) Math.ceil(binsize), nodeTypes, (int) Math
				.ceil(binsize));
		this.name = name;
		this.index = index;
	}

	@Override
	public boolean recompute() {
		return this.compute();
	}

}
