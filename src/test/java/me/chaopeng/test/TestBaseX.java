package me.chaopeng.test;

import static org.junit.Assert.*;
import me.chaopeng.utils.codec.BaseX;

import org.junit.Test;

public class TestBaseX {

	@Test
	public void testBase10ToBaseX() {
		long num = 9866612356L;
		for (int i = 2; i <= 62; i++) {
			String basex = BaseX.base10ToBaseX(num, i);
			long base10 = BaseX.baseXTOBase10(basex, i);
			assertEquals("error base="+i, num, base10);
		}
		
	}

}
