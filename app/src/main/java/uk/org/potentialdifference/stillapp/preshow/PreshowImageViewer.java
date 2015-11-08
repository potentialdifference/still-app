package uk.org.potentialdifference.stillapp.preshow;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import uk.org.potentialdifference.stillapp.R;

public class PreshowImageViewer extends AppCompatActivity {
    public static final String IMAGE_NUMBER_EXTRA = "uk.org.potentialdifference.stillapp.preshowImageId";


    private ViewPager pager;
    private final static Integer[] imageResourceIds = new Integer[]{
            R.drawable.placeholder_1, R.drawable.placeholder_2, R.drawable.placeholder_3
    };
    private final static Integer[] captionResourceIds = new Integer[]{
            R.string.image_caption_1, R.string.image_caption_2, R.string.image_caption_3
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_preshow_image_viewer);

        Bundle extras = getIntent().getExtras();
        int imageNumber = extras.getInt(IMAGE_NUMBER_EXTRA);
        int captionResId = 0;
        int imageResId = 0;

        if(imageNumber>=0 && imageNumber<this.captionResourceIds.length){
            captionResId = this.captionResourceIds[imageNumber];
        }
        if(imageNumber>=0 && imageNumber<this.imageResourceIds.length){
            imageResId= this.imageResourceIds[imageNumber];
        }



        ImageView imageView = (ImageView) findViewById(R.id.preshow_image);
        imageView.setImageResource(imageResId);


        TextView txt = (TextView) findViewById(R.id.preshow_caption);
        Typeface font = Typeface.createFromAsset(getAssets(), "american-typewriter.ttf");
        txt.setTypeface(font);
        txt.setText(captionResId);

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
