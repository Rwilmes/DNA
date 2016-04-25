package dna.util.network;

import java.io.FileNotFoundException;

import dna.util.network.tcp.TCPEvent.TCPEventField;
import dna.util.network.tcp.TCPEventReader;

public class NetFlowReader2 extends TCPEventReader {

	public NetFlowReader2(String dir, String filename)
			throws FileNotFoundException {
		super(dir, filename, "\t", "MM-dd-yyyy HH:mm:ss", "ss.SSSSSS", null,
				new TCPEventField[] { TCPEventField.TIME, TCPEventField.NONE,
						TCPEventField.NONE, TCPEventField.SERVICE,
						TCPEventField.SRC_IP, TCPEventField.NONE,
						TCPEventField.DST_IP, TCPEventField.NONE,
						TCPEventField.NONE, TCPEventField.NONE,
						TCPEventField.NONE, TCPEventField.NONE,
						TCPEventField.NONE, TCPEventField.NONE,
						TCPEventField.NONE, TCPEventField.SRC_PORT,
						TCPEventField.DST_PORT });
	}

}
