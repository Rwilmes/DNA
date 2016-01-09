package dna.graph.generators.network;

import java.util.ArrayList;
import java.util.HashMap;

import dna.graph.Graph;
import dna.graph.datastructures.GraphDataStructure;

public abstract class NetworkGraph extends Graph {

	protected ArrayList<Integer> ports;
	protected HashMap<Integer, Integer> portMap;
	protected ArrayList<String> ips;
	protected HashMap<String, Integer> ipMap;

	public NetworkGraph(String name, long timestamp, GraphDataStructure gds,
			ArrayList<Integer> ports, ArrayList<String> ips) {
		super(name, timestamp, gds);

		this.ports = ports;
		this.ips = ips;

		// map
		map(ports, ips);

		// init
		init();
	}

	public HashMap<Integer, Integer> getPortMap() {
		return portMap;
	}

	public HashMap<String, Integer> getIpMap() {
		return ipMap;
	}

	protected abstract void map(ArrayList<Integer> ports, ArrayList<String> ips);

	protected abstract void init();

}
