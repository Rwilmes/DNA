package dna.util.network.tcp;

import java.io.FileNotFoundException;

import dna.util.network.tcp.TCPEvent.TCPEventField;

public class DefaultTCPEventReader extends TCPEventReader {

	public DefaultTCPEventReader() throws FileNotFoundException {
		this(null, null);
	}

	public DefaultTCPEventReader(String dir, String filename)
			throws FileNotFoundException {
		super(dir, filename, new TCPEventField[] { TCPEventField.ID,
				TCPEventField.TIME, TCPEventField.DURATION,
				TCPEventField.SERVICE, TCPEventField.SRC_PORT,
				TCPEventField.DST_PORT, TCPEventField.SRC_IP,
				TCPEventField.DST_IP, TCPEventField.ATTACK_SCORE,
				TCPEventField.NAME });
	}

}
