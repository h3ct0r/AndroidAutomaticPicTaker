package com.example.rezeck.photoservice.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteConversor {

	/**
	 * Extract an signed integer vale from a byte array. It uses 4 bytes.
	 * 
	 * @param data
	 * @param i
	 *            index of the data
	 * @return extracted integer.
	 */
	public static int extractSignedInt(byte[] data, int i) {
		ByteBuffer bb = ByteBuffer.wrap(data, i, 4);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getInt();
	}

	/**
	 * Extract an unsigned short vale from a byte array. It uses 2 bytes.
	 * 
	 * @param data
	 * @param i
	 *            index of the data
	 * @return extracted short.
	 */
	public static int extractSignedShort(byte[] data, int i) {
		ByteBuffer bb = ByteBuffer.wrap(data, i, 2);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getShort();
	}
	
	/**
	 * Extract an unsigned short vale from a byte array. It uses 2 bytes.
	 * 
	 * @param data
	 * @param i
	 *            index of the data
	 * @return extracted short.
	 */
	public static int extractShort(int[] data, int i) {
		return data[i] | data[i + 1] << 8;
	}
}
