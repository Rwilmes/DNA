package dna.util.network.tcp;

import java.io.FileNotFoundException;

import dna.labels.labeler.darpa.EntryBasedAttackLabeler;
import dna.util.network.tcp.TCPEvent.TCPEventField;

public class TCPPacketReader extends TCPEventReader {

	public TCPPacketReader(String dir, String filename,
			EntryBasedAttackLabeler labeler) throws FileNotFoundException {
		super(dir, filename, ",", "yyyy-MM-dd HH:mm:ss.SSSSSS", "s.SSS",
				labeler, new TCPEventField[] { TCPEventField.ID,
						TCPEventField.TIME, TCPEventField.SERVICE,
						TCPEventField.SRC_IP, TCPEventField.SRC_PORT,
						TCPEventField.DST_IP, TCPEventField.DST_PORT,
						TCPEventField.NONE });
	}
}
