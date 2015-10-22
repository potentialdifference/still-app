package uk.org.potentialdifference.stillapp;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Created by henry on 16/10/15.
 */
public class ImageUploadService extends IntentService {

    private static final String AWS_ACCESS_KEY    = "AKIAJAHUGAVEKK6T4CEA";
    private static final String AWS_ACCESS_SECRET = "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";

    public static final String EXTRA_IMAGEDATA = "uk.org.potentialdifference.EXTRA_IMAGEDATA";
    public static final String EXTRA_IMAGENAME = "uk.org.potentialdifference.EXTRA_IMAGENAME";

    public ImageUploadService () {
        super("ImageUploadService");
    }

    protected void onHandleIntent(Intent intent) {
        AmazonS3Client s3Client;
        byte[] imageData;
        String imageName;
        Bundle extras;

        extras = intent.getExtras();
        imageData = extras.getByteArray(EXTRA_IMAGEDATA);
        imageName = extras.getString(EXTRA_IMAGENAME);

        s3Client = new AmazonS3Client(new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_ACCESS_SECRET));
        s3Client.setRegion(Region.getRegion(Regions.EU_WEST_1));

        InputStream is = new ByteArrayInputStream(imageData);
        PutObjectRequest or = new PutObjectRequest("still-app", imageName, is, null);
        Log.i("ImageUploadService", "Uploading " + imageName);
        s3Client.putObject(or);


    }
}
