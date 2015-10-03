package com.matthewjconrad.facerecognizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Add face to training library
 * 
 * @author Matthew
 *
 */
public class AddFace extends Activity implements View.OnClickListener {

	// declare constants and objects
	private static final int ACTION_TAKE_PHOTO = 1;
	private static final String BITMAP_STORAGE_KEY = "viewbitmap";
	private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY = "imageviewvisibility";

	private ImageView mImageView;
	private Bitmap mImageBitmap;
	private String mCurrentPhotoPath;
	private String mFileListPath;

	String albumPath = "/storage/emulated/0/DCIM/FaceRecognizer/";
	String trueFileName, fileList = "";
	int numFaces;
	String faceName = null, nameForFile;
	Button picBtn, saveBtn, discardBtn;
	TextView nameText;
	EditText nameEdit;
	Bitmap bitmap;
	FaceDetector fd;

	SharedPreferences sharedPrefs;

	private AlbumStorageDirFactory mAlbumStorageDirFactory = null;

	/**
	 * Called when the activity is first created; sets the content view and
	 * initializes all buttons and image views, and the storage directory.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.addface);

		this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		this.mImageBitmap = null;
		ActionBar actionBar = this.getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		this.picBtn = (Button) this.findViewById(R.id.btnPic);
		this.saveBtn = (Button) this.findViewById(R.id.btnSave);
		this.discardBtn = (Button) this.findViewById(R.id.btnDiscard);
		this.nameEdit = (EditText) this.findViewById(R.id.editName);
		this.mImageView = (ImageView) this.findViewById(R.id.imgFace);
		this.mAlbumStorageDirFactory = new BaseAlbumDirFactory();

		this.picBtn.setOnClickListener(this);
		this.saveBtn.setOnClickListener(this);
		this.discardBtn.setOnClickListener(this);

		this.saveBtn.setEnabled(false);
		this.discardBtn.setEnabled(false);
	}

	/**
	 * Sets behavior for each button: capture button will start a camera intent,
	 * discard will delete a taken image, save will save a taken image.
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		// Capture button
		case R.id.btnPic:

			// if an image path exists, delete it
			if (this.mCurrentPhotoPath != null) {
				File uselessFile = new File(this.mCurrentPhotoPath);
				uselessFile.delete();
			}

			// if a name hasn't been entered into the edit text, notify the
			// user
			if (this.nameEdit.getText().toString().equals("")) {
				// create alert dialog builder
				AlertDialog.Builder alert = new AlertDialog.Builder(AddFace.this);
				alert.setMessage("Please enter a name before trying to take a photo.");
				alert.setCancelable(false);

				// if user presses try again, restart activity
				alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				});

				alert.show();

			} else { // otherwise, set the name and dispatch the photo intent
				this.setName();
				this.dispatchTakePictureIntent(ACTION_TAKE_PHOTO);
			}
			break;
		// Discard button
		case R.id.btnDiscard:

			// delete the current image
			if (this.mCurrentPhotoPath != null) {
				File uselessFile = new File(this.mCurrentPhotoPath);
				uselessFile.delete();
			}

			// disable the image view, and disable the save and discard buttons
			this.mImageView.setVisibility(View.INVISIBLE);
			this.saveBtn.setEnabled(false);
			this.discardBtn.setEnabled(false);

			break;
		// Save button
		case R.id.btnSave:

			// create file output stream
			FileOutputStream out;
			try {
				// save the image; notify user through Toast
				out = new FileOutputStream(this.mCurrentPhotoPath);
				this.bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
				Toast.makeText(this, "Face saved!", Toast.LENGTH_SHORT).show();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			// Generate the list of image files for use in face recognition
			this.generateFileList();

			// Reset the image path to null, disable the save and discard
			// buttons
			this.mCurrentPhotoPath = null;
			this.saveBtn.setEnabled(false);
			this.discardBtn.setEnabled(false);

			break;
		}
	}

	/**
	 * Sets behavior for buttons in the action bar. The application icon will
	 * return to the main application screen.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// app icon in action bar clicked; go home
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			this.startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Deletes the image to prevent errors, and resets the imageView, save
	 * button, and discard button to their default states
	 * 
	 */
	@Override
	public void onPause() {
		super.onPause();

		// delete the image
		if (this.mCurrentPhotoPath != null) {
			File uselessFile = new File(this.mCurrentPhotoPath);
			uselessFile.delete();
		}

		// set the image view to invisible, disable the save and discard buttons
		this.mImageView.setVisibility(View.INVISIBLE);
		this.saveBtn.setEnabled(false);
		this.discardBtn.setEnabled(false);
	}

	/**
	 * Creates a dialog to notify the user that a face was not detected.
	 * 
	 */
	public void noFaceDetectedDialog() {
		// create alert dialog builder
		AlertDialog.Builder alert = new AlertDialog.Builder(AddFace.this);
		alert.setTitle("No face detected!");
		alert.setMessage("We couldn't find a face in your picture!");
		alert.setCancelable(false);

		// if user presses try again, restart activity
		alert.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				if (AddFace.this.mCurrentPhotoPath != null) {
					File uselessFile = new File(AddFace.this.mCurrentPhotoPath);
					uselessFile.delete();
				}
				AddFace.this.setName();
				AddFace.this.dispatchTakePictureIntent(ACTION_TAKE_PHOTO);
			}
		});

		alert.show();

	}

	/**
	 * Sets the name for file based on the contents of the editText. Gets name
	 * from the editText, then replaces all whitespace with a special character.
	 * 
	 */
	public void setName() {
		// Get name from editText
		this.faceName = this.nameEdit.getText().toString();
		String temp = this.faceName;

		// Replace any whitespace with a special character
		this.nameForFile = temp.replaceAll("\\s", "@");
	}

	/**
	 * Generates the text file containing the list of images used for training
	 * in the face recognizer.
	 * 
	 * Function iterates through the application's media directory, and assigns
	 * the file a person number based on the previous name. After the list has
	 * been generated, it is saved to a text file.
	 * 
	 */
	public void generateFileList() {
		File albumF = this.getAlbumDir();
		int personNum = 0;
		String personName;
		String previousName = "";
		this.fileList = "";
		for (File f : albumF.listFiles()) {
			if (f.isFile() && f.getName().contains(".png")) {
				String nameFromFile = f.getName();
				int indexOfUnderscore = nameFromFile.indexOf("_");

				String temp = nameFromFile.substring(0, indexOfUnderscore);
				personName = temp;

				if (!personName.equals(previousName)) {
					personNum++;
				}

				this.fileList = this.fileList + personNum + " " + personName + " " + this.getAlbumDir() + "/"
						+ nameFromFile + "\n";
				previousName = personName;
			}
		}
		this.mFileListPath = this.albumPath + "TrainingList.txt";
		FileOutputStream outToFile;
		try {
			outToFile = new FileOutputStream(AddFace.this.mFileListPath);
			outToFile.write(this.fileList.getBytes());
			outToFile.flush();
			outToFile.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Indicates whether the specified action can be used as an intent. This
	 * method queries the package manager for installed packages that can
	 * respond to an intent with the specified action. If no suitable package is
	 * found, this method returns false.
	 * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
	 * 
	 * @param context
	 *            The application's environment.
	 * @param action
	 *            The Intent action to check for availability.
	 * 
	 * @return True if an Intent with the specified action can be sent and
	 *         responded to, false otherwise.
	 */
	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	/**
	 * Sets activity behavior when an intent returns. Handles cases for the
	 * application returning from taking a photo.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTION_TAKE_PHOTO: {
			if (resultCode == RESULT_OK) {

				if (this.mCurrentPhotoPath != null) {
					this.setPic();
					if (this.numFaces > 0) {
						this.galleryAddPic();
					} else {
						this.noFaceDetectedDialog();
					}
				}
			}
			break;
		}
		}
	}

	/**
	 * Retrieves album name for the application.
	 * 
	 * @return the album name specified in the application's string.xml file
	 */
	private String getAlbumName() {
		return this.getString(R.string.album_name);
	}

	/**
	 * Finds (or creates) the storage directory for the images taken by the
	 * application.
	 * 
	 * @return the storage directory for the application
	 */
	private File getAlbumDir() {
		File storageDir = null;

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

			storageDir = this.mAlbumStorageDirFactory.getAlbumStorageDir(this.getAlbumName());

			if (storageDir != null) {
				if (!storageDir.mkdirs()) {
					if (!storageDir.exists()) {
						Log.d("CameraSample", "failed to create directory");
						return null;
					}
				}
			}

		} else {
			Log.v(this.getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
		}

		return storageDir;
	}

	/**
	 * Creates the image file for the specified person. Takes name from input,
	 * appends a number based on how many files in the directory contain that
	 * name, and creates the file.
	 * 
	 * @param name
	 *            the name of the person in the photo
	 * @return the file for the image
	 * @throws IOException
	 */
	private File createImageFile(String name) throws IOException {
		// Create an image file name
		int picNum = 1;
		File albumF = this.getAlbumDir();
		for (File f : albumF.listFiles()) {
			if (f.isFile()) {
				String nameFromFile = f.getName();
				if (nameFromFile.contains(name)) {
					picNum++;
				}
			}
		}
		String imageFileName = name + "_" + picNum;
		this.trueFileName = albumF.getAbsolutePath() + "/" + imageFileName + ".png";
		File imageF = new File(this.trueFileName);
		return imageF;
	}

	/**
	 * Creates the image file for the given filename, and sets the file path.
	 * 
	 * @param string
	 *            the filename to be created
	 * @return the created file
	 * @throws IOException
	 */
	private File setUpPhotoFile(String string) throws IOException {

		File f = this.createImageFile(string);
		this.mCurrentPhotoPath = f.getAbsolutePath();

		return f;
	}

	/**
	 * Processes the taken image. Function scales the image, rotates it to
	 * display in portrait, and detects a face in the image. If a face is found,
	 * the image is cropped to the face, displayed in the image view, then
	 * rescaled to 92 x 112 pixels and converted to grayscale.
	 */
	@SuppressWarnings("deprecation")
	private void setPic() {

		/*
		 * There isn't enough memory to open up more than a couple camera photos
		 */
		/* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the ImageView */
		int targetW = 306;
		int targetH = 408;

		/* Get the size of the image */
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inPreferredConfig = Bitmap.Config.RGB_565;
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(this.mCurrentPhotoPath, bmOptions);
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;

		/* Figure out which way needs to be reduced less */
		int scaleFactor = 1;
		if ((targetW > 0) || (targetH > 0)) {
			scaleFactor = Math.min(photoW / targetW, photoH / targetH);
		}

		/* Set bitmap options to scale the image decode target */
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
		this.bitmap = BitmapFactory.decodeFile(this.mCurrentPhotoPath, bmOptions);

		/* Rotate bitmap */
		Matrix matrix = new Matrix();
		matrix.postRotate(90);
		this.bitmap = Bitmap.createBitmap(this.bitmap, 0, 0, this.bitmap.getWidth(), this.bitmap.getHeight(), matrix,
				true);

		/* Detect faces */
		this.fd = new FaceDetector(this.bitmap.getWidth(), this.bitmap.getHeight(), 1);
		FaceDetector.Face[] faces = new FaceDetector.Face[1];
		this.numFaces = this.fd.findFaces(this.bitmap, faces);

		/*
		 * If face detected, crop image to face and convert to gray-scale; save
		 * image to database folder as .png
		 */
		if (this.numFaces == 0) {
			File uselessFile = new File(this.trueFileName);
			uselessFile.delete();
		} else if (this.numFaces > 0) {
			int faceWidth = (int) ((int) faces[0].eyesDistance() * 2.5);
			int faceHeight = (int) ((int) faces[0].eyesDistance() * 3.5);
			PointF midpoint = new PointF();
			faces[0].getMidPoint(midpoint);
			int startX = (int) midpoint.x - (faceWidth / 2);
			int startY = (int) midpoint.y - (faceHeight / 2);

			this.bitmap = Bitmap.createBitmap(this.bitmap, startX, startY, faceWidth, faceHeight);

			this.bitmap = Bitmap.createScaledBitmap(this.bitmap, 230, 280, false);
			/* Associate the Bitmap to the ImageView */
			this.mImageView.setImageBitmap(this.bitmap);
			this.mImageView.setVisibility(View.VISIBLE);
			this.saveBtn.setEnabled(true);
			this.discardBtn.setEnabled(true);

			// finish processing
			this.bitmap = Bitmap.createScaledBitmap(this.bitmap, 92, 112, false);
			this.bitmap = this.toGrayscale(this.bitmap);

		}

	}

	/**
	 * Adds the image to the device's gallery application.
	 */
	private void galleryAddPic() {
		Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
		File f = new File(this.mCurrentPhotoPath);
		Uri contentUri = Uri.fromFile(f);
		mediaScanIntent.setData(contentUri);
		this.sendBroadcast(mediaScanIntent);
	}

	/**
	 * Starts the camera intent to let the user take an image.
	 * 
	 * @param actionCode
	 *            the integer which determines which action to take
	 */
	private void dispatchTakePictureIntent(int actionCode) {

		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		switch (actionCode) {
		case ACTION_TAKE_PHOTO:
			File f = null;

			try {
				f = this.setUpPhotoFile(this.nameForFile);
				this.mCurrentPhotoPath = f.getAbsolutePath();
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));

			} catch (IOException e) {
				e.printStackTrace();
				f = null;
				this.mCurrentPhotoPath = null;
			}
			break;

		default:
			break;
		} // switch

		this.startActivityForResult(takePictureIntent, actionCode);
	}

	/**
	 * Converts a given bitmap to grayscale.
	 * 
	 * @param bmpOriginal
	 *            the bitmap image to convert
	 * @return the grayscale bitmap image
	 */
	public Bitmap toGrayscale(Bitmap bmpOriginal) {
		int width, height;
		height = bmpOriginal.getHeight();
		width = bmpOriginal.getWidth();

		Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		Canvas c = new Canvas(bmpGrayscale);
		Paint paint = new Paint();
		ColorMatrix cm = new ColorMatrix();
		cm.setSaturation(0);
		ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
		paint.setColorFilter(f);
		c.drawBitmap(bmpOriginal, 0, 0, paint);
		return bmpGrayscale;
	}

	/**
	 * Some lifecycle callbacks so that the image can survive orientation change
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelable(BITMAP_STORAGE_KEY, this.mImageBitmap);
		outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY, (this.mImageBitmap != null));
		super.onSaveInstanceState(outState);
	}

	/**
	 * Restores the state of the activity.
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		this.mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
		this.mImageView.setImageBitmap(this.mImageBitmap);
		this.mImageView.setVisibility(savedInstanceState.getBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY)
				? ImageView.VISIBLE : ImageView.INVISIBLE);

	}

}