package uk.org.potentialdifference.stillapp;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import uk.org.potentialdifference.stillapp.nodefs.NodeFSResponse;
import uk.org.potentialdifference.stillapp.nodefs.NodeFSService;

/**
 * Created by henry on 16/10/15.
 */
public class ImageUploadService extends IntentService {

    private static final String TAG = "IMAGE_UPLOAD_SERVICE";
    private static final String AWS_ACCESS_KEY    = "AKIAJAHUGAVEKK6T4CEA";
    private static final String AWS_ACCESS_SECRET = "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";

    private static final String BASE_SERVER_URL = "http://192.168.0.6:8080/";

    //note the "key" they are using here is pretty weak security as it is simply put as a path param. But better than nothing
    private static final String FS_KEY = "stillappkey579xtz";

    public static final String EXTRA_IMAGEDATA = "uk.org.potentialdifference.EXTRA_IMAGEDATA";
    public static final String EXTRA_IMAGEDIR = "uk.org.potentialdifference.EXTRA_IMAGEDIR";
    public static final String EXTRA_IMAGENAME = "uk.org.potentialdifference.EXTRA_IMAGENAME";

    public ImageUploadService () {
        super(ImageUploadService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent called...");

        Bundle extras = intent.getExtras();
        byte[] imageData = extras.getByteArray(EXTRA_IMAGEDATA);
        String imageDir = extras.getString(EXTRA_IMAGEDIR);
        String imageName = extras.getString(EXTRA_IMAGENAME);

        Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_SERVER_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient())
                .build();
        NodeFSService nodeFsService = retrofit.create(NodeFSService.class);

        Call<NodeFSResponse> createDirectoryCall = nodeFsService.createDirectory(FS_KEY, imageDir);
        Call<NodeFSResponse> createFileCall = nodeFsService.createFile(FS_KEY, imageDir, imageName);
        RequestBody body = RequestBody.create(MediaType.parse("image/jpeg"), imageData);
        Call<NodeFSResponse> saveCall = nodeFsService.saveFileContents(FS_KEY, imageDir, imageName, body);
        try {
            Response createDirectoryResponse = createDirectoryCall.execute().raw();
            Log.i(TAG, "response from create directory: " + createDirectoryResponse.toString());
            Response createFileResponse = createFileCall.execute().raw();
            Log.i(TAG, "response from create file: " + createFileResponse.toString());
            Response saveResponse = saveCall.execute().raw();
            Log.i(TAG, "response from save: "+saveResponse.toString());

        } catch(Exception e){
            //handle
            Log.e(TAG, "Error uploading file", e);
        }

        Log.i("ImageUploadService", "Uploading " + imageName);
        Log.i(TAG, "Stopping...");

        this.stopSelf();
    }
}
