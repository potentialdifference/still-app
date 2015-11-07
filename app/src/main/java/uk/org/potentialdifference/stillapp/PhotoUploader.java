package uk.org.potentialdifference.stillapp;

import android.content.Context;
import android.content.Intent;
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

public class PhotoUploader {

    private String TAG = "PhotoUploader";
    private static final String BASE_SERVER_URL = "http://192.168.0.6:8080/";
    private static final String FS_KEY = "stillappkey579xtz";

    public void uploadBytes(Context context, String tag, byte[] imageData) {
        Log.d(TAG, "uploadBytes called");
        String imageName = String.format("%s_%d.jpg", tag, System.currentTimeMillis());
        UserIdentifier uid = new UserIdentifier(context);
        String imageDir = uid.getIdentifier();

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


    }
}
