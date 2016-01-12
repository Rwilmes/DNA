package dna.graph.generators.network;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import dna.graph.datastructures.GraphDataStructure;

/**
 * Creates an empty network graph.
 * 
 * @author Rwilmes
 * 
 */
public class EmptyNetwork extends NetworkGraphGenerator {

	public EmptyNetwork(String name, GraphDataStructure gds, long timestampInit)
			throws IOException, ParseException {
		super(name, gds, timestampInit, null, null);
	}

	public EmptyNetwork(GraphDataStructure gds) throws IOException,
			ParseException {
		this("EmptyNetwork", gds, 0);
	}

	@Override
	public NetworkGraph generate() {
		NetworkGraph g = this.newGraphInstance();

		ArrayList<Integer> ports = new ArrayList<Integer>();
		ArrayList<String> ips = new ArrayList<String>();

		g.setPorts(ports);
		g.setIps(ips);
		g.map();

		return g;
	}

}
