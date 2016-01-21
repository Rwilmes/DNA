package dna.graph.generators.network;

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

	public NetworkEdge(int src, int dst, long time) {
		this.src = src;
		this.dst = dst;
		this.time = time;
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

	public void setTime(long t) {
		this.time = t;
	}

	public boolean sameEdge(NetworkEdge e) {
		return ((src == e.getSrc()) && (dst == e.getDst()));
	}
}
