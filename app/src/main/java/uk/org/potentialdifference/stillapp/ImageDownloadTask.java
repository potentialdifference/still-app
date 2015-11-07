package uk.org.potentialdifference.stillapp;

import android.content.Context;
import android.util.Log;

import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;

import retrofit.Call;
import uk.org.potentialdifference.stillapp.webservice.StillAppService;

/**
 * Created by russell on 07/11/2015.
 */
public class ImageDownloadTask extends BaseImageServiceTask<String>{

    private static String TAG = "ImageDownloadTask";
    private final Context context;
    private ImageDownloadDelegate delegate;
    private byte[] imageBytes;

    public ImageDownloadTask(Context context, ImageDownloadDelegate delegate){

        this.context = context;
        this.delegate = delegate;
    }

    @Override
    protected Void doInBackground(String... params) {
        boolean safe = isConnectedToSafeWifi(context);
        if (!safe) {
            Log.d(TAG, "not connected to safe wifi network");
            return null;
        }
        StillAppService stillAppService = getService(context);
        String url = params[0];
        String path = url;
        if(url.contains("public/")){
            path = url.substring(7);
        }
        Call<ResponseBody> getImageCall = stillAppService.getPublicFile("WP_20150531_013.jpg");
        Response response = null;
        try {
            response = getImageCall.execute().raw();
            imageBytes = response.body().bytes();
        } catch (IOException e) {
            Log.e(TAG, "Error downloading file", e);
        }

        return null;

    }

    @Override
    protected void onPostExecute(Void v) {
        super.onPostExecute(null);
        if (delegate != null) {
            delegate.imageDownloadComplete(imageBytes);
        }
    }
}
