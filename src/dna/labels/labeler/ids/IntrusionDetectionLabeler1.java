package dna.labels.labeler.ids;

import java.util.ArrayList;

import dna.graph.Graph;
import dna.graph.generators.GraphGenerator;
import dna.labels.Label;
import dna.labels.labeler.Labeler;
import dna.metrics.IMetric;
import dna.metrics.degree.DegreeDistributionR;
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

	private static String type = "M1";

	private int totalThreshold = 1000;

	public IntrusionDetectionLabeler1(int totalThreshold) {
		this();
		this.totalThreshold = totalThreshold;
	}

	public IntrusionDetectionLabeler1() {
		this("IDS");
	}

	public IntrusionDetectionLabeler1(String name) {
		super(name);
	}

	@Override
	public boolean isApplicable(GraphGenerator gg, BatchGenerator bg,
			IMetric[] metrics) {
		boolean foundR;
		boolean foundU;
		if (Labeler.getMetric(metrics, DegreeDistributionR.class) == null) {
			foundR = false;
			// Log.warn(getName() + ": metric '" + "DegreeDistributionR"
			// + "' not found!");
			// return false;
		} else {
			foundR = true;
		}

		if (Labeler.getMetric(metrics, DegreeDistributionU.class) == null) {
			foundU = false;
			// Log.warn(getName() + ": metric '" + "DegreeDistributionR"
			// + "' not found!");
			// return false;
		} else {
			foundU = true;
		}

		if (!foundR && !foundU) {
			Log.warn(getName() + ": neither metric '" + "DegreeDistributionR"
					+ "' nor " + "DegreeDistributionU" + " found!");
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

		MetricData degree = batchData.getMetrics().get("DegreeDistributionR");
		if (degree == null)
			degree = batchData.getMetrics().get("DegreeDistributionU");

		MetricData motifs = batchData.getMetrics().get("DirectedMotifsU");

		// if graph to small -> do nothing
		if (degree.getValues().get("OutDegreeMax").getValue() >= 5
				&& degree.getValues().get("InDegreeMax").getValue() >= 5) {
			double total = motifs.getValues().get("TOTAL").getValue();

			if (total > totalThreshold) {
				double dmotif2 = motifs.getValues().get("DM02").getValue();

				double rel = dmotif2 / total;
				if (rel >= 0.3) {
					Label l = new Label(name, type, "true");
					list.add(l);
					Log.info(batchData.getTimestamp() + "  <-  " + l.toString());

				}
			}
		}

		return list;
	}
}