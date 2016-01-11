package dna.graph.generators.network.model1;

import java.util.ArrayList;
import java.util.HashMap;

import dna.graph.datastructures.GDS;
import dna.graph.datastructures.GraphDataStructure;
import dna.graph.generators.network.NetworkGraph;
import dna.graph.nodes.DirectedNode;

/**
 * Model1 refers to the first model in my network-analysis. Graph-Structure as
 * follows:<br>
 * 
 * Nodes = Ports, Hosts
 * 
 * @author Rwilmes
 * 
 */
public class Model1Graph extends NetworkGraph {

	public Model1Graph(String name, long timestamp, GraphDataStructure gds,
			ArrayList<Integer> ports, ArrayList<String> ips) {
		super(name, timestamp, gds, ports, ips);
	}

	@Override
	protected void map(ArrayList<Integer> ports, ArrayList<String> ips) {
		// map ports and ips
		this.portMap = new HashMap<Integer, Integer>();
		this.ipMap = new HashMap<String, Integer>();
		for (int i = 0; i < ports.size(); i++)
			portMap.put(ports.get(i), i);

		for (int i = 0; i < ips.size(); i++)
			ipMap.put(ips.get(i), i + portMap.size());
	}

	protected void init() {
		// add nodes
		for (int p : ports)
			addNode(new DirectedNode(portMap.get(p), GDS.directed()));
		for (String ip : ips)
			addNode(new DirectedNode(ipMap.get(ip), GDS.directed()));
	}

}
