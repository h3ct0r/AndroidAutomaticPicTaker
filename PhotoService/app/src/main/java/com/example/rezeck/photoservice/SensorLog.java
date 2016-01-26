package com.example.rezeck.photoservice;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by rezeck on 25/01/16.
 */
public class SensorLog implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mAcc;

    private final Context context;
    private File path, file;
    private FileOutputStream stream;

    public SensorLog(Context context) {
        this.context = context;

        path = android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        file = new File(path, "acc_data.txt");

        try{
            stream = new FileOutputStream(file);
        } catch (Exception error) {
            Log.d("debug", "Can't save in this file!");
        }

        mSensorManager = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);
        mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAcc, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void cancel(){
        try {
            stream.close();
        } catch (Exception error) {
            Log.d("debug", "Can't cancel to write in file. This file don't exist anymore.");
        }
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        // The light sensor returns a single value.
        // Many sensors return 3 values, one for each axis.

        String data = event.values[0] + "\t" + event.values[1] + "\t" + event.values[2] + "\n";
        //Log.d("debug", "data: " + data);
        try{
            stream.write(data.getBytes());
        } catch (Exception error) {
            Log.d("debug", "Can't save {" + data + "} in this file!");
        }
        // Do something with this sensor value.
    }
}
