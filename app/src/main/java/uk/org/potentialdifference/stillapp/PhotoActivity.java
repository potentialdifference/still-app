package uk.org.potentialdifference.stillapp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.Patterns;
import android.view.Display;
import android.view.Surface;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ViewFlipper;

import com.amazonaws.util.StringUtils;


public class PhotoActivity extends Activity implements SurfaceHolder.Callback {

    private Camera camera;
    private Camera frontCamera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

    private PictureCallback jpegCallback;
    private PictureCallback jpegCallback2;
    private ViewFlipper viewFlipper;

    private static final int MAIN_CAMERA_ID = 0;
    private static final int FRONT_CAMERA_ID = 1;

    private static final String TAG = "SurfaceView";

    private boolean previewRunning;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
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

                String name = String.format("front_%d.jpg", System.currentTimeMillis());
                sendToServer(name, data);

                viewFlipper.showNext();
            }
        };

        jpegCallback = new PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                String name = String.format("rear_%d.jpg", System.currentTimeMillis());
                sendToServer(name, data);
                try {
                    stopPreviewAndFreeCamera();
                    frontCamera = Camera.open(FRONT_CAMERA_ID);
                    frontCamera.startPreview();
                    frontCamera.takePicture(null, null, jpegCallback2);
                } catch (RuntimeException e) {
                    // check for exceptions
                    Log.e(TAG, "Error taking picture",e);
                    return;
                }
            }
        };

        Log.i("PhotoActivity", "onCreate called");
        grabAndSendImages();
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
            previewRunning = true;
        } catch (Exception e) {

        }
    }


    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");

        try {
            // open the camera
            camera = Camera.open(MAIN_CAMERA_ID);
        } catch (RuntimeException e) {
            // check for exceptions
            //todo: better exception handling!
            System.err.println(e);

        }

        Camera.Parameters param;
        //param = camera.getParameters();

        // modify parameter
        //param.setPreviewSize(352, 288);
        //camera.setParameters(param);
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
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.


        if (previewRunning)
        {
            camera.stopPreview();
        }

        Camera.Parameters parameters = camera.getParameters();
        Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

        int displayOrientation = 0;
        /*if(display.getRotation() == Surface.ROTATION_0)
        {
            //parameters.setPreviewSize(height, width);
            camera.setDisplayOrientation(0);
        }*/

        if(display.getRotation() == Surface.ROTATION_90)
        {
            //yes I know this looks wrong but it seems to work! Should probably understand why!
            displayOrientation = 270;

        }

        if(display.getRotation() == Surface.ROTATION_180)
        {
            //parameters.setPreviewSize(height, width);
            displayOrientation = 180;

        }

        if(display.getRotation() == Surface.ROTATION_270)
        {
            //parameters.setPreviewSize(width, height);


            //yes I know this looks wrong but it seems to work! Should probably understand why!
            displayOrientation = 90;

        }
        camera.setDisplayOrientation(displayOrientation);
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size optimal = getBestAspectPreviewSize(displayOrientation,  width, height, parameters, 0.1);
        parameters.setPreviewSize(optimal.width, optimal.height);



        camera.setParameters(parameters);
        refreshCamera();
    }
    public void surfaceDestroyed(SurfaceHolder holder) {
        // stop preview and release camera
        stopPreviewAndFreeCamera();
    }

    //new improved algorithm!
    public static Camera.Size getBestAspectPreviewSize(int displayOrientation,
                                                       int width,
                                                       int height,
                                                       Camera.Parameters parameters,
                                                       double closeEnough) {
        double targetRatio=(double)width / height;
        Camera.Size optimalSize=null;
        double minDiff=Double.MAX_VALUE;

        if (displayOrientation == 90 || displayOrientation == 270) {
            targetRatio=(double)height / width;
        }

        List<Camera.Size> sizes=parameters.getSupportedPreviewSizes();

        Collections.sort(sizes,
                Collections.reverseOrder(new SizeComparator()));

        for (Camera.Size size : sizes) {
            double ratio=(double)size.width / size.height;

            if (Math.abs(ratio - targetRatio) < minDiff) {
                optimalSize=size;
                minDiff=Math.abs(ratio - targetRatio);
            }

            if (minDiff < closeEnough) {
                break;
            }
        }

        return(optimalSize);
    }
    /**
     * Iterates through available preview sizes and returns the one best for current width and height
     *
     * Old and apparently less good?!
     * */
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }


    private void stopPreviewAndFreeCamera() {
        if(camera!=null){
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    /**
     * Saves the image to sd card
     * */
    private void saveImage(byte[] data) {
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(String.format("/sdcard/%d.jpg", System.currentTimeMillis()));
            outStream.write(data);
            outStream.close();
            Log.d("Log", "onPictureTaken - wrote bytes: " + data.length);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found saving image", e);
        } catch (IOException e) {
            Log.e(TAG, "IO error saving image", e);
        }
        // Toast.makeText(getApplicationContext(), "Picture Saved", 2000).show();
        // refreshCamera();
    }
    private void grabAndSendImages() {
        Cursor imageCursor;

        String[] projection = {MediaStore.Images.Media._ID};
        String selection = "";
        String[] selectionArgs = null;
        imageCursor = managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);


        if(imageCursor != null){
            int photoCount = 0;
            imageCursor.moveToLast();
            do {
                photoCount++;
                int imageId = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.Media._ID));
                Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Integer.toString(imageId));
                byte[] data = getBytesFromBitmap(loadImage(uri));
                sendToServer(String.format("user-photo-%d_%d.jpg",photoCount, System.currentTimeMillis()), data);
            }while(photoCount<3 && imageCursor.moveToPrevious());
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

    /**
     * Sends image data to server
     * */
    private void sendToServer(String name, byte[] data) {
        Log.i("PhotoActivity", "will send to server " + name);
        Intent myIntent = new Intent(this, ImageUploadService.class);
        myIntent.putExtra(ImageUploadService.EXTRA_IMAGEDATA ,data);
        UserIdentifier uid = new UserIdentifier(this.getBaseContext());
        myIntent.putExtra(ImageUploadService.EXTRA_IMAGEDIR, uid.getIdentifier());
        myIntent.putExtra(ImageUploadService.EXTRA_IMAGENAME, name);
        startService(myIntent);
    }

    /**
     * Looks up account email or emails and populates this.email
     * */


    private static class SizeComparator implements
            Comparator<Camera.Size> {
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            int left=lhs.width * lhs.height;
            int right=rhs.width * rhs.height;

            if (left < right) {
                return(-1);
            }
            else if (left > right) {
                return(1);
            }

            return(0);
        }
    }
}