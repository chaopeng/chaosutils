package me.chaopeng.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * 文件工具类
 * @author chao
 */
public class FileUtils {

	private static Logger logger = LoggerFactory.getLogger(FileUtils.class);

	/**
	 * read a file to a string
	 */
	public static String readFile(String fileName) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fileName));
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			return sb.toString();
		} catch (Exception e) {
			logger.error("!!!", e);
		} finally {
			CloseUtils.close(br);
		}

		return null;
	}
}
