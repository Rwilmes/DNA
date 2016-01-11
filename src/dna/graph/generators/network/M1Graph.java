package dna.graph.generators.network;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;

import dna.graph.datastructures.GraphDataStructure;
import dna.graph.nodes.Node;

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
public class M1Graph extends NetworkGraphGenerator {

	public M1Graph(GraphDataStructure gds, String dir, String filename)
			throws IOException, ParseException {
		this("M1Graph-Generator", gds, 0, dir, filename);
	}

	public M1Graph(String name, GraphDataStructure gds, long timestampInit,
			String dir, String filename) throws IOException, ParseException {
		super(name, gds, timestampInit, dir, filename);
	}

	@Override
	public NetworkGraph generate() {
		NetworkGraph g = this.newGraphInstance();

		ArrayList<Integer> ports = new ArrayList<Integer>();
		ArrayList<String> ips = new ArrayList<String>();

		while (isNextEventPossible()) {
			// read next event
			NetworkEvent e = getNextEvent();

			// add ports and ips to list
			int port = e.getDstPort();
			String srcIp = e.getSrcIp();
			String dstIp = e.getDstIp();

			if (!ports.contains(port))
				ports.add(port);
			if (!ips.contains(srcIp))
				ips.add(srcIp);
			if (!ips.contains(dstIp))
				ips.add(dstIp);
		}

		// sort lists
		Collections.sort(ports);
		Collections.sort(ips);

		// map ports and ips
		g.setPorts(ports);
		g.setIps(ips);
		g.map();

		// add nodes
		for (int i = 0; i < ports.size(); i++) {
			Node node = this.gds.newNodeInstance(g.getPortMap().get(
					ports.get(i)));
			g.addNode(node);
		}
		for (int i = 0; i < ips.size(); i++) {
			Node node = this.gds.newNodeInstance(g.getIpMap().get(ips.get(i)));
			g.addNode(node);
		}

		// return
		return g;
	}

}
