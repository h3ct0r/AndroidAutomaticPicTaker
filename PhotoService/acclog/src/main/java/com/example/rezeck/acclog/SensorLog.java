package com.example.rezeck.acclog;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;

/**
 * Created by rezeck on 25/01/16.
 */
public class SensorLog implements SensorEventListener {
    private SensorManager mSensorManager;
    private TextView tvX, tvY, tvZ;
    private Sensor mAcc;

    private final Context context;
    private File path, file;
    private FileOutputStream stream;

    private Boolean isRunning = false;

    public SensorLog(Context context) {
        this.context = context;
        Activity a = (Activity)context;
        tvX = (TextView) a.findViewById(R.id.textViewX);
        tvY = (TextView) a.findViewById(R.id.textViewY);
        tvZ = (TextView) a.findViewById(R.id.textViewZ);
        tvX.setText("");
        tvY.setText("");
        tvZ.setText("");


        mSensorManager = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);
        mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAcc, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void cancel(){
        try {
            stream.close();
            isRunning = false;
            tvX.setText("");
            tvY.setText("");
            tvZ.setText("");
        } catch (Exception error) {
            Log.d("debug", "Can't cancel to write in file. This file don't exist anymore.");
            isRunning = false;
        }
    }

    public void play(){
        isRunning = true;

        path = new File("/sdcard/accLog/");
        path.mkdirs();

        String date = (DateFormat.format("dd_MM_yyyy_hh_mm_ss", new java.util.Date()).toString());

        Log.d("debug", path.toString()+"/"+date);

        file = new File(path, "acc_data_"+date+".txt");
        try{
            stream = new FileOutputStream(file);
        } catch (Exception error) {
            Log.d("debug", "Can't save in this file!");
            isRunning = false;
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
        if (!isRunning) return;

        float x = event.values[0] + 0.0f;
        float y = event.values[1] + 0.0f;
        float z = event.values[2] + 0.0f;

        tvX.setText("X: " + String.format("%.10f", x));
        tvY.setText("Y: " + String.format("%.10f", y));
        tvZ.setText("Z: " + String.format("%.10f", z));

        String data = x + "\t" + y + "\t" + z + "\n";
        //Log.d("debug", "data: " + data);
        try{
            stream.write(data.getBytes());
        } catch (Exception error) {
            Log.d("debug", "Can't save {" + data + "} in this file!");
            isRunning = false;
        }
        // Do something with this sensor value.
    }
}
