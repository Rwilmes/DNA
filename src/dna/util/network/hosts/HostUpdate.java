package dna.util.network.hosts;

import org.joda.time.DateTime;

import dna.util.network.netflow.NetflowEvent;
import dna.util.network.netflow.NetflowEvent.NetflowDirection;

public class HostUpdate {

	protected DateTime initTime;
	protected DateTime expireTime;

	protected long flowsIn;
	protected long flowsOut;

	protected long packetsIn;
	protected long packetsOut;

	protected long bytesIn;
	protected long bytesOut;

	protected String portIn;
	protected String portOut;

	public HostUpdate(DateTime initTime, DateTime expireTime, long flowsIn,
			long flowsOut, long packetsIn, long packetsOut, long bytesIn,
			long bytesOut, String portIn, String portOut) {
		this.initTime = initTime;
		this.expireTime = expireTime;

		this.flowsIn = flowsIn;
		this.flowsOut = flowsOut;
		this.packetsIn = packetsIn;
		this.packetsOut = packetsOut;
		this.bytesIn = bytesIn;
		this.bytesOut = bytesOut;

		this.portIn = portIn;
		this.portOut = portOut;
	}

	public DateTime getInitTime() {
		return initTime;
	}

	public DateTime getExpireTime() {
		return expireTime;
	}

	public long getFlowsIn() {
		return flowsIn;
	}

	public long getFlowsOut() {
		return flowsOut;
	}

	public long getPacketsIn() {
		return packetsIn;
	}

	public long getPacketsOut() {
		return packetsOut;
	}

	public long getBytesIn() {
		return bytesIn;
	}

	public long getBytesOut() {
		return bytesOut;
	}

	public String getPortIn() {
		return portIn;
	}

	public String getPortOut() {
		return portOut;
	}

	public static HostUpdate getSrcUpdateFromEvent(NetflowEvent event,
			int expireTimeSeconds) {
		int flowsIn = 0;
		int flowsOut = 0;

		NetflowDirection dir = event.getDirection();

		if (dir != null) {

			switch (dir) {
			case backward:
				flowsIn = 1;
				break;
			case bidirectional:
				flowsIn = 1;
				flowsOut = 1;
				break;
			case forward:
				flowsIn = 0;
				flowsOut = 1;
				break;
			}
		}

		String portIn = event.getSrcPort();
		String portOut = event.getDstPort();

		if (portIn == null || portIn.equals("null"))
			portIn = "none";
		if (portOut == null || portOut.equals("null"))
			portOut = "none";

		return new HostUpdate(event.getTime(), event.getTime().plusSeconds(
				expireTimeSeconds), flowsIn, flowsOut, event.getPacketsToSrc(),
				event.getPacketsToDestination(), event.getBytesToSrc(),
				event.getBytesToDestination(), portIn, portOut);
	}

	public static HostUpdate getDstUpdateFromEvent(NetflowEvent event,
			int expireTimeSeconds) {
		int flowsIn = 0;
		int flowsOut = 0;

		NetflowDirection dir = event.getDirection();

		if (dir != null) {
			switch (dir) {
			case backward:
				flowsIn = 1;
				break;
			case bidirectional:
				flowsIn = 1;
				flowsOut = 1;
				break;
			case forward:
				flowsIn = 0;
				flowsOut = 1;
				break;
			}
		}

		String portIn = event.getSrcPort();
		String portOut = event.getDstPort();

		if (portIn == null || portIn.equals("null"))
			portIn = "none";
		if (portOut == null || portOut.equals("null"))
			portOut = "none";

		return new HostUpdate(event.getTime(), event.getTime().plusSeconds(
				expireTimeSeconds), flowsOut, flowsIn,
				event.getPacketsToDestination(), event.getPacketsToSrc(),
				event.getBytesToDestination(), event.getBytesToSrc(), portOut,
				portIn);
	}
}
