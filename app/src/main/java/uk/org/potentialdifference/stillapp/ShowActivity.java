package uk.org.potentialdifference.stillapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_10;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

import uk.org.potentialdifference.stillapp.imageservice.ImageDownloadDelegate;
import uk.org.potentialdifference.stillapp.imageservice.ImageDownloadTask;
import uk.org.potentialdifference.stillapp.imageservice.ImageUploadDelegate;
import uk.org.potentialdifference.stillapp.imageservice.ImageUploadTask;
import uk.org.potentialdifference.stillapp.imageservice.UploadJob;

public class ShowActivity extends AppCompatActivity implements StillWebsocketDelegate, Camera.PictureCallback{

    private static String TAG = "ShowActivity";
    private WebSocketClient mWebSocketClient;
    private ImageView imageView;
    private Camera mCamera;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private Uri photoUri;
    private Activity mActivity;
    Camera.PictureCallback cb = this;

    boolean isTakingPhoto = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);
        //stop the screen from going to sleep:
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (photoUri == null && savedInstanceState != null && savedInstanceState.containsKey("photoUri")) {
            Log.d(TAG, "loading photoUri from saved state");
            photoUri = savedInstanceState.getParcelable("photoUri");
        }
        // If still null...
        if (photoUri == null) {
            Log.d(TAG, "creating new photoUri");
            photoUri = Uri.fromFile(getPublicMediaFile());
        }



        imageView = (ImageView) findViewById(R.id.imageView);

        setupWebSockets();

        mActivity = this;
        Button button = (Button) findViewById(R.id.take_photo);
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

    protected void setupWebSockets() {
        URI uri;

        try {
            uri = new URI(String.format("wss://%s:%s",getString(R.string.still_server_hostname), getString(R.string.still_server_http_port)));
        } catch (URISyntaxException e) {
            Log.d(TAG, "error creating URI");
            e.printStackTrace();
            return;
        }

        Log.d(TAG, "creating websocket client...");
        mWebSocketClient = new StillWebsocketClient(this, uri, new Draft_10(), this);
        Log.d(TAG, "created websocket client");
        mWebSocketClient.connect();

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

    @Override
    public void showImage(Bitmap image) {
        imageView.setImageBitmap(image);
    }

    @Override
    public void hideImage(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ColorDrawable cd = new ColorDrawable(Color.parseColor("#FF000000"));
                imageView.setImageDrawable(cd);
            }
        });

        }

        @Override
    public void exitShowMode() {
        //exit the activity
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    finish();
                }
            });
        }

    @Override
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

    protected void onResume() {
        super.onResume();
        if (mCamera == null) {
            mCamera = Camera.open(1);
            mCamera.startPreview();
        }
        Log.d(TAG, "onResume");
    }
    protected void onStart() {
        super.onStart();
        isTakingPhoto = false;

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
}
