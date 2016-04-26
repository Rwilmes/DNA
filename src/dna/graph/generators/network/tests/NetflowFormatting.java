package dna.graph.generators.network.tests;

import java.io.IOException;

import argList.ArgList;
import argList.types.atomic.IntArg;
import argList.types.atomic.StringArg;
import dna.io.Reader;
import dna.io.Writer;

public class NetflowFormatting {

	public static void main(String[] args) throws IOException {
		ArgList<NetflowFormatting> argList = new ArgList<NetflowFormatting>(
				NetflowFormatting.class, new StringArg("srcPath",
						"path to the source data file"), new StringArg(
						"dstPath", "path to the dataset destination file"),
				new IntArg("skipFirst", "number of first lines to be skipped"),
				new StringArg("dateFilter",
						"suffix of dates to be filtered. Example: " + '"'
								+ "-98"
								+ " will filter all dates ending on -98"));
		NetflowFormatting nff = argList.getInstance(args);
		nff.format();
	}

	protected String srcPath;
	protected String dstPath;
	protected int skipFirst;
	protected String filterDateSuffix;

	public NetflowFormatting(String srcPath, String dstPath, Integer skipFirst,
			String filterDateSuffix) {
		this.srcPath = srcPath;
		this.dstPath = dstPath;
		this.skipFirst = skipFirst;
		this.filterDateSuffix = filterDateSuffix;
	}

	public void format() throws IOException {
		System.out.println("Starting transformation");
		System.out.println(srcPath + "  ->  " + dstPath);
		Reader r = new Reader("", srcPath);
		Writer w = new Writer("", dstPath);

		String line = r.readString();
		int counter = 0;
		while (line != null) {
			String[] splits = line.split("\t");

			if (counter < skipFirst || splits[0].endsWith(filterDateSuffix)) {
				line = r.readString();
				counter++;
				continue;
			}

			// System.out.println(line);
			// System.out.println("splits: " + splits.length);
			//

			String temp = splits[0].replace("-98", "-1998") + " ";
			for (int i = 1; i < splits.length - 1; i++) {
				// System.out.println("\t" + i + "\t" + splits[i]);
				temp += splits[i] + "\t";
			}

			temp += splits[splits.length - 1];
			w.writeln(temp);
			line = r.readString();
			counter++;

			if (counter % 1000 == 0)
				System.out.println(counter);
		}

		w.close();
		r.close();
	}
}
