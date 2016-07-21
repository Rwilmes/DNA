package dna.util.network.hosts;

import java.util.HashMap;

public class HostStats {

	protected HashMap<Host, Long> numberOfFlowsIn;
	protected HashMap<Host, Long> numberOfFlowsOut;
	protected HashMap<Host, Long> numberOfPacketsIn;
	protected HashMap<Host, Long> numberOfPacketsOut;
	protected HashMap<Host, Long> numberOfBytesIn;
	protected HashMap<Host, Long> numberOfBytesOut;

	protected String name;

	public HostStats(String name) {
		this.name = name;
		this.numberOfFlowsIn = new HashMap<Host, Long>();
		this.numberOfFlowsOut = new HashMap<Host, Long>();
		this.numberOfPacketsIn = new HashMap<Host, Long>();
		this.numberOfPacketsOut = new HashMap<Host, Long>();
		this.numberOfBytesIn = new HashMap<Host, Long>();
		this.numberOfBytesOut = new HashMap<Host, Long>();
	}

	public String getName() {
		return name;
	}

	public void updateFlows(Host h, long flowsIn, long flowsOut) {
		if (numberOfFlowsIn.containsKey(h)) {
			numberOfFlowsIn.put(h, numberOfFlowsIn.get(h) + flowsIn);
			numberOfFlowsOut.put(h, numberOfFlowsOut.get(h) + flowsOut);
		} else {
			numberOfFlowsIn.put(h, flowsIn);
			numberOfFlowsOut.put(h, flowsOut);
		}
	}

	public void updatePackets(Host h, long packetsIn, long packetsOut) {
		if (numberOfPacketsIn.containsKey(h)) {
			numberOfPacketsIn.put(h, numberOfPacketsIn.get(h) + packetsIn);
			numberOfPacketsOut.put(h, numberOfPacketsOut.get(h) + packetsOut);
		} else {
			numberOfPacketsIn.put(h, packetsIn);
			numberOfPacketsOut.put(h, packetsOut);
		}
	}

	public void updateBytes(Host h, long bytesIn, long bytesOut) {
		if (numberOfBytesIn.containsKey(h)) {
			numberOfBytesIn.put(h, numberOfBytesIn.get(h) + bytesIn);
			numberOfBytesOut.put(h, numberOfBytesOut.get(h) + bytesOut);
		} else {
			numberOfBytesIn.put(h, bytesIn);
			numberOfBytesOut.put(h, bytesOut);
		}
	}

	public long getFlowsIn(Host h) {
		return numberOfFlowsIn.get(h);
	}

	public long getFlowsOut(Host h) {
		return numberOfFlowsOut.get(h);
	}

	public long getPacketsIn(Host h) {
		return numberOfPacketsIn.get(h);
	}

	public long getPacketsOut(Host h) {
		return numberOfPacketsOut.get(h);
	}

	public long getBytesIn(Host h) {
		return numberOfBytesIn.get(h);
	}

	public long getBytesOut(Host h) {
		return numberOfBytesOut.get(h);
	}
}
