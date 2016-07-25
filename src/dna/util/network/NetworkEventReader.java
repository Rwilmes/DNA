package dna.util.network;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import dna.graph.generators.network.NetworkEdge;
import dna.graph.generators.network.UpdateEvent;
import dna.util.Config;
import dna.util.network.NetworkEventOld.NetworkEventField;

/**
 * Reader that reads NetworkEvents from a list-file.
 * 
 * @author Rwilmes
 * 
 */
public class NetworkEventReader extends NetworkReader {

	protected String separator;
	protected NetworkEventField[] fields;

	protected boolean finished;

	protected NetworkEvent bufferedEvent;

	protected String timeFormatPattern;
	protected DateTimeFormatter timeFormat;

	protected String dir;
	protected String filename;

	public NetworkEventReader(String dir, String filename,
			NetworkEventField... fields) throws FileNotFoundException {
		this(dir, filename, Config.get("TCP_LIST_DEFAULT_SEPARATOR"), Config
				.get("TCP_LIST_DEFAULT_TIME_FORMAT"), fields);
	}

	public NetworkEventReader(String dir, String filename, String separator,
			String timeFormat, NetworkEventField... fields)
			throws FileNotFoundException {
		super(dir, filename);
		this.dir = dir;
		this.filename = filename;
		this.separator = separator;
		this.fields = fields;
		this.timeFormatPattern = timeFormat;
		this.timeFormat = DateTimeFormat.forPattern(timeFormat);
		this.finished = false;
	}

	public NetworkEvent getNextEvent() {
		if (finished)
			return null;

		NetworkEvent e = this.bufferedEvent;
		String line;
		try {
			line = readString();
			if (line != null)
				this.bufferedEvent = parseLine(line);
			else {
				this.finished = true;
				this.bufferedEvent = null;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		return e;
	}

	public boolean isNextEventPossible() {
		return !finished || bufferedEvent != null;
	}

	protected NetworkEventOld parseLine(String line) {
		String[] splits = line.split(this.separator);

		DateTime time = null;
		int srcPort = -1;
		int dstPort = -1;
		String srcIp = null;
		String dstIp = null;

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
				break;
			case NONE:
				break;
			}
		}

		return new NetworkEventOld(time, srcIp, srcPort, dstIp, dstPort);
	}

	public String getDir() {
		return dir;
	}

	public String getFilename() {
		return filename;
	}

	public String getTimeFormatPattern() {
		return timeFormatPattern;
	}

	public DateTimeFormatter getTimeFormat() {
		return timeFormat;
	}

	@Override
	public ArrayList<NetworkEvent> getEventsUntil(DateTime threshold) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<UpdateEvent> getDecrementEvents(long threshold) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getNextEventTimestamp() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getNextDecrementEventsTimestamp() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public HashMap<String, Integer> getWeightDecrementals(
			ArrayList<NetworkEdge> decrementEdges) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEventQueueEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isGenerateEmptyBatches() {
		// TODO Auto-generated method stub
		return false;
	}
}
