package dna.graph.generators.network;

import java.io.IOException;
import java.text.ParseException;

import dna.graph.datastructures.GraphDataStructure;
import dna.graph.generators.GraphGenerator;
import dna.io.Reader;
import dna.util.network.NetworkEvent;
import dna.util.network.NetworkEventReader;
import dna.util.network.tcp.DefaultTCPEventReader;

/**
 * Abstract class for a graph generator that generates a graph based on a TCP
 * list file.
 * 
 * @author Rwilmes
 * 
 */
public abstract class NetworkGraphGenerator extends GraphGenerator {

	protected Reader r;
	protected boolean finished;

	protected NetworkEvent bufferedEvent;

	protected NetworkEventReader reader;

	public NetworkGraphGenerator(String name, GraphDataStructure gds,
			long timestampInit, String dir, String filename)
			throws IOException, ParseException {
		super(name, null, gds, timestampInit, 0, 0);

		// if no dir or filename, do nothing
		if (dir == null || filename == null) {
			this.finished = true;
			return;
		}

		// read and buffer first event
		this.finished = false;
		this.r = Reader.getReader(dir, filename);
		this.reader = new DefaultTCPEventReader(dir, filename);
		if (!this.reader.isNextEventPossible()) {
			this.finished = true;
			this.reader.close();
		} else
			this.bufferedEvent = this.reader.getNextEvent();
	}

	@Override
	public abstract NetworkGraph generate();

	public NetworkGraph newGraphInstance() {
		GraphDataStructure newGDS = gds.clone();
		newGDS.newNetworkGraphInstance("", 1, 2, 3);
		return newGDS.newNetworkGraphInstance(this.getName(),
				this.timestampInit, this.nodesInit, this.edgesInit);
	}
}
