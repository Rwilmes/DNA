package dna.labels.labeler.ids;

import java.util.ArrayList;

import dna.graph.Graph;
import dna.graph.generators.GraphGenerator;
import dna.labels.Label;
import dna.labels.labeler.Labeler;
import dna.metrics.IMetric;
import dna.metrics.weights.EdgeWeightsR;
import dna.series.data.BatchData;
import dna.series.data.MetricData;
import dna.updates.batch.Batch;
import dna.updates.generators.BatchGenerator;
import dna.util.Log;

/**
 * First approach of an IDS-labeler based on modell 1.
 * 
 * @author Rwilmes
 * 
 */
public class IntrusionDetectionLabeler2 extends Labeler {

	public static String maxType = "max";
	public static String avgMaxType = "product";

	private static String name = "DoS";

	private static int maxThreshold = 800;
	private static int avgMaxProductThreshold = 15000;

	public IntrusionDetectionLabeler2() {
		super(name);
	}

	@Override
	public boolean isApplicable(GraphGenerator gg, BatchGenerator bg,
			IMetric[] metrics) {
		if (Labeler.getMetric(metrics, EdgeWeightsR.class) == null) {
			Log.warn(getName() + ": metric '" + "EdgeWeightsR" + "' not found!");
			return false;
		}
		return true;
	}

	@Override
	public ArrayList<Label> computeLabels(Graph g, Batch batch,
			BatchData batchData, IMetric[] metrics) {
		ArrayList<Label> list = new ArrayList<Label>();

		MetricData edgeWeights = batchData.getMetrics().get("EdgeWeightsR-1.0");

		double max = edgeWeights.getValues().get("MaxWeight").getValue();
		double avg = edgeWeights.getValues().get("AverageWeight").getValue();

		if (max >= maxThreshold) {
			Label l = new Label(name, maxType, "true");
			list.add(l);
			Log.info(batchData.getTimestamp() + "  <-  " + l.toString());
		}
		if ((avg * max) >= avgMaxProductThreshold) {
			Label l = new Label(name, avgMaxType, "true");
			list.add(l);
			Log.info(batchData.getTimestamp() + "  <-  " + l.toString());
		}

		return list;
	}
}
