package com.example.rezeck.photoservice;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity implements SurfaceHolder.Callback {

    private Toast genericToast;
    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private boolean previewing = false;
    private PhotoHandler ph;
    private int PHOTO_INTERVAL = 900000;
    //private final int DEFAULT_PHOTO_INTERVAL = 900000;
    //private int PHOTO_QUANTITY_PER_STEP = 1;
    //private final int DEFAULT_PHOTO_QUANTITY_PER_STEP = 1;
    private TextView tv;
    private long timeLastBackButtonPressed = 0L;
    private int resPostion = 0;
    private AlertDialog.Builder singlechoicedialog;
    //private AlertDialog.Builder secondsIntervalDialog;
    //private AlertDialog.Builder burstPerShootDialog;
    private List<Size> cameraSizes;
    private ImageButton mButton;
    private ImageButton stopButton;
    private ImageButton gpsButton;

    //private View secondsChoiceView;
    //private View burstSizeChoiceView;

    private boolean isPaused = false;
    private String gpsStatus = "GPS is not enabled.";

    private static PhotoTask pt = null;

    public class PhotoTask extends AsyncTask<Void, Void, Boolean> {
        public boolean isrunning = true;

        public PhotoTask() {
            isrunning = true;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            ph.canTakePhoto = true;
            while (isrunning){
                if (camera != null && ph.canTakePhoto) {
                    if (!isPaused){
                        ph.mytimer = System.nanoTime();
                        ph.canTakePhoto = false;
                        camera.takePicture(myShutterCallback, myPictureCallback_RAW, ph);
                        //camera.startPreview();
                        previewing = true;
                    }
                }

            }
            return true;
        }

        @Override
        protected void onCancelled() {
            Log.d("debug", "Cancelling task");
        }


        @Override
        protected void onPostExecute(final Boolean success) {
            //startAsyncTask();
            //cancel(true);
        }
    }


    ShutterCallback myShutterCallback = new ShutterCallback() {

        @Override
        public void onShutter() {
        }
    };

    PictureCallback myPictureCallback_RAW = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {
        }
    };

    @Override
    public void onBackPressed() {
        long actualMiliSecs = System.currentTimeMillis();
        if (actualMiliSecs - timeLastBackButtonPressed <= 2000) {
            pt.isrunning = false;
            pt.cancel(true);
            this.finish();
            System.exit(0);
        }
        else {
            timeLastBackButtonPressed = System.currentTimeMillis();
            genericToast
                    .setText((String) getText(R.string.press_back_again_to_close));
            genericToast.show();
        }
        return;
    }

    @SuppressLint("ShowToast")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        genericToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        singlechoicedialog = new AlertDialog.Builder(this);
       // secondsIntervalDialog = new AlertDialog.Builder(this);
       // burstPerShootDialog = new AlertDialog.Builder(this);

        LayoutInflater factory = LayoutInflater.from(this);
       // secondsChoiceView = factory.inflate(R.layout.seconds_dialog, null);
       // burstSizeChoiceView = factory.inflate(R.layout.burst_size_dialog, null);

        mButton = (ImageButton) findViewById(R.id.button1);
        mButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);

                if (pt == null){
                    pt = (new PhotoTask());
                    pt.execute();
                }
                isPaused = false;
            }
        });

        stopButton = (ImageButton) findViewById(R.id.stopbt);
        stopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.GONE);
                isPaused = true;
            }
        });

        gpsButton = (ImageButton) findViewById(R.id.btn_gps);
        gpsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeButtonImage();
                ph.gpsAsctec();
            }
        });

        ph = new PhotoHandler(this, genericToast);
        changeButtonImage();

    }

    private void changeButtonImage(){
        if (ph != null)
            gpsStatus = ph.getGpsStatus();

        Log.d("debug", ph.getGpsStatus());

        if (gpsStatus.equals("Using GPS Asctec.")){
            gpsButton.setBackgroundResource(R.drawable.gpsasctec);
            return;
        }

        if (gpsStatus.equals("Using GPS phones.")){
            gpsButton.setBackgroundResource(R.drawable.gpsphone);
            return;
        }

        gpsButton.setBackgroundResource(R.drawable.gpsempty);
    }

    public void startAsyncTask() {
        new CountDownTimer(PHOTO_INTERVAL, 1000) {

            public void onFinish() {
               // (new PhotoTask()).execute();
                changeButtonImage();
            }

            public void onTick(long millisUntilFinished) {
                //Log.d("Debug", "Seconds remaining: " + millisUntilFinished / 1000);
                //tv.setText("Seconds remaining: " + millisUntilFinished / 1000);
            }
        }.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

        if (previewing) {
            camera.stopPreview();

            previewing = false;
        }

        if (camera != null) {
            try {
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                previewing = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        genericToast.setText("Number of cameras = "
                + Camera.getNumberOfCameras());
        genericToast.show();

        camera = Camera.open();
        Camera.Parameters parameters = camera.getParameters();


        //Log.d("debug", camera.getParameters().flatten().toString());

        parameters.setFocusMode("infinity");
        parameters.setSceneMode("landscape");


        if (android.os.Build.MODEL.equals("GT-I9505")){
            // Adjust to Samsung S4
            parameters.setFocusMode("infinity");
            parameters.setSceneMode("steadyphoto");
            Log.d("debug", "My phone is a " + android.os.Build.MODEL);
        }

        // parameters.setPreviewSize(surfaceView.getWidth(),
        // surfaceView.getHeight());

        cameraSizes = parameters.getSupportedPictureSizes();
        CharSequence[] items = new String[cameraSizes.size()];

        int w, h;
        for (int i = 0; i < cameraSizes.size(); i++) {
            Size s = cameraSizes.get(i);
            w = s.width;
            h = s.height;
            items[i] = new String(w + "x" + h);
        }

        singlechoicedialog.setTitle("Select image resolution ");
        singlechoicedialog.setSingleChoiceItems(items, -1,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        resPostion = item;

                        // get selected value
                        Camera.Parameters parameters = camera.getParameters();
                        Size size = cameraSizes.get(item);
                        parameters.setPictureSize(size.width, size.height);
                        camera.setParameters(parameters);
                        dialog.cancel();
                    }
                });
        AlertDialog alert_dialog = singlechoicedialog.create();
        alert_dialog.show();
        alert_dialog.getListView().setItemChecked(resPostion, true);

        /*secondsIntervalDialog.setTitle("Define seconds to shoot interval ");
        secondsIntervalDialog.setMessage("Enter seconds :");
        secondsIntervalDialog.setView(secondsChoiceView);
        secondsIntervalDialog.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        EditText userSeconds;
                        userSeconds = (EditText) secondsChoiceView
                                .findViewById(R.id.seconds);
                        String seconds = userSeconds.getText().toString();
                        try {
                            PHOTO_INTERVAL = (Integer.parseInt(seconds) * 1000);
                        } catch (Exception e) {
                            PHOTO_INTERVAL = DEFAULT_PHOTO_INTERVAL;
                        }
                        Log.d("Debug", "PHOTO_INTERVAL: " + PHOTO_INTERVAL / 1000);
                        return;
                    }
                });
        secondsIntervalDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        PHOTO_INTERVAL = DEFAULT_PHOTO_INTERVAL;
                        return;
                    }
                });

        AlertDialog textDialog = secondsIntervalDialog.create();
        textDialog.show();*/

        /*burstPerShootDialog.setTitle("Define burst number ");
        burstPerShootDialog.setMessage("Enter number of photos taken by every burst :");
        burstPerShootDialog.setView(burstSizeChoiceView);
        burstPerShootDialog.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        EditText userSeconds;
                        userSeconds = (EditText) burstSizeChoiceView
                                .findViewById(R.id.size);
                        String seconds = userSeconds.getText().toString();
                        try {
                            PHOTO_QUANTITY_PER_STEP = (Integer.parseInt(seconds));
                        } catch (Exception e) {
                            PHOTO_QUANTITY_PER_STEP = DEFAULT_PHOTO_QUANTITY_PER_STEP;
                        }
                        ph.setQuantityPerBurst(PHOTO_QUANTITY_PER_STEP);
                        return;
                    }
                });
        burstPerShootDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        PHOTO_QUANTITY_PER_STEP = DEFAULT_PHOTO_QUANTITY_PER_STEP;
                        ph.setQuantityPerBurst(PHOTO_QUANTITY_PER_STEP);
                        return;
                    }
                });

        AlertDialog burstDialog = burstPerShootDialog.create();
        burstDialog.show();*/

        camera.setParameters(parameters);

        Log.d("debug", "Focus Mode: " + camera.getParameters().getFocusMode());
        Log.d("debug", "Scene Mode: " + camera.getParameters().getSceneMode());
        Log.d("debug", "Scene Mode Suport: " + camera.getParameters().getSupportedSceneModes());

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
        camera = null;
        previewing = false;
    }
}
