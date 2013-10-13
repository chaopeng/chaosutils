package me.chaopeng.test;

import static org.junit.Assert.*;
import me.chaopeng.utils.codec.Zip;

import org.junit.Test;

public class TestZip {

	@Test
	public void test() {
		String before = "hello world!!!";
		byte[] bytes = Zip.zip(before.getBytes());
		String after = new String(Zip.unzip(bytes));
		
		assertEquals(before, after);
	}

}
