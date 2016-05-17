package dna.util.network;

import org.joda.time.DateTime;

/**
 * A simple network event.<br>
 * 
 * Defined by a specific time, srcIp, srcPort, dstIp and dstPort.
 * 
 * @author Rwilmes
 * 
 */
public class NetworkEventOld extends NetworkEvent {

	public enum NetworkEventField {
		TIME, SRC_IP, SRC_PORT, DST_IP, DST_PORT, NONE
	};

	protected DateTime time;

	protected int srcPort;
	protected int dstPort;

	protected String srcIp;
	protected String dstIp;

	public NetworkEventOld(DateTime time, String srcIp, int srcPort,
			String dstIp, int dstPort) {
		super(time);
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

	public String getTimeReadable() {
		if (time == null)
			return "??:??:??";

		String hours = "" + time.getHourOfDay();
		hours = (hours.length() == 1) ? "0" + hours : hours;
		String mins = "" + time.getMinuteOfHour();
		mins = (mins.length() == 1) ? "0" + mins : mins;
		String secs = "" + time.getSecondOfMinute();
		secs = (secs.length() == 1) ? "0" + secs : secs;

		return hours + ":" + mins + ":" + secs;
	}
}
