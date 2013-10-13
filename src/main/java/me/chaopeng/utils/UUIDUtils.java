package me.chaopeng.utils;

import java.util.UUID;

import org.apache.commons.codec.binary.Base64;

/**
 * UUID工具
 * 
 * @author chao
 * 
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
	 * @return 压缩后的uuid串
	 */
	public static String shortUuid() {
		UUID uuid = UUID.randomUUID();
		return compressedUUID(uuid);
	}
	
	/**
	 * unrecommended 压缩uuid串
	 * @param uuidString
	 * @return 压缩后的uuid串
	 */
	public static String compress(String uuidString) {
		UUID uuid = UUID.fromString(uuidString);
		return compressedUUID(uuid);
	}

	/**
	 * unrecommended 解压缩uuid串
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
}
