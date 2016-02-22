package dna.labels.labeler.ids;

import java.util.ArrayList;

import dna.graph.Graph;
import dna.graph.generators.GraphGenerator;
import dna.labels.Label;
import dna.labels.labeler.Labeler;
import dna.metrics.IMetric;
import dna.metrics.degree.DegreeDistributionU;
import dna.metrics.motifs.DirectedMotifsU;
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
public class IntrusionDetectionLabeler1 extends Labeler {

	private static String name = "IDS";
	private static String type = "M1";

	public IntrusionDetectionLabeler1() {
		super(name);
	}

	@Override
	public boolean isApplicable(GraphGenerator gg, BatchGenerator bg,
			IMetric[] metrics) {
		if (Labeler.getMetric(metrics, DegreeDistributionU.class) == null) {
			Log.warn(getName() + ": metric '" + "DegreeDistributionU"
					+ "' not found!");
			return false;
		}
		if (Labeler.getMetric(metrics, DirectedMotifsU.class) == null) {
			Log.warn(getName() + ": metric '" + "DirectedMotifsU"
					+ "' not found!");
			return false;
		}
		return true;
	}

	@Override
	public ArrayList<Label> computeLabels(Graph g, Batch batch,
			BatchData batchData, IMetric[] metrics) {
		ArrayList<Label> list = new ArrayList<Label>();

		MetricData degree = batchData.getMetrics().get("DegreeDistributionU");
		MetricData motifs = batchData.getMetrics().get("DirectedMotifsU");

		// if graph to small -> do nothing
		if (degree.getValues().get("OutDegreeMax").getValue() >= 5
				&& degree.getValues().get("InDegreeMax").getValue() >= 5) {
			double total = motifs.getValues().get("TOTAL").getValue();
			double dmotif2 = motifs.getValues().get("DM02").getValue();

			double rel = dmotif2 / total;
			if (rel >= 0.3) {
				Label l = new Label(name, type, "true");
				list.add(l);
				Log.info(batchData.getTimestamp() + "  <-  " + l.toString());
			}
		}

		return list;
	}
}
