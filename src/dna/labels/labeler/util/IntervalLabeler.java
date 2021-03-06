package dna.labels.labeler.util;

import java.util.ArrayList;

import dna.graph.Graph;
import dna.graph.generators.GraphGenerator;
import dna.labels.Label;
import dna.labels.labeler.Labeler;
import dna.metrics.IMetric;
import dna.series.data.BatchData;
import dna.updates.batch.Batch;
import dna.updates.generators.BatchGenerator;

/**
 * A labeler who labels all batches inside the given interval.
 * 
 * @author Rwilmes
 */
public class IntervalLabeler extends Labeler {

	private static String name = "IntervalLabeler";
	private static String value = "1";

	private String type;
	private String v;
	private long from;
	private long to;

	public IntervalLabeler(String type, long from, long to) {
		this(name, type, value, from, to);
	}

	public IntervalLabeler(String name, String type, String value, long from,
			long to) {
		super(name);
		this.type = type;
		this.v = value;
		this.from = from;
		this.to = to;
	}

	@Override
	public boolean isApplicable(GraphGenerator gg, BatchGenerator bg,
			IMetric[] metrics) {
		return true;
	}

	@Override
	public ArrayList<Label> computeLabels(Graph g, Batch batch,
			BatchData batchData, IMetric[] metrics) {
		ArrayList<Label> list = new ArrayList<Label>();

		if (this.from <= batchData.getTimestamp()
				&& batchData.getTimestamp() <= this.to)
			list.add(new Label(this.getName(), this.type, this.v));

		return list;
	}

}
