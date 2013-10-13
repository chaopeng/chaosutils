package me.chaopeng.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * ScriptsUtils - 脚本工具类
 * 
 * <p>格式为javascript,支持脚本缓存</p>
 * @author chao
 *
 */
public class ScriptsUtils {
	private static ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
	private static Map<String, CompiledScript> scripts = new ConcurrentHashMap<String, CompiledScript>();
	
	/**存储脚本
	 * 
	 * @throws ScriptException 
	 */
	public static void put(String name, String src) throws ScriptException {
		Compilable compilable = (Compilable) engine;
		CompiledScript script = compilable.compile(src);
		scripts.put(name, script);
	}
	
	/**
	 * 注意参数类型可以是public class or public static class
	 * 
	 * @param name 脚本名
	 * @param params 参数
	 * @return 脚本执行结果
	 * @throws ScriptException 
	 */
	public static Object eval(String name, Map<String, Object> params) throws ScriptException {
		CompiledScript script = scripts.get(name);
		if(script == null) {
			throw new ScriptException("no such methor!");
		}
		
		Bindings b = engine.createBindings();
		if(params!=null){
			b.putAll(params);
		}
		return script.eval(b);
	}
	
}
