package uk.org.potentialdifference.stillapp;

import android.os.AsyncTask;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Created by henry on 15/10/15.
 */

class ImageUpload  extends AsyncTask<ImageData, Void, Void> {

    private Exception exception;
    AmazonS3Client s3Client;

    protected Void doInBackground(ImageData... data) {
        try {
            ImageData imageData = data[0];
            String aws_access_key = "AKIAJAHUGAVEKK6T4CEA";
            String aws_secret_key = "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";
            s3Client = new AmazonS3Client(new BasicAWSCredentials( aws_access_key, aws_secret_key ) );
            s3Client.setRegion(Region.getRegion(Regions.EU_WEST_1));

            String key = String.format("%s_%s_%d.jpg", imageData.email, imageData.face, System.currentTimeMillis());
            InputStream is = new ByteArrayInputStream(imageData.data);
            PutObjectRequest or = new PutObjectRequest("still-app", key, is, null);
            s3Client.putObject(or);

        } catch (Exception e) {
            this.exception = e;
        }
        return null;
    }

    protected void onPostExecute() {
        // TODO: check this.exception
        // TODO: do something with the feed
    }
}
