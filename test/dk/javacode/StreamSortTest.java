package dk.javacode;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StreamSortTest {
	
	public static final String data = "data"; 

	@Before
	public void setUp() throws Exception {

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws IOException {
		BufferedReader r = new BufferedReader(new FileReader(new File(data, "sort_me.txt")));
		StreamSort sorter = new StreamSort();
		BufferedReader in = sorter.sort(r);
		int linecount = 0;
		String last = null;
		while (in.ready()) {
			linecount++;
			String c = in.readLine();
			if (last != null) {
				assertTrue(c + ":" + last, c.compareTo(last) >= 0);
			}
			last = c;
			System.out.println(c);
		}
		IOUtils.closeQuietly(in);
		assertEquals(45, linecount);
	}
	
	@Test
	public void testLarge() throws IOException {
		BufferedReader r = new BufferedReader(new FileReader(new File(data, "yay.txt")));
		StreamSort sorter = new StreamSort();
		BufferedReader in = sorter.sort(r);
		int linecount = 0;
		String last = null;
		while (in.ready()) {
			linecount++;
			String c = in.readLine();
			if (last != null) {
				assertTrue(c + ":" + last, c.compareTo(last) >= 0);
			}
			last = c;
//			System.out.println(c);
		}
		System.out.println(linecount);
//		assertEquals(45, linecount);
		assertTrue(true);
	}

}
