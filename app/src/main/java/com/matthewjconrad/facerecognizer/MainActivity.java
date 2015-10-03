package com.matthewjconrad.facerecognizer;

import java.io.File;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity implements
		View.OnClickListener {

	private Button recognizeFace, addFace;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		this.recognizeFace = (Button) this.findViewById(R.id.btnRecognize);
		this.addFace = (Button) this.findViewById(R.id.btnAddFace);

		// Set the on click listeners
		this.recognizeFace.setOnClickListener(this);
		this.addFace.setOnClickListener(this);
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnRecognize:
			// start camera and facial recognition
			if (this.haveFacesToTrain()) {
				Intent startRecognition = new Intent(
						"com.matthewjconrad.facerecognizer.FACERECOGNITION");
				this.startActivity(startRecognition);
			} else {
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setTitle("No Faces to Train From");
				alert.setMessage("Please add faces to the database before attempting to recognize!");

				// if "OK" button pressed, save, rename photo, and
				// add to gallery, then call continue dialog
				alert.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {

							}
						});

				// draw the alert dialog
				alert.show();

			}
			break;
		case R.id.btnAddFace:
			// start activity to add face to database
			Intent addFace = new Intent("com.matthewjconrad.facerecognizer.ADDFACE");
			MainActivity.this.startActivity(addFace);
			break;
		}

	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void onDestroy() {
		super.onDestroy();
	}

	/**
	 * Checks to see if the media folder for the application and the list of
	 * faces to train from both exist.
	 * 
	 * @return whether or not a list of faces to train exists
	 */
	private boolean haveFacesToTrain() {
		boolean facesToTrain = false;
		File faceDirectory = new File("/storage/emulated/0/DCIM/FaceRecognizer");
		if (faceDirectory.exists()) {
			File trainingList = new File(
					"/storage/emulated/0/DCIM/FaceRecognizer/TrainingList.txt");
			if (trainingList.exists()) {
				facesToTrain = true;
			}
		}
		return facesToTrain;
	}

}