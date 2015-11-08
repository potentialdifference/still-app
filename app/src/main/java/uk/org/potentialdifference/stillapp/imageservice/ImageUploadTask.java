package uk.org.potentialdifference.stillapp.imageservice;

import android.content.Context;
import android.util.Log;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;


import retrofit.Call;
import uk.org.potentialdifference.stillapp.R;
import uk.org.potentialdifference.stillapp.UserIdentifier;
import uk.org.potentialdifference.stillapp.imageservice.webservice.StillAppResponse;
import uk.org.potentialdifference.stillapp.imageservice.webservice.StillAppService;

/**
 * Created by henry on 31/10/15.
 */
public class ImageUploadTask extends BaseImageServiceTask<UploadJob> {

    private static String TAG = "ImageUploadTask";
    private ImageUploadDelegate delegate;
    private Context context;

    public ImageUploadTask(Context context, ImageUploadDelegate delegate) {
        this.context = context;
        this.delegate = delegate;
    }

    @Override
    protected Void doInBackground(UploadJob... params) {

        Log.d(TAG, "uploadBytes called");
        boolean safe = isConnectedToSafeWifi(context);
        if (!safe) {
            Log.d(TAG, "not connected to safe wifi network");
            return null;
        }

        UserIdentifier uid = new UserIdentifier(context);

        StillAppService stillAppService = getService(context);


        try {

            for (UploadJob job : params) {
                if (job == null || job.getData() == null) {
                    //nothing to do!
                    continue;
                }
                byte[] imageData = job.getData();

                String name = job.getName() == null ? "" : job.getName();

                RequestBody body = RequestBody.create(MediaType.parse("multipart/form-data"), imageData);
                Call<StillAppResponse> uploadImageCall = stillAppService.uploadPrivateFile(context.getString(R.string.still_server_auth_header), uid.getIdentifier(), name, body);

                Response uploadImageResponse = uploadImageCall.execute().raw();
                Log.i(TAG, "response from upload image: " + uploadImageResponse.toString());
            }


        } catch (Exception e) {
            //handle
            Log.e(TAG, "Error uploading file", e);
        }

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

