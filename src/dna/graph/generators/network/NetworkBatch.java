package dna.graph.generators.network;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;

import dna.graph.Graph;
import dna.updates.batch.Batch;
import dna.updates.generators.BatchGenerator;
import dna.util.Log;
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

	public abstract Batch craftBatch(Graph g, DateTime timestamp,
			ArrayList<TCPEvent> events,
			HashMap<String, Integer> edgeWeighChanges);

	/** Increments the threshold by the given batchLength. **/
	private void incrementThreshold() {
		threshold = threshold.plusSeconds(batchLength);
	}

	/** Sets the threshold to the next step. **/
	private void stepToThreshold(long nextThreshold) {
		long diff = nextThreshold - threshold.getMillis();
		double multi = diff / (1000.0 * batchLength);
		int multiplier = (multi < 1) ? 1 : (int) Math.floor(multi);
		threshold = threshold.plusSeconds(batchLength * multiplier);
	}

	public Batch generate(Graph graph) {
		//
		if (!init) {
			this.threshold = new DateTime(TimeUnit.SECONDS.toMillis(graph
					.getTimestamp())).plusSeconds(batchLength);
			init = true;
		}

		// get events
		ArrayList<TCPEvent> events = reader.getEventsUntil(threshold);
		ArrayList<NetworkEdge> decrementEdges = reader
				.getDecrementEdges(threshold.getMillis());

		// if both empty -> increase threshold and call generate again
		if (events.isEmpty() && decrementEdges.isEmpty()) {
			long nextEventTimestamp = reader.getNextEventTimestamp();
			long nextDecrementTimestamp = reader
					.getNextDecrementEdgesTimestamp();

			if (nextEventTimestamp > -1 && nextDecrementTimestamp > -1) {
				// both evens valid -> step to next timestamp
				stepToThreshold(Math.min(nextEventTimestamp,
						nextDecrementTimestamp));
			} else if (nextEventTimestamp == -1) {
				if (nextDecrementTimestamp == -1) {
					// no next events, should not occur
					Log.warn("no next events in queue!");
				} else {
					// only next decrement edge event valid
					stepToThreshold(nextDecrementTimestamp);
				}
			} else if (nextDecrementTimestamp == -1) {
				// only next event valid
				stepToThreshold(nextEventTimestamp);
			}

			return generate(graph);
		}

		// get weight changes from events in queue
		HashMap<String, Integer> edgeWeightMap = reader
				.getWeightDecrementals(decrementEdges);

		// return crafted batch
		if (!reader.isNextEventPossible() && reader.isEventQueueEmpty())
			finished = true;
		return craftBatch(graph, threshold, events, edgeWeightMap);
	}

	public Batch generate3(Graph graph) {
		// list of events
		ArrayList<TCPEvent> events = new ArrayList<TCPEvent>();

		if (!init) {
			this.threshold = new DateTime(TimeUnit.SECONDS.toMillis(graph
					.getTimestamp())).plusSeconds(batchLength);
			init = true;
		}

		// handle buffered event
		if (bufferedEvent != null) {
			// if after threshold -> increment threshold and recurse
			if (bufferedEvent.getTime().isAfter(threshold)) {
				incrementThreshold();
				return generate3(graph);
			}

			// add event
			events.add(bufferedEvent);

			// if reader has no next event -> this was the last -> return batch
			if (!reader.isNextEventPossible()) {
				bufferedEvent = null;
				finished = true;
				return craftBatch(graph, null, events, null);
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
			return generate3(graph);
		}

		// craft batch from event
		return craftBatch(graph, null, events, null);
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
