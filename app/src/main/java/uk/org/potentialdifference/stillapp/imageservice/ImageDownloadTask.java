package uk.org.potentialdifference.stillapp.imageservice;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import retrofit.Call;
import uk.org.potentialdifference.stillapp.R;
import uk.org.potentialdifference.stillapp.imageservice.webservice.StillAppService;

/**
 * Created by russell on 07/11/2015.
 */
public class ImageDownloadTask extends BaseImageServiceTask<String>{

    private static String TAG = "ImageDownloadTask";
    private final Context context;
    private ImageDownloadDelegate delegate;
    private Bitmap image;

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
        String url = String.format("https://%s:%s%s",context.getString(R.string.still_server_hostname), context.getString(R.string.still_server_https_port), params[0]);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", context.getString(R.string.still_server_private_auth_header))
                .build();

        Response response = null;
        try {
            response = getSslClient(context).newCall(request).execute();
            byte[] imageBytes = response.body().bytes();
            image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } catch (IOException e) {
            Log.e(TAG, "Error downloading file", e);
        }




        return null;

    }

    @Override
    protected void onPostExecute(Void v) {
        super.onPostExecute(null);
        if (delegate != null) {
            delegate.imageDownloadComplete(image);
        }
    }

}
