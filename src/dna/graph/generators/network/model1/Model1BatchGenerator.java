package dna.graph.generators.network.model1;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;

import org.joda.time.DateTime;

import dna.graph.edges.DirectedEdge;
import dna.graph.generators.network.tcplist.TCPListEvent;
import dna.io.Reader;
import dna.test.gds.GDS;
import dna.updates.batch.Batch;
import dna.updates.update.EdgeAddition;

/**
 * Generates batches from a TCPList according to Model1.
 * 
 * @author Rwilmes
 * 
 */
public class Model1BatchGenerator {

	private String name;

	private Model1Graph g;
	private String dir;
	private String filename;

	private boolean init;
	private boolean finished;

	private Reader r;

	private int batchLength;
	private DateTime threshold;

	private TCPListEvent bufferedEntry;

	private long timestamp;

	public Model1BatchGenerator(String name, Model1Graph g, int batchLength,
			String dir, String filename) throws IOException {
		this.name = name;
		this.g = g;
		this.batchLength = batchLength;
		this.dir = dir;
		this.filename = filename;
		this.finished = false;
		this.init = false;

		this.timestamp = g.getTimestamp();

		// init reader
		this.r = Reader.getReader(dir, filename);
	}

	public Batch generate() throws IOException, ParseException {
		String line;

		Batch b = new Batch(GDS.directed, timestamp, ++timestamp);

		if (this.bufferedEntry != null)
			handleEntry(b, this.bufferedEntry);

		// while still lines to read -> read and craft batches
		// will abort when time is out of interval
		while ((line = this.r.readString()) != null) {
			TCPListEvent entry = TCPListEvent.getFromString(line);
			DateTime time = entry.getTime();

			// only do this the first time
			if (!this.init) {
				this.threshold = time.plusSeconds(this.batchLength);
				this.init = true;
			}

			// check if out of interval
			if (time.isAfter(this.threshold)) {
				// System.out.println("AFTER!");
				this.threshold = this.threshold.plusSeconds(this.batchLength);
				this.bufferedEntry = entry;
				return b;
			} else {
				// handle changes
				this.handleEntry(b, entry);
			}

		}

		this.finished = true;
		return b;
	}

	protected void handleEntry(Batch b, TCPListEvent entry) {
		// if port == 0, return
		if (entry.getDstPort() == 0)
			return;

		// print entry for debugging
		entry.print();

		// get srcIp, dstIp and dstPort
		String srcIp = entry.getSrcIp();
		String dstIp = entry.getDstIp();
		int dstPort = entry.getDstPort();

		// get mapping
		HashMap<String, Integer> ipMap = this.g.getIpMap();
		int srcId = ipMap.get(srcIp);
		int dstId = ipMap.get(dstIp);
		int portId = this.g.getPortMap().get(dstPort);

		// add edge addition
		EdgeAddition ea1 = new EdgeAddition(new DirectedEdge(srcId + "->"
				+ portId, this.g));
		EdgeAddition ea2 = new EdgeAddition(new DirectedEdge(portId + "->"
				+ dstId, this.g));

		b.add(ea1);
		b.add(ea2);
	}

	public boolean isFinished() {
		return finished;
	}

}
