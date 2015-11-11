package uk.org.potentialdifference.stillapp.preshow;

import android.content.Intent;
import android.os.Bundle;


import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import uk.org.potentialdifference.stillapp.R;

public class PreshowImages extends AppCompatActivity {

    private static final String TAG = "PreshowImages";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preshow_images);



        Button button1 = (Button) findViewById(R.id.preshow_button_1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPreshowImageViewer(0);
            }
        });
        Button button2 = (Button) findViewById(R.id.preshow_button_2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPreshowImageViewer(1);
            }
        });
        Button button3 = (Button) findViewById(R.id.preshow_button_3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPreshowImageViewer(2);
            }
        });
        Button button4 = (Button) findViewById(R.id.preshow_button_4);
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPreshowImageViewer(3);
            }
        });
        Button button5 = (Button) findViewById(R.id.preshow_button_5);
        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPreshowImageViewer(4);
            }
        });
        Button back  = (Button) findViewById(R.id.preshow_button_back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });



    }

    private void launchPreshowImageViewer(int imageNumber){
        Intent imageViewIntent = new Intent(this, PreshowImageViewer.class);

                imageViewIntent.putExtra(PreshowImageViewer.IMAGE_NUMBER_EXTRA, imageNumber);
                startActivity(imageViewIntent);

        }


}
