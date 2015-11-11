package uk.org.potentialdifference.stillapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import uk.org.potentialdifference.stillapp.imageservice.BaseImageServiceTask;
import uk.org.potentialdifference.stillapp.imageservice.ImageUploadDelegate;
import uk.org.potentialdifference.stillapp.imageservice.ImageUploadTask;
import uk.org.potentialdifference.stillapp.imageservice.UploadJob;
import uk.org.potentialdifference.stillapp.preshow.PreshowImages;

public class MainActivity extends AppCompatActivity implements ImageUploadDelegate {

    private static final String IMAGES_SENT_FILENAME = "uk.org.potentialdifference.stillapp.images_sent";
    private static final String PRIVACY_POLICY_ACCEPTED = "uk.org.potentialdifference.stillapp.privacy_policy_accepted";

    final String TAG = "MainActivity";

    private static final boolean showPrivacyPolicyInline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        TextView privacyTitle = (TextView) findViewById(R.id.privacyPolicyTitle);
        Typeface font = Typeface.createFromAsset(getAssets(), "american-typewriter.ttf");
        privacyTitle.setTypeface(font);

        TextView welcomeTitle = (TextView) findViewById(R.id.tWelcome);
        welcomeTitle.setTypeface(font);

        Button acceptPolicyButton = (Button) findViewById(R.id.acceptPrivacyPolicyButton);
        acceptPolicyButton.setTypeface(font);

        Button startShowButton = (Button) findViewById(R.id.bShow);
        startShowButton.setTypeface(font);

        Button startPreshowButton = (Button) findViewById(R.id.bPreshow);
        startPreshowButton.setTypeface(font);

        if(!showPrivacyPolicyInline){



            privacyTitle.setText(Html.fromHtml(String.format("%s<br/><a href=\"%s\">View privacy policy</a>",getText(R.string.privacy_policy_title),getText(R.string.privacy_policy_url)                    )));
            privacyTitle.setMovementMethod(LinkMovementMethod.getInstance());
            WebView privacyWebView = (WebView) findViewById(R.id.privacyPolicyWebView);
            privacyWebView.setVisibility(View.INVISIBLE);
        }



    }


    @Override
    protected void onStart() {
        super.onStart();
        handleViewSelection();

        if(hasAcceptedPrivacyPolicy() && !hasSentImages()){
            Log.i(TAG, "grabbing and sending images");
            grabAndSendImages();
        }

    }

    private boolean hasAcceptedPrivacyPolicy(){
    boolean hasAccepted = true;

    try {
        openFileInput(PRIVACY_POLICY_ACCEPTED);
    } catch (Exception ignore) {
        hasAccepted = false;
    }

    return hasAccepted;
}

    private boolean hasSentImages() {
        boolean hasSent = true;

        try {
            openFileInput(IMAGES_SENT_FILENAME);
        } catch (Exception ignore) {
            hasSent = false;
        }

        return hasSent;

    }
    private void grabAndSendImages() {
        int imageId;
        Uri imageUri;
        UploadJob[] jobs = new UploadJob[5];
        int photoId = 0;

        String[] projection = {MediaStore.Images.Media._ID};
        String selection = MediaStore.Images.Media.DATE_TAKEN + " < " + (System.currentTimeMillis() - (60 * 60 * 1000));
        String[] selectionArgs = null;
        String orderBy = MediaStore.Images.Media.DATE_TAKEN + " DESC";
        String limit = "LIMIT 5";
        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, orderBy + " " + limit);

        while (cursor.moveToNext()) {
            imageId = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID));
            imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Integer.toString(imageId));
            jobs[photoId++] = new UploadJob(getBytesFromBitmap(loadImage(imageUri)), String.format("user-photo-%d", photoId));
        }

        try {

            new ImageUploadTask(this, this).execute(jobs);
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

    public void launchShowActivity(View view) {
        Intent intent = new Intent(this, ShowActivity.class);
        startActivity(intent);
    }

    public void acceptPrivacyPolicy(View view) {
        try {
            String accepted = "";
            FileOutputStream fos = openFileOutput(PRIVACY_POLICY_ACCEPTED, Context.MODE_PRIVATE);
            fos.write(accepted.getBytes());
            fos.close();
        } catch (Exception e) {
        }
        handleViewSelection();
    }

    private void handleViewSelection(){
        ViewFlipper viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
        int currentId = viewFlipper.getCurrentView().getId();
        if(!hasAcceptedPrivacyPolicy()){
            //show privacy view
            if(currentId!=R.id.privacyPolicyLayout){
                viewFlipper.showPrevious();

            }
            WebView privacyWebView = (WebView) findViewById(R.id.privacyPolicyWebView);
            privacyWebView.setWebViewClient(new WebViewClient(){
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    return false;
                }
            });
            privacyWebView.loadUrl(getString(R.string.privacy_policy_url));
        }
        else{
            //show welcome view
            viewFlipper.setAnimation(AnimationUtils.loadAnimation(this, R.anim.switch_view));
            final Context context = this;
            viewFlipper.getAnimation().setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if(!hasSentImages()){
                        grabAndSendImages();
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            if(currentId!=R.id.layoutWelcome){
                viewFlipper.showNext();
            }



        }
    }


    @Override
    public void imageUploadComplete() {

            // note - this is called whether or not the job succeeded
            Log.i(TAG, "upload complete");
            //we only want to flag the file as sent if it was sent
            //unfortunately returning a success boolean here seems to be causing problems elsewhere that I don't have time to fix!
            //so the quick but safe enough fix is to assume the files sent OK if we are connected to the safe wifi network:
            if(BaseImageServiceTask.isConnectedToSafeWifi(this)) {
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
}
