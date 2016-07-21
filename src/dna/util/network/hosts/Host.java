package dna.util.network.hosts;

import java.util.ArrayList;
import java.util.LinkedList;

import org.joda.time.DateTime;

public class Host {

	protected String add;
	protected DateTime initTime;
	protected DateTime expireTime;

	protected DateTime currentTime;

	protected long flowsIn;
	protected long flowsOut;

	protected long packetsIn;
	protected long packetsOut;

	protected long bytesIn;
	protected long bytesOut;

	protected ArrayList<String> portsIn;
	protected ArrayList<String> portsOut;

	protected LinkedList<HostUpdate> updateQueue;

	public Host(String add, DateTime initTime, DateTime expireTime) {
		this(add, initTime, expireTime, 0, 0, 0, 0, 0, 0,
				new ArrayList<String>(), new ArrayList<String>());
	}

	public Host(String add, DateTime initTime, DateTime expireTime,
			long flowsIn, long flowsOut, long packetsIn, long packetsOut,
			long bytesIn, long bytesOut, ArrayList<String> portsIn,
			ArrayList<String> portsOut) {
		this.add = add;
		this.initTime = initTime;
		this.currentTime = initTime;
		this.expireTime = expireTime;
		this.flowsIn = flowsIn;
		this.flowsOut = flowsOut;
		this.packetsIn = packetsIn;
		this.packetsOut = packetsOut;
		this.bytesIn = bytesIn;
		this.bytesOut = bytesOut;

		this.portsIn = portsIn;
		this.portsOut = portsOut;
		this.updateQueue = new LinkedList<HostUpdate>();
	}

	public String getAddress() {
		return this.add;
	}

	public DateTime getInitTime() {
		return this.initTime;
	}

	public DateTime getCurrentTime() {
		return this.currentTime;
	}

	public DateTime getExpireTime() {
		return this.expireTime;
	}

	public void setExpireTime(DateTime expireTime) {
		this.expireTime = expireTime;
	}

	public long getFlowsIn() {
		return flowsIn;
	}

	public void setFlowsIn(long flowsIn) {
		this.flowsIn = flowsIn;
	}

	public void incrFlowsIn(long incr) {
		this.flowsIn += incr;
	}

	public void decrFlowsIn(long decr) {
		this.flowsIn -= decr;
	}

	public long getFlowsOut() {
		return flowsOut;
	}

	public void setFlowsOut(long flowsOut) {
		this.flowsOut = flowsOut;
	}

	public void incrFlowsOut(long incr) {
		this.flowsOut += incr;
	}

	public void decrFlowsOut(long decr) {
		this.flowsOut -= decr;
	}

	public long getPacketsIn() {
		return packetsIn;
	}

	public void setPacketsIn(long packetsIn) {
		this.packetsIn = packetsIn;
	}

	public void incrPacketsIn(long incr) {
		this.packetsIn += incr;
	}

	public void decrPacketsIn(long decr) {
		this.packetsIn -= decr;
	}

	public long getPacketsOut() {
		return packetsOut;
	}

	public void setPacketsOut(long packetsOut) {
		this.packetsOut = packetsOut;
	}

	public void incrPacketsOut(long incr) {
		this.packetsOut += incr;
	}

	public void decrPacketsOut(long decr) {
		this.packetsOut -= decr;
	}

	public long getBytesIn() {
		return bytesIn;
	}

	public void setBytesIn(long bytesIn) {
		this.bytesIn = bytesIn;
	}

	public void incrBytesIn(long incr) {
		this.bytesIn += incr;
	}

	public void decrBytesIn(long decr) {
		this.bytesIn -= decr;
	}

	public long getBytesOut() {
		return bytesOut;
	}

	public void setBytesOut(long bytesOut) {
		this.bytesOut = bytesOut;
	}

	public void incrBytesOut(long incr) {
		this.bytesOut += incr;
	}

	public void decrBytesOut(long decr) {
		this.bytesOut -= decr;
	}

	public ArrayList<String> getPortsIn() {
		return portsIn;
	}

	public ArrayList<String> getPortsOut() {
		return portsOut;
	}

	public void addPortIn(String port) {
		if (!this.portsIn.contains(port))
			this.portsIn.add(port);
	}

	public void remPortIn(String port) {
		if (this.portsIn.contains(port))
			this.portsIn.remove(port);
	}

	public void addPortsIn(ArrayList<String> ports) {
		for (String port : ports) {
			if (!this.portsIn.contains(port))
				this.portsIn.add(port);
		}
	}

	public void remPortsIn(ArrayList<String> ports) {
		for (String port : ports) {
			if (this.portsIn.contains(port))
				this.portsIn.remove(port);
		}
	}

	public void addPortOut(String port) {
		if (!this.portsOut.contains(port))
			this.portsOut.add(port);
	}

	public void remPortOut(String port) {
		if (this.portsOut.contains(port))
			this.portsOut.remove(port);
	}

	public void addPortsOut(ArrayList<String> ports) {
		for (String port : ports) {
			if (!this.portsOut.contains(port))
				this.portsOut.add(port);
		}
	}

	public void remPortsOut(ArrayList<String> ports) {
		for (String port : ports) {
			if (this.portsOut.contains(port))
				this.portsOut.remove(port);
		}
	}

	public String toString() {
		return "Host: " + getAddress() + "  @ " + getInitTime().toString()
				+ "\tuntil: " + getExpireTime().toString();
	}

	public boolean equals(Host h) {
		if (h.getAddress().equals(this.add))
			return true;
		else
			return false;
	}

	public void applyUpdate(HostUpdate update) {
		updateQueue.add(update);
		incrFlowsIn(update.getFlowsIn());
		incrFlowsOut(update.getFlowsOut());
		incrPacketsIn(update.getPacketsIn());
		incrPacketsOut(update.getPacketsOut());
		incrBytesIn(update.getBytesIn());
		incrBytesOut(update.getBytesOut());
		addPortIn(update.getPortIn());
		addPortOut(update.getPortOut());
	}

	public boolean update(DateTime time) {
		boolean somethingHappened = false;
		while (updateQueue.size() > 0
				&& !updateQueue.getFirst().getExpireTime().isAfter(time)) {
			HostUpdate update = updateQueue.pop();
			decrFlowsIn(update.getFlowsIn());
			decrFlowsOut(update.getFlowsOut());
			decrPacketsIn(update.getPacketsIn());
			decrPacketsOut(update.getPacketsOut());
			decrBytesIn(update.getBytesIn());
			decrBytesOut(update.getBytesOut());
			remPortIn(update.getPortIn());
			remPortOut(update.getPortOut());
			somethingHappened = true;
		}

		return somethingHappened;
	}

}