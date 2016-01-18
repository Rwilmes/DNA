package dna.util.network.tcp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import dna.util.Config;
import dna.util.network.NetworkEventReader;
import dna.util.network.tcp.TCPEvent.TCPEventField;

/**
 * Reader that reads TCPEvents from a TCP-List-File. <br>
 * 
 * Also maps ips and ports to nodes. These mapping may be accessed via several
 * ways.
 * 
 * @author Rwilmes
 * 
 */
public class TCPEventReader extends NetworkEventReader {

	protected static final int ipOffset = 100000;

	protected TCPEvent bufferedEvent;
	protected TCPEventField[] fields;

	protected DateTimeFormatter durationFormat;
	protected String durationFormatPattern;

	protected ArrayList<Integer> ports;
	protected HashMap<Integer, Integer> portMap;

	protected ArrayList<String> ips;
	protected HashMap<String, Integer> ipMap;

	public TCPEventReader(String dir, String filename, TCPEventField... fields)
			throws FileNotFoundException {
		this(dir, filename, Config.get("TCP_LIST_DEFAULT_SEPARATOR"), Config
				.get("TCP_LIST_DEFAULT_TIME_FORMAT"), Config
				.get("TCP_LIST_DEFAULT_DURATION_FORMAT"), fields);
	}

	public TCPEventReader(String dir, String filename, String separator,
			String timeFormat, String durationFormat, TCPEventField... fields)
			throws FileNotFoundException {
		this(dir, filename, separator, timeFormat, durationFormat,
				new ArrayList<Integer>(), new HashMap<Integer, Integer>(),
				new ArrayList<String>(), new HashMap<String, Integer>(), fields);
	}

	protected TCPEventReader(String dir, String filename, String separator,
			String timeFormat, String durationFormat, ArrayList<Integer> ports,
			HashMap<Integer, Integer> portMap, ArrayList<String> ips,
			HashMap<String, Integer> ipMap, TCPEventField... fields)
			throws FileNotFoundException {
		super(dir, filename, separator, timeFormat);
		this.durationFormatPattern = durationFormat;
		this.durationFormat = DateTimeFormat.forPattern(durationFormat);
		this.fields = fields;
		this.ports = ports;
		this.portMap = portMap;
		this.ips = ips;
		this.ipMap = ipMap;

		try {
			this.bufferedEvent = parseLine(readString());
		} catch (IOException e) {
			this.finished = true;
			e.printStackTrace();
		}
	}

	public void setAll(ArrayList<Integer> ports,
			HashMap<Integer, Integer> portMap, ArrayList<String> ips,
			HashMap<String, Integer> ipMap) {
		this.ports = ports;
		this.portMap = portMap;
		this.ips = ips;
		this.ipMap = ipMap;
	}

	@Override
	public TCPEvent getNextEvent() {
		if (finished)
			return null;

		TCPEvent e = this.bufferedEvent;
		String line;
		try {
			line = readString();
			if (line != null)
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
		String[] splits = line.split(this.separator);

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

		return new TCPEvent(id, time, duration, service, srcPort, dstPort,
				srcIp, dstIp, attackScore, name);
	}

	public ArrayList<Integer> getPorts() {
		return this.ports;
	}

	public HashMap<Integer, Integer> getPortMap() {
		return this.portMap;
	}

	public ArrayList<String> getIps() {
		return this.ips;
	}

	public HashMap<String, Integer> getIpMap() {
		return this.ipMap;
	}

	public int addPort(int port) {
		this.ports.add(port);
		int mapping = mapPort(port);
		this.portMap.put(port, mapping);
		return mapping;
	}

	public int addIp(String ip) {
		this.ips.add(ip);
		int mapping = mapIp(ip);
		this.ipMap.put(ip, mapping);
		return mapping;
	}

	/** Maps a port to its id. **/
	protected int mapPort(int port) {
		return port;
	}

	/** Maps an ip to its id. **/
	protected int mapIp(String ip) {
		return (this.getIps().size() - 1) + ipOffset;
	}

	public boolean containsPort(int port) {
		return this.ports.contains(port);
	}

	public boolean containsIp(String ip) {
		return this.ips.contains(ip);
	}

	public int getPortMapping(int port) {
		return this.portMap.get(port);
	}

	public int getIpMapping(String ip) {
		return this.ipMap.get(ip);
	}

	public TCPEventReader copy() throws FileNotFoundException {
		return new TCPEventReader(dir, filename, separator, timeFormatPattern,
				durationFormatPattern, ports, portMap, ips, ipMap, fields);
	}
}
