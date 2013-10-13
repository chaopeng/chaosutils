package me.chaopeng.utils.codec;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.chaopeng.utils.CloseUtils;

public class Zip {
	
	private static final Logger logger = LoggerFactory.getLogger(Zip.class);
	private static final int BUFF_LEN = 65536;
	
	 /**
	  * 压缩
	  * 
	  * @param data 待压缩数据
	  * @return 压缩返回
	  */
	public static byte[] zip(byte[] data){
		byte[] output = new byte[0];

		Deflater compresser = new Deflater();

		compresser.reset();
		compresser.setInput(data);
		compresser.finish();
		ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
		try {
			byte[] buf = new byte[BUFF_LEN];
			while (!compresser.finished()) {
				int i = compresser.deflate(buf);
				bos.write(buf, 0, i);
			}
			output = bos.toByteArray();
		} catch (Exception e) {
			output = data;
			logger.error("zip error", e);
		} finally {
			CloseUtils.close(bos);
		}
		compresser.end();
		return output;
	 }
	 
	 /**
	  * 解压缩 
	  * @param data 待解压数据
	  * @return 解压后数据
	  */
	public static byte[] unzip(byte[] data) {
		byte[] output = new byte[0];

		Inflater decompresser = new Inflater();
		decompresser.reset();
		decompresser.setInput(data);

		ByteArrayOutputStream o = new ByteArrayOutputStream(data.length);
		try {
			byte[] buf = new byte[BUFF_LEN];
			while (!decompresser.finished()) {
				int i = decompresser.inflate(buf);
				o.write(buf, 0, i);
			}
			output = o.toByteArray();
		} catch (Exception e) {
			output = data;
			logger.error("zip error", e);
		} finally {
			CloseUtils.close(o);
		}

		decompresser.end();
		return output;
	}
}
