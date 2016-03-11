package dna.metrics.degree;

import java.util.Iterator;

import dna.graph.Graph;
import dna.graph.IElement;
import dna.graph.nodes.DirectedNode;
import dna.graph.nodes.UndirectedNode;
import dna.graph.weights.IWeightedEdge;
import dna.graph.weights.Weight;
import dna.graph.weights.intW.IntWeight;
import dna.metrics.algorithms.IRecomputation;
import dna.series.data.distr.BinnedIntDistr;
import dna.updates.batch.Batch;

public class WeightedDegreeDistributionR extends DegreeDistribution implements
		IRecomputation {

	protected BinnedIntDistr inOutDegree;

	public WeightedDegreeDistributionR() {
		super("WeightedDegreeDistributionR");
	}

	public WeightedDegreeDistributionR(String[] nodeTypes) {
		super("WeightedDegreeDistributionR", nodeTypes);
	}

	@Override
	public boolean isApplicable(Graph g) {
		return g.getGraphDatastructures().isEdgeType(IWeightedEdge.class)
				&& g.getGraphDatastructures().isEdgeWeightType(IntWeight.class);
	}

	@Override
	public boolean isApplicable(Batch b) {
		return b.getGraphDatastructures().isEdgeType(IWeightedEdge.class)
				&& b.getGraphDatastructures().isEdgeWeightType(IntWeight.class);
	}

	@Override
	public boolean recompute() {
		return this.compute();
	}

	@Override
	protected boolean compute() {
		if (this.g.isDirected()) {
			this.degree = new BinnedIntDistr("DegreeDistribution");
			this.inDegree = new BinnedIntDistr("InDegreeDistribution");
			this.outDegree = new BinnedIntDistr("OutDegreeDistribution");
			for (IElement n_ : this.getNodesOfAssignedTypes()) {
				DirectedNode n = (DirectedNode) n_;
				int inWeight = 0;
				int outWeight = 0;

				// out-going
				Iterator<IElement> iterator = n.getOutgoingEdges().iterator();
				while (iterator.hasNext()) {
					Weight w = ((IWeightedEdge) iterator.next()).getWeight();
					if (w instanceof IntWeight)
						outWeight += ((IntWeight) w).getWeight();
				}

				// incoming
				iterator = n.getIncomingEdges().iterator();
				while (iterator.hasNext()) {
					Weight w = ((IWeightedEdge) iterator.next()).getWeight();
					if (w instanceof IntWeight)
						inWeight += ((IntWeight) w).getWeight();
				}

				this.degree.incr(n.getDegree(), (outWeight + inWeight));
				this.inDegree.incr(n.getInDegree(), inWeight);
				this.outDegree.incr(n.getOutDegree(), outWeight);
			}
		} else {
			this.degree = new BinnedIntDistr("DegreeDistribution");
			this.inDegree = null;
			this.outDegree = null;
			for (IElement n_ : this.getNodesOfAssignedTypes()) {
				UndirectedNode n = (UndirectedNode) n_;
				int weight = 0;
				Iterator<IElement> iterator = n.getEdges().iterator();
				while (iterator.hasNext()) {
					Weight w = ((IWeightedEdge) iterator.next()).getWeight();
					if (w instanceof IntWeight)
						weight += ((IntWeight) w).getWeight();
				}

				this.degree.incr(n.getDegree(), weight);
			}
		}
		return true;
	}

}
