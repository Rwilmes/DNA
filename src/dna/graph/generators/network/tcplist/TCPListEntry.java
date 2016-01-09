package dna.graph.generators.network.tcplist;

import java.text.ParseException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import dna.graph.generators.network.NetworkEvent;

/**
 * Represents one line in a TCPDump-List.
 * 
 * @author Rwilmes
 * 
 */
public class TCPListEntry extends NetworkEvent {

	private long id;

	private DateTime duration;

	private String service;
	private double attackScore;
	private String name;

	public TCPListEntry(long id, DateTime time, DateTime duration,
			String service, int srcPort, int dstPort, String srcIp,
			String dstIp, double attackScore, String name) {
		super(time, srcIp, srcPort, dstIp, dstPort);
		this.id = id;
		this.duration = duration;
		this.service = service;
		this.attackScore = attackScore;
		this.name = name;
	}

	public long getId() {
		return id;
	}

	public DateTime getDuration() {
		return duration;
	}

	public String getService() {
		return service;
	}

	public double getAttackScore() {
		return attackScore;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return "TCPListEntry: " + id + "\t" + srcIp + ":" + srcPort + " to "
				+ dstIp + ":" + dstPort + " " + service + "\t" + attackScore
				+ " " + name + "\t" + " at " + time.toString();
	}

	/** Parses a String to a TCPListEntry object. **/
	public static TCPListEntry getFromString(String line) throws ParseException {
		// split
		String[] splits = line.split(" ");

		// format times
		DateTimeFormatter timeFormat = DateTimeFormat
				.forPattern("dd/MM/yyyyHH:mm:ss");
		DateTime time = timeFormat.parseDateTime(splits[2] + splits[3]);
		DateTimeFormatter durationFormat = DateTimeFormat
				.forPattern("HH:mm:ss");
		DateTime duration = durationFormat.parseDateTime(splits[4]);

		// try to parse ports
		int srcPort = 0;
		int dstPort = 0;

		try {
			srcPort = Integer.parseInt(splits[6]);
		} catch (NumberFormatException e) {
		}
		try {
			dstPort = Integer.parseInt(splits[7]);
		} catch (NumberFormatException e) {
		}

		// craft and return
		return new TCPListEntry(Integer.parseInt(splits[0]), time, duration,
				splits[5], srcPort, dstPort, splits[8], splits[9],
				Double.parseDouble(splits[10]), splits[11]);
	}

}
