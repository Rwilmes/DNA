package dna.metrics.assortativity;

import dna.graph.Graph;
import dna.graph.IElement;
import dna.graph.edges.DirectedEdge;
import dna.graph.edges.DirectedWeightedEdge;
import dna.graph.edges.Edge;
import dna.graph.edges.UndirectedEdge;
import dna.graph.edges.UndirectedWeightedEdge;
import dna.graph.nodes.DirectedNode;
import dna.graph.nodes.Node;
import dna.graph.nodes.UndirectedNode;
import dna.graph.weights.Weight;
import dna.graph.weights.doubleW.DoubleWeight;
import dna.graph.weights.intW.IntWeight;
import dna.metrics.IMetric;
import dna.metrics.Metric;
import dna.metrics.algorithms.IDynamicAlgorithm;
import dna.metrics.algorithms.IRecomputation;
import dna.series.data.Value;
import dna.series.data.distr.BinnedDoubleDistr;
import dna.series.data.distr.Distr;
import dna.series.data.nodevaluelists.NodeNodeValueList;
import dna.series.data.nodevaluelists.NodeValueList;
import dna.updates.batch.Batch;
import dna.util.DataUtils;
import dna.util.Log;
import dna.util.parameters.Parameter;
import dna.util.parameters.StringParameter;

/**
 * Implementation of <b>Assortativity Coefficient</b>. Assortartivity is also
 * known as "Homophily" or "assartative mixing (by degree)".
 * <p>
 * The assortativity coefficient is a {@code double} value >= -1 and <= 1. The
 * more nodes with equal degree tend to connect together, the closer this
 * coefficient is at 1. The more nodes with different degree tend to connect
 * together, the closer this coefficient is at -1. For directed graphs either
 * the node in- or outdegree is important (use the right constructor though).
 * </p>
 * <p>
 * To control the usage of edge weights in weighted graphs and to determine
 * whether compute coefficient for indegree or outdegree in directed graphs, use
 * Parameters.
 * </p>
 * 
 * @see AssortativityR This metric as {@link IRecomputation}.
 * @see AssortativityU This metric as {@link IDynamicAlgorithm}.
 */
public abstract class Assortativity extends Metric implements IMetric {

	/**
	 * Setting for {@link Parameter} "DirectedDegreeType".
	 */
	public static enum DirectedDegreeType {
		IN("In"), OUT("Out");

		private final StringParameter param;

		DirectedDegreeType(String value) {
			this.param = new StringParameter("DirectedDegreeType", value);
		}

		public StringParameter StringParameter() {
			return this.param;
		}
	}

	/**
	 * Setting for {@link Parameter} "EdgeWeightType".
	 */
	public static enum EdgeWeightType {
		IGNORE_WEIGHTS("Unweighted"), USE_WEIGHTS("Weighted");

		private final StringParameter param;

		EdgeWeightType(String value) {
			this.param = new StringParameter("EdgeWeightType", value);
		}

		public StringParameter StringParameter() {
			return this.param;
		}
	}

	/**
	 * To check equality of metrics in {@link #equals(IMetric)}, the
	 * assortativity coefficient {@link #r} is compared. This value is the
	 * allowed difference of two values to still accept them as equal.
	 */
	public static final double ACCEPTED_ERROR_FOR_EQUALITY = 1.0E-4;

	/**
	 * Is either "out" (default) or "in", depending on the {@link Parameter} in
	 * {@link #Assortativity(String, DirectedDegreeType, EdgeWeightType)}. This
	 * value determines whether nodes in directed graphs are compared by there
	 * in- or outdegree and is ignored for undirected graphs.
	 */
	DirectedDegreeType directedDegreeType;

	/**
	 * Is either "unweighted" (default) or "weighted", depending on the
	 * {@link Parameter} in
	 * {@link #Assortativity(String, DirectedDegreeType, EdgeWeightType)} . This
	 * value determines whether edge weights in weighted graphs are ignored not
	 * (will always be ignored for weighted graphs).
	 */
	EdgeWeightType edgeWeightType;

	/** The sum of all edge weights in the graph. */
	public double totalEdgeWeight;

	// For each edge between node u,v:
	// sum of {edgeWeight(u,v) * [weightedDegree(u) * weightedDegree(v)]}
	public double sum1;
	// sum of {edgeWeight(u,v) * [weightedDegree(u) + weightedDegree(v)]}
	public double sum2;
	// sum of {edgeWeight(u,v) * [weightedDegree(u)^2 + weightedDegree(v)^2]}
	public double sum3;

	/** The assortativity value, normally abbreviated with r. */
	public double r;

	/**
	 * Initializes {@link Assortativity}. Implicitly sets degree type for
	 * directed graphs to outdegree and ignores edge weights (if any).
	 * 
	 * @param name
	 *            The name of the metric, e.g. <i>AssortativityWeightedR</i> for
	 *            the Assortativity Recomputation and
	 *            <i>AssortativityWeightedU</i> for the Assortativity Updates.
	 */
	public Assortativity(String name) {
		this(name, DirectedDegreeType.OUT, EdgeWeightType.IGNORE_WEIGHTS);
	}

	/**
	 * Initializes {@link Assortativity}. Implicitly sets degree type for
	 * directed graphs to outdegree and ignores edge weights (if any).
	 * 
	 * @param name
	 *            The name of the metric, e.g. <i>AssortativityWeightedR</i> for
	 *            the Assortativity Recomputation and
	 *            <i>AssortativityWeightedU</i> for the Assortativity Updates.
	 * @param nodeTypes
	 *            types of nodes that should be processed
	 */
	public Assortativity(String name, String[] nodeTypes) {
		this(name, DirectedDegreeType.OUT, EdgeWeightType.IGNORE_WEIGHTS,
				nodeTypes);
	}

	/**
	 * Initializes {@link Assortativity}.
	 * 
	 * @param name
	 *            The name of the metric, e.g. <i>AssortativityWeightedR</i> for
	 *            the Assortativity Recomputation and
	 *            <i>AssortativityWeightedU</i> for the Assortativity Updates.
	 * @param directedDegreeType
	 *            <i>in</i> or <i>out</i>, determining whether to use in- or
	 *            outdegree for directed graphs. Will be ignored for undirected
	 *            graphs.
	 * @param edgeWeightType
	 *            <i>weighted</i> or <i>unweighted</i>, determining whether to
	 *            use edge weights in weighted graphs or not. Will be ignored
	 *            for unweighted graphs.
	 */
	public Assortativity(String name, DirectedDegreeType directedDegreeType,
			EdgeWeightType edgeWeightType) {
		super(name, IMetric.MetricType.exact, directedDegreeType
				.StringParameter(), edgeWeightType.StringParameter());

		this.directedDegreeType = directedDegreeType;
		this.edgeWeightType = edgeWeightType;
	}

	/**
	 * Initializes {@link Assortativity}.
	 * 
	 * @param name
	 *            The name of the metric, e.g. <i>AssortativityWeightedR</i> for
	 *            the Assortativity Recomputation and
	 *            <i>AssortativityWeightedU</i> for the Assortativity Updates.
	 * @param directedDegreeType
	 *            <i>in</i> or <i>out</i>, determining whether to use in- or
	 *            outdegree for directed graphs. Will be ignored for undirected
	 *            graphs.
	 * @param edgeWeightType
	 *            <i>weighted</i> or <i>unweighted</i>, determining whether to
	 *            use edge weights in weighted graphs or not. Will be ignored
	 *            for unweighted graphs.
	 * @param nodeTypes
	 *            types of nodes that should be processed
	 */
	public Assortativity(String name, DirectedDegreeType directedDegreeType,
			EdgeWeightType edgeWeightType, String[] nodeTypes) {
		super(name, IMetric.MetricType.exact, nodeTypes, directedDegreeType
				.StringParameter(), edgeWeightType.StringParameter());

		this.directedDegreeType = directedDegreeType;
		this.edgeWeightType = edgeWeightType;
	}

	/**
	 * Static compution. Used by {@link AssortativityR#recompute()} and
	 * {@link AssortativityU#init()}.
	 * 
	 * @return True if something was computed and false if no computation was
	 *         done because graph does not fit.
	 */
	boolean compute() {

		if (DirectedWeightedEdge.class.isAssignableFrom(this.g
				.getGraphDatastructures().getEdgeType())) {

			// directed weighted graph

			if (this.edgeWeightType.equals(EdgeWeightType.USE_WEIGHTS))
				return this.computeForDirectedWeightedGraph();
			else if (this.edgeWeightType.equals(EdgeWeightType.IGNORE_WEIGHTS))
				return this.computeForDirectedUnweightedGraph();

		} else if (UndirectedWeightedEdge.class.isAssignableFrom(this.g
				.getGraphDatastructures().getEdgeType())) {

			// undirected weighted graph

			if (this.edgeWeightType.equals(EdgeWeightType.USE_WEIGHTS))
				return this.computeForUndirectedWeightedGraph();
			else if (this.edgeWeightType.equals(EdgeWeightType.IGNORE_WEIGHTS))
				return this.computeForUndirectedUnweightedGraph();

		} else if (DirectedNode.class.isAssignableFrom(this.g
				.getGraphDatastructures().getNodeType())) {

			// directed unweighted graph
			return this.computeForDirectedUnweightedGraph();

		} else if (UndirectedNode.class.isAssignableFrom(this.g
				.getGraphDatastructures().getNodeType())) {

			// undirected unweighted graph
			return this.computeForUndirectedUnweightedGraph();

		}

		return false;
	}

	/**
	 * Computing for graphs with directed edges based only on current snapshot.
	 */
	private boolean computeForDirectedUnweightedGraph() {
		int srcNodeDegree = 0, dstNodeDegree = 0;
		this.totalEdgeWeight = 0;
		for (IElement n_ : this.getNodesOfAssignedTypes()) {
			DirectedNode n = (DirectedNode) n_;
			for (IElement e_ : n.getOutgoingEdges()) {
				DirectedEdge e = (DirectedEdge) e_;
				if (this.directedDegreeType.equals(DirectedDegreeType.OUT)) {
					srcNodeDegree = e.getSrc().getOutDegree();
					dstNodeDegree = e.getDst().getOutDegree();
				} else if (this.directedDegreeType
						.equals(DirectedDegreeType.IN)) {
					srcNodeDegree = e.getSrc().getInDegree();
					dstNodeDegree = e.getDst().getInDegree();
				}

				this.increaseSum123(srcNodeDegree, dstNodeDegree);
				this.totalEdgeWeight++;
			}
		}

		// for (IElement iElement : this.g.getEdges()) {
		// DirectedEdge edge = (DirectedEdge) iElement;
		// if (this.directedDegreeType.equals(DirectedDegreeType.OUT)) {
		// srcNodeDegree = edge.getSrc().getOutDegree();
		// dstNodeDegree = edge.getDst().getOutDegree();
		// } else if (this.directedDegreeType.equals(DirectedDegreeType.IN)) {
		// srcNodeDegree = edge.getSrc().getInDegree();
		// dstNodeDegree = edge.getDst().getInDegree();
		// }
		// this.increaseSum123(srcNodeDegree, dstNodeDegree);
		// this.totalEdgeWeight++;
		// }
		// this.totalEdgeWeight = this.g.getEdgeCount();

		this.setR();

		return true;
	}

	/**
	 * {@link #computeForWeightedGraphs()} for graphs with directed weighted
	 * edges.
	 */
	private boolean computeForDirectedWeightedGraph() {
		double edgeWeight;

		for (IElement n_ : this.getNodesOfAssignedTypes()) {
			DirectedNode n = (DirectedNode) n_;
			for (IElement e_ : n.getOutgoingEdges()) {
				DirectedWeightedEdge e = (DirectedWeightedEdge) e_;
				edgeWeight = this.weight(e.getWeight());
				this.increaseSum123(this.weightedDegree(e.getSrc()),
						this.weightedDegree(e.getDst()), edgeWeight);
			}
		}

		// for (IElement iElement : this.g.getEdges()) {
		// DirectedWeightedEdge edge = (DirectedWeightedEdge) iElement;
		// edgeWeight = this.weight(edge.getWeight());
		// this.totalEdgeWeight += edgeWeight;
		//
		// this.increaseSum123(this.weightedDegree(edge.getSrc()),
		// this.weightedDegree(edge.getDst()), edgeWeight);
		// }

		this.setR();

		return true;
	}

	/**
	 * Computing for graphs with undirected edges based only on current
	 * snapshot.
	 */
	private boolean computeForUndirectedUnweightedGraph() {
		this.totalEdgeWeight = 0;

		for (IElement n_ : this.getNodesOfAssignedTypes()) {
			UndirectedNode n = (UndirectedNode) n_;
			for (IElement e_ : n.getEdges()) {
				UndirectedEdge e = (UndirectedEdge) e_;
				this.increaseSum123(e.getNode1().getDegree(), e.getNode2()
						.getDegree());
				this.totalEdgeWeight++;
			}
		}

		// for (IElement iElement : this.g.getEdges()) {
		// UndirectedEdge edge = (UndirectedEdge) iElement;
		// this.increaseSum123(edge.getNode1().getDegree(), edge.getNode2()
		// .getDegree());
		// this.totalEdgeWeight++;
		// }
		// this.totalEdgeWeight = this.g.getEdgeCount();

		this.setR();

		return true;
	}

	/**
	 * {@link #computeForWeightedGraphs()} for graphs with undirected weighted
	 * edges.
	 */
	private boolean computeForUndirectedWeightedGraph() {
		double edgeWeight;

		for (IElement n_ : this.getNodesOfAssignedTypes()) {
			UndirectedNode n = (UndirectedNode) n_;
			for (IElement e_ : n.getEdges()) {
				UndirectedWeightedEdge e = (UndirectedWeightedEdge) e_;
				edgeWeight = this.weight(e.getWeight());
				this.increaseSum123(this.weightedDegree(e.getNode1()),
						this.weightedDegree(e.getNode2()), edgeWeight);
				this.totalEdgeWeight += edgeWeight;
			}
		}

		// for (IElement iElement : this.g.getEdges()) {
		// UndirectedWeightedEdge edge = (UndirectedWeightedEdge) iElement;
		// edgeWeight = this.weight(edge.getWeight());
		// this.increaseSum123(this.weightedDegree(edge.getNode1()),
		// this.weightedDegree(edge.getNode2()), edgeWeight);
		// this.totalEdgeWeight += edgeWeight;
		// }

		this.setR();

		return true;
	}

	void decreaseSum123(double srcNodeDegree, double dstNodeDegree) {
		this.decreaseSum123(srcNodeDegree, dstNodeDegree, 1.0);
	}

	void decreaseSum123(double srcWeightedDegree, double dstWeightedDegree,
			double edgeWeight) {
		this.sum1 -= edgeWeight * (srcWeightedDegree * dstWeightedDegree);
		this.sum2 -= edgeWeight * (srcWeightedDegree + dstWeightedDegree);
		this.sum3 -= edgeWeight
				* (srcWeightedDegree * srcWeightedDegree + dstWeightedDegree
						* dstWeightedDegree);
	}

	@Override
	public boolean equals(IMetric m) {
		if (!this.isComparableTo(m)) {
			return false;
		}
		boolean success = true;
		Assortativity m_ = (Assortativity) m;
		success &= DataUtils.equals(this.r, m_.r, "r");
		success &= DataUtils.equals(this.totalEdgeWeight, m_.totalEdgeWeight,
				"totalEdgeWeight");
		success &= DataUtils.equals(this.sum1, m_.sum1, "sum1");
		success &= DataUtils.equals(this.sum2, m_.sum2, "sum2");
		success &= DataUtils.equals(this.sum3, m_.sum3, "sum3");
		return success;
	}

	@Override
	public Distr<?, ?>[] getDistributions() {
		return new Distr<?, ?>[] { new BinnedDoubleDistr("test-exact", 0.5,
				new long[] { 0, 1, 2 }, 3) };
	}

	@Override
	public NodeNodeValueList[] getNodeNodeValueLists() {
		return new NodeNodeValueList[] {};
	}

	@Override
	public NodeValueList[] getNodeValueLists() {
		return new NodeValueList[] {};
	}

	@Override
	public Value[] getValues() {
		Value v1 = new Value("AssortativityCoefficient", this.r);
		Value v2 = new Value("totalEdgeWeight", this.totalEdgeWeight);
		Value v3 = new Value("sum1", this.sum1);
		Value v4 = new Value("sum2", this.sum2);
		Value v5 = new Value("sum3", this.sum3);
		return new Value[] { v1, v2, v3, v4, v5 };
	}

	void increaseSum123(double srcNodeDegree, double dstNodeDegree) {
		this.increaseSum123(srcNodeDegree, dstNodeDegree, 1.0);
	}

	void increaseSum123(double srcWeightedDegree, double dstWeightedDegree,
			double edgeWeight) {
		this.sum1 += edgeWeight * (srcWeightedDegree * dstWeightedDegree);
		this.sum2 += edgeWeight * (srcWeightedDegree + dstWeightedDegree);
		this.sum3 += edgeWeight
				* (srcWeightedDegree * srcWeightedDegree + dstWeightedDegree
						* dstWeightedDegree);
	}

	@Override
	public boolean isApplicable(Batch b) {
		return true;
	}

	@Override
	public boolean isApplicable(Graph g) {
		return true;
	}

	@Override
	public boolean isComparableTo(IMetric m) {
		return m != null
				&& m instanceof Assortativity
				&& ((Assortativity) m).directedDegreeType
						.equals(this.directedDegreeType)
				&& ((Assortativity) m).edgeWeightType
						.equals(this.edgeWeightType);
	}

	/**
	 * Computes {@link #r} based upon {@link #totalEdgeWeight} and {@link #sum1}
	 * , {@link #sum2}, {@link #sum3}.
	 */
	public void setR() {
		// For unweighted graphs the total edge weight is set to the number of
		// edges (i.e. every egde is weighted with 1)

		if (this.totalEdgeWeight == 0.0) {
			this.r = 0.0;
			return;
		}

		final double sum1m = this.sum1 / this.totalEdgeWeight;

		double sum2m = this.sum2 / (2 * this.totalEdgeWeight);
		sum2m *= sum2m;

		final double sum3m = this.sum3 / (2 * this.totalEdgeWeight);

		final double enumerator = sum1m - sum2m;
		final double denominator = sum3m - sum2m;

		this.r = enumerator == denominator ? 1 : enumerator / denominator;
	}

	/**
	 * @param w
	 *            Any {@link Weight}.
	 * @return Given w as double value.
	 */
	double weight(Weight w) {
		if (w instanceof IntWeight)
			return (double) ((IntWeight) w).getWeight();
		else if (w instanceof DoubleWeight)
			return ((DoubleWeight) w).getWeight();
		else
			return Double.NaN;
	}

	/**
	 * @return The weighted degree of the given node, i.e. the sum of all
	 *         weights of outgoing/incoming/all edges of this node.
	 */
	double weightedDegree(Node node) {
		double weightedDegree = 0.0;

		if (node instanceof DirectedNode) {

			if (this.directedDegreeType.equals(DirectedDegreeType.OUT))
				for (IElement iEdge : ((DirectedNode) node).getOutgoingEdges())
					weightedDegree += this
							.weight(((DirectedWeightedEdge) iEdge).getWeight());
			else if (this.directedDegreeType.equals(DirectedDegreeType.IN))
				for (IElement iEdge : ((DirectedNode) node).getIncomingEdges())
					weightedDegree += this
							.weight(((DirectedWeightedEdge) iEdge).getWeight());

		} else if (node instanceof UndirectedNode) {

			for (IElement iEdge : node.getEdges())
				weightedDegree += this.weight(((UndirectedWeightedEdge) iEdge)
						.getWeight());

		} else {
			Log.error("Returned weighted degree "
					+ weightedDegree
					+ " for node "
					+ node
					+ " because it is neither a directed node nor an undirected node.");
		}

		return weightedDegree;
	}

	protected boolean shouldEdgeBeProcessed(Edge e) {
		return !this.areNodesTyped() || this.isNodeOfAssignedType(e.getN1());
	}

}
