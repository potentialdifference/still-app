package uk.org.potentialdifference.stillapp;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import uk.org.potentialdifference.stillapp.nodefs.NodeFSResponse;
import uk.org.potentialdifference.stillapp.nodefs.NodeFSService;
import uk.org.potentialdifference.stillapp.webservice.StillAppResponse;
import uk.org.potentialdifference.stillapp.webservice.StillAppService;

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
                Call<StillAppResponse> uploadImageCall = stillAppService.uploadPrivateFile(FS_KEY, uid.getIdentifier(), name, body);

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

