package dna.graph.generators.network;

import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.HashMap;

import org.joda.time.DateTime;

import dna.graph.Graph;
import dna.io.Reader;
import dna.updates.batch.Batch;
import dna.updates.generators.BatchGenerator;
import dna.util.Log;
import dna.util.parameters.Parameter;

/**
 * A batch-generator which creates batches based on a tcp-list file.
 * 
 * @author Rwilmes
 * 
 */
public abstract class NetworkBatch extends BatchGenerator {

	protected String dir;
	protected String filename;

	protected int batchLength;

	protected DateTime threshold;

	protected boolean init;
	protected boolean finished;

	protected Reader r;

	protected TCPListEvent bufferedEntry;

	public NetworkBatch(String name, String dir,
			String filename, int batchIntervalInSeconds) throws IOException {
		super(name, null);
		this.dir = dir;
		this.filename = filename;
		this.batchLength = batchIntervalInSeconds;

		this.init = false;
		this.finished = false;

		// init reader
		this.r = Reader.getReader(dir, filename);
	}

	public abstract void onEvent(Graph g, Batch b, NetworkEvent e,
			HashMap<Integer, Integer> portMap, HashMap<String, Integer> ipMap);

	@Override
	public Batch generate(Graph g) {
		Batch b = new Batch(g.getGraphDatastructures(), g.getTimestamp(),
				g.getTimestamp() + 1, 0, 0, 0, 0, 0, 0);

		NetworkGraph graph;
		if (g instanceof NetworkGraph) {
			graph = (NetworkGraph) g;
		} else {
			Log.warn("cannot generate NetworkBatch for non-NetworkGraph!");
			return b;
		}

		HashMap<Integer, Integer> portMap = graph.getPortMap();
		HashMap<String, Integer> ipMap = graph.getIpMap();

		String line;

		if (this.bufferedEntry != null)
			onEvent(g, b, this.bufferedEntry, portMap, ipMap);

		// while still lines to read -> read and craft batches
		// will abort when time is out of interval
		try {
			while ((line = this.r.readString()) != null) {
				TCPListEvent event;

				event = TCPListEvent.getFromString(line);

				DateTime time = event.getTime();

				// only do this the first time
				if (!this.init) {
					this.threshold = time.plusSeconds(this.batchLength);
					this.init = true;
				}

				// check if out of interval
				if (time.isAfter(this.threshold)) {
					// System.out.println("AFTER!");
					this.threshold = this.threshold
							.plusSeconds(this.batchLength);
					this.bufferedEntry = event;
					return b;
				} else {
					// handle changes
					this.onEvent(g, b, event, portMap, ipMap);
				}
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}

		this.finished = true;
		return b;
	}

	@Override
	public void reset() {
		try {
			this.r.close();
			this.r = Reader.getReader(dir, filename);
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
