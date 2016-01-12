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

	protected static final int ipOffset = 100000;

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

	public int getPortMapping(int port) {
		return portMap.get(port);
	}

	public int getIpMapping(String ip) {
		return ipMap.get(ip);
	}

	public ArrayList<Integer> getPorts() {
		return ports;
	}

	public ArrayList<String> getIps() {
		return ips;
	}

	public void map() {
		map(ports, ips);
	}

	public boolean hasPort(int port) {
		return this.ports.contains(port);
	}

	public boolean hasIp(String ip) {
		return this.ips.contains(ip);
	}

	public int addPort(int port) {
		if (!this.ports.contains(port)) {
			this.ports.add(port);
			this.portMap.put(port, port);
		}
		return port;
	}

	public int addIp(String ip) {
		if (!this.ips.contains(ip)) {
			int amount = this.ips.size();
			int mapping = amount + ipOffset;
			this.ips.add(ip);
			this.ipMap.put(ip, mapping);
			return mapping;
		}
		return this.ipMap.get(ip);
	}

	public void map(ArrayList<Integer> ports, ArrayList<String> ips) {
		// map ports and ips
		this.portMap = new HashMap<Integer, Integer>();
		this.ipMap = new HashMap<String, Integer>();
		for (int i = 0; i < ports.size(); i++)
			portMap.put(ports.get(i), ports.get(i));

		for (int i = 0; i < ips.size(); i++)
			ipMap.put(ips.get(i), i + NetworkGraph.ipOffset);
	}

}
