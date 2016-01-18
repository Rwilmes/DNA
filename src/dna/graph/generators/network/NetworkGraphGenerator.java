package dna.graph.generators.network;

import java.io.IOException;
import java.text.ParseException;

import dna.graph.Graph;
import dna.graph.datastructures.GraphDataStructure;
import dna.graph.generators.GraphGenerator;
import dna.util.network.NetworkEvent;
import dna.util.network.tcp.DefaultTCPEventReader;
import dna.util.network.tcp.TCPEventReader;

/**
 * Abstract class for a graph generator that generates a graph based on a TCP
 * list file.
 * 
 * @author Rwilmes
 * 
 */
public abstract class NetworkGraphGenerator extends GraphGenerator {

	protected boolean finished;

	protected NetworkEvent bufferedEvent;

	protected TCPEventReader reader;

	public NetworkGraphGenerator(String name, GraphDataStructure gds,
			long timestampInit, String dir, String filename)
			throws IOException, ParseException {
		this(name, gds, timestampInit, new DefaultTCPEventReader(dir, filename));
	}

	public NetworkGraphGenerator(String name, GraphDataStructure gds,
			long timestampInit, TCPEventReader reader) throws IOException,
			ParseException {
		super(name, null, gds, timestampInit, 0, 0);
		this.reader = reader;

		// if no dir or filename, do nothing
		if (reader == null) {
			this.finished = true;
			return;
		}

		// read and buffer first event
		this.finished = false;
		if (!this.reader.isNextEventPossible()) {
			this.finished = true;
			this.reader.close();
		} else
			this.bufferedEvent = this.reader.getNextEvent();
	}

	@Override
	public abstract Graph generate();
	
	public TCPEventReader getReader() {
		return reader;
	}
}
