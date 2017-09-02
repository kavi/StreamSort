package dk.javacode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.IOUtils;

public class StreamSort {

	private static final int BUFFER_SIZE = 8192;
	private static final int MERGE_SIZE = 16;

	private File tmpDir = new File("tmp");

	private List<File> w_tmpfiles = new ArrayList<File>();
	private List<File> r_tmpfiles = new ArrayList<File>();

	private Comparator<String> comparator = new DefaultComparator();

	public boolean deleteTmpFileOnExit = true;

	public StreamSort() {
		super();
	}

	public StreamSort(File tmpDir) {
		super();
		this.tmpDir = tmpDir;
	}

	public StreamSort(Comparator<String> comparator) {
		super();
		this.comparator = comparator;
	}

	public StreamSort(File tmpDir, Comparator<String> comparator) {
		super();
		this.tmpDir = tmpDir;
		this.comparator = comparator;
	}

	/**
	 * Sort the data in the given reader using intermediate files. The temporary
	 * file the returned reader points to will be deleted when the reader is closed!
	 * 
	 * If deleteTmpFileOnExit is set to true (default) a deleteOnExit will attempt
	 * to ensure deletion upon JVM termination.
	 * 
	 * @param in
	 *            The reader containing data to sort
	 * @return A reader containing sorted data (this will point to file
	 *         containing the data)
	 */
	public BufferedReader sort(BufferedReader in) {
		// Make sure tmp dir exists
		if (!tmpDir.exists()) {
			tmpDir.mkdir();
		}

		int chunks = 0;
		try {
			// First pass - split into a number of sorted files
			while (in.ready()) {
				readOneChunk(in, chunks);
				chunks++;
			}
			int pass = 2;
			int chunksread = 0;
			int newchunks = 0;
			do {
				newchunks = 0;
				chunksread = 0;
				while (chunks > 0) {
					int q = chunks < MERGE_SIZE ? chunks : MERGE_SIZE;
					chunks -= q;
					mergePass(pass, chunksread, newchunks, q);
					chunksread += q;
					newchunks++;
				}
				// Remove read chunks
				for (File f : r_tmpfiles) {
					f.delete();
				}
				r_tmpfiles.clear();
				chunks = newchunks;
				// System.out.println(pass);
				pass++;
			} while (newchunks > 1 && pass < 10);
			File resultfile = getTmpFile(pass - 1, 0);
			if (deleteTmpFileOnExit) {
				resultfile.deleteOnExit();
			}
			return new SortedReader(new FileReader(resultfile), resultfile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	/**
	 * Merge chunks for a given pass (will be written to a chunk-file) The pass
	 * merged is (nextpass - 1).
	 * 
	 * @param nextpass
	 *            The number of the pass to write merged chunks to
	 * @param chunksread
	 *            How many chunks have already been read
	 * @param chunknumber
	 *            The current chunknumber to write to
	 * @param q
	 *            How many chunks should be read by this merge
	 * @throws IOException
	 */
	private void mergePass(int nextpass, int chunksread, int chunknumber, int q) throws IOException {
		BufferedWriter out = getTmpWriter(nextpass, chunknumber);
		BufferedReader[] readers = new BufferedReader[q];
		try {
			String[] current = new String[q];
			for (int i = 0; i < q; i++) {
				readers[i] = getTmpReader(nextpass - 1, i + chunksread);
			}
			// Prepare
			for (int i = 0; i < q; i++) {
				if (readers[i].ready()) {
					current[i] = readers[i].readLine();
				}
			}
			// Move along
			while (anyNotNull(current)) {
				String low = current[0];
				int index = 0;
				for (int i = 1; i < q; i++) {
					if (current[i] != null) {
						if (low == null || comparator.compare(current[i], low) < 0) {
							low = current[i];
							index = i;
						}
					}
				}
				out.write(low + "\n");
				if (readers[index].ready()) {
					current[index] = readers[index].readLine();
				} else {
					current[index] = null;
				}
			}
		} catch (IOException e) {
			throw e;
		} finally {
			out.flush();
			IOUtils.closeQuietly(out);
			for (BufferedReader br : readers) {
				if (br != null) {
					IOUtils.closeQuietly(br);
				}
			}
		}
	}

	/**
	 * Read a chunk from the main InputStream, sort it and write it to a "chunk"
	 * file.
	 * 
	 * @param in
	 * @param currentChunk
	 * @throws IOException
	 */
	private void readOneChunk(BufferedReader in, int currentChunk) throws IOException {
		BufferedWriter w = null;
		List<String> buffer = new ArrayList<String>(BUFFER_SIZE);
		int pass = 1;
		try {
			for (int i = 0; i < BUFFER_SIZE && in.ready(); i++) {
				buffer.add(in.readLine());
			}
			Collections.sort(buffer, comparator);
			w = getTmpWriter(pass, currentChunk);
			for (String s : buffer) {
				w.write(s + "\n");
			}
			w.flush();
			IOUtils.closeQuietly(w);
			buffer.clear();
		} catch (IOException e) {
			throw e;
		} finally {
			if (w != null) {
				IOUtils.closeQuietly(w);
			}
		}
	}

	/**
	 * Returns true if any of the values in the array are not null.
	 * 
	 * @param values
	 * @return
	 * @throws IOException
	 */
	private boolean anyNotNull(String[] values) throws IOException {
		for (String s : values) {
			if (s != null) {
				return true;
			}
		}
		return false;
	}

	private BufferedWriter getTmpWriter(int pass, int chunkNumber) throws IOException {
		File f = getTmpFile(pass, chunkNumber);
		w_tmpfiles.add(f);
		return new BufferedWriter(new FileWriter(f));
	}

	private BufferedReader getTmpReader(int pass, int chunkNumber) throws IOException {
		File f = getTmpFile(pass, chunkNumber);
		r_tmpfiles.add(f);
		return new BufferedReader(new FileReader(f));
	}

	private File getTmpFile(int pass, int chunkNumber) throws IOException {
		File f = new File(tmpDir, "pass" + pass + "_" + chunkNumber);
		return f;
	}

	public class DefaultComparator implements Comparator<String> {
		@Override
		public int compare(String o1, String o2) {
			return o1.compareTo(o2);
		}

	}
}
