package dna.util.network.netflow;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import dna.graph.generators.network.NetworkEdge;
import dna.labels.labeler.darpa.EntryBasedAttackLabeler;
import dna.util.Config;
import dna.util.network.NetworkEvent;
import dna.util.network.NetworkReader;
import dna.util.network.netflow.NetflowEvent.NetflowDirection;
import dna.util.network.netflow.NetflowEvent.NetflowEventField;

public class NetflowEventReader extends NetworkReader {

	// CLASS
	protected String separator;
	protected NetflowEvent bufferedEvent;
	protected NetflowEventField[] fields;

	// configuration
	protected int edgeLifeTimeSeconds;
	protected int batchIntervalSeconds;

	protected boolean generateEmptyBatches;
	protected boolean removeZeroDegreeNodes;
	protected boolean removeZeroWeightEdges;

	// times
	protected DateTime initTimestamp;
	protected DateTime maximumTimestamp;
	protected DateTime minimumTimestamp;

	// labels
	protected EntryBasedAttackLabeler labeler;
	protected ArrayList<String> labelsOccuredInCurrentBatch;

	// edge queue
	public LinkedList<NetworkEdge> edgeQueue = new LinkedList<NetworkEdge>();

	protected boolean debug = false;

	// reader
	protected boolean finished;

	protected DateTimeFormatter timeFormat;
	protected DateTimeFormatter dateFormat;

	protected String dir;
	protected String filename;

	public NetflowEventReader(String dir, String filename,
			NetflowEventField... fields) throws FileNotFoundException {
		this(dir, filename, Config.get("NETFLOW_READER_DEFAULT_SEPARATOR"),
				Config.get("NETFLOW_READER_DEFAULT_DATE_FORMAT"), Config
						.get("NETFLOW_READER_DEFAULT_TIME_FORMAT"), fields);
	}

	public NetflowEventReader(String dir, String filename, String separator,
			String dateFormat, String timeFormat, NetflowEventField... fields)
			throws FileNotFoundException {
		super(dir, filename);
		this.dir = dir;
		this.filename = filename;
		this.separator = separator;
		this.fields = fields;
		this.timeFormat = DateTimeFormat.forPattern(timeFormat);
		this.dateFormat = DateTimeFormat.forPattern(dateFormat);

		this.removeZeroDegreeNodes = Config
				.getBoolean("NETFLOW_READER_DEFAULT_REMOVE_ZERO_DEGREE_NODES");
		this.removeZeroWeightEdges = Config
				.getBoolean("NETFLOW_READER_DEFAULT_REMOVE_ZERO_WEIGHT_EDGES");

		this.finished = false;

		this.edgeQueue = new LinkedList<NetworkEdge>();

		skipToInitEvent();
	}

	protected void skipToInitEvent() {
		try {
			this.bufferedEvent = parseLine(readString());

			if (this.minimumTimestamp != null) {
				while (this.bufferedEvent.getTime().isBefore(
						this.minimumTimestamp)) {
					this.bufferedEvent = parseLine(readString());

					if (this.bufferedEvent == null)
						break;
				}
			}

			if (this.bufferedEvent != null)
				this.initTimestamp = this.bufferedEvent.getTime();
			else
				this.finished = true;
		} catch (IOException e) {
			this.finished = true;
			e.printStackTrace();
		}
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

	protected String getIdentifier(int src, int dst) {
		return src + "->" + dst;
	}

	/**
	 * Returns the timestamp of the next decrement-edge event or -1 if queue is
	 * empty.
	 **/
	public long getNextDecrementEdgesTimestamp() {
		if (isEventQueueEmpty())
			return -1;
		else
			return getFirstEdgeFromQueue().getTime();
	}

	/** Returns all decrement events until the threshold. **/
	public ArrayList<NetworkEdge> getDecrementEdges(long threshold) {
		ArrayList<NetworkEdge> list = new ArrayList<NetworkEdge>();

		boolean finished = false;
		while (!finished && !edgeQueue.isEmpty()) {
			NetworkEdge e = getFirstEdgeFromQueue();
			long t = e.getTime();

			if (t <= threshold)
				list.add(popFirstEdgeFromQueue());
			else
				finished = true;
		}

		return list;
	}

	public void addEdgeToQueue(NetworkEdge e) {
		// only add events that are before maximumTimestamp
		if (this.maximumTimestamp != null)
			if (e.getTime() > this.maximumTimestamp.getMillis())
				return;

		edgeQueue.add(e);
	}

	public NetworkEdge getFirstEdgeFromQueue() {
		return edgeQueue.getFirst();
	}

	public NetworkEdge popFirstEdgeFromQueue() {
		return edgeQueue.removeFirst();
	}

	public boolean isEventQueueEmpty() {
		return edgeQueue.isEmpty();
	}

	/** Returns the timestamp of the next event or -1 if no event is buffered. **/
	public long getNextEventTimestamp() {
		if (bufferedEvent != null)
			return bufferedEvent.getTime().getMillis();
		else
			return -1;
	}

	/** Reads and returns all events until the threshold is reached. **/
	public ArrayList<NetworkEvent> getEventsUntil(DateTime threshold) {
		ArrayList<NetworkEvent> events = new ArrayList<NetworkEvent>();

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

	public NetflowEvent getNextEvent() {
		if (finished)
			return null;

		NetflowEvent e = this.bufferedEvent;
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

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	protected NetflowEvent parseLine(String line) {
		System.out.println(line);

		String[] splits = line.split(this.separator);

		long id = 0;

		DateTime time = null;
		DateTime date = new DateTime(0);
		String srcAddress = null;
		String dstAddress = null;
		NetflowDirection direction = null;
		String flags = null;
		String connectionState = null;
		double duration = 0;
		String label = null;

		String protocol = null;
		String srcPort = null;
		String dstPort = null;

		int packets = 0;
		int packetsToSrc = 0;
		int packetsToDestination = 0;

		int bytes = 0;
		int bytesToSrc = 0;
		int bytesToDestination = 0;

		for (int i = 0; (i < this.fields.length && i < splits.length); i++) {
			String x = splits[i];
			switch (this.fields[i]) {
			case Bytes:
				bytes = Integer.parseInt(x);
				break;
			case BytesToDestination:
				bytesToDestination = Integer.parseInt(x);
				break;
			case BytesToSrc:
				bytesToSrc = Integer.parseInt(x);
				break;
			case ConnectionState:
				connectionState = x;
				break;
			case Date:
				date = this.dateFormat.parseDateTime(x);
				break;
			case Direction:
				if (x.equals("->") || x.equals("-->"))
					direction = NetflowDirection.forward;
				if (x.equals("<-") || x.equals("<--"))
					direction = NetflowDirection.backward;
				if (x.equals("<->") || x.equals("<-->"))
					direction = NetflowDirection.bidirectional;
				break;
			case DstAddress:
				dstAddress = x;
				break;
			case DstPort:
				dstPort = x;
				break;
			case Duration:
				duration = Double.parseDouble(x);
				break;
			case Flags:
				flags = x;
				break;
			case Label:
				label = x;
				break;
			case None:
				break;
			case numberOfNetflows:
				break;
			case Packets:
				packets = Integer.parseInt(x);
				break;
			case PacketsToDestination:
				packetsToDestination = Integer.parseInt(x);
				break;
			case PacketToSrc:
				packetsToSrc = Integer.parseInt(x);
				break;
			case Protocol:
				protocol = x;
				break;
			case SrcAddress:
				srcAddress = x;
				break;
			case SrcPort:
				srcPort = x;
				break;
			case Time:
				time = this.timeFormat.parseDateTime(x);
				break;
			}
		}

		DateTime dateTime = (time != null) ? date.plusSeconds(time
				.getSecondOfDay()) : date;

		return new NetflowEvent(id, dateTime, srcAddress, dstAddress, duration,
				direction, connectionState, flags, protocol, srcPort, dstPort,
				packets, packetsToSrc, packetsToDestination, bytes, bytesToSrc,
				bytesToDestination, label);
	}

	public String getDir() {
		return dir;
	}

	public String getFilename() {
		return filename;
	}

	public int getBatchIntervalSeconds() {
		return batchIntervalSeconds;
	}

	public NetflowEventField[] getFields() {
		return fields;
	}

	public void setFields(NetflowEventField[] fields) {
		this.fields = fields;
	}

	public int getEdgeLifeTimeSeconds() {
		return edgeLifeTimeSeconds;
	}

	public void setEdgeLifeTimeSeconds(int edgeLifeTimeSeconds) {
		this.edgeLifeTimeSeconds = edgeLifeTimeSeconds;
	}

	public boolean isGenerateEmptyBatches() {
		return generateEmptyBatches;
	}

	public void setGenerateEmptyBatches(boolean generateEmptyBatches) {
		this.generateEmptyBatches = generateEmptyBatches;
	}

	public boolean isRemoveZeroDegreeNodes() {
		return removeZeroDegreeNodes;
	}

	public void setRemoveZeroDegreeNodes(boolean removeZeroDegreeNodes) {
		this.removeZeroDegreeNodes = removeZeroDegreeNodes;
	}

	public boolean isRemoveZeroWeightEdges() {
		return removeZeroWeightEdges;
	}

	public void setRemoveZeroWeightEdges(boolean removeZeroWeightEdges) {
		this.removeZeroWeightEdges = removeZeroWeightEdges;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public void setBatchIntervalSeconds(int batchIntervalSeconds) {
		this.batchIntervalSeconds = batchIntervalSeconds;
	}

	public DateTime getInitTimestamp() {
		return this.initTimestamp;
	}

}