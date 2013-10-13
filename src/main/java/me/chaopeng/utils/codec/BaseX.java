package me.chaopeng.utils.codec;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 2-62进制工具，范围为[0-9a-zA-Z]
 * @author chao
 *
 */
public class BaseX {
	private static char[] S = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
			'9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
			'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y',
			'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
			'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y',
			'Z' };
	
	private static Map<Character, Integer> charToInt = new HashMap<Character, Integer>();
	
	static{
		for (int i = 0; i < S.length; i++) {
			char c = S[i];
			charToInt.put(c, i);
		}
	}
	
	private static final Logger logger = LoggerFactory.getLogger(BaseX.class);

	/**
	 * 10进制转x进制
	 * @param num 10进制数字
	 * @param toBase 目标进制
	 * @return 转换后数字
	 * @throws BaseXException 
	 */
	public static String base10ToBaseX(long num, int toBase) {
		if(toBase<2 || toBase>62) {
			logger.error("base is error");
			return "";
		}
		StringBuilder sb = new StringBuilder();
		while (num > 0) {
			int i = (int) (num % toBase);
			num /= toBase;
			sb.append(S[i]);
		}
		return sb.reverse().toString();
	}
	
	/**
	 * x进制转10进制
	 * @param numStr x进制数字
	 * @param fromBase 原进制
	 * @return 10进制数字
	 * @throws BaseXException 
	 */
	public static long BaseXTOBase10(String numStr, int fromBase) {
		if(fromBase<2 || fromBase>62) {
			logger.error("base is error");
			return 0;
		}
		int len = numStr.length();
		long res = 0;
		long b = 1;
		for(int i = len - 1; i >= 0; --i){
			char c = numStr.charAt(i);
			int num = 0;
			try {
				num = getCharNum(c, fromBase);
			} catch (NumberException e) {
				logger.error("base error ch="+c+",base="+fromBase);
				return 0;
			}
			res += num * b;
			b *= fromBase;
		}
		return res;
	}
	
	private static int getCharNum(char c, int base) throws NumberException{
		Integer num = charToInt.get(c);
		if(num == null || num >= base){
			throw new NumberException();
		}
		return num;
	}
	
	private static class NumberException extends Exception {
		private static final long serialVersionUID = 1L;
	}
}
