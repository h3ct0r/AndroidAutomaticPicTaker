package com.example.rezeck.photoservice.util.crc16;

/**
 * Implementation for CRF16 in Java. This code is a migration of the Hummingbird
 * C code.
 * 
 */
public class CRC16JavaImp implements com.example.rezeck.photoservice.util.crc16.ICRC16 {

	private int crc_update(int crc, int data) {
		data ^= (crc & 0xff);
		data ^= (data << 4) & 0xff;

		return ((((int) data << 8) | ((crc >> 8) & 0xff)) ^ (char) (data >> 4) ^ ((int) data << 3)) & 0xffff;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see robotninja.communication.serial.android.crc16.ICRC16#crc16(int[])
	 */
	@Override
	public int crc16(int[] data) {
		int crc = 0xff;

		for (int i = 0; i < data.length; i++) {
			crc = crc_update(crc, data[i]);
		}
		return crc;
	}
}
