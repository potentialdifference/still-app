package uk.org.potentialdifference.stillapp;

import android.content.Context;
import android.os.AsyncTask;
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
 * Created by henry on 31/10/15.
 */
public class ImageUploadTask extends AsyncTask<byte[], Void, Void> {

    static String TAG = "ImageUploadTask";
    ImageUploadDelegate delegate;
    Context context;
    private static final String BASE_SERVER_URL = "http://192.168.0.6:8080/";
    private static final String FS_KEY = "stillappkey579xtz";

    public ImageUploadTask(Context context, ImageUploadDelegate delegate) {
        this.context = context;
        this.delegate = delegate;
    }

    @Override
    protected Void doInBackground(byte[]... params) {
        Log.d(TAG, "uploadBytes called");
        byte[] imageData = params[0];
        String imageName = String.format("%d.jpg", System.currentTimeMillis());
        UserIdentifier uid = new UserIdentifier(this.context);
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

        Log.i("ImageUploadService", "Uploading " + imageName);
        return null;
    }
    @Override
    protected void onPostExecute(Void v) {
        super.onPostExecute(null);
        if (delegate != null) {
            delegate.imageUploadComplete();
        }
    }
}
