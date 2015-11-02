package uk.org.potentialdifference.stillapp;


import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

public class SoundPlayerActivity extends AppCompatActivity {


    private static MediaPlayer mediaPlayer;



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
        if(mediaPlayer == null){
            initializeMP();
        }

        //when audio playback is complete, we want to navigate back:
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {

                finish();
            }
        });
        mediaPlayer.start();
    }






    public void initializeMP()
    {
        mediaPlayer = MediaPlayer.create(this, R.raw.russell_talks_about_vivian);

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
}
