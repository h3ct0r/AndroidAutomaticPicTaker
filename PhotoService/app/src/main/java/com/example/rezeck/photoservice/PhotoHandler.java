package com.example.rezeck.photoservice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

//import com.example.rezeck.autopictakerwithgps.MainActivity.
//import com.example.photoservice.MainActivity.PhotoTask;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.location.Location;
import android.media.ExifInterface;
import android.net.ParseException;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class PhotoHandler implements PictureCallback {

    private final Context context;
    private static final String DEBUG_TAG = "DEBUG_TAG";
    private Integer quantityPerStep = 1;
    private Integer photoCounter = 0;
    private Toast mToast;
    private GpsHelper gpsHelper = null;
    //private TextView tv = null;
    private AsctecDriver driver = null;
    private Boolean isConnected = false;
    private String gpsStatus;

    public PhotoHandler(Context context, Toast toast) {
        this.context = context;
        // this.mToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
        this.mToast = toast;

        gpsStatus = "GPS is not enabled.";

        gpsHelper = new GpsHelper(context);
        if (gpsHelper.isGPSenabled()) gpsStatus =  "Using GPS phones.";

        driver = new AsctecDriver();
        gpsAsctec();
        //  this.tv = (TextView) ((Activity) context).findViewById(R.id.textGPS);
    }

    public String getGpsStatus(){
        return gpsStatus;
    }

    public boolean gpsAsctec(){
        Boolean success = false;
        if (!isConnected) {
            try {
                driver.connect(getSerialParameters((Activity) this.context));
                Log.d("Asctec", ">> Connected");
                success = true;
                gpsStatus = "Using GPS Asctec.";
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Asctec", e.getMessage());
            }
        } else {
            try {
                driver.disconnect();
                Log.d("Asctec", "GPS Asctec is Disconnected.");
                success = true;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Asctec", e.getMessage());
            }
        }
        if(success) isConnected = !isConnected;

        return isConnected;
    }

    public void setQuantityPerBurst(int quantity) {
        this.quantityPerStep = quantity;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

        File pictureFileDir = getDir();

        if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {

            Log.d(DEBUG_TAG, "Can't create directory to save image.");
            mToast.setText("Can't create directory to save image.");
            mToast.show();
            return;

        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
        String date = dateFormat.format(new Date());
        String photoFile = "Picture_" + date + "_" + photoCounter + ".jpg";

        String filename = pictureFileDir.getPath() + File.separator + photoFile;

        double lat = 0;
        double lon = 0;

        double[] senseGPS = null;

        if (isConnected) {// GPS from Asctec drone is available
            senseGPS = driver.senseGPS();
        }

        gpsStatus = "GPS is not enabled.";

        if (senseGPS != null) {
            lat = senseGPS[0];
            lon = senseGPS[1];
            gpsStatus = "Using GPS Asctec.";
        }else{ // GPS phones
            gpsHelper.getMyLocation();

            if(gpsHelper.isGPSenabled()){
                lat = gpsHelper.getLatitude();
                lon = gpsHelper.getLongitude();
                gpsStatus = "Using GPS phones.";
            }
        }

        //String debugGps = String.valueOf(lat) + " " + String.valueOf(lon);
        //this.tv.setText(debugGps);
        //Log.d(DEBUG_TAG, debugGps);
        //mToast.setText(debugGps);
        //mToast.show();

        (new WriteToFileTask(data, filename, lat, lon)).execute();

        photoCounter++;
        if (photoCounter < quantityPerStep) {
            camera.startPreview();
            camera.takePicture(null, null, this);
        } else {
            photoCounter = 0;
        }
        camera.startPreview();
    }

    private File getDir() {
        File sdDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        return new File(sdDir, "test_GPS_" + String.valueOf(day) + "-"
                + String.valueOf(month) + "-" + String.valueOf(year));
    }

    public class WriteToFileTask extends AsyncTask<Void, Void, Boolean> {

        private byte[] data;
        private String picturePath;
        private String errorMsg = "";
        double lat = 0;
        double lon = 0;

        public WriteToFileTask(byte[] data, String path, double lat, double lon) {
            this.data = data;
            this.picturePath = path;
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            File pictureFile = new File(this.picturePath);

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                MarkGeoTagImage(this.picturePath, this.lat, this.lon);
            } catch (Exception error) {
                Log.d(DEBUG_TAG, "File" + this.picturePath + "not saved: "
                        + error.getMessage());
                this.errorMsg = error.getMessage();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                mToast.setText("New Image saved:" + this.picturePath);
                mToast.show();
            } else {
                mToast.setText("Image could not be saved. " + this.errorMsg);
                mToast.show();
            }
        }

        @Override
        protected void onCancelled() {

        }

        /**
         * Write Location information to image.
         *
         * @param imagePath : image absolute path
         * @return : location information
         */
        public void MarkGeoTagImage(String imagePath, double lat, double lon) {
            try {
                ExifInterface exif = new ExifInterface(imagePath);
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE,
                        GPS.convert(lat));
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF,
                        GPS.latitudeRef(lat));
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,
                        GPS.convert(lon));
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF,
                        GPS.longitudeRef(lon));

                exif.saveAttributes();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Read location information from image.
     *
     * @param imagePath
     *            : image absolute path
     * @return : loation information
     */
    public Location readGeoTagImage(String imagePath) {
        Location loc = new Location("");
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            float[] latlong = new float[2];
            if (exif.getLatLong(latlong)) {
                loc.setLatitude(latlong[0]);
                loc.setLongitude(latlong[1]);
            }
            String date = exif.getAttribute(ExifInterface.TAG_DATETIME);
            SimpleDateFormat fmt_Exif = new SimpleDateFormat(
                    "yyyy:MM:dd HH:mm:ss");
            try {
                loc.setTime(fmt_Exif.parse(date).getTime());
            } catch (java.text.ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return loc;
    }

    private HashMap<String, Object> getSerialParameters(Activity act) {
        HashMap<String, Object> params = new HashMap<String, Object>();

        Log.i(this.getClass().getName(), "Activity -> " + act);

        params.put("Activity", act);
        params.put("baudrate", 57600);
        params.put("dataBits", 8);
        params.put("stopBits", 1);
        params.put("parity", 0);

        return params;
    }
}

// Code to convert Degrees to DMS unit

class GPS {
    private static StringBuilder sb = new StringBuilder(20);

    /**
     * returns ref for latitude which is S or N.
     *
     * @param latitude
     * @return S or N
     */
    public static String latitudeRef(final double latitude) {
        return latitude < 0.0d ? "S" : "N";
    }

    /**
     * returns ref for latitude which is S or N.
     *
     * @param latitude
     * @return S or N
     */
    public static String longitudeRef(final double longitude) {
        return longitude < 0.0d ? "W" : "E";
    }

    /**
     * convert latitude into DMS (degree minute second) format. For instance<br/>
     * -79.948862 becomes<br/>
     * 79/1,56/1,55903/1000<br/>
     * It works for latitude and longitude<br/>
     *
     * @param latitude
     *            could be longitude.
     * @return
     */
    public static final String convert(double latitude) {
        latitude = Math.abs(latitude);
        final int degree = (int) latitude;
        latitude *= 60;
        latitude -= degree * 60.0d;
        final int minute = (int) latitude;
        latitude *= 60;
        latitude -= minute * 60.0d;
        final int second = (int) (latitude * 1000.0d);
        sb.setLength(0);
        sb.append(degree);
        sb.append("/1,");
        sb.append(minute);
        sb.append("/1,");
        sb.append(second);
        sb.append("/1000,");
        return sb.toString();
    }

}
