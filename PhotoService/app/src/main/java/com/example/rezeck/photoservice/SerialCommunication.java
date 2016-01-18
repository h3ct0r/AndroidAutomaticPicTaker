package com.example.rezeck.photoservice;

/**
 * Created by h3ct0r on 13/1/16.
 */

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SerialCommunication implements ICommunication {

    private static final int SEND_TIMEOUT = 100;

    private static final String TAG = SerialCommunication.class.getSimpleName();

    private UsbSerialPort serialPort;

    @Override
    public synchronized void send(byte[] data) {
        if(serialPort == null){
            Log.e(TAG, "Serial port not initiated");
            return;
        }

        try {
            serialPort.write(data, 1000);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

    }

    @Override
    public synchronized void send(byte data) {
        if(serialPort == null){
            Log.e(TAG, "Serial port not initiated");
            return;
        }

        try {
            serialPort.write(new byte[] { data }, SEND_TIMEOUT);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public synchronized int receive(byte[] buffer) {
        if(serialPort == null){
            Log.e(TAG, "Serial port not initiated, return -1");
            return -1;
        }

        try {
            return serialPort.read(buffer, SEND_TIMEOUT);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return -1;
        }

    }

    @Override
    public void connect(Map<String, Object> params) throws Exception {
        Activity activity = (Activity) params.get("Activity");

        // Serial parameters
        int baudRate = (Integer) params.get("baudrate");
        int dataBits = (Integer) params.get("dataBits");
        int stopBits = (Integer) params.get("stopBits");
        int parity = (Integer) params.get("parity");


        UsbManager manager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            throw new Exception("Error opening the driver, no available drivers.");
        }

        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            throw new Exception("Error opening the driver, connection is null.");
        }

        serialPort = driver.getPorts().get(0);
        try {
            serialPort.open(connection);
            serialPort.setParameters(baudRate, dataBits, stopBits, parity);
        } catch (IOException e) {
            serialPort.close();
            throw new Exception("Error opening the driver.", e);
        }
    }

    @Override
    public void disconnect() throws Exception {
        serialPort.close();
    }

}
