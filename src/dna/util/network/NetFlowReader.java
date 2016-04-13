package dna.util.network;

import java.io.FileNotFoundException;

import dna.labels.labeler.darpa.EntryBasedAttackLabeler;
import dna.util.Config;
import dna.util.network.tcp.TCPEvent.TCPEventField;
import dna.util.network.tcp.TCPEventReader;

public class NetFlowReader extends TCPEventReader {

	// this(dir, filename, Config.get("TCP_LIST_DEFAULT_SEPARATOR"), Config
	// .get("TCP_LIST_DEFAULT_TIME_FORMAT"), Config
	// .get("TCP_LIST_DEFAULT_DURATION_FORMAT"), labeler, fields);
	//
	public NetFlowReader(String dir, String filename,
			EntryBasedAttackLabeler labeler) throws FileNotFoundException {
		super(dir, filename, Config.get("TCP_LIST_DEFAULT_SEPARATOR"),
				"yyyy-MM-dd HH:mm:ss.SSS", "s.SSS", labeler,
				new TCPEventField[] { TCPEventField.TIME,
						TCPEventField.DURATION, TCPEventField.SERVICE,
						TCPEventField.SRC_IP, TCPEventField.SRC_PORT,
						TCPEventField.NONE, TCPEventField.DST_IP,
						TCPEventField.DST_PORT, TCPEventField.NONE,
						TCPEventField.NONE, TCPEventField.NONE,
						TCPEventField.NONE, TCPEventField.NONE,
						TCPEventField.NAME });
	}

}
