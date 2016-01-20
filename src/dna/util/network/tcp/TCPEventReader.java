package dna.util.network.tcp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import dna.graph.nodes.Node;
import dna.util.Config;
import dna.util.Log;
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
	protected HashMap<Integer, Node> portNodeMap;
	protected ArrayList<Node> portNodes;

	protected ArrayList<String> ips;
	protected HashMap<String, Integer> ipMap;
	protected HashMap<String, Node> ipNodeMap;
	protected ArrayList<Node> ipNodes;

	protected boolean removeZeroDegreeNodes = true;

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
				new HashMap<Integer, Node>(), new ArrayList<Node>(),
				new ArrayList<String>(), new HashMap<String, Integer>(),
				new HashMap<String, Node>(), new ArrayList<Node>(), fields);
	}

	protected TCPEventReader(String dir, String filename, String separator,
			String timeFormat, String durationFormat, ArrayList<Integer> ports,
			HashMap<Integer, Integer> portMap,
			HashMap<Integer, Node> portNodeMap, ArrayList<Node> portNodes,
			ArrayList<String> ips, HashMap<String, Integer> ipMap,
			HashMap<String, Node> ipNodeMap, ArrayList<Node> ipNodes,
			TCPEventField... fields) throws FileNotFoundException {
		super(dir, filename, separator, timeFormat);
		this.durationFormatPattern = durationFormat;
		this.durationFormat = DateTimeFormat.forPattern(durationFormat);
		this.fields = fields;
		this.ports = ports;
		this.portMap = portMap;
		this.portNodeMap = portNodeMap;
		this.portNodes = portNodes;

		this.ips = ips;
		this.ipMap = ipMap;
		this.ipNodeMap = ipNodeMap;
		this.ipNodes = ipNodes;

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

	public HashMap<Integer, Node> getPortNodeMap() {
		return portNodeMap;
	}

	public ArrayList<String> getIps() {
		return this.ips;
	}

	public HashMap<String, Integer> getIpMap() {
		return this.ipMap;
	}

	public HashMap<String, Node> getIpNodeMap() {
		return ipNodeMap;
	}

	public void addPortNode(int port, Node node) {
		if (!ports.contains(port))
			ports.add(port);
		if (!portNodes.contains(node))
			portNodes.add(node);
		if (!portNodeMap.containsKey(port))
			portNodeMap.put(port, node);
	}

	public void addIpNode(String ip, Node node) {
		if (!ips.contains(ip))
			ips.add(ip);
		if (!ipNodes.contains(node))
			ipNodes.add(node);
		if (!ipNodeMap.containsKey(ip))
			ipNodeMap.put(ip, node);
	}

	public void removeNode(Node node) {
		if (portNodeMap.containsValue(node)) {
			int index = portNodes.indexOf(node);
			portNodeMap.remove(ports.get(index));
			portNodes.remove(index);
			ports.remove(index);
		}
		if (ipNodeMap.containsValue(node)) {
			int index = ipNodes.indexOf(node);
			ipNodeMap.remove(ips.get(index));
			ipNodes.remove(index);
			ips.remove(index);
		}
	}

	public void removeIpNode(String ip) {
		ips.remove(ips.indexOf(ip));
		ipNodeMap.remove(ip);
	}

	public void removePortNode(int port) {
		ports.remove(ports.indexOf(port));
		portNodeMap.remove(port);
	}

	public int addPort(int port) {
		if (!ports.contains(port))
			this.ports.add(port);
		int mapping = mapPort(port);

		if (!portMap.containsKey(mapping))
			this.portMap.put(port, mapping);

		return mapping;
	}

	public int addIp(String ip) {
		if (!ips.contains(ip))
			this.ips.add(ip);
		int mapping = mapIp(ip);

		if (!ipMap.containsKey(ip))
			this.ipMap.put(ip, mapping);

		return mapping;
	}

	/** Maps a port to its id. **/
	public int mapPort(int port) {
		return port;
	}

	/** Maps an ip to its id. **/
	public int mapIp(String ip) {
		return (this.getIps().size() - 1) + ipOffset;
	}

	public boolean containsPortNode(int port) {
		return this.ports.contains(port) && this.portNodeMap.containsKey(port);
	}

	public boolean containsPort(int port) {
		return this.ports.contains(port);
	}

	public int mapp(String ip) {
		if (this.ips.contains(ip)) {
			return ipMap.get(ip);
		} else {
			int mapping = mapIp(ip);
			this.ips.add(ip);
			this.ipMap.put(ip, mapping);
			return mapping;
		}
	}

	public int mapp(int port) {
		if (this.ports.contains(port)) {
			return portMap.get(port);
		} else {
			int mapping = mapPort(port);
			this.ports.add(port);
			this.portMap.put(port, mapping);
			return mapping;
		}
	}

	public boolean containsIp(String ip) {
		return this.ips.contains(ip);
	}

	public boolean containsIpNode(String ip) {
		// System.out.println("containsIpNode?? \t" + ip + "\t" +
		// ips.contains(ip) + "\t" + ip+ "\t" +
		// this.ipNodeMap.containsKey(mapp(ip)));
		return this.ips.contains(ip) && this.ipNodeMap.containsKey(ip);
	}

	public int getPortMapping(int port) {
		return this.portMap.get(port);
	}

	public int getIpMapping(String ip) {
		return this.ipMap.get(ip);
	}

	public TCPEventReader copy() throws FileNotFoundException {
		return new TCPEventReader(dir, filename, separator, timeFormatPattern,
				durationFormatPattern, ports, portMap, portNodeMap, portNodes,
				ips, ipMap, ipNodeMap, ipNodes, fields);
	}

	public boolean isRemoveZeroDegreeNodes() {
		return removeZeroDegreeNodes;
	}

	public void printMappings() {
		Log.infoSep();
		Log.info("PORTS:");
		for (int i = 0; i < ports.size(); i++) {
			Log.info("\t" + ports.get(i) + "\t=>\t" + portMap.get(ports.get(i)));
		}
		Log.info("IPS:");
		for (int i = 0; i < ips.size(); i++) {
			Log.info("\t" + ips.get(i) + "\t=>\t" + ipMap.get(ips.get(i)));
		}
		Log.infoSep();
	}
}
