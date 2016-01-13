package dna.util.network.tcp;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import dna.util.Config;
import dna.util.network.NetworkEventReader;
import dna.util.network.tcp.TCPEvent.TCPEventField;

/**
 * Reader that reads TCPEvents from a TCP-List-File.
 * 
 * @author Rwilmes
 * 
 */
public class TCPEventReader extends NetworkEventReader {

	protected TCPEvent bufferedEvent;
	protected TCPEventField[] fields;

	protected DateTimeFormatter durationFormat;

	public TCPEventReader(String dir, String filename, TCPEventField... fields)
			throws FileNotFoundException {
		this(dir, filename, Config.get("TCP_LIST_DEFAULT_DELIMITER"), Config
				.get("TCP_LIST_DEFAULT_TIME_FORMAT"), Config
				.get("TCP_LIST_DEFAULT_DURATION_FORMAT"), fields);
	}

	public TCPEventReader(String dir, String filename, String delimiter,
			String timeFormat, String durationFormat, TCPEventField... fields)
			throws FileNotFoundException {
		super(dir, filename, delimiter, timeFormat);
		this.durationFormat = DateTimeFormat.forPattern(durationFormat);
		this.fields = fields;
		try {
			this.bufferedEvent = parseLine(readString());
		} catch (IOException e) {
			this.finished = true;
			e.printStackTrace();
		}
	}

	@Override
	public TCPEvent getNextEvent() {
		if (finished)
			return null;

		TCPEvent e = this.bufferedEvent;
		String line;
		try {
			line = readString();
			System.out.println("XXXX:   " + line);
			if (line != null)
				// return parseLine(line);
				this.bufferedEvent = parseLine(line);
			else
				this.finished = true;
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		return e;
	}

	@Override
	protected TCPEvent parseLine(String line) {
		String[] splits = line.split(this.delimiter);

		long id = -1;
		DateTime time = null;
		DateTime duration = null;
		String service = null;
		int srcPort = 0;
		int dstPort = 0;
		String srcIp = null;
		String dstIp = null;
		double attackScore = 0;
		String name = null;

		for (int i = 0; (i < this.fields.length && i < splits.length); i++) {
			String x = splits[i];
			switch (this.fields[i]) {
			case TIME:
				time = timeFormat.parseDateTime(x);
				break;
			case SRC_IP:
				srcIp = x;
				break;
			case SRC_PORT:
				try {
					srcPort = Integer.parseInt(x);
				} catch (NumberFormatException e) {
				}
				break;
			case DST_IP:
				dstIp = x;
				break;
			case DST_PORT:
				try {
					dstPort = Integer.parseInt(x);
				} catch (NumberFormatException e) {
				}
			case ATTACK_SCORE:
				try {
					attackScore = Double.parseDouble(x);
				} catch (NumberFormatException e) {
				}
				break;
			case DURATION:
				duration = durationFormat.parseDateTime(x);
				break;
			case ID:
				id = Long.parseLong(x);
				break;
			case NAME:
				name = x;
				break;
			case SERVICE:
				service = x;
				break;
			case NONE:
				break;
			}
		}

		System.out.println("RETURNING TCP EVENT: " + id + "\t" + srcIp + "\t"
				+ dstIp + "\t" + dstPort);

		return new TCPEvent(id, time, duration, service, srcPort, dstPort,
				srcIp, dstIp, attackScore, name);
	}
}
