package com.matthewjconrad.facerecognizer;

/* FaceRecognition.java
 * 
 * MODIFIED BY MATTHEW CONRAD
 * CONRAD.272@OSU.EDU
 * JULY 2013
 *
 * Created on Dec 7, 2011, 1:27:25 PM
 *
 * Description: Recognizes faces.
 *
 * Copyright (C) Dec 7, 2011, Stephen L. Reed, Texai.org. (Fixed April 22, 2012, Samuel Audet)
 *
 * This file is a translation from the OpenCV example http://www.shervinemami.info/faceRecognition.html, ported
 * to Java using the JavaCV library.  Notable changes are the addition of the Java Logging framework and the
 * installation of image files in a data directory child of the working directory. Some of the code has
 * been expanded to make debugging easier.  Expected results are 100% recognition of the lower3.txt test
 * image index set against the all10.txt training image index set.  See http://en.wikipedia.org/wiki/Eigenface
 * for a technical explanation of the algorithm.
 *
 * stephenreed@yahoo.com
 *
 * FaceRecognition is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * FaceRecognition is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaCV.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
import static com.googlecode.javacv.cpp.opencv_core.CV_32FC1;
import static com.googlecode.javacv.cpp.opencv_core.CV_32SC1;
import static com.googlecode.javacv.cpp.opencv_core.CV_L1;
import static com.googlecode.javacv.cpp.opencv_core.CV_STORAGE_READ;
import static com.googlecode.javacv.cpp.opencv_core.CV_STORAGE_WRITE;
import static com.googlecode.javacv.cpp.opencv_core.CV_TERMCRIT_ITER;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_32F;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvConvertScale;
import static com.googlecode.javacv.cpp.opencv_core.cvCopy;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateMat;
import static com.googlecode.javacv.cpp.opencv_core.cvGetTickCount;
import static com.googlecode.javacv.cpp.opencv_core.cvGetTickFrequency;
import static com.googlecode.javacv.cpp.opencv_core.cvMinMaxLoc;
import static com.googlecode.javacv.cpp.opencv_core.cvNormalize;
import static com.googlecode.javacv.cpp.opencv_core.cvOpenFileStorage;
import static com.googlecode.javacv.cpp.opencv_core.cvReadByName;
import static com.googlecode.javacv.cpp.opencv_core.cvReadIntByName;
import static com.googlecode.javacv.cpp.opencv_core.cvReadStringByName;
import static com.googlecode.javacv.cpp.opencv_core.cvRect;
import static com.googlecode.javacv.cpp.opencv_core.cvReleaseFileStorage;
import static com.googlecode.javacv.cpp.opencv_core.cvReleaseImage;
import static com.googlecode.javacv.cpp.opencv_core.cvResetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvSetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;
import static com.googlecode.javacv.cpp.opencv_core.cvTermCriteria;
import static com.googlecode.javacv.cpp.opencv_core.cvWrite;
import static com.googlecode.javacv.cpp.opencv_core.cvWriteInt;
import static com.googlecode.javacv.cpp.opencv_core.cvWriteString;
import static com.googlecode.javacv.cpp.opencv_highgui.CV_LOAD_IMAGE_GRAYSCALE;
import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;
import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;
import static com.googlecode.javacv.cpp.opencv_legacy.CV_EIGOBJ_NO_CALLBACK;
import static com.googlecode.javacv.cpp.opencv_legacy.cvCalcEigenObjects;
import static com.googlecode.javacv.cpp.opencv_legacy.cvEigenDecomposite;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.SparseArray;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.Toast;

import com.googlecode.javacpp.FloatPointer;
import com.googlecode.javacpp.Pointer;
import com.googlecode.javacv.cpp.opencv_core.CvFileStorage;
import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.CvTermCriteria;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

/**
 * Recognizes faces.
 * 
 * @author reed; revised by Matthew Conrad
 */
@SuppressLint("DefaultLocale")
public class FaceRecognition extends Activity {

	/** the logger */
	private static final Logger LOGGER = Logger.getLogger(FaceRecognition.class.getName());
	/** the number of training faces */
	private int nTrainFaces = 0;
	/** the training face image array */
	IplImage[] trainingFaceImgArr;
	/** the test face image array */
	IplImage[] testFaceImgArr;
	/** the person number array */
	CvMat personNumTruthMat;
	/** the number of persons */
	int nPersons;
	/** the person names */
	final List<String> personNames = new ArrayList<String>();
	/** map of personNumber to personName */
	SparseArray<String> peopleMap = new SparseArray<String>();
	/** the number of eigenvalues */
	int nEigens = 0;
	/** eigenvectors */
	IplImage[] eigenVectArr;
	/** eigenvalues */
	CvMat eigenValMat;
	/** the average image */
	IplImage pAvgTrainImg;
	/** the projected training faces */
	CvMat projectedTrainFaceMat;

	// declare constants, objects
	Button btnTrain, btnRecognize;
	ImageView mImageView;
	Bitmap finalBitmap;
	QuickContactBadge contactBadge;
	String albumPath = "/storage/emulated/0/DCIM/FaceRecognizer/";
	String recText = this.albumPath + "Recognize.txt";
	String recPhoto = this.albumPath + "QUERY.png";
	String mCurrentPhotoPath;
	String faceName, nameForFile, fileList = "", contactID;
	int numFaces, nearest;
	private String mFileListPath;

	SharedPreferences sharedPrefs;

	private static final int ACTION_TAKE_PHOTO_B = 1;

	/**
	 * Called when the activity is first created; sets the content view and
	 * initializes all necessary objects, then creates the file list needed to
	 * recognize faces, performs recognition training, launches the camera, and
	 * sets up the text file used for recognition
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.face_rec_layout);

		File uselessFile = new File(this.recPhoto);
		uselessFile.delete();

		this.contactBadge = (QuickContactBadge) this.findViewById(R.id.contactBadge);
		this.contactBadge.setMode(ContactsContract.QuickContact.MODE_LARGE);

		this.finalBitmap = null;
		this.generateFileList();
		this.createPeopleMap(this.albumPath + "TrainingList.txt");
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			private ProgressDialog pd;

			@Override
			protected void onPreExecute() {
				this.pd = new ProgressDialog(FaceRecognition.this);
				this.pd.setTitle("Processing...");
				this.pd.setMessage("Learning from face database.");
				this.pd.setCancelable(true);
				this.pd.setIndeterminate(true);
				this.pd.show();
			}

			@Override
			protected Void doInBackground(Void... arg0) {
				FaceRecognition.this.learn(FaceRecognition.this.albumPath + "TrainingList.txt");
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				this.pd.dismiss();
				FaceRecognition.this.dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);

			}
		};
		task.execute((Void[]) null);

		FileOutputStream outTextFile;
		String textForList = "100 QUERY " + FaceRecognition.this.recPhoto;
		try {
			outTextFile = new FileOutputStream(FaceRecognition.this.recText);
			outTextFile.write(textForList.getBytes());
			outTextFile.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

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
		case ACTION_TAKE_PHOTO_B:
			File f = new File(this.recPhoto);
			this.mCurrentPhotoPath = f.getAbsolutePath();
			takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
			break;

		default:
			break;

		} // switch

		this.startActivityForResult(takePictureIntent, actionCode);
	}

	/**
	 * Handles activity behavior when intent returns. When application returns
	 * from camera intent, the picture is processed, recognition is performed,
	 * and the appropriate action given the recognition results is executed.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTION_TAKE_PHOTO_B: {
			if (resultCode == RESULT_OK) {

				if (this.mCurrentPhotoPath != null) {
					this.setPic();
					this.mCurrentPhotoPath = null;
				}

				// if a face was detected in the image, begin facial
				// recognition
				if (this.numFaces > 0) {
					boolean acceptableMatch = FaceRecognition.this.recognizeFileList(FaceRecognition.this.recText);

					// if the recognizer could not identify an acceptable
					// match, offer to add the person to the database
					if (!acceptableMatch) {
						this.noMatchDialog();
						break;
					} else {
						this.faceRecognizedDialog();
						File queryPhoto = new File(FaceRecognition.this.recPhoto);
						queryPhoto.delete();
					}
				} else {
					this.noFaceDetectedDialog();
					// delete the query photo
					File queryPhoto = new File(FaceRecognition.this.recPhoto);
					queryPhoto.delete();

				}
			}
			break;

		} // ACTION_TAKE_PHOTO_B

		} // switch

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
		File albumF = new File(this.albumPath);
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

				this.fileList = this.fileList + personNum + " " + personName + " " + this.albumPath + nameFromFile
						+ "\n";
				previousName = personName;
			}
		}
		this.mFileListPath = this.albumPath + "TrainingList.txt";
		FileOutputStream outToFile;
		try {
			outToFile = new FileOutputStream(this.mFileListPath);
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
	 * Creates a dialog, which presents the user the option to recognize another
	 * face. If the user selects yes, the camera is restarted; otherwise, the
	 * application returns to the main screen.
	 */
	public void continueDialog() {
		// create alert dialog builder
		AlertDialog.Builder alert = new AlertDialog.Builder(FaceRecognition.this);
		alert.setTitle("Continue?");
		alert.setMessage("Do you want to recognize another face?");
		alert.setCancelable(true);

		// when user presses yes, restart activity
		alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				FaceRecognition.this.dispatchTakePictureIntent(FaceRecognition.ACTION_TAKE_PHOTO_B);
			}
		});

		// when user presses no, return to main screen
		alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				Intent returnToStart = new Intent("com.matthewjconrad.facerecognizer.MAINACTIVITY");

				returnToStart.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				returnToStart.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				FaceRecognition.this.startActivity(returnToStart);
			}
		});

		alert.show();
	}

	/**
	 * Creates a dialog, which notifies the user that a face was not detected.
	 * When the user selects "try again," the camera is restarted.
	 */
	public void noFaceDetectedDialog() {
		// create alert dialog builder
		AlertDialog.Builder alert = new AlertDialog.Builder(FaceRecognition.this);
		alert.setTitle("No face detected!");
		alert.setMessage("We couldn't find a face in your picture!");
		alert.setCancelable(false);

		// if user presses try again, restart activity
		alert.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				Intent restartActivity = new Intent("com.matthewjconrad.facerecognizer.FACERECOGNITION");
				FaceRecognition.this.startActivity(restartActivity);
			}
		});

		alert.show();

		// delete the query photo
		File queryPhoto = new File(FaceRecognition.this.recPhoto);
		queryPhoto.delete();

	}

	/**
	 * Creates a dialog notifying the user that an acceptable match was not
	 * found, and offers to add the person to the database. If the user chooses
	 * to add the image, a second alert is created with an edit text for the
	 * desired name. If either dialog is cancelled, the function calls the
	 * continue dialog.
	 */
	public void noMatchDialog() {
		// create alert dialog builder
		AlertDialog.Builder alert = new AlertDialog.Builder(FaceRecognition.this);
		alert.setTitle("No match found!");
		alert.setMessage("We could not find a match. Add person to database?");

		// if "OK" button pressed, create new alert dialog
		alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				// create alert dialog builder
				AlertDialog.Builder alert = new AlertDialog.Builder(FaceRecognition.this);
				alert.setTitle("Name");
				alert.setMessage("Please enter the name of the person whose photo you want to add to the database.");

				// create editText to change name
				final EditText input = new EditText(FaceRecognition.this);
				input.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
				alert.setView(input);

				// if "OK" button pressed, remove
				// any whitespace in the name, try
				// to create the image file, and
				// return to main activity
				alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						FaceRecognition.this.faceName = input.getText().toString();
						String temp = FaceRecognition.this.faceName;
						FaceRecognition.this.nameForFile = temp.replaceAll("\\s", "@");
						try {
							FaceRecognition.this.createImageFile();
							Toast.makeText(FaceRecognition.this, "Fave saved!", Toast.LENGTH_SHORT).show();
						} catch (IOException e) {
							e.printStackTrace();
						}

						FaceRecognition.this.generateFileList();

						FaceRecognition.this.continueDialog();
					}
				});

				// if "Cancel" pressed, delete query photo and return to main
				// activity
				alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						File queryPhoto = new File(FaceRecognition.this.recPhoto);
						queryPhoto.delete();
						FaceRecognition.this.continueDialog();
					}
				});

				// draw the alert dialog
				alert.show();
			}
		});

		// if "Cancel" pressed, return to main activity
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				File queryPhoto = new File(FaceRecognition.this.recPhoto);
				queryPhoto.delete();
				FaceRecognition.this.continueDialog();
			}
		});

		// draw the alert dialog
		alert.show();
	}

	/**
	 * Creates a dialog notifying the user of a successful match. Upon pressing
	 * "ok," a quick contact badge for the matched person is displayed, if one
	 * exists, and the add match dialog is called.
	 */
	public void faceRecognizedDialog() {

		// create alert dialog builder
		AlertDialog.Builder alert = new AlertDialog.Builder(FaceRecognition.this);
		alert.setTitle("Match found!");
		String name = this.peopleMap.get(this.nearest).replaceAll("@", " ");
		this.contactID = this.queryDB(name);

		alert.setMessage("Recognized " + name + "!");
		if (!this.contactID.equals("")) {
			Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(this.contactID));
			this.contactBadge.assignContactUri(uri);
		}

		alert.setCancelable(false);

		// if "OK" button pressed, save, rename photo, and
		// add to gallery, then return to main screen
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {

				if (!FaceRecognition.this.contactID.equals("")) {
					FaceRecognition.this.contactBadge.performClick();
				} else {
					Toast.makeText(FaceRecognition.this, "No contact to display", Toast.LENGTH_SHORT).show();
				}
				FaceRecognition.this.addMatchDialog();
			}
		});

		// draw the alert dialog
		alert.show();
	}

	/**
	 * Creates a dialog offering to add the image of a matched face to the
	 * database. If the user selects "OK", the image is renamed, saved, and
	 * added to the gallery. Regardless of the user's choice, the continue
	 * dialog is called.
	 */
	public void addMatchDialog() {

		// create alert dialog builder
		AlertDialog.Builder alert = new AlertDialog.Builder(FaceRecognition.this);
		alert.setTitle("Add to database");
		String name = this.peopleMap.get(this.nearest).replaceAll("@", " ");
		alert.setMessage("Add picture of " + name + " to database?");

		// if "OK" button pressed, save, rename photo, and
		// add to gallery, then call continue dialog
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				FaceRecognition.this.nameForFile = FaceRecognition.this.peopleMap.get(FaceRecognition.this.nearest)
						.replaceAll("\\s", "@");
				try {
					FaceRecognition.this.createImageFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
				FaceRecognition.this.generateFileList();
				FaceRecognition.this.continueDialog();
			}
		});
		// if "Cancel" pressed, call continue dialog
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				File queryPhoto = new File(FaceRecognition.this.recPhoto);
				queryPhoto.delete();
				FaceRecognition.this.continueDialog();
			}
		});

		// draw the alert dialog
		alert.show();
	}

	/**
	 * Creates the image file for the current person. Takes name that has been
	 * stored, appends a number based on how many files in the directory contain
	 * that name, and creates the file.
	 * 
	 * @throws IOException
	 */
	private void createImageFile() throws IOException {
		// Create an image file name
		int picNum = 1;
		File albumF = new File(this.albumPath);
		for (File f : albumF.listFiles()) {
			if (f.isFile()) {
				String nameFromFile = f.getName();
				if (nameFromFile.contains(this.nameForFile)) {
					picNum++;
				}
			}
		}
		String imageFileName = this.nameForFile + "_" + picNum;
		String fullFile = this.albumPath + imageFileName + ".png";
		FileOutputStream out;
		try {
			out = new FileOutputStream(fullFile);
			this.finalBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		FaceRecognition.this.galleryAddPic(fullFile);
		File queryPhoto = new File(FaceRecognition.this.recPhoto);
		queryPhoto.delete();

	}

	/**
	 * Adds the image to the device's gallery application.
	 */
	private void galleryAddPic(String pathName) {
		Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
		File f = new File(pathName);
		Uri contentUri = Uri.fromFile(f);
		mediaScanIntent.setData(contentUri);
		this.sendBroadcast(mediaScanIntent);
		File queryPhoto = new File(FaceRecognition.this.recPhoto);
		queryPhoto.delete();
	}

	/**
	 * Processes the taken image. Function scales the image, rotates it to
	 * display in portrait, and detects a face in the image. If a face is found,
	 * the image is cropped to the face, displayed in the image view, then
	 * rescaled to 92 x 112 pixels and converted to grayscale. Finally, the
	 * image is saved.
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
		Bitmap bitmap = BitmapFactory.decodeFile(this.mCurrentPhotoPath, bmOptions);

		/* Rotate bitmap */
		Matrix matrix = new Matrix();
		matrix.postRotate(90);
		bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

		/* Detect faces */
		FaceDetector fd = new FaceDetector(bitmap.getWidth(), bitmap.getHeight(), 1);
		FaceDetector.Face[] faces = new FaceDetector.Face[1];
		this.numFaces = fd.findFaces(bitmap, faces);

		/*
		 * If face detected, crop image to face and convert to gray-scale; save
		 * image to database folder as .png
		 */
		if (this.numFaces == 0) {
			File uselessFile = new File(this.recPhoto);
			uselessFile.delete();
		} else if (this.numFaces > 0) {
			int faceWidth = (int) ((int) faces[0].eyesDistance() * 2.5);
			int faceHeight = (int) ((int) faces[0].eyesDistance() * 3.5);
			PointF midpoint = new PointF();
			faces[0].getMidPoint(midpoint);

			int startX = (int) midpoint.x - (faceWidth / 2);
			int startY = (int) midpoint.y - (faceHeight / 2);

			bitmap = Bitmap.createBitmap(bitmap, startX, startY, faceWidth, faceHeight);
			bitmap = Bitmap.createScaledBitmap(bitmap, 92, 112, false);
			this.finalBitmap = this.toGrayscale(bitmap);

			FileOutputStream out;
			try {
				out = new FileOutputStream(this.mCurrentPhotoPath);
				this.finalBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

		}

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
	 * Trains from the data in the given training text index file, and store the
	 * trained data into the file 'facedata.xml'.
	 * 
	 * @param trainingFileName
	 *            the given training text index file
	 */
	public void learn(final String trainingFileName) {
		int i;

		// load training data
		LOGGER.info("===========================================");
		LOGGER.info("Loading the training images in " + trainingFileName);
		this.trainingFaceImgArr = this.loadFaceImgArray(trainingFileName);
		this.nTrainFaces = this.trainingFaceImgArr.length;
		LOGGER.info("Got " + this.nTrainFaces + " training images");
		if (this.nTrainFaces < 3) {
			LOGGER.severe("Need 3 or more training faces\n" + "Input file contains only " + this.nTrainFaces);
			return;
		}

		// do Principal Component Analysis on the training faces
		this.doPCA();

		LOGGER.info("projecting the training images onto the PCA subspace");
		// project the training images onto the PCA subspace
		this.projectedTrainFaceMat = cvCreateMat(this.nTrainFaces, // rows
				this.nEigens, // cols
				CV_32FC1); // type, 32-bit float, 1 channel

		// initialize the training face matrix - for ease of debugging
		for (int i1 = 0; i1 < this.nTrainFaces; i1++) {
			for (int j1 = 0; j1 < this.nEigens; j1++) {
				this.projectedTrainFaceMat.put(i1, j1, 0.0);
			}
		}

		LOGGER.info("created projectedTrainFaceMat with " + this.nTrainFaces + " (nTrainFaces) rows and " + this.nEigens
				+ " (nEigens) columns");
		if (this.nTrainFaces < 5) {
			LOGGER.info("projectedTrainFaceMat contents:\n" + this.oneChannelCvMatToString(this.projectedTrainFaceMat));
		}

		final FloatPointer floatPointer = new FloatPointer(this.nEigens);
		for (i = 0; i < this.nTrainFaces; i++) {
			cvEigenDecomposite(this.trainingFaceImgArr[i], // obj
					this.nEigens, // nEigObjs
					this.eigenVectArr, // eigInput (Pointer)
					0, // ioFlags
					null, // userData (Pointer)
					this.pAvgTrainImg, // avg
					floatPointer); // coeffs (FloatPointer)

			if (this.nTrainFaces < 5) {
				LOGGER.info("floatPointer: " + this.floatPointerToString(floatPointer));
			}
			for (int j1 = 0; j1 < this.nEigens; j1++) {
				this.projectedTrainFaceMat.put(i, j1, floatPointer.get(j1));
			}
		}
		if (this.nTrainFaces < 5) {
			LOGGER.info("projectedTrainFaceMat after cvEigenDecomposite:\n" + this.projectedTrainFaceMat);
		}

		// store the recognition data as an xml file
		this.storeTrainingData();

		// Save all the eigenvectors as images, so that they can be checked.
		this.storeEigenfaceImages();
	}

	/**
	 * Recognizes the face in each of the test images given, and compares the
	 * results with the truth.
	 * 
	 * @param szFileTest
	 *            the index file of test images
	 * @return True if an acceptable match was found, false otherwise
	 */
	public boolean recognizeFileList(final String szFileTest) {
		LOGGER.info("===========================================");
		LOGGER.info("recognizing faces indexed from " + szFileTest);
		int i = 0;
		int nTestFaces = 0; // the number of test images
		CvMat trainPersonNumMat; // the person numbers during training
		float[] projectedTestFace;
		int nCorrect = 0;
		int nWrong = 0;
		double timeFaceRecognizeStart;
		double tallyFaceRecognizeTime;
		float confidence = 0.0f;
		boolean acceptableMatch = false;

		// load test images and ground truth for person number
		this.testFaceImgArr = this.loadFaceImgArray(szFileTest);
		nTestFaces = this.testFaceImgArr.length;

		LOGGER.info(nTestFaces + " test faces loaded");

		// load the saved training data
		trainPersonNumMat = this.loadTrainingData();
		if (trainPersonNumMat == null) {
			return false;
		}

		// project the test images onto the PCA subspace
		projectedTestFace = new float[this.nEigens];
		timeFaceRecognizeStart = cvGetTickCount(); // Record the timing.

		for (i = 0; i < nTestFaces; i++) {
			int iNearest;

			// project the test image onto the PCA subspace
			cvEigenDecomposite(this.testFaceImgArr[i], // obj
					this.nEigens, // nEigObjs
					this.eigenVectArr, // eigInput (Pointer)
					0, // ioFlags
					null, // userData
					this.pAvgTrainImg, // avg
					projectedTestFace); // coeffs

			// LOGGER.info("projectedTestFace\n" +
			// floatArrayToString(projectedTestFace));

			final FloatPointer pConfidence = new FloatPointer(confidence);
			iNearest = this.findNearestNeighbor(projectedTestFace, new FloatPointer(pConfidence));
			confidence = pConfidence.get();
			this.nearest = trainPersonNumMat.data_i().get(iNearest);

			LOGGER.info("nearest = " + this.nearest + ", Confidence = " + confidence);
			if (confidence > 0.250) {
				acceptableMatch = true;
			} else {
				acceptableMatch = false;
			}
		}
		tallyFaceRecognizeTime = cvGetTickCount() - timeFaceRecognizeStart;
		if (nCorrect + nWrong > 0) {
			LOGGER.info(
					"TOTAL TIME: " + (tallyFaceRecognizeTime / (cvGetTickFrequency() * 1000.0 * (nCorrect + nWrong)))
							+ " ms average.");
		}
		return acceptableMatch;
	}

	/**
	 * Generates a SparseArray used to associate a person's name with their
	 * assigned number in the image list.
	 * 
	 * @param filename
	 *            the file name of the image list
	 */
	private void createPeopleMap(String filename) {
		BufferedReader imgListFile;
		String imgFilename;
		int iFace = 0;
		int nFaces = 0;

		try {
			// open the input file
			imgListFile = new BufferedReader(new FileReader(filename));

			// count the number of faces
			while (true) {
				final String line = imgListFile.readLine();
				if (line == null || line.isEmpty()) {
					break;
				}
				nFaces++;
			}
			LOGGER.info("nFaces: " + nFaces);

			imgListFile = new BufferedReader(new FileReader(filename));

			this.personNames.clear(); // Make sure it starts as empty.
			this.nPersons = 0;

			// store the face images in an array
			for (iFace = 0; iFace < nFaces; iFace++) {
				String personName;
				String sPersonName;
				int personNumber;

				// read person number (beginning with 1), their name and the
				// image filename.
				final String line = imgListFile.readLine();
				if (line.isEmpty()) {
					break;
				}
				final String[] tokens = line.split(" ");
				personNumber = Integer.parseInt(tokens[0]);
				personName = tokens[1];
				String nameToPut = personName.replaceAll("@", " ");
				imgFilename = tokens[2];
				this.peopleMap.put(personNumber, nameToPut);
				sPersonName = personName;
				LOGGER.info("Got " + iFace + " " + personNumber + " " + personName + " " + imgFilename);

				// Check if a new person is being loaded.
				if (personNumber > this.nPersons) {
					// Allocate memory for the extra person (or possibly
					// multiple), using this new person's name.
					this.personNames.add(sPersonName);
					this.nPersons = personNumber;
					LOGGER.info("Got new person " + sPersonName + " -> nPersons = " + this.nPersons + " ["
							+ this.personNames.size() + "]");
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Reads the names & image filenames of people from a text file, and loads
	 * all those images listed.
	 * 
	 * @param filename
	 *            the training file name
	 * @return the face image array
	 */
	private IplImage[] loadFaceImgArray(final String filename) {
		IplImage[] faceImgArr;
		BufferedReader imgListFile;
		String imgFilename;
		int iFace = 0;
		int nFaces = 0;
		int i;
		try {
			// open the input file
			imgListFile = new BufferedReader(new FileReader(filename));

			// count the number of faces
			while (true) {
				final String line = imgListFile.readLine();
				if (line == null || line.isEmpty()) {
					break;
				}
				nFaces++;
			}
			LOGGER.info("nFaces: " + nFaces);
			imgListFile.close();

			imgListFile = new BufferedReader(new FileReader(filename));

			// allocate the face-image array and person number matrix
			faceImgArr = new IplImage[nFaces];
			this.personNumTruthMat = cvCreateMat(1, // rows
					nFaces, // cols
					CV_32SC1); // type, 32-bit unsigned, one channel

			// initialize the person number matrix - for ease of debugging
			for (int j1 = 0; j1 < nFaces; j1++) {
				this.personNumTruthMat.put(0, j1, 0);
			}

			this.personNames.clear(); // Make sure it starts as empty.
			this.nPersons = 0;

			// store the face images in an array
			for (iFace = 0; iFace < nFaces; iFace++) {
				String personName;
				String sPersonName;
				int personNumber;

				// read person number (beginning with 1), their name and the
				// image filename.
				final String line = imgListFile.readLine();
				if (line.isEmpty()) {
					break;
				}
				final String[] tokens = line.split(" ");
				personNumber = Integer.parseInt(tokens[0]);
				personName = tokens[1];
				imgFilename = tokens[2];
				this.peopleMap.put(personNumber, personName);
				sPersonName = personName;
				LOGGER.info("Got " + iFace + " " + personNumber + " " + personName + " " + imgFilename);

				// Check if a new person is being loaded.
				if (personNumber > this.nPersons) {
					// Allocate memory for the extra person (or possibly
					// multiple), using this new person's name.
					this.personNames.add(sPersonName);
					this.nPersons = personNumber;
					LOGGER.info("Got new person " + sPersonName + " -> nPersons = " + this.nPersons + " ["
							+ this.personNames.size() + "]");
				}

				// Keep the data
				this.personNumTruthMat.put(0, // i
						iFace, // j
						personNumber); // v

				// load the face image
				faceImgArr[iFace] = cvLoadImage(imgFilename, // filename
						CV_LOAD_IMAGE_GRAYSCALE); // isColor

				if (faceImgArr[iFace] == null) {
					imgListFile.close();
					throw new RuntimeException("Can't load image from " + imgFilename);

				}
			}

			imgListFile.close();

		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		LOGGER.info("Data loaded from '" + filename + "': (" + nFaces + " images of " + this.nPersons + " people).");
		final StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("People: ");
		if (this.nPersons > 0) {
			stringBuilder.append("<").append(this.personNames.get(0)).append(">");
		}
		for (i = 1; i < this.nPersons && i < this.personNames.size(); i++) {
			stringBuilder.append(", <").append(this.personNames.get(i)).append(">");
		}
		LOGGER.info(stringBuilder.toString());

		return faceImgArr;
	}

	/**
	 * Does the Principal Component Analysis, finding the average image and the
	 * eigenfaces that represent any image in the given dataset.
	 */
	private void doPCA() {
		int i;
		CvTermCriteria calcLimit;
		CvSize faceImgSize = new CvSize();

		// set the number of eigenvalues to use
		this.nEigens = this.nTrainFaces - 1;

		LOGGER.info("allocating images for principal component analysis, using " + this.nEigens
				+ (this.nEigens == 1 ? " eigenvalue" : " eigenvalues"));

		// allocate the eigenvector images
		faceImgSize.width(this.trainingFaceImgArr[0].width());
		faceImgSize.height(this.trainingFaceImgArr[0].height());
		this.eigenVectArr = new IplImage[this.nEigens];
		for (i = 0; i < this.nEigens; i++) {
			this.eigenVectArr[i] = cvCreateImage(faceImgSize, // size
					IPL_DEPTH_32F, // depth
					1); // channels
		}

		// allocate the eigenvalue array
		this.eigenValMat = cvCreateMat(1, // rows
				this.nEigens, // cols
				CV_32FC1); // type, 32-bit float, 1 channel

		// allocate the averaged image
		this.pAvgTrainImg = cvCreateImage(faceImgSize, // size
				IPL_DEPTH_32F, // depth
				1); // channels

		// set the PCA termination criterion
		calcLimit = cvTermCriteria(CV_TERMCRIT_ITER, // type
				this.nEigens, // max_iter
				1); // epsilon

		LOGGER.info("computing average image, eigenvalues and eigenvectors");
		// compute average image, eigenvalues, and eigenvectors
		cvCalcEigenObjects(this.nTrainFaces, // nObjects
				this.trainingFaceImgArr, // input
				this.eigenVectArr, // output
				CV_EIGOBJ_NO_CALLBACK, // ioFlags
				0, // ioBufSize
				null, // userData
				calcLimit, this.pAvgTrainImg, // avg
				this.eigenValMat.data_fl()); // eigVals

		LOGGER.info("normalizing the eigenvectors");
		cvNormalize(this.eigenValMat, // src (CvArr)
				this.eigenValMat, // dst (CvArr)
				1, // a
				0, // b
				CV_L1, // norm_type
				null); // mask
	}

	/** Stores the training data to the file 'data/facedata.xml'. */
	private void storeTrainingData() {
		CvFileStorage fileStorage;
		int i;

		LOGGER.info("writing facedata.xml");

		// create a file-storage interface
		fileStorage = cvOpenFileStorage(this.albumPath + "facedata.xml", // filename
				null, // memstorage
				CV_STORAGE_WRITE, // flags
				null); // encoding

		// Store the person names. Added by Shervin.
		cvWriteInt(fileStorage, // fs
				"nPersons", // name
				this.nPersons); // value

		for (i = 0; i < this.nPersons; i++) {
			String varname = "personName_" + (i + 1);
			cvWriteString(fileStorage, // fs
					varname, // name
					this.personNames.get(i), // string
					0); // quote
		}

		// store all the data
		cvWriteInt(fileStorage, // fs
				"nEigens", // name
				this.nEigens); // value

		cvWriteInt(fileStorage, // fs
				"nTrainFaces", // name
				this.nTrainFaces); // value

		cvWrite(fileStorage, // fs
				"trainPersonNumMat", // name
				this.personNumTruthMat); // value

		cvWrite(fileStorage, // fs
				"eigenValMat", // name
				this.eigenValMat); // value

		cvWrite(fileStorage, // fs
				"projectedTrainFaceMat", // name
				this.projectedTrainFaceMat);

		cvWrite(fileStorage, // fs
				"avgTrainImg", // name
				this.pAvgTrainImg); // value

		for (i = 0; i < this.nEigens; i++) {
			String varname = "eigenVect_" + i;
			cvWrite(fileStorage, // fs
					varname, // name
					this.eigenVectArr[i]); // value
		}

		// release the file-storage interface
		cvReleaseFileStorage(fileStorage);
	}

	/**
	 * Opens the training data from the file 'facedata.xml'.
	 * 
	 * @return the person numbers during training, or null if not successful
	 */
	private CvMat loadTrainingData() {
		LOGGER.info("loading training data");
		CvMat pTrainPersonNumMat = null; // the person numbers during training
		CvFileStorage fileStorage;
		int i;

		// create a file-storage interface
		fileStorage = cvOpenFileStorage(this.albumPath + "facedata.xml", // filename
				null, // memstorage
				CV_STORAGE_READ, // flags
				null); // encoding
		if (fileStorage == null) {
			LOGGER.severe("Can't open training database file 'facedata.xml'.");
			return null;
		}

		// Load the person names.
		this.personNames.clear(); // Make sure it starts as empty.
		this.nPersons = cvReadIntByName(fileStorage, // fs
				null, // map
				"nPersons", // name
				0); // default_value
		if (this.nPersons == 0) {
			LOGGER.severe("No people found in the training database 'facedata.xml'.");
			return null;
		} else {
			LOGGER.info(this.nPersons + " persons read from the training database");
		}

		// Load each person's name.
		for (i = 0; i < this.nPersons; i++) {
			String sPersonName;
			String varname = "personName_" + (i + 1);
			sPersonName = cvReadStringByName(fileStorage, // fs
					null, // map
					varname, "");
			this.personNames.add(sPersonName);
		}
		LOGGER.info("person names: " + this.personNames);

		// Load the data
		this.nEigens = cvReadIntByName(fileStorage, // fs
				null, // map
				"nEigens", 0); // default_value
		this.nTrainFaces = cvReadIntByName(fileStorage, null, // map
				"nTrainFaces", 0); // default_value
		Pointer pointer = cvReadByName(fileStorage, // fs
				null, // map
				"trainPersonNumMat"); // name
		pTrainPersonNumMat = new CvMat(pointer);

		pointer = cvReadByName(fileStorage, // fs
				null, // map
				"eigenValMat"); // name
		this.eigenValMat = new CvMat(pointer);

		pointer = cvReadByName(fileStorage, // fs
				null, // map
				"projectedTrainFaceMat"); // name
		this.projectedTrainFaceMat = new CvMat(pointer);

		pointer = cvReadByName(fileStorage, null, // map
				"avgTrainImg");
		this.pAvgTrainImg = new IplImage(pointer);

		this.eigenVectArr = new IplImage[this.nTrainFaces];
		for (i = 0; i <= this.nEigens; i++) {
			String varname = "eigenVect_" + i;
			pointer = cvReadByName(fileStorage, null, // map
					varname);
			this.eigenVectArr[i] = new IplImage(pointer);
		}

		// release the file-storage interface
		cvReleaseFileStorage(fileStorage);

		LOGGER.info("Training data loaded (" + this.nTrainFaces + " training images of " + this.nPersons + " people)");
		final StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("People: ");
		if (this.nPersons > 0) {
			stringBuilder.append("<").append(this.personNames.get(0)).append(">");
		}
		for (i = 1; i < this.nPersons; i++) {
			stringBuilder.append(", <").append(this.personNames.get(i)).append(">");
		}
		LOGGER.info(stringBuilder.toString());
		return pTrainPersonNumMat;
	}

	/** Saves all the eigenvectors as images, so that they can be checked. */
	private void storeEigenfaceImages() {
		// Store the average image to a file
		LOGGER.info("Saving the image of the average face as 'out_averageImage.bmp'");
		cvSaveImage(this.albumPath + "out_averageImage.bmp", this.pAvgTrainImg);

		// Create a large image made of many eigenface images.
		// Must also convert each eigenface image to a normal 8-bit UCHAR image
		// instead of a 32-bit float image.
		LOGGER.info("Saving the " + this.nEigens + " eigenvector images as 'out_eigenfaces.bmp'");

		if (this.nEigens > 0) {
			// Put all the eigenfaces next to each other.
			int COLUMNS = 8; // Put upto 8 images on a row.
			int nCols = Math.min(this.nEigens, COLUMNS);
			int nRows = 1 + (this.nEigens / COLUMNS); // Put the rest on new
														// rows.
			int w = this.eigenVectArr[0].width();
			int h = this.eigenVectArr[0].height();
			CvSize size = cvSize(nCols * w, nRows * h);
			final IplImage bigImg = cvCreateImage(size, IPL_DEPTH_8U, // depth,
																		// 8-bit
																		// Greyscale
																		// UCHAR
																		// image
					1); // channels
			for (int i = 0; i < this.nEigens; i++) {
				// Get the eigenface image.
				IplImage byteImg = this.convertFloatImageToUcharImage(this.eigenVectArr[i]);
				// Paste it into the correct position.
				int x = w * (i % COLUMNS);
				int y = h * (i / COLUMNS);
				CvRect ROI = cvRect(x, y, w, h);
				cvSetImageROI(bigImg, // image
						ROI); // rect
				cvCopy(byteImg, // src
						bigImg, // dst
						null); // mask
				cvResetImageROI(bigImg);
				cvReleaseImage(byteImg);
			}
			cvSaveImage(this.albumPath + "out_eigenfaces.bmp", // filename
					bigImg); // image
			cvReleaseImage(bigImg);
		}
	}

	/**
	 * Converts the given float image to an unsigned character image.
	 * 
	 * @param srcImg
	 *            the given float image
	 * @return the unsigned character image
	 */
	private IplImage convertFloatImageToUcharImage(IplImage srcImg) {
		IplImage dstImg;
		if ((srcImg != null) && (srcImg.width() > 0 && srcImg.height() > 0)) {
			// Spread the 32bit floating point pixels to fit within 8bit pixel
			// range.
			CvPoint minloc = new CvPoint();
			CvPoint maxloc = new CvPoint();
			double[] minVal = new double[1];
			double[] maxVal = new double[1];
			cvMinMaxLoc(srcImg, minVal, maxVal, minloc, maxloc, null);
			// Deal with NaN and extreme values, since the DFT seems to give
			// some NaN results.
			if (minVal[0] < -1e30) {
				minVal[0] = -1e30;
			}
			if (maxVal[0] > 1e30) {
				maxVal[0] = 1e30;
			}
			if (maxVal[0] - minVal[0] == 0.0f) {
				maxVal[0] = minVal[0] + 0.001; // remove potential divide by
												// zero errors.
			} // Convert the format
			dstImg = cvCreateImage(cvSize(srcImg.width(), srcImg.height()), 8, 1);
			cvConvertScale(srcImg, dstImg, 255.0 / (maxVal[0] - minVal[0]),
					-minVal[0] * 255.0 / (maxVal[0] - minVal[0]));
			return dstImg;
		}
		return null;
	}

	/**
	 * Find the most likely person based on a detection. Returns the index, and
	 * stores the confidence value into pConfidence.
	 * 
	 * @param projectedTestFace
	 *            the projected test face
	 * @param pConfidencePointer
	 *            a pointer containing the confidence value
	 * @param iTestFace
	 *            the test face index
	 * @return the index
	 */
	private int findNearestNeighbor(float projectedTestFace[], FloatPointer pConfidencePointer) {
		double leastDistSq = Double.MAX_VALUE;
		int i = 0;
		int iTrain = 0;
		int iNearest = 0;

		LOGGER.info("................");
		LOGGER.info("find nearest neighbor from " + this.nTrainFaces + " training faces");
		for (iTrain = 0; iTrain < this.nTrainFaces; iTrain++) {
			LOGGER.info("considering training face " + (iTrain + 1));
			double distSq = 0;

			for (i = 0; i < this.nEigens; i++) {
				LOGGER.info("  projected test face distance from eigenface " + (i + 1) + " is " + projectedTestFace[i]);

				float projectedTrainFaceDistance = (float) this.projectedTrainFaceMat.get(iTrain, i);
				float d_i = projectedTestFace[i] - projectedTrainFaceDistance;
				distSq += d_i * d_i; // / eigenValMat.data_fl().get(i); //
										// Mahalanobis distance (might give
										// better
										// results than Eucalidean distance)
				// if (iTrain < 5) {
				// LOGGER.info(" ** projected training face " + (iTrain + 1)
				// + " distance from eigenface " + (i + 1) + " is " +
				// projectedTrainFaceDistance);
				// LOGGER.info(" distance between them " + d_i);
				// LOGGER.info(" distance squared " + distSq);
				// }
			}

			if (distSq < leastDistSq) {
				leastDistSq = distSq;
				iNearest = iTrain;
				LOGGER.info("  training face " + (iTrain + 1) + " is the new best match, least squared distance: "
						+ leastDistSq);
			}
		}

		// Return the confidence level based on the Euclidean distance,
		// so that similar images should give a confidence between 0.5 to 1.0,
		// and very different images should give a confidence between 0.0 to
		// 0.5.
		float pConfidence = (float) (1.0f - Math.sqrt(leastDistSq / (this.nTrainFaces * this.nEigens)) / 255.0f);
		pConfidencePointer.put(pConfidence);

		LOGGER.info("training face " + (iNearest + 1) + " is the final best match, confidence " + pConfidence);
		return iNearest;
	}

//	/**
//	 * Returns a string representation of the given float array.
//	 * 
//	 * @param floatArray
//	 *            the given float array
//	 * @return a string representation of the given float array
//	 */
//	private String floatArrayToString(final float[] floatArray) {
//		final StringBuilder stringBuilder = new StringBuilder();
//		boolean isFirst = true;
//		stringBuilder.append('[');
//		for (int i = 0; i < floatArray.length; i++) {
//			if (isFirst) {
//				isFirst = false;
//			} else {
//				stringBuilder.append(", ");
//			}
//			stringBuilder.append(floatArray[i]);
//		}
//		stringBuilder.append(']');
//
//		return stringBuilder.toString();
//	}

	/**
	 * Returns a string representation of the given float pointer.
	 * 
	 * @param floatPointer
	 *            the given float pointer
	 * @return a string representation of the given float pointer
	 */
	private String floatPointerToString(final FloatPointer floatPointer) {
		final StringBuilder stringBuilder = new StringBuilder();
		boolean isFirst = true;
		stringBuilder.append('[');
		for (int i = 0; i < floatPointer.capacity(); i++) {
			if (isFirst) {
				isFirst = false;
			} else {
				stringBuilder.append(", ");
			}
			stringBuilder.append(floatPointer.get(i));
		}
		stringBuilder.append(']');

		return stringBuilder.toString();
	}

	/**
	 * Returns a string representation of the given one-channel CvMat object.
	 * 
	 * @param cvMat
	 *            the given CvMat object
	 * @return a string representation of the given CvMat object
	 */
	public String oneChannelCvMatToString(final CvMat cvMat) {
		// Preconditions
		if (cvMat.channels() != 1) {
			throw new RuntimeException("illegal argument - CvMat must have one channel");
		}

		final int type = cvMat.type();
		StringBuilder s = new StringBuilder("[ ");
		for (int i = 0; i < cvMat.rows(); i++) {
			for (int j = 0; j < cvMat.cols(); j++) {
				if (type == CV_32FC1 || type == CV_32SC1) {
					s.append(cvMat.get(i, j));
				} else {
					throw new RuntimeException(
							"illegal argument - CvMat must have one channel and type of float or signed integer");
				}
				if (j < cvMat.cols() - 1) {
					s.append(", ");
				}
			}
			if (i < cvMat.rows() - 1) {
				s.append("\n  ");
			}
		}
		s.append(" ]");
		return s.toString();
	}

	// Method to query the contact database and retrieve information such as
	// phone number and email
	/**
	 * Queries the contact database by display name, and returns the contact ID.
	 * 
	 * @author Matthew Conrad
	 * @param cName
	 *            -The contact name.
	 * @return id-The contact ID received from the database.
	 */
	public String queryDB(String cName) {
		String id = "";

		// Gets content from the contact database
		ContentResolver cr = this.getContentResolver();
		Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

		// Make sure there are contacts
		if (cur.getCount() > 0) {
			// Iterate through contacts
			while (cur.moveToNext()) {
				// Name of the contact in the database
				String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				// Check to see if the name you are looking for
				// has been found
				if (name.toLowerCase().replaceAll("\\s", "").equals(cName.toLowerCase().replaceAll("\\s", ""))) {
					// Id of the contact in the database
					id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));

				}

			}
		}
		// Close the cursor
		cur.close();

		return id;
	}
}