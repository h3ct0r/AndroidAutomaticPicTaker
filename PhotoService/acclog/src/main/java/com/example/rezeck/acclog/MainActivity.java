package com.example.rezeck.acclog;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageButton;
import android.view.View;


public class MainActivity extends AppCompatActivity {

    private  SensorLog sensorLog;
    private ImageButton mButton;
    private Boolean isPlaying = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = (ImageButton) findViewById(R.id.imageButton);
        mButton.setImageResource(R.drawable.stop);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPlaying = !isPlaying;
                if (isPlaying) {
                    mButton.setImageResource(R.drawable.play);
                    sensorLog.play();

                } else {
                    mButton.setImageResource(R.drawable.stop);
                    sensorLog.cancel();
                }

            }
        });

        sensorLog = (new SensorLog(this));
    }
}
