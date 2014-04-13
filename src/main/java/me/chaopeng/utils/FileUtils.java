package me.chaopeng.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * 文件工具类
 *
 * @author chao
 */
public class FileUtils {

	private static Logger logger = LoggerFactory.getLogger(FileUtils.class);

	/**
	 * read a file to a string
	 */
	public static String readFile(String filePath) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filePath));
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

	/**
	 * read a file to a string
	 */
	public static String readFile(File file) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
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

	/**
	 * get md5 of file
	 */
	public static String fileMD5(String filePath) {
		FileInputStream fis = null;
		String md5 = null;
		try {
			fis = new FileInputStream(new File(filePath));
			md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
		} catch (Exception e) {
			logger.error("!!!", e);
		} finally {
			CloseUtils.close(fis);
		}
		return md5;
	}

	/**
	 * get md5 of file
	 */
	public static String fileMD5(File file) {
		FileInputStream fis = null;
		String md5 = null;
		try {
			fis = new FileInputStream(file);
			md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
		} catch (Exception e) {
			logger.error("!!!", e);
		} finally {
			CloseUtils.close(fis);
		}
		return md5;
	}


}
