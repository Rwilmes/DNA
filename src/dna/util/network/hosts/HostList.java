package dna.util.network.hosts;

import java.util.ArrayList;

import org.joda.time.DateTime;

public class HostList {

	protected ArrayList<Host> list;

	public HostList(String name) {
		this.list = new ArrayList<Host>();
	}

	public ArrayList<Host> getList() {
		return this.list;
	}

	public void addHost(Host h) {
		if (!this.list.contains(h))
			this.list.add(h);
	}

	public void removeHost(String add) {
		Host host = null;
		for (Host h : this.list) {
			if (h.getAddress().equals(add)) {
				host = h;
				break;
			}
		}

		if (host != null)
			this.list.remove(host);
	}

	public boolean isHostActive(String add) {
		for (Host h : this.list) {
			if (h.getAddress().equals(add))
				return true;
		}
		return false;
	}

	public boolean update(DateTime updateTime) {
		boolean somethingHappened = false;
		for (Host h : this.list) {
			somethingHappened = somethingHappened | h.update(updateTime);
		}

		return somethingHappened;
	}

	public Host refreshHost(String add, DateTime initTime, DateTime expireTime) {
		for (Host h : this.list) {
			if (h.getAddress().equals(add)) {
				h.setExpireTime(expireTime);
				return h;
			}
		}

		Host h = new Host(add, initTime, expireTime);
		this.addHost(h);
		return h;
	}
}
