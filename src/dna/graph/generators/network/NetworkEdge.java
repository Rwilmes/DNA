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
	protected double weight;

	public NetworkEdge(int src, int dst, long time) {
		this(src, dst, time, 0);
	}

	public NetworkEdge(int src, int dst, long time, double weight) {
		this.src = src;
		this.dst = dst;
		this.time = time;
		this.weight = weight;
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

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public void setTime(long t) {
		this.time = t;
	}

	public boolean sameEdge(NetworkEdge e) {
		return ((src == e.getSrc()) && (dst == e.getDst()));
	}

	public String toString() {
		return "NetworkEdge: " + src + "\t=>\t" + dst + "\tw=" + weight
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
