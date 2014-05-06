package com.example.photoservice;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.example.photoservice.MainActivity.PhotoTask;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class PhotoHandler implements PictureCallback {

	private final Context context;
	private static final String DEBUG_TAG = "DEBUG_TAG";
	private Integer quantityPerStep = 1;
	private Integer photoCounter = 0;
	private Toast mToast;
	
	public PhotoHandler(Context context, Toast toast) {
		this.context = context;
		//this.mToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
		this.mToast = toast;
	}

	public void setQuantityPerBurst(int quantity){
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
		String photoFile = "Picture_" + date + "_"+photoCounter+".jpg";

		String filename = pictureFileDir.getPath() + File.separator + photoFile;
		
		(new WriteToFileTask(data, filename)).execute();
		
		photoCounter++;
		if(photoCounter < quantityPerStep){
			camera.startPreview();
			camera.takePicture(null,null, this);
		}
		else{
			photoCounter = 0;
		}
		camera.startPreview();
	}

	private File getDir() {
		File sdDir = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		return new File(sdDir, "test_marco");
	}
	
	public class WriteToFileTask extends AsyncTask<Void, Void, Boolean> {

		private byte[] data;
		private String picturePath;
		private String errorMsg = "";
		
		public WriteToFileTask(byte[] data, String path) {
			this.data = data;
			this.picturePath = path;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			File pictureFile = new File(this.picturePath);

			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();
			} catch (Exception error) {
				Log.d(DEBUG_TAG,
						"File" + this.picturePath + "not saved: " + error.getMessage());
				this.errorMsg = error.getMessage();
				return false;
			}
			return true;
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			if(success){
				mToast.setText("New Image saved:" + this.picturePath);
				mToast.show();
			}
			else{
				mToast.setText("Image could not be saved. "+this.errorMsg);
				mToast.show();
			}
		}

		@Override
		protected void onCancelled() {

		}
	}
}