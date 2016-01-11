package dna.graph.generators.network;

import java.util.ArrayList;
import java.util.HashMap;

import dna.graph.Graph;
import dna.graph.datastructures.GraphDataStructure;

public class NetworkGraph extends Graph {

	protected ArrayList<Integer> ports;
	protected HashMap<Integer, Integer> portMap;
	protected ArrayList<String> ips;
	protected HashMap<String, Integer> ipMap;

	protected Graph g;

	public NetworkGraph(String name, long timestamp, GraphDataStructure gds,
			ArrayList<Integer> ports, ArrayList<String> ips) {
		super(name, timestamp, gds);

		this.ports = ports;
		this.ips = ips;

		// map
		map(ports, ips);
	}

	public NetworkGraph(Graph g, ArrayList<Integer> ports, ArrayList<String> ips) {
		this(g.getName(), g.getTimestamp(), g.getGraphDatastructures(), ports,
				ips);
		this.g = g;
	}

	public NetworkGraph(String name, long timestamp, GraphDataStructure gds,
			int nodesInit, int edgesInit) {
		super(name, timestamp, gds, nodesInit, edgesInit);

		this.ports = new ArrayList<Integer>();
		this.ips = new ArrayList<String>();
	}

	public void setPorts(ArrayList<Integer> ports) {
		this.ports = ports;
	}

	public void setIps(ArrayList<String> ips) {
		this.ips = ips;
	}

	public HashMap<Integer, Integer> getPortMap() {
		return portMap;
	}

	public HashMap<String, Integer> getIpMap() {
		return ipMap;
	}

	public void map() {
		map(ports, ips);
	}

	public void map(ArrayList<Integer> ports, ArrayList<String> ips) {
		// map ports and ips
		this.portMap = new HashMap<Integer, Integer>();
		this.ipMap = new HashMap<String, Integer>();
		for (int i = 0; i < ports.size(); i++)
			portMap.put(ports.get(i), i);

		for (int i = 0; i < ips.size(); i++)
			ipMap.put(ips.get(i), i + portMap.size());
	}

}
