package uk.org.potentialdifference.stillapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PhotoUploader {

    private String TAG = "PhotoUploader";

    public void uploadBytes(Context context, String tag, byte[] data) {
        Log.d(TAG, "uploadBytes called");
        String name = String.format("%s_%d.jpg", tag, System.currentTimeMillis());
        Intent myIntent = new Intent(context, ImageUploadService.class);
        myIntent.putExtra(ImageUploadService.EXTRA_IMAGEDATA, data);
        UserIdentifier uid = new UserIdentifier(context);
        myIntent.putExtra(ImageUploadService.EXTRA_IMAGEDIR, uid.getIdentifier());
        myIntent.putExtra(ImageUploadService.EXTRA_IMAGENAME, name);
        Log.d(TAG, "starting ImageUploadService...");
        context.startService(myIntent);
    }
}
