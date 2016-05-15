package dna.util.network.netflow;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import dna.io.Reader;
import dna.labels.labeler.darpa.EntryBasedAttackLabeler;
import dna.util.Config;
import dna.util.network.netflow.NetflowEvent.NetflowDirection;
import dna.util.network.netflow.NetflowEvent.NetflowEventField;

public class NetflowEventReader extends Reader {

	// CLASS
	protected String separator;
	protected NetflowEvent bufferedEvent;
	protected NetflowEventField[] fields;

	protected NetflowEventField source;
	protected NetflowEventField destination;
	protected NetflowEventField[] intermediateNodes;

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
		this.finished = false;
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

	protected NetflowEvent parseLine(String line) {
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

		DateTime dateTime = date.plusSeconds(time.getSecondOfDay());

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

}
