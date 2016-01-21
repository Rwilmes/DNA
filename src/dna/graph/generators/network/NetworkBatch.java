package dna.graph.generators.network;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.joda.time.DateTime;

import dna.graph.Graph;
import dna.graph.edges.Edge;
import dna.graph.nodes.Node;
import dna.updates.batch.Batch;
import dna.updates.generators.BatchGenerator;
import dna.util.network.NetworkEvent;
import dna.util.network.tcp.DefaultTCPEventReader;
import dna.util.network.tcp.TCPEvent;
import dna.util.network.tcp.TCPEventReader;

/**
 * A batch-generator which creates batches based on a tcp-list file.
 * 
 * @author Rwilmes
 * 
 */
public abstract class NetworkBatch extends BatchGenerator {

	protected int batchLength;

	protected DateTime threshold;

	protected boolean init;
	protected boolean finished;

	protected TCPEventReader reader;

	protected TCPEvent bufferedEntry;

	public NetworkBatch(String name, TCPEventReader reader,
			int batchIntervalInSeconds) throws FileNotFoundException {
		super(name, null);
		this.reader = reader;
		this.batchLength = batchIntervalInSeconds;

		this.init = false;
		this.finished = false;
	}

	public NetworkBatch(String name, String dir, String filename,
			int batchIntervalInSeconds) throws IOException {
		this(name, new DefaultTCPEventReader(dir, filename),
				batchIntervalInSeconds);
	}

	public abstract void onEvent(Graph g, Batch b, NetworkEvent e,
			HashMap<Integer, Node> portMap, HashMap<String, Node> ipMap,
			HashMap<Integer, Integer> nodeDegreeChangeMap,
			ArrayList<Edge> addedEdges, ArrayList<Edge> removedEdges);

	public abstract Batch craftBatch(Graph g, ArrayList<TCPEvent> events);

	@Override
	public Batch generate(Graph graph) {
		Batch b = new Batch(graph.getGraphDatastructures(),
				graph.getTimestamp(), graph.getTimestamp() + 1, 0, 0, 0, 0, 0,
				0);
		boolean outOfBounds = false;
		ArrayList<TCPEvent> events = new ArrayList<TCPEvent>();

		if (this.bufferedEntry != null) {
			events.add(this.bufferedEntry);
		}

		while (this.reader.isNextEventPossible() && !outOfBounds) {
			TCPEvent e = this.reader.getNextEvent();
			DateTime time = e.getTime();

			if (!this.init) {
				this.threshold = time.plusSeconds(this.batchLength);
				this.init = true;
			}

			// check if out of interval
			if (time.isAfter(this.threshold)) {
				this.threshold = this.threshold.plusSeconds(this.batchLength);
				this.bufferedEntry = e;
				outOfBounds = true;
			} else {
				events.add(e);
			}
		}

		ArrayList<Integer> ports = new ArrayList<Integer>();
		ArrayList<Long> portTimes = new ArrayList<Long>();
		ArrayList<String> ips = new ArrayList<String>();
		ArrayList<Long> ipTimes = new ArrayList<Long>();

		// gather ips and ports
		for (int i = 0; i < events.size(); i++) {
			TCPEvent e = events.get(i);
			String srcIp = e.getSrcIp();
			String dstIp = e.getDstIp();
			int dstPort = e.getDstPort();
			long t = e.getTime().getMillis();

			if (!ips.contains(srcIp)) {
				ips.add(srcIp);
				ipTimes.add(t);
			} else {
				ipTimes.set(ips.indexOf(srcIp), t);
			}
			if (!ips.contains(dstIp)) {
				ips.add(dstIp);
				ipTimes.add(t);
			} else {
				ipTimes.set(ips.indexOf(dstIp), t);
			}
			if (!ports.contains(dstPort)) {
				ports.add(dstPort);
				portTimes.add(t);
			} else {
				portTimes.set(ports.indexOf(dstPort), t);
			}
		}

		// end-condition
		if (!this.reader.isNextEventPossible() && !outOfBounds) {
			this.bufferedEntry = null;
			this.finished = true;
		}

		return craftBatch(graph, events);
	}

	@Override
	public void reset() {
		try {
			this.reader = this.reader.copy();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean isFurtherBatchPossible(Graph g) {
		if (finished)
			return false;
		else
			return true;
	}
}
