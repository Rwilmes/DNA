package dna.graph.generators.network;

import java.io.IOException;
import java.text.ParseException;

import dna.graph.datastructures.GraphDataStructure;
import dna.graph.generators.GraphGenerator;
import dna.graph.generators.network.tcplist.TCPListEvent;
import dna.io.Reader;

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

		String line = r.readString();
		if (line == null) {
			this.finished = true;
			r.close();
		} else
			this.bufferedEvent = TCPListEvent.getFromString(line);
	}

	@Override
	public abstract NetworkGraph generate();

	public NetworkEvent getNextEvent() {
		if (finished)
			return null;

		NetworkEvent e = this.bufferedEvent;
		String line;
		try {
			line = r.readString();
			if (line != null)
				this.bufferedEvent = TCPListEvent.getFromString(line);
			else
				this.finished = true;
		} catch (IOException | ParseException e1) {
			e1.printStackTrace();
		}

		return e;
	}

	public boolean isNextEventPossible() {
		return !finished;
	}

	public NetworkGraph newGraphInstance() {
		GraphDataStructure newGDS = gds.clone();
		newGDS.newNetworkGraphInstance("", 1, 2, 3);
		return newGDS.newNetworkGraphInstance(this.getName(),
				this.timestampInit, this.nodesInit, this.edgesInit);
	}
}
