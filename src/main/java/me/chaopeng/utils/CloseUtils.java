package me.chaopeng.utils;

import java.io.Closeable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 可关闭对象关闭工具
 * @author chao
 *
 */
public class CloseUtils {
	private static final Logger logger = LoggerFactory.getLogger(CloseUtils.class);
	
	/**关闭*/
	public static void close(Object o) {
		if (o!=null) {
			try {
				if (o instanceof Closeable) {
					Closeable closer = (Closeable) o;
					closer.close();
				}
				// TODO add other closer when you meet
			} catch (Exception e) {
				logger.error("closer exception!", e);
			}
		}
	}
}
