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

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;

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

/**
 * Created by henry on 31/10/15.
 */
public class ImageUploadTask extends AsyncTask<UploadJob, Void, Void> {

    static String TAG = "ImageUploadTask";
    ImageUploadDelegate delegate;
    Context context;
    private static final String SERVER_PROTOCOL = "https";
    private static final String SERVER_HOSTNAME = "192.168.0.6";
    private static final String SERVER_PORT = "8080";


    private static final String FS_KEY = "stillappkey579xtz";
    private static final String[] SAFE_NETWORK_SSIDS = {"roomie", "still"};

    public ImageUploadTask(Context context, ImageUploadDelegate delegate) {
        this.context = context;
        this.delegate = delegate;
    }

    @Override
    protected Void doInBackground(UploadJob... params) {

        Log.d(TAG, "uploadBytes called");
        String wifiId = getWifiSSID();
        boolean safe = false;
        for(String id : SAFE_NETWORK_SSIDS){
            if(id.equals(wifiId)){
                safe=true;
                continue;
            }
        }
        if(!safe){
            Log.d(TAG, "not connected to safe wifi network");
            return null;
        }

        UserIdentifier uid = new UserIdentifier(this.context);
        String imageDir = uid.getIdentifier();

        Retrofit retrofit = new Retrofit.Builder().baseUrl(String.format("%s://%s:%s", SERVER_PROTOCOL, SERVER_HOSTNAME, SERVER_PORT))
                .addConverterFactory(GsonConverterFactory.create())
                .client(getSslClient(context))
                .build();
        NodeFSService nodeFsService = retrofit.create(NodeFSService.class);

        Call<NodeFSResponse> createDirectoryCall = nodeFsService.createDirectory(FS_KEY, imageDir);
        try {
            Response createDirectoryResponse = createDirectoryCall.execute().raw();
            Log.i(TAG, "response from create directory: " + createDirectoryResponse.toString());

            for (UploadJob job:params) {
                if (job == null || job.getData() == null) {
                    //nothing to do!
                    continue;
                }
                byte[] imageData = job.getData();

                String name = job.getName() == null ? "" : job.getName();
                String imageName = String.format("%s_%d.jpg", name, System.currentTimeMillis());
                Call<NodeFSResponse> createFileCall = nodeFsService.createFile(FS_KEY, imageDir, imageName);
                RequestBody body = RequestBody.create(MediaType.parse("image/jpeg"), imageData);
                Call<NodeFSResponse> saveCall = nodeFsService.saveFileContents(FS_KEY, imageDir, imageName, body);

                Response createFileResponse = createFileCall.execute().raw();
                Log.i(TAG, "response from create file: " + createFileResponse.toString());
                Response saveResponse = saveCall.execute().raw();
                Log.i(TAG, "response from save: " + saveResponse.toString());
            }
        } catch(Exception e){
            //handle
            Log.e(TAG, "Error uploading file", e);
        }

        return null;
    }

    private static OkHttpClient getSslClient(Context context){

        OkHttpClient client = new OkHttpClient();

        try{
            //load the certificate containing our trusted CAs
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream certInputStream = context.getResources().openRawResource(R.raw.server_cert);
            Certificate ca;
            try {
                ca = cf.generateCertificate(certInputStream);
            }finally{
                certInputStream.close();
            }

            //create a trust manager that trusts the CAs in our keystore
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContext sslContext  = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            client.setSslSocketFactory(sslContext.getSocketFactory());
            //client.setHostnameVerifier(new AllowAllHostnameVerifier());
            client.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    if(hostname.equals(SERVER_HOSTNAME)){
                        return true;
                    }
                    else {
                        return false;
                    }
                }
            });


        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException  | CertificateException | IOException e) {
            e.printStackTrace();
        }
        return client;

    }

    @Override
    protected void onPostExecute(Void v) {
        super.onPostExecute(null);
        if (delegate != null) {
            delegate.imageUploadComplete();
        }
    }

    private String getWifiSSID(){
            String ssid = "";
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !(connectionInfo.getSSID().equals(""))) {
                ssid = connectionInfo.getSSID();

            }
            if (ssid.startsWith("\"") && ssid.endsWith("\"")){
                ssid = ssid.substring(1, ssid.length()-1);
            }
                return ssid;
            }
    }