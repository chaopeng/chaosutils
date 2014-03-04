package me.chaopeng.utils;

import org.apache.commons.codec.binary.Base64;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UUID工具
 *
 * @author chao
 */
public class UUIDUtils {

	/**
	 * 获取一个uuid串
	 *
	 * @return 完整的uuid串
	 */
	public String uuid() {
		return UUID.randomUUID().toString();
	}

	/**
	 * 获取一个短uuid,使用base64压缩
	 *
	 * @return 压缩后的uuid串
	 */
	public static String shortUuid() {
		UUID uuid = UUID.randomUUID();
		return compressedUUID(uuid);
	}

	/**
	 * unrecommended 压缩uuid串
	 *
	 * @param uuidString
	 * @return 压缩后的uuid串
	 */
	public static String compress(String uuidString) {
		UUID uuid = UUID.fromString(uuidString);
		return compressedUUID(uuid);
	}

	/**
	 * unrecommended 解压缩uuid串
	 *
	 * @param compressedUuid
	 * @return 完整的uuid串
	 */
	public static String uncompress(String compressedUuid) {
		if (compressedUuid.length() != 22) {
			throw new IllegalArgumentException("Invalid uuid!");
		}
		byte[] byUuid = Base64.decodeBase64(compressedUuid + "==");
		long most = bytes2long(byUuid, 0);
		long least = bytes2long(byUuid, 8);
		UUID uuid = new UUID(most, least);
		return uuid.toString();
	}

	private static String compressedUUID(UUID uuid) {
		byte[] byUuid = new byte[16];
		long least = uuid.getLeastSignificantBits();
		long most = uuid.getMostSignificantBits();
		long2bytes(most, byUuid, 0);
		long2bytes(least, byUuid, 8);
		String compressUUID = Base64.encodeBase64URLSafeString(byUuid);
		return compressUUID;
	}

	private static void long2bytes(long value, byte[] bytes, int offset) {
		for (int i = 7; i > -1; i--) {
			bytes[offset++] = (byte) ((value >> 8 * i) & 0xFF);
		}
	}

	private static long bytes2long(byte[] bytes, int offset) {
		long value = 0;
		for (int i = 7; i > -1; i--) {
			value |= (((long) bytes[offset++]) & 0xFF) << 8 * i;
		}
		return value;
	}


	/**
	 * twitter distributed uuid implementation <a href="https://github.com/twitter/snowflake">see</a>
	 * <p/>
	 * +-------------------------+------------------+-----------------------+ <br>
	 * | 41bit millis timestamp  | 6bit mechine id  | 17bit sequence number | <br>
	 * +-------------------------+------------------+-----------------------+ <br>
	 */
	public static class Snowflake {
		private final int mechineId;

		private int sequence = 0;
		private final ConcurrentLinkedQueue<Long> queue = new ConcurrentLinkedQueue<>();
		private volatile int size = 0;
		private final AtomicBoolean filling = new AtomicBoolean(false);

		private final static int MECHINE_ID_BITS = 6;
		private final static int SEQUENCE_BITS = 17;
		private final static int SEQUENCE_MASK = -1 ^ (-1 << SEQUENCE_BITS);

		private final static int MECHINE_ID_SHIFT = SEQUENCE_BITS;
		private final static int TIMESTAMP_SHIFT = SEQUENCE_BITS + MECHINE_ID_BITS;

		private static Snowflake ins;

		/**
		 * @param mechineId must < 64
		 */
		public Snowflake(int mechineId) {
			if (mechineId >= 64) {
				throw new RuntimeException("mechineId must < 64");
			}

			this.mechineId = mechineId << MECHINE_ID_SHIFT;
			ins = this;
			fill();
		}

		/**
		 * must ensure Snowflake(mechineId) is called
		 * @return 64bit uuid
		 */
		public static Long get() {
			Long res = ins.queue.poll();
			--ins.size;
			if (ins.size < 1000 && !ins.filling.get()) {
				ins.fill();
			}

			return res;
		}

		private synchronized void fill() {
			if (size < 1000 && !filling.get()) {
				filling.set(true);
				ThreadPool.getPool().execute(new Runnable() {

					private long lastTimestamp = System.currentTimeMillis();

					@Override
					public void run() {
						int s = queue.size();
						while (++s < 10000) {
							sequence = (sequence + 1) & SEQUENCE_MASK;
							if(sequence == 0) {
								long time = System.currentTimeMillis();
								while (time <= lastTimestamp) {
									time = System.currentTimeMillis();
								}
								lastTimestamp = time;
							}

							long res = 0;
							res += lastTimestamp << TIMESTAMP_SHIFT;
							res += mechineId;

							res += sequence;
							queue.add(res);
						}
						size = queue.size();
						filling.set(false);
					}
				});
			}
		}
	}

}
