package me.chaopeng.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptException;

import me.chaopeng.test.domain.Int;
import me.chaopeng.utils.ScriptsUtils;

import org.junit.Test;

public class TestScriptsUtils {

	@Test
	public void testUnkownScript() {
		try {
			ScriptsUtils.eval("unknow", null);
		} catch (ScriptException e) {
			assertEquals(e.getMessage(), "no such methor!");
		}
	}
	
	@Test
	public void testScript() {
		try {
			ScriptsUtils.put("test", "a+b");
			
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("a", 1);
			params.put("b", 1);
			Object res = ScriptsUtils.eval("test", params);
			assertEquals(res.getClass(), Double.class);
			assertEquals(res, 2.0);
			
		} catch (ScriptException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testScriptWithMethod() {
		try {
			ScriptsUtils.put("test2", "a+b.getI()");
			
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("a", 1);
			params.put("b", new Int(1));
			Object res = ScriptsUtils.eval("test2", params);
			assertEquals(res.getClass(), Double.class);
			assertEquals(res, 2.0);
			
		} catch (ScriptException e) {
			fail(e.getMessage());
		}
	}
}
