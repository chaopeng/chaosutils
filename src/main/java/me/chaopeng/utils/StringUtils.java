package me.chaopeng.utils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * string工具
 * @author chao
 *
 */
public class StringUtils {
	/**
	 * 连接数组
	 */
	public static String join(String s, Object...objects){
		if(objects.length==0){
			return "";
		}
		StringBuilder sb = new StringBuilder(objects[0].toString());
		for (int i = 1; i < objects.length; i++) {
			sb.append(s).append(objects[i]);
		}
		return sb.toString();
	}

	/**
	 * 连接数组
	 */
	public static String join(String s, List ls){
		if(ls.size()==0){
			return "";
		}
		StringBuilder sb = new StringBuilder(ls.get(0).toString());
		for (int i = 1; i < ls.size(); i++) {
			sb.append(s).append(ls.get(i));
		}
		return sb.toString();
	}

	/**
	 * 大写首字母
	 */
	public static String upFirst(String str) {
		return str.substring(0, 1).toUpperCase().concat(str.substring(1));
	}

	private static final Pattern ALL_UP_PATTERN = Pattern.compile("[A-Z]*");

	/**
	 * 下划线风格转小写驼峰
	 */
	public static String underlineToLowerCamal(String s){
		if (s.contains("_")) {
			s = s.toLowerCase();
			String[] ss = s.split("_");
			for (int i = 1; i < ss.length; i++) {
				ss[i] = upFirst(ss[i]);
			}
			return join("", ss);
		} else if(ALL_UP_PATTERN.matcher(s).matches()){
			s = s.toLowerCase();
		}
		return s;
	}

	/**
	 * 下划线风格转大写驼峰
	 */
	public static String underlineToUpperCamal(String s){
		return upFirst(underlineToLowerCamal(s));
	}


	/**
	 * 驼峰转下划线,未处理大小写
	 */
	public static String camalToUnderline(String s){
		StringBuilder sb = new StringBuilder();
		if(s.length()>0){
			sb.append(s.charAt(0));
		}
		for (int i = 1; i < s.length(); i++) {
			char c = s.charAt(i);
			if(Character.isUpperCase(c)){
				sb.append("_");
			}
			sb.append(c);
		}
		return sb.toString();
	}

}
