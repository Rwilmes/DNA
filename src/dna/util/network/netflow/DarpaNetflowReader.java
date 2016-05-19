package dna.util.network.netflow;

import java.io.FileNotFoundException;

public class DarpaNetflowReader extends DefaultNetflowReader {

	public DarpaNetflowReader(String dir, String filename)
			throws FileNotFoundException {
		super(dir, filename, -21600);
	}

}
