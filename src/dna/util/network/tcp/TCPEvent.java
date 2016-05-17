package dna.util.network.tcp;

import java.text.ParseException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import dna.util.network.NetworkEvent;

/**
 * Represents one line in a TCPDump-List.
 * 
 * @author Rwilmes
 * 
 */
public class TCPEvent extends NetworkEvent {

	public enum TCPEventField {
		ID, TIME, DURATION, SRC_IP, SRC_PORT, DST_IP, DST_PORT, SERVICE, NAME, ATTACK_SCORE, NONE
	};

	private long id;

	private DateTime duration;

	private String service;
	private double attackScore;
	private String name;

	private String srcIp;
	private String dstIp;
	private int srcPort;
	private int dstPort;

	public TCPEvent(long id, DateTime time, DateTime duration, String service,
			int srcPort, int dstPort, String srcIp, String dstIp,
			double attackScore, String name) {
		super(time);
		this.id = id;
		this.duration = duration;
		this.service = service;
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		this.srcIp = srcIp;
		this.dstIp = dstIp;
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
	public static TCPEvent getFromString(String line) throws ParseException {
		// split
		String[] splits = line.split("\\t");

		// format times
		DateTimeFormatter timeFormat = DateTimeFormat
				.forPattern("dd/MM/yyyyHH:mm:ss");
		DateTime time = timeFormat.parseDateTime(splits[2]);
		DateTimeFormatter durationFormat = DateTimeFormat
				.forPattern("HH:mm:ss");
		DateTime duration = durationFormat.parseDateTime(splits[3]);

		// try to parse ports
		int srcPort = 0;
		int dstPort = 0;

		try {
			srcPort = Integer.parseInt(splits[4]);
		} catch (NumberFormatException e) {
		}
		try {
			dstPort = Integer.parseInt(splits[5]);
		} catch (NumberFormatException e) {
		}

		// craft and return
		return new TCPEvent(Integer.parseInt(splits[0]), time, duration,
				splits[6], srcPort, dstPort, splits[7], splits[8],
				Double.parseDouble(splits[9]), splits[10]);
	}

}
