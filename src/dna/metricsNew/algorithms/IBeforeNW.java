package dna.metricsNew.algorithms;

import dna.updates.update.NodeWeight;

public interface IBeforeNW extends IDynamicAlgorithm {
	public boolean applyBeforeUpdate(NodeWeight nw);
}
