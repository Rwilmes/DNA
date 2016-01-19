package dna.graph.generators.network.m1;

import java.io.IOException;
import java.text.ParseException;

import dna.graph.Graph;
import dna.graph.datastructures.GraphDataStructure;
import dna.graph.generators.network.NetworkGraph;
import dna.graph.nodes.Node;
import dna.util.network.NetworkEvent;
import dna.util.network.tcp.TCPEventReader;

/**
 * NetworkGraphGenerator for Model1.<br>
 * 
 * Nodes = Ports and Hosts<br>
 * 
 * onInit: scans tcp file and adds all ports and hosts as nodes to the graph.
 * 
 * @author Rwilmes
 * 
 */
public class M1Graph extends NetworkGraph {

	public M1Graph(GraphDataStructure gds, TCPEventReader reader)
			throws IOException, ParseException {
		super("M1-GraphGenerator", gds, 0, reader);
	}

	public M1Graph(GraphDataStructure gds, long timestampInit,
			TCPEventReader reader) throws IOException, ParseException {
		super("M1-GraphGenerator", gds, timestampInit, reader);
	}

	@Override
	public Graph generate() {
		Graph g = this.newGraphInstance();

		while (reader.isNextEventPossible()) {
			// read next event
			NetworkEvent e = reader.getNextEvent();

			// add ports and ips to list
			int port = e.getDstPort();
			String srcIp = e.getSrcIp();
			String dstIp = e.getDstIp();

			if (!reader.containsPort(port)) {
				reader.addPort(port);

				// add node
				Node node = this.gds.newNodeInstance(reader
						.getPortMapping(port));
				g.addNode(node);
			}
			if (!reader.containsIp(srcIp)) {
				reader.addIp(srcIp);

				// add node
				Node node = this.gds
						.newNodeInstance(reader.getIpMapping(srcIp));
				g.addNode(node);
			}
			if (!reader.containsIp(dstIp)) {
				reader.addIp(dstIp);

				// add node
				Node node = this.gds
						.newNodeInstance(reader.getIpMapping(dstIp));
				g.addNode(node);
			}
		}

		// return
		return g;
	}

}
