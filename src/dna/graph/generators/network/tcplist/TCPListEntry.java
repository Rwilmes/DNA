package dna.graph.generators.network.tcplist;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import dna.graph.generators.network.NetworkEvent;

/**
 * Represents one line in a TCPDump-List.
 * 
 * @author Rwilmes
 * 
 */
public class TCPListEntry extends NetworkEvent {

	private long id;

	private Date duration;

	private String service;
	private double attackScore;
	private String name;

	public TCPListEntry(long id, Date time, Date duration, String service,
			int srcPort, int dstPort, String srcIp, String dstIp,
			double attackScore, String name) {
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

	public Date getDuration() {
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

		// parse timestamps
		DateFormat timeFormat = new SimpleDateFormat("dd/MM/yyyyhh:mm:ss");
		Date time = timeFormat.parse(splits[2] + splits[3]);
		DateFormat durationFormat = new SimpleDateFormat("hh:mm:ss");
		Date duration = durationFormat.parse(splits[4]);

		return new TCPListEntry(Integer.parseInt(splits[0]), time, duration,
				splits[5], Integer.parseInt(splits[6]),
				Integer.parseInt(splits[7]), splits[8], splits[9],
				Double.parseDouble(splits[10]), splits[11]);
	}

}
