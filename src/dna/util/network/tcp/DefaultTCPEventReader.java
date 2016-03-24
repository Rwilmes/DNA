package dna.util.network.tcp;

import java.io.FileNotFoundException;

import dna.labels.labeler.darpa.EntryBasedAttackLabeler;
import dna.util.network.tcp.TCPEvent.TCPEventField;

public class DefaultTCPEventReader extends TCPEventReader {

	public DefaultTCPEventReader() throws FileNotFoundException {
		this(null, null, null);
	}

	public DefaultTCPEventReader(String dir, String filename)
			throws FileNotFoundException {
		this(dir, filename, null);
	}

	public DefaultTCPEventReader(String dir, String filename,
			EntryBasedAttackLabeler labeler) throws FileNotFoundException {
		super(dir, filename, labeler, new TCPEventField[] { TCPEventField.ID,
				TCPEventField.TIME, TCPEventField.DURATION,
				TCPEventField.SERVICE, TCPEventField.SRC_PORT,
				TCPEventField.DST_PORT, TCPEventField.SRC_IP,
				TCPEventField.DST_IP, TCPEventField.ATTACK_SCORE,
				TCPEventField.NAME });
	}

}
