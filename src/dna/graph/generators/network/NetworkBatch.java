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

	protected TCPEvent bufferedEvent;

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

	/** Increments the threshold by the given batchLength. **/
	private void incrementThreshold() {
		threshold = threshold.plusSeconds(batchLength);
	}

	public Batch generate(Graph graph) {
		// list of events
		ArrayList<TCPEvent> events = new ArrayList<TCPEvent>();

		if (!init) {
			this.threshold = new DateTime(graph.getTimestamp())
					.plusSeconds(batchLength);
			init = true;
		}

		// handle buffered event
		if (bufferedEvent != null) {
			// if after threshold -> increment threshold and recurse
			if (bufferedEvent.getTime().isAfter(threshold)) {
				incrementThreshold();
				return generate(graph);
			}

			// add event
			events.add(bufferedEvent);

			// if reader has no next event -> this was the last -> return batch
			if (!reader.isNextEventPossible()) {
				bufferedEvent = null;
				finished = true;
				return craftBatch(graph, events);
			}
		}

		// read events until threshold reached
		while (reader.isNextEventPossible()) {
			TCPEvent e = reader.getNextEvent();
			DateTime t = e.getTime();

			// if t is after threshold -> return
			if (t.isAfter(threshold)) {
				// buffer event
				this.bufferedEvent = e;
				break;
			} else {
				// add event to list
				events.add(e);
			}
		}

		// if no events -> all after threshold -> recurse
		if (events.size() == 0) {
			incrementThreshold();
			return generate(graph);
		}

		// craft batch from event
		return craftBatch(graph, events);
	}

	public Batch generate2(Graph graph) {
		// list of events
		ArrayList<TCPEvent> events = new ArrayList<TCPEvent>();

		// always buffer 1 event (when out of bounds keep it and go on with
		// reading next turn.
		if (this.bufferedEvent != null) {
			events.add(this.bufferedEvent);
		}

		// read events
		boolean outOfBounds = false;
		while (this.reader.isNextEventPossible() && !outOfBounds) {
			TCPEvent e = this.reader.getNextEvent();
			DateTime time = e.getTime();

			if (!this.init) {
				this.threshold = time.plusSeconds(this.batchLength);
				this.init = true;
			}

			System.out.println(graph.getTimestamp() + "\tthreshold: "
					+ this.threshold.getMillis() + "\ttime: "
					+ time.getMillis() + "\tafter:"
					+ time.isAfter(this.threshold));
			System.out.println("\tthreshold: "
					+ this.threshold.toString(reader.getTimeFormat())
					+ "\t\ttime: " + time.toString(reader.getTimeFormat()));

			// check if out of interval
			if (time.isAfter(this.threshold)) {
				this.threshold = this.threshold.plusSeconds(this.batchLength);
				this.bufferedEvent = e;
				outOfBounds = true;
			} else {
				events.add(e);
			}
		}

		// end-condition
		if (!this.reader.isNextEventPossible() && !outOfBounds) {
			this.bufferedEvent = null;
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
