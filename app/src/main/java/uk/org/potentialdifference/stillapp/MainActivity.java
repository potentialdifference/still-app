package uk.org.potentialdifference.stillapp;

import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    Camera mCamera;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    String mCurrentPhotoPath;
    PictureCallback jpegCallback2 = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            mCamera.stopPreview();
            mCamera.release();
            String name = String.format("front_%d.jpg", System.currentTimeMillis());
            sendToServer(name, data);
            dispatchTakePictureIntent();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCamera = Camera.open(1);

        Button button = (Button) findViewById(R.id.bPhoto);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // Take a picture
                mCamera.startPreview();
                mCamera.takePicture(null, null, jpegCallback2);
            }
        });
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void sendToServer(String name, byte[] data) {
        Log.i("PhotoActivity", "will send to server " + name);
        Intent myIntent = new Intent(this, ImageUploadService.class);
        myIntent.putExtra(ImageUploadService.EXTRA_IMAGEDATA ,data);
        UserIdentifier uid = new UserIdentifier(this.getBaseContext());
        myIntent.putExtra(ImageUploadService.EXTRA_IMAGEDIR, uid.getIdentifier());
        myIntent.putExtra(ImageUploadService.EXTRA_IMAGENAME, name);
        startService(myIntent);
    }
}
