package dna.util.network.netflow;

import org.joda.time.DateTime;

/**
 * Reprenents one netflow event.
 * 
 * @author Rwilmes
 * 
 */
public class NetflowEvent {

	public enum NetflowEventField {
		Date, Time, SrcAddress, DstAddress, Duration, Protocol, SrcPort, DstPort, Direction, None, Packets, PacketToSrc, PacketsToDestination, Bytes, BytesToSrc, BytesToDestination, Label, Flags, ConnectionState
	}

	public enum NetflowDirection {
		forward, backward, bidirectional
	}

	// CLASS
	protected long id;
	protected DateTime time;

	protected String srcAddress;
	protected String dstAddress;
	protected double duration;

	protected NetflowDirection direction;

	protected String flags;
	protected String connectionState;

	protected String protocol;
	protected String srcPort;
	protected String dstPort;

	protected int packets;
	protected int packetsToSrc;
	protected int packetsToDestination;

	protected int bytes;
	protected int bytesToSrc;
	protected int bytesToDestination;

	protected String label;

	public NetflowEvent(long id, DateTime time, String srcAddress,
			String dstAddress, double duration, NetflowDirection direction,
			String flags, String connectionState, String protocol,
			String srcPort, String dstPort, int packets, int packetsToSrc,
			int packetsToDestination, int bytes, int bytesToSrc,
			int bytesToDestination, String label) {
		super();

		this.srcAddress = srcAddress;
		this.dstAddress = dstAddress;
		this.duration = duration;
		this.direction = direction;
		this.flags = flags;
		this.connectionState = connectionState;

		this.protocol = protocol;
		this.srcPort = srcPort;
		this.dstPort = dstPort;

		this.packets = packets;
		this.packetsToSrc = packetsToSrc;
		this.packetsToDestination = packetsToDestination;

		this.bytes = bytes;
		this.bytesToSrc = bytesToSrc;
		this.bytesToDestination = bytesToDestination;

		this.label = label;
	}
}
