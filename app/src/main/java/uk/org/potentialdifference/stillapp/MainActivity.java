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
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements ImageUploadDelegate, PictureCallback {

    final String TAG = "MainActivity";
    Camera mCamera;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    Uri photoUri;
    Activity mActivity;
    PictureCallback cb = this;

    boolean isTakingPhoto = false;

    public void onPictureTaken(byte[] data, Camera camera) {
        Log.d(TAG, "onPictureTaken");
        isTakingPhoto = false;
        try {

            // We call get to make our asyncTask synchronous

            new ImageUploadTask(this, this).execute(new UploadJob(data, "front")).get();
        } catch (Exception e) {
            Log.e(TAG, "ImageUploadTask interrupted");
        }
    }

    public void imageUploadComplete() {
        dispatchTakePictureIntent();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        mActivity = this;
        Button button = (Button) findViewById(R.id.bPhoto);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.d(TAG, "onClick");
                // Take a picture
                // If this is a double-click, we might have already released the camera
                if (isTakingPhoto == false) {
                    mCamera.takePicture(null, null, cb);
                    isTakingPhoto = true;
                }
            }
        });

        // We might have been re-created after a photo has been taken
        // - what is its location? Hard-code for now...
        try {
            this.photoUri = createImageURI();
        } catch (IOException e) {
            Toast.makeText(this, "Couldn't create picture file", Toast.LENGTH_SHORT);
        }

    }

    protected void onStart() {
        super.onStart();

        isTakingPhoto = false;




        Log.d(TAG, "onStart");
    }


    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop");
    }


    protected void onResume() {
        super.onResume();
        if (mCamera == null) {
            mCamera = Camera.open(1);
            mCamera.startPreview();
        }
        Log.d(TAG, "onResume");
    }

    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        if (mCamera != null ) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    private Uri createImageURI() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        // String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        // TODO fixme
        File image = new File("/mnt/sdcard/Pictures/still-app-photo-123456789");
        return Uri.fromFile(image);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            if (photoUri != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        Log.d(TAG, "onActivityResult called");
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                if (photoUri != null) {
                    try {
                        Log.d(TAG, "onActivityResult OK");
                        Log.d(TAG, "photoUri:" + photoUri);
                        Bitmap bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), photoUri);
                        if (bm != null) {
                            byte[] bytes = getBytesFromBitmap(bm);
                            Log.d(TAG, "Uploading bytes...");
                            try {
                                // We call get to make our asyncTask synchronous
                                new ImageUploadTask(this, this).execute(new UploadJob(bytes, "rear")).get();
                            } catch (Exception e) {
                                Log.e(TAG, "ImageUploadTask interrupted");
                            }
                            // new PhotoUploader().uploadBytes(mActivity, "rear", bytes);
                        } else {
                            Log.d(TAG, "Couldn't create bitmap from photo");
                            Toast.makeText(this, "Couldn't create bitmap from photo", Toast.LENGTH_SHORT).show();
                        }
                        Log.d(TAG, "onActivityResult do we get here?");
                    } catch (FileNotFoundException e) {
                        Log.d(TAG, "FileNotFound exception in onActivityResult");
                        Log.d(TAG, "photoUri:" + photoUri);
                    } catch (IOException e) {
                        Log.d(TAG, "IOException exception in onActivityResult");
                        Log.d(TAG, "photoUri:" + photoUri);
                        Log.d(TAG, e.toString());
                    }
                }
            }
        }
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
                // new PhotoUploader().uploadBytes(this.getBaseContext(), String.format("user-photo-%d", photoCount), data);
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

    public void launchSoundPlayer(View view) {
        Intent intent = new Intent(this, SoundPlayerActivity.class);
        startActivity(intent);
    }
}
