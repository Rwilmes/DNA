package dna.labels.labeler.darpa;

import java.util.ArrayList;

import dna.graph.Graph;
import dna.graph.generators.GraphGenerator;
import dna.graph.generators.network.m1.M1Batch;
import dna.labels.Label;
import dna.labels.labeler.Labeler;
import dna.metrics.IMetric;
import dna.series.data.BatchData;
import dna.updates.batch.Batch;
import dna.updates.generators.BatchGenerator;
import dna.util.network.tcp.TCPEventReader;

/**
 * A labeler which labels batches based on the attack-labels of the actual tcp
 * entries.
 * 
 * @author Rwilmes
 * 
 */
public class EntryBasedAttackLabeler extends Labeler {

	protected TCPEventReader reader;

	public EntryBasedAttackLabeler() {
		super("DarpaEntryBasedLabeler");
	}

	@Override
	public boolean isApplicable(GraphGenerator gg, BatchGenerator bg,
			IMetric[] metrics) {
		if (bg instanceof M1Batch)
			return true;
		else
			return false;
	}

	@Override
	public ArrayList<Label> computeLabels(Graph g, Batch batch,
			BatchData batchData, IMetric[] metrics) {
		ArrayList<Label> list = new ArrayList<Label>();

		if (this.reader != null) {
			for (String s : this.reader.getOccuredAttacks())
				list.add(new Label("attack", s, "true"));
		}

		return list;
	}

	public void registerEventReader(TCPEventReader reader) {
		this.reader = reader;
	}

}
