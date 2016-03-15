package dna.util.network.tcp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import dna.graph.generators.network.NetworkEdge;
import dna.graph.weights.longW.LongWeight;
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

	// STATICS
	public static final int defaultBatchLengthSeconds = 1;
	public static final long defaultEdgeLifeTimeMillis = 60000;

	public static int timestampOffset = 0;

	public static final boolean defaultRemoveInactiveEdges = true;
	public static final boolean defaultRemoveZeroDegreeNodes = true;

	public static final int defaultEdgeWeightIncrSteps = 1;
	public static final int defaultEdgeWeightDecrSteps = 1;

	// CLASS
	protected static final int ipOffset = 100000;
	protected static final int servicePortOffset = 100000;

	protected TCPEvent bufferedEvent;
	protected TCPEventField[] fields;

	protected DateTimeFormatter durationFormat;
	protected String durationFormatPattern;

	protected ArrayList<Integer> ports;
	protected HashMap<Integer, Integer> portMap;

	protected ArrayList<String> ips;
	protected HashMap<String, Integer> ipMap;

	protected ArrayList<String> activeNodes;

	protected HashMap<String, Integer> servicePortMap;

	protected HashMap<NetworkEdge, LongWeight> edgeWeightMap;

	protected boolean removeZeroDegreeNodes;
	protected boolean removeInactiveEdges;
	protected long edgeLifetimeMillis;

	protected int edgeWeightsIncrSteps;
	protected int edgeWeightsDecrSteps;

	protected int batchIntervalInSeconds;

	protected DateTime initTimestamp;

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
				defaultBatchLengthSeconds, defaultRemoveZeroDegreeNodes,
				defaultRemoveInactiveEdges, defaultEdgeLifeTimeMillis,
				defaultEdgeWeightIncrSteps, defaultEdgeWeightDecrSteps,
				new ArrayList<Integer>(), new HashMap<Integer, Integer>(),
				new ArrayList<String>(), new HashMap<String, Integer>(),
				new ArrayList<String>(),
				new HashMap<NetworkEdge, LongWeight>(),
				new HashMap<String, Integer>(), fields);
	}

	protected TCPEventReader(String dir, String filename, String separator,
			String timeFormat, String durationFormat,
			int batchIntervalInSeconds, boolean removeZeroDegreeNodes,
			boolean removeInactiveEdges, long edgeLifetimeMillis,
			int edgeWeightIncrSteps, int edgeWeightDecrSteps,
			ArrayList<Integer> ports, HashMap<Integer, Integer> portMap,
			ArrayList<String> ips, HashMap<String, Integer> ipMap,
			ArrayList<String> activeNodes,
			HashMap<NetworkEdge, LongWeight> edgeWeightMap,
			HashMap<String, Integer> servicePortMap, TCPEventField... fields)
			throws FileNotFoundException {
		super(dir, filename, separator, timeFormat);

		// init
		this.batchIntervalInSeconds = batchIntervalInSeconds;

		this.removeZeroDegreeNodes = removeZeroDegreeNodes;

		this.removeInactiveEdges = removeInactiveEdges;
		this.edgeLifetimeMillis = edgeLifetimeMillis;
		this.edgeWeightsIncrSteps = edgeWeightIncrSteps;
		this.edgeWeightsDecrSteps = edgeWeightDecrSteps;

		this.servicePortMap = servicePortMap;

		this.durationFormatPattern = durationFormat;
		this.durationFormat = DateTimeFormat.forPattern(durationFormat);
		this.fields = fields;
		this.ports = ports;
		this.portMap = portMap;

		this.ips = ips;
		this.ipMap = ipMap;

		this.activeNodes = activeNodes;
		this.edgeWeightMap = edgeWeightMap;
		this.eq = new LinkedList<NetworkEdge>();

		try {
			this.bufferedEvent = parseLine(readString());
			this.initTimestamp = this.bufferedEvent.getTime();
		} catch (IOException e) {
			this.finished = true;
			e.printStackTrace();
		}
	}

	public DateTime getInitTimestamp() {
		return this.initTimestamp;
	}

	@Override
	public TCPEvent getNextEvent() {
		TCPEvent e = this.bufferedEvent;

		String line;
		try {
			line = readString();
			if (line != null)
				this.bufferedEvent = parseLine(line);
			else {
				this.bufferedEvent = null;
				this.finished = true;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		return e;
	}

	/** Reads and returns all events until the threshold is reached. **/
	public ArrayList<TCPEvent> getEventsUntil(DateTime threshold) {
		ArrayList<TCPEvent> events = new ArrayList<TCPEvent>();

		// if next event above threshold -> return empty list
		if (bufferedEvent != null)
			if (bufferedEvent.getTime().isAfter(threshold))
				return events;

		// read events and add them to list
		while (isNextEventPossible()) {
			// if next-event after threshold, return
			if (bufferedEvent.getTime().isAfter(threshold))
				return events;

			events.add(getNextEvent());
		}

		// return
		return events;
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
				if (TCPEventReader.timestampOffset > 0)
					time = time.plusSeconds(TCPEventReader.timestampOffset);
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

		if (srcPort == 0) {
			srcPort = mapServiceToPort(service);
		}
		if (dstPort == 0) {
			dstPort = mapServiceToPort(service);
		}

		TCPEvent e = new TCPEvent(id, time, duration, service, srcPort,
				dstPort, srcIp, dstIp, attackScore, name);
		System.out.println(line);
		System.out.println("\t" + e.toString());
		return new TCPEvent(id, time, duration, service, srcPort, dstPort,
				srcIp, dstIp, attackScore, name);
	}

	public void addPort(int port) {
		if (!ports.contains(port))
			ports.add(port);
	}

	public void addIp(String ip) {
		if (!ips.contains(ip))
			ips.add(ip);
	}

	public void addNode(String id) {
		if (!activeNodes.contains(id))
			activeNodes.add(id);
	}

	public boolean isNodeActive(String id) {
		return activeNodes.contains(id);
	}

	public void removeNode(String id) {
		if (activeNodes.contains(id))
			activeNodes.remove(activeNodes.indexOf(id));
	}

	/** Maps a port to its id. **/
	public int mapPort(int port) {
		return port;
	}

	/** Maps an ip to its id. **/
	public int mapIp(String ip) {
		return (this.ips.size() - 1) + ipOffset;
	}

	public int getNextMapping() {
		return (this.ips.size() + this.ports.size());
	}

	public boolean containsPort(int port) {
		return this.ports.contains(port);
	}

	public int mapp(String ip) {
		if (this.ips.contains(ip)) {
			return ipMap.get(ip);
		} else {
			// int mapping = mapIp(ip);
			int mapping = getNextMapping();
			this.ips.add(ip);
			this.ipMap.put(ip, mapping);
			return mapping;
		}
	}

	public int mapp(int port) {
		if (this.ports.contains(port)) {
			return portMap.get(port);
		} else {
			// int mapping = mapPort(port);
			int mapping = getNextMapping();
			this.ports.add(port);
			this.portMap.put(port, mapping);
			return mapping;
		}
	}

	public boolean containsIp(String ip) {
		return this.ips.contains(ip);
	}

	public TCPEventReader copy() throws FileNotFoundException {
		return new TCPEventReader(dir, filename, separator, timeFormatPattern,
				durationFormatPattern, batchIntervalInSeconds,
				removeZeroDegreeNodes, removeInactiveEdges, edgeLifetimeMillis,
				edgeWeightsIncrSteps, edgeWeightsDecrSteps, ports, portMap,
				ips, ipMap, activeNodes, edgeWeightMap, servicePortMap, fields);
	}

	public boolean isRemoveZeroDegreeNodes() {
		return this.removeZeroDegreeNodes;
	}

	public void setRemoveZeroDegreeNodes(boolean flag) {
		this.removeZeroDegreeNodes = flag;
	}

	public void setRemoveInactiveEdges(boolean flag) {
		this.removeInactiveEdges = flag;
	}

	public boolean isRemoveInactiveEdges() {
		return this.removeInactiveEdges;
	}

	public int getBatchInterval() {
		return this.batchIntervalInSeconds;
	}

	public void setBatchInterval(int seconds) {
		this.batchIntervalInSeconds = seconds;
	}

	public long getEdgeLifeTimeMillis() {
		return this.edgeLifetimeMillis;
	}

	public void setEdgeLifeTime(long millis) {
		this.edgeLifetimeMillis = millis;
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

	public LongWeight getEdgeWeight(NetworkEdge e) {
		if (this.edgeWeightMap.containsKey(e))
			return this.edgeWeightMap.get(e);
		else
			return null;
	}

	public boolean isEdgeWeightZero(NetworkEdge e) {
		if (this.edgeWeightMap.containsKey(e))
			return (this.edgeWeightMap.get(e).getWeight() == 0);
		else
			return true;
	}

	public void setEdgeWeight(NetworkEdge e, long w) {
		this.edgeWeightMap.put(e, new LongWeight(w));
	}

	public void incrementEdgeWeight(NetworkEdge e) {
		System.out.println("INCREMENTING EDGEWEIGHT : " + e.toString());
		// get weight
		long w = 0;
		if (this.edgeWeightMap.containsKey(e)) {
			w = this.edgeWeightMap.get(e).getWeight();
		}

		// put weight
		this.edgeWeightMap.put(e, new LongWeight(w + 1));
	}

	public void decrementEdgeWeight(NetworkEdge e) {
		// get weight
		long w = this.edgeWeightMap.get(e).getWeight();

		// put weight
		this.edgeWeightMap.put(e, new LongWeight(w - 1));
	}

	public HashMap<NetworkEdge, LongWeight> getEdgeWeightMap() {
		return this.edgeWeightMap;
	}

	public LinkedList<NetworkEdge> eq = new LinkedList<NetworkEdge>();

	public void addEdgeToQueue(NetworkEdge e) {
		eq.add(e);
	}

	public NetworkEdge getFirstEdgeFromQueue() {
		return eq.getFirst();
	}

	public NetworkEdge popFirstEdgeFromQueue() {
		return eq.removeFirst();
	}

	public boolean isEventQueueEmpty() {
		return eq.isEmpty();
	}

	/** Returns all decrement events until the threshold. **/
	public ArrayList<NetworkEdge> getDecrementEdges(long threshold) {
		ArrayList<NetworkEdge> list = new ArrayList<NetworkEdge>();

		boolean finished = false;
		while (!finished && !eq.isEmpty()) {
			NetworkEdge e = getFirstEdgeFromQueue();
			long t = e.getTime();

			if (t <= threshold)
				list.add(popFirstEdgeFromQueue());
			else
				finished = true;
		}

		return list;
	}

	/** Returns a map with the sum of all weight decrementals per edge. **/
	public HashMap<String, Integer> getWeightDecrementals(
			ArrayList<NetworkEdge> allEdges) {
		HashMap<String, Integer> wcMap = new HashMap<String, Integer>();
		for (int i = 0; i < allEdges.size(); i++) {
			NetworkEdge e = allEdges.get(i);
			decrementWeightChanges(e.getSrc(), e.getDst(), wcMap);
		}

		return wcMap;
	}

	protected String getIdentifier(int src, int dst) {
		return src + "->" + dst;
	}

	protected void incrementWeightChanges(int src, int dst,
			HashMap<String, Long> map) {
		// add incrementals to map
		String identifier = getIdentifier(src, dst);
		if (map.containsKey(identifier)) {
			map.put(identifier, map.get(identifier) + 1);
		} else {
			map.put(identifier, 1L);
		}
	}

	protected void decrementWeightChanges(int src, int dst,
			HashMap<String, Integer> map) {
		// add incrementals to map
		String identifier = getIdentifier(src, dst);
		if (map.containsKey(identifier)) {
			map.put(identifier, map.get(identifier) - 1);
		} else {
			map.put(identifier, -1);
		}
	}

	protected int mapServiceToPort(String service) {
		if (this.servicePortMap.containsKey(service)) {
			return servicePortMap.get(service);
		} else {
			int mapping = servicePortOffset + this.servicePortMap.size();
			return mapping;
		}
	}
}
