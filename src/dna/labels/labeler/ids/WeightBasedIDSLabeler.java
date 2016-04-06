package dna.labels.labeler.ids;

import java.util.ArrayList;

import dna.graph.Graph;
import dna.graph.generators.GraphGenerator;
import dna.labels.Label;
import dna.labels.labeler.Labeler;
import dna.metrics.IMetric;
import dna.metrics.degree.WeightedDegreeDistributionR;
import dna.series.data.BatchData;
import dna.series.data.MetricData;
import dna.updates.batch.Batch;
import dna.updates.generators.BatchGenerator;
import dna.util.Log;

/**
 * Second approach of an IDS-labeler based on model B.
 * 
 * @author Rwilmes
 * 
 */
public class WeightBasedIDSLabeler extends Labeler {

	private static String name = "ids-wbl";
	private static String type = "M2";

	private int threshold = 400;

	public WeightBasedIDSLabeler() {
		super("IDSWeightBasedLabeler");
	}

	public WeightBasedIDSLabeler(int threshold) {
		this();
		this.threshold = threshold;
	}

	@Override
	public boolean isApplicable(GraphGenerator gg, BatchGenerator bg,
			IMetric[] metrics) {
		if (Labeler.getMetric(metrics, WeightedDegreeDistributionR.class) == null) {
			Log.warn(getName() + ": metric '" + "WeightedDegreeDistributionR"
					+ "' not found!");
			return false;
		}
		return true;
	}

	@Override
	public ArrayList<Label> computeLabels(Graph g, Batch batch,
			BatchData batchData, IMetric[] metrics) {
		ArrayList<Label> list = new ArrayList<Label>();

		MetricData weightedDegrees = batchData.getMetrics().get(
				"WeightedDegreeDistributionR");

		double max = weightedDegrees.getValues().get("WeightedInDegreeMax")
				.getValue();

		if (max >= threshold) {
			Label l = new Label(name, type, "true");
			list.add(l);
			Log.info(batchData.getTimestamp() + "  <-  " + l.toString());
		}

		return list;
	}
}
