package dna.graph.generators.network;

public class NodeUpdate extends UpdateEvent {

	protected int index;
	protected long time;
	protected double[] updates;

	public NodeUpdate(int index, long time, double[] updates) {
		super(time);
		this.index = index;
		this.updates = updates;
	}

	public int getIndex() {
		return index;
	}

	public double[] getUpdates() {
		return updates;
	}

}
