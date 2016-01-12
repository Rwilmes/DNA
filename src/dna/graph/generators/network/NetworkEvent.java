package dna.graph.generators.network;

import org.joda.time.DateTime;

/**
 * A simple network event.<br>
 * 
 * Defined by a specific time, srcIp, srcPort, dstIp and dstPort.
 * 
 * @author Rwilmes
 * 
 */
public class NetworkEvent {

	public enum NetworkEventField {
		TIME, SRC_IP, SRC_PORT, DST_IP, DST_PORT, NONE
	};

	protected DateTime time;

	protected int srcPort;
	protected int dstPort;

	protected String srcIp;
	protected String dstIp;

	public NetworkEvent(DateTime time, String srcIp, int srcPort, String dstIp,
			int dstPort) {
		this.time = time;
		this.srcIp = srcIp;
		this.srcPort = srcPort;
		this.dstIp = dstIp;
		this.dstPort = dstPort;
	}

	public DateTime getTime() {
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
