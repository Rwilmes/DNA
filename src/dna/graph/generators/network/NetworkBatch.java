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

	@Override
	public Batch generate(Graph graph) {
		Batch b = new Batch(graph.getGraphDatastructures(),
				graph.getTimestamp(), graph.getTimestamp() + 1, 0, 0, 0, 0, 0,
				0);

		HashMap<Integer, Node> portMap = new HashMap<Integer, Node>();
		HashMap<String, Node> ipMap = new HashMap<String, Node>();
		HashMap<Integer, Integer> nodeDegreeChangeMap = new HashMap<Integer, Integer>();
		ArrayList<Edge> addedEdges = new ArrayList<Edge>();
		ArrayList<Edge> removedEdges = new ArrayList<Edge>();

		if (this.bufferedEntry != null)
			onEvent(graph, b, this.bufferedEntry, portMap, ipMap,
					nodeDegreeChangeMap, addedEdges, removedEdges);

		while (this.reader.isNextEventPossible()) {
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

				// print mappings (FOR DEBUG)
				// reader.printMappings();

				// if (reader.isRemoveZeroDegreeNodes()) {
				// for (IElement n_ : graph.getNodes()) {
				// Node n = (Node) n_;
				//
				// int change = 0;
				// if (nodeDegreeChangeMap.containsKey(n.getIndex()))
				// change = nodeDegreeChangeMap.get(n.getIndex());
				// System.out.println("blablain node " + n
				// + "  ?? \tdegree: " + n.getDegree()
				// + "\tchange: " + change +"\tremove: " + ((n.getDegree() +
				// change) <= 0));
				// if ((n.getDegree() + change) <= 0)
				// b.add(new NodeRemoval(n));
				// }
				// }

				return b;
			} else {
				// handle changes
				this.onEvent(graph, b, e, portMap, ipMap, nodeDegreeChangeMap,
						addedEdges, removedEdges);
			}
		}

		this.finished = true;
		return b;
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
