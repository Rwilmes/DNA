package dna.util.network.netflow;

import java.io.FileNotFoundException;

import dna.util.network.netflow.NetflowEvent.NetflowEventField;

public class DefaultNetflowReader extends NetflowEventReader {

	// FORMAT:
	// StartTime
	// Fraction
	// Flags
	// Type (Protocol)
	// SrcAddr
	// Dir
	// DstAddr
	// SrcPkt
	// DstPkt
	// SrcBytes
	// DstBytes
	// State
	// Dur
	// SrcBps
	// DstBps
	// Sport
	// Dport
	protected static final NetflowEventField[] fields = {
			NetflowEventField.Date, NetflowEventField.None,
			NetflowEventField.Flags, NetflowEventField.Protocol,
			NetflowEventField.SrcAddress, NetflowEventField.Direction,
			NetflowEventField.DstAddress,
			NetflowEventField.PacketsToDestination,
			NetflowEventField.PacketToSrc,
			NetflowEventField.BytesToDestination, NetflowEventField.BytesToSrc,
			NetflowEventField.ConnectionState, NetflowEventField.Duration,
			NetflowEventField.None, NetflowEventField.None,
			NetflowEventField.SrcPort, NetflowEventField.DstPort };

	// constructor
	public DefaultNetflowReader(String dir, String filename)
			throws FileNotFoundException {
		super(dir, filename, "\t", "MM-dd-yyyy HH:mm:ss", "HH:mm:ss", fields);
	}

}