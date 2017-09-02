package dk.javacode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class SortedReader extends BufferedReader {
	
	private File file;

	public SortedReader(FileReader wrapped, File file) {
		super(wrapped);
		this.file = file;
	}

	@Override
	public void close() throws IOException {
		super.close();
		file.delete();
	}
	
	

}
