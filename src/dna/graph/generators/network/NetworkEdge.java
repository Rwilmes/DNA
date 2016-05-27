package dna.graph.generators.network;

import java.util.ArrayList;

/**
 * Class which represents one edge in a network-graph. <br>
 * 
 * Contains mapped index of the source and destination nodes and the recent
 * timestamp.
 * 
 * @author Rwilmes
 * 
 */
public class NetworkEdge {

	protected int src;
	protected int dst;
	protected long time;
	protected double[] weights;

	public NetworkEdge(int src, int dst, long time) {
		this(src, dst, time, new double[] { 0 });
	}

	public NetworkEdge(int src, int dst, long time, double[] weights) {
		this.src = src;
		this.dst = dst;
		this.time = time;
		this.weights = weights;
	}

	public int getSrc() {
		return src;
	}

	public int getDst() {
		return dst;
	}

	public long getTime() {
		return time;
	}

	public double[] getWeights() {
		return weights;
	}

	public void setWeights(double[] weights) {
		this.weights = weights;
	}

	public void setTime(long t) {
		this.time = t;
	}

	public boolean sameEdge(NetworkEdge e) {
		return ((src == e.getSrc()) && (dst == e.getDst()));
	}

	public String toString() {
		return "NetworkEdge: " + src + "\t=>\t" + dst + "\tw=" + weights
				+ "\t@\t" + time;
	}

	/** Returns if the same edge is contained in the given list. **/
	public boolean containedIn(ArrayList<NetworkEdge> list) {
		boolean added = false;
		NetworkEdge ne = null;
		for (int j = 0; j < list.size(); j++) {
			ne = list.get(j);
			if (ne.getSrc() == src && ne.getDst() == dst) {
				added = true;
				break;
			}
		}

		return added;
	}
}
