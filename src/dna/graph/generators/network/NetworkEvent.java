package dna.graph.generators.network;

import java.util.Date;

/**
 * A network event.
 * 
 * @author Rwilmes
 * 
 */
public class NetworkEvent {

	protected Date time;

	protected int srcPort;
	protected int dstPort;

	protected String srcIp;
	protected String dstIp;

	public NetworkEvent(Date time, String srcIp, int srcPort, String dstIp,
			int dstPort) {
		this.time = time;
		this.srcIp = srcIp;
		this.srcPort = srcPort;
		this.dstIp = dstIp;
		this.dstPort = dstPort;
	}

	public Date getTime() {
		return time;
	}

	public int getSrcPort() {
		return srcPort;
	}

	public int getDstPort() {
		return dstPort;
	}

	public String getSrcIp() {
		return srcIp;
	}

	public String getDstIp() {
		return dstIp;
	}

	public void print() {
		System.out.println(toString());
	}

	public String toString() {
		return "Network-Event: " + srcIp + ":" + srcPort + " to " + dstIp + ":"
				+ dstPort + " at " + time.toString();
	}
}
